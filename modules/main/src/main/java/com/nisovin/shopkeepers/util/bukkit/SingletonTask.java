package com.nisovin.shopkeepers.util.bukkit;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Represents a task that is triggered from the server's main thread and of which only one execution
 * can take place simultaneously.
 * <p>
 * The task is usually run asynchronously, but it may also run on the server's main thread (for
 * example when forcing an immediate execution, such as during shutdown).
 * <p>
 * While the task is already running asynchronously, any subsequent requests to execute the task are
 * deferred until after the task finishes its current execution. The next execution of the task is
 * then responsible to perform the work that has accumulated in the meantime.
 * <p>
 * If not noted otherwise, all methods are meant to be called from the main thread. The task cannot
 * be interacted with from within executions, such as attempting to trigger another execution.
 * <p>
 * A possible use case is an IO task that writes to a specific file, but of which only one execution
 * is allowed to take place at the same time.
 */
public abstract class SingletonTask {

	private enum State {
		/**
		 * Default state when no execution is in progress.
		 */
		NOT_RUNNING,
		/**
		 * An execution is being prepared.
		 */
		PREPARING,
		/**
		 * An execution has been initiated and is pending execution.
		 */
		PENDING,
		/**
		 * An execution is in progress.
		 * <p>
		 * For an asynchronous execution this state may get set asynchronously. In this case,
		 * checking for it has to be guarded by the execution lock.
		 */
		EXECUTING,
		/**
		 * The synchronous callback is being executed.
		 */
		SYNC_CALLBACK;
	}

	private final Plugin plugin;
	// The lock used to coordinate the main thread with threads executing the task asynchronously.
	// Note: This lock is not acquired for synchronous executions.
	// Note: This lock is not provided to the outside of this class, because it is not suited for
	// the coordination with other tasks. Each task execution completes with the execution of its
	// synchronous callback. So instead, other tasks can use #awaitExecutions() to wait for any
	// current and pending executions to complete.
	private final Object executionLock = new Object();

	private State state = State.NOT_RUNNING;
	// The Bukkit task asynchronously executing this task. Only relevant for async executions.
	private @Nullable BukkitTask asyncTask = null;
	// The (internal) callbacks of the current execution:
	// Run immediately, possibly asynchronously:
	private @Nullable Runnable internalCallback = null;
	private @Nullable Runnable internalSyncCallback = null;
	// Flag that indicates whether the task should execute again after its current execution
	// finishes:
	private boolean runAgain = false;
	// Whether the pending next execution shall be executed synchronously:
	private boolean runAgainSync = false;

	// Information and statistics about the last execution:
	// These values get incrementally replaced during the next execution. So they are only valid
	// during the period after the last execution (including inside the user callback), and the
	// beginning of the next execution.
	private boolean asyncExecution;
	private long startTimeNanos;
	private long preparationEndTimeNanos;
	private long preparationDurationMillis;
	private long lockAcquireDurationMillis;
	private long executionDelayMillis;
	private long executionDurationMillis;
	private long totalDurationMillis;

	public SingletonTask(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	/**
	 * Checks if an execution is currently in progress.
	 * 
	 * @return <code>true</code> if an execution is in progress
	 */
	public final boolean isRunning() {
		assert Bukkit.isPrimaryThread();
		return (state != State.NOT_RUNNING);
	}

	/**
	 * Checks whether an execution is in progress that is currently being post-processed.
	 * 
	 * @return <code>true</code> if there is an execution that is currently being post-processed
	 */
	public final boolean isPostProcessing() {
		assert Bukkit.isPrimaryThread();
		return (state == State.SYNC_CALLBACK);
	}

	/**
	 * Checks whether there is an execution pending, which will start once the current execution
	 * completes.
	 * 
	 * @return <code>true</code> if there is an execution pending
	 */
	public final boolean isExecutionPending() {
		return runAgain;
	}

	private boolean isWithinSyncExecution() {
		assert Bukkit.isPrimaryThread();
		return (state == State.PREPARING)
				|| (asyncTask == null && state == State.EXECUTING)
				|| (state == State.SYNC_CALLBACK);
	}

	private void validateMainThreadAndNotWithinExecution() {
		Validate.State.isTrue(Bukkit.isPrimaryThread(),
				"This operation has to be called from the main thread!");
		if (this.isWithinSyncExecution()) {
			Validate.State.error(
					"This operation is not allowed to be called from within the task's execution!"
			);
		}
		// Note: If there is an async execution in progress, we may observe state PENDING or state
		// EXECUTING. Calling this method from the main thread is allowed in this case.
	}

	/**
	 * Performs shutdown logic for this task.
	 * <p>
	 * This will wait for any currently active execution and any pending requested execution to
	 * finish.
	 * <p>
	 * This method cannot be called from within an execution, i.e. from within {@link #prepare()},
	 * {@link #execute()}, or {@link #syncCallback()}.
	 */
	public final void shutdown() {
		this.validateMainThreadAndNotWithinExecution();
		// Wait for any current async execution to finish. This may also trigger and wait for any
		// other pending execution.
		this.awaitExecutions();
	}

	/**
	 * Requests an (asynchronous) execution of the task.
	 * <p>
	 * If an asynchronous execution is already in progress, the task will be executed again once the
	 * current execution completes.
	 * <p>
	 * During plugin disable (after the plugin has already been marked as {@link Plugin#isEnabled()
	 * disabled}), asynchronous executions are not supported. Any execution requests will then take
	 * place immediately.
	 * <p>
	 * This method cannot be called from within an execution, i.e. from within {@link #prepare()},
	 * {@link #execute()}, or {@link #syncCallback()}.
	 */
	public final void run() {
		if (plugin.isEnabled()) {
			this.runTask(true);
		} else {
			// During plugin disable, all executions take place immediately:
			this.runTask(false);
		}
	}

	/**
	 * Requests a synchronous execution of the task.
	 * <p>
	 * If an asynchronous execution is already in progress, this method waits for it to finish
	 * before performing the requested execution. This method blocks until the requested execution
	 * completes.
	 * <p>
	 * This method cannot be called from within an execution, i.e. from within {@link #prepare()},
	 * {@link #execute()}, or {@link #syncCallback()}.
	 */
	public final void runImmediately() {
		this.runTask(false);
	}

	/**
	 * Waits (blocking!) for any current and pending executions to complete.
	 * <p>
	 * This method cannot be called from within an execution, i.e. from within {@link #prepare()},
	 * {@link #execute()}, or {@link #syncCallback()}.
	 * <p>
	 * Implementation notes:<br>
	 * If an async execution has already been scheduled, but not yet started, the async execution is
	 * cancelled and then run synchronously. This is because the Bukkit scheduler starts async tasks
	 * from within the main thread, which we are blocking currently. Also note: We do not skip the
	 * execution in this case in order to ensure that progress is made. Otherwise, frequent requests
	 * to start the task could continually abort the task so that it is never able to complete.
	 * <p>
	 * Otherwise, if the async execution has already started, this will wait for it to finish. The
	 * sync callback will then need to be manually run by us in order to complete the previous
	 * execution.
	 * <p>
	 * If there is another execution pending, the sync callback of the previous execution will
	 * trigger another sync execution. This method will also wait for that second execution to
	 * finish.
	 */
	public final void awaitExecutions() {
		this.validateMainThreadAndNotWithinExecution();
		if (asyncTask == null) {
			// There is no async execution in progress.
			// Assertion: Consequently, there is also no pending request to run the task again.
			assert !runAgain;
			return;
		}

		// If an async execution is currently executing, acquiring the lock will wait for it to
		// finish.
		// If the async execution has not taken the lock yet, then it is either still pending to be
		// started by the Scheduler on the main thread (which we are currently blocking), or it has
		// already been started but not acquired the lock yet. In the latter case there may or may
		// not already exist a worker thread for it, and it may or may not have checked its
		// cancellation state yet.
		synchronized (executionLock) {
			if (state == State.PENDING) {
				// The task has not yet started its execution / taken the lock yet.
				// We cancel it and manually run it here. Note: We cancel this within the locked
				// section, because the cancellation is only required (and effective) if the task
				// has not started executing yet. Reliably checking for this state requires the
				// lock.
				// However, a worker thread may already exist for it and the task may already have
				// checked its cancellation state. This is why the async task has to check its
				// cancellation state again within the locked section in order to reliably detect
				// it.
				assert asyncTask != null;
				asyncTask.cancel();
			} else {
				assert state == State.EXECUTING;
				// Because we were able to acquire the lock, and we are no longer in state PENDING,
				// the task must already have finished its execution and async callback. However,
				// its sync callback is still pending to be run. We will manually run it here.
			}
		}
		// Else: state is PENDING and execution got cancelled:
		boolean hasExecuted = (state == State.EXECUTING);

		// If the execution got cancelled, we manually run it here.
		// Any other pending execution will take place immediately:
		runAgainSync = true;
		if (!hasExecuted) {
			// We reset the asyncTask variable here to indicate that we are synchronously executing
			// (required for the validation of runTask(boolean)). The asyncExecution variable still
			// reflects the original state.
			asyncTask = null;
			this.executeTask(null);
			// Assertion: This will also invoke the immediate callback, which invokes the sync
			// callback, which executes immediately as well because we are already in the main
			// thread here.
			assert internalSyncCallback == null;
		} else {
			// Else: The execution took place asynchronously, so the sync callback must still be
			// pending.
			Unsafe.assertNonNull(internalSyncCallback).run();
			// This will reset the execution state, so that the pending task for the sync callback
			// can detect that it has already been run here.
		}
		// Note: The sync callback of the previous execution will also trigger and immediately
		// execute any pending execution.
		runAgainSync = false; // Reset

		// At this point, any previously executing and pending executions have been completed, and
		// any state related to them has been reset.
	}

	private void runTask(boolean async) {
		this.validateMainThreadAndNotWithinExecution();
		// During plugin disable, only synchronous executions are allowed, because the Bukkit
		// Scheduler does no longer accept new tasks. This has to be ensured by the caller (i.e.
		// other methods of this class).
		Validate.State.isTrue(!async || plugin.isEnabled(),
				"Cannot execute asynchronously during or after plugin disable!");

		// Is another async execution already in progress?
		if (asyncTask != null) {
			// Trigger another execution once the current execution finishes:
			runAgain = true;
			if (async) {
				// This execution will be triggered once the current async execution finishes.
				return;
			} else {
				// Wait for the current async execution to finish and then immediately execute
				// again:
				this.awaitExecutions();
				return;
			}
		} // Else: There is no other execution in progress currently.
		assert state == State.NOT_RUNNING;
		assert asyncTask == null;
		assert internalCallback == null;
		assert internalSyncCallback == null;
		assert !runAgain;
		// Other execution information and statistics get incrementally overwritten during the next
		// execution.

		// Keep track of information and statistics about this execution:
		asyncExecution = async;
		startTimeNanos = System.nanoTime();

		// User preparation:
		state = State.PREPARING;
		this.prepare();

		// Immediate callback (possibly run asynchronously):
		internalCallback = () -> {
			assert state == State.EXECUTING;
			// Ensure that we continue on the main thread for the synchronous callback:
			// This gets omitted here if the plugin got disabled during an asynchronous execution.
			// In this case the sync callback is manually run when the shutdown is handled on the
			// main thread.
			// Assertion: The syncCallback is still the one associated with this callback's
			// execution. Preparation of any subsequent execution either waits for the previous
			// execution and this callback to complete, or the async task for this execution and
			// callback are cancelled and invoked manually.
			// Also note: If this callback is run from the main thread, the sync callback is run
			// immediately.
			SchedulerUtils.runOnMainThreadOrOmit(
					plugin,
					Unsafe.assertNonNull(internalSyncCallback)
			);
		};

		// Sync callback: Gets run on the main thread after the execution has completed. This is run
		// prior to any subsequent execution and is responsible to reset any state associated with
		// this execution.
		// Note: This needs to be a new runnable (cannot be a lambda), in order to be able to
		// reliable use the object's identity to identify whether the callback has already been run.
		internalSyncCallback = this.createInternalSyncCallbackTask();

		preparationEndTimeNanos = System.nanoTime();
		preparationDurationMillis = TimeUnit.NANOSECONDS.toMillis(
				preparationEndTimeNanos - startTimeNanos
		);
		state = State.PENDING;

		if (async) {
			// Asynchronous execution:
			// If the async task gets cancelled later, a new execution (and new asyncTask) may get
			// prepared before this task is actually run. We therefore cannot retrieve the executing
			// task from this class' asyncTask variable. Instead we capture the task here.
			// TODO Measure the time it takes to schedule the async task as part of the preparation?
			// Tricky, since in general there is no guarantee about the order in which the task and
			// any following instructions are executed.
			this.asyncTask = this.createInternalAsyncTask().runTaskAsynchronously();
		} else {
			// Synchronous execution:
			this.executeTask(null);
		}
	}

	/**
	 * The type of the task determines how it is shown and merged with other tasks in timings
	 * reports. This task class is only exposed so that subclasses can construct task instances of a
	 * custom distinct type that derives from this type. The behavior of this task cannot be changed
	 * by subclasses.
	 */
	public abstract class InternalAsyncTask implements Runnable {

		private @Nullable BukkitTask task; // Captured Bukkit task

		protected InternalAsyncTask() {
		}

		private BukkitTask runTaskAsynchronously() {
			this.task = Bukkit.getScheduler().runTaskAsynchronously(plugin, this);
			return task;
		}

		@Override
		public final void run() {
			executeTask(task);
		}
	}

	/**
	 * Creates an {@link InternalAsyncTask}.
	 * 
	 * @return the task instance, not <code>null</code>
	 */
	protected abstract InternalAsyncTask createInternalAsyncTask();

	/**
	 * The type of the task determines how it is shown and merged with other tasks in timings
	 * reports. This task class is only exposed so that subclasses can construct task instances of a
	 * custom distinct type that derives from this type. The behavior of this task cannot be changed
	 * by subclasses.
	 */
	public class InternalSyncCallbackTask implements Runnable {
		@Override
		public final void run() {
			// We omit running this callback if it has already been run (for example when awaiting
			// the execution to finish).
			// We check the identity of the callback here (instead of checking for null), because if
			// the callback has already been run manually, another execution (and its sync callback)
			// might already have been prepared while the task for this callback (started by the
			// async callback) was still pending execution.
			if (internalSyncCallback != this) {
				return;
			}

			// Reset state related to the previous execution.
			// It is important that these get reset in the sync execution context so that we can
			// reason about them within the main thread without requiring synchronization.
			internalSyncCallback = null;
			// Note: The (async) callback has already been run at this point.
			internalCallback = null;
			asyncTask = null;

			assert state == State.EXECUTING;
			state = State.SYNC_CALLBACK;

			// Synchronous user callback:
			syncCallback();

			// This execution has been completed:
			state = State.NOT_RUNNING;

			// Trigger another execution if there is a pending execution request:
			if (runAgain) {
				runAgain = false;
				if (runAgainSync) {
					SingletonTask.this.runImmediately();
				} else {
					SingletonTask.this.run();
				}
			}
			assert !runAgain;
		}
	}

	/**
	 * Creates an {@link InternalSyncCallbackTask}.
	 * 
	 * @return the task instance, not <code>null</code>
	 */
	protected abstract InternalSyncCallbackTask createInternalSyncCallbackTask();

	// Potentially run asynchronously.
	// asyncTask: The async task executing this method. Null for sync executions.
	// If the async task got cancelled and another execution has already been started, this may not
	// match the current value of this class' asyncTask variable.
	private void executeTask(@Nullable BukkitTask asyncTask) {
		if (asyncTask != null) {
			// Asynchronous execution:
			// Requires the lock for coordination with the main thread, and might have been
			// cancelled.
			final long lockAcquireStartTimeNanos = System.nanoTime();
			synchronized (executionLock) {
				final long localLockAcquireDurationMillis = TimeUnit.NANOSECONDS.toMillis(
						System.nanoTime() - lockAcquireStartTimeNanos
				);

				// If the async task has been cancelled, we skip the execution.
				// In this case, the execution and callbacks are run on the main thread (when this
				// execution got cancelled).
				// The async task has already checked the cancellation state. However, there is a
				// race condition between checking the state and the actual execution. Checking it
				// here again within the locked section avoids this race condition.
				if (asyncTask.isCancelled()) {
					return;
				}

				// We only set the timing information if this task has not been cancelled.
				// Otherwise, another task might already be in progress.
				lockAcquireDurationMillis = localLockAcquireDurationMillis;

				// Actual execution:
				this.doExecuteTask();
			}
		} else {
			// Synchronous execution:
			// Does not require the lock.
			lockAcquireDurationMillis = 0L;

			// Actual execution:
			this.doExecuteTask();
		}
	}

	// Potentially run asynchronously.
	private void doExecuteTask() {
		// Execution has started:
		state = State.EXECUTING;
		final long executionStartTimeNanos = System.nanoTime();
		executionDelayMillis = TimeUnit.NANOSECONDS.toMillis(
				executionStartTimeNanos - preparationEndTimeNanos
		);

		// User execution:
		this.execute();

		// Immediate (potentially async) callback:
		Unsafe.assertNonNull(internalCallback).run();

		final long executionEndTimeNanos = System.nanoTime();
		executionDurationMillis = TimeUnit.NANOSECONDS.toMillis(
				executionEndTimeNanos - executionStartTimeNanos
		);
		totalDurationMillis = TimeUnit.NANOSECONDS.toMillis(
				executionEndTimeNanos - startTimeNanos
		);
	}

	// EXECUTION INFORMATION AND STATISTICS

	/**
	 * Checks whether the last execution has (initially) been asynchronous.
	 * <p>
	 * Note that if a request is made to immediately execute the task, any currently pending
	 * asynchronous execution may actually take place synchronously on the main thread but still be
	 * considered 'asynchronous' by this method.
	 * 
	 * @return <code>true</code> if asynchronous
	 */
	public final boolean isAsyncExecution() {
		return asyncExecution;
	}

	/**
	 * Gets the preparation duration of the previous execution.
	 * 
	 * @return the preparation duration in milliseconds
	 */
	public final long getPreparationDuration() {
		return preparationDurationMillis;
	}

	/**
	 * Gets the duration it took to acquire the execution lock.
	 * <p>
	 * The lock is only acquired for asynchronous executions. For synchronous executions this
	 * returns <code>0</code>.
	 * <p>
	 * This value might be useful for debugging purposes and should usually be relatively small
	 * (less than <code>1</code> millisecond).
	 * 
	 * @return the duration in milliseconds it took to acquire the execution lock
	 */
	public final long getLockAcquireDuration() {
		return lockAcquireDurationMillis;
	}

	/**
	 * Gets the delay between the task {@link #prepare() preparation} and the actual start of the
	 * previous execution.
	 * <p>
	 * Synchronous executions take place immediately after preparation. Consequently, this value
	 * will usually only be interesting for {@link #isAsyncExecution() asynchronous executions}.
	 * <p>
	 * For asynchronous executions this includes the time it took to acquire the
	 * {@link #getLockAcquireDuration() execution lock}, but which should usually be very small and
	 * therefore not noticeable in this statistic.
	 * 
	 * @return the execution delay in milliseconds
	 */
	public final long getExecutionDelay() {
		return executionDelayMillis;
	}

	/**
	 * Gets the duration of the previous execution.
	 * 
	 * @return the execution duration in milliseconds
	 */
	public final long getExecutionDuration() {
		return executionDurationMillis;
	}

	/**
	 * Gets the total duration of the previous execution, from preparation to execution completion,
	 * including all delays caused by the scheduling of any asynchronous task.
	 * <p>
	 * This does not include the execution delay and duration of the synchronous callback.
	 * 
	 * @return the total execution duration in milliseconds
	 */
	public final long getTotalDuration() {
		return totalDurationMillis;
	}

	/**
	 * Gets a one-line summary of the timing statistics of the last execution.
	 * <p>
	 * Detailed timing entries with a value of <code>0</code> are omitted.
	 * 
	 * @return the timing summary String
	 */
	public final String getExecutionTimingString() {
		StringBuilder sb = new StringBuilder();
		sb.append(totalDurationMillis).append(" ms");
		String details = this.getExecutionTimingDetailString();
		if (!details.isEmpty()) {
			sb.append(" (").append(details).append(")");
		}
		return sb.toString();
	}

	// Can be empty if there is no noteworthy detail information.
	private String getExecutionTimingDetailString() {
		StringBuilder sb = new StringBuilder();
		boolean firstEntry = true;

		if (preparationDurationMillis > 0) {
			firstEntry = false;
			sb.append("Preparation: ").append(preparationDurationMillis).append(" ms");
		}

		if (executionDelayMillis > 0) {
			if (!firstEntry) {
				sb.append(", ");
			}
			firstEntry = false;

			if (asyncExecution) {
				sb.append("Async execution delay: ");
			} else {
				sb.append("Sync execution delay: ");
			}
			sb.append(executionDelayMillis).append(" ms");

			// The lock acquire duration is part of the execution delay:
			if (lockAcquireDurationMillis > 0) {
				sb.append(" (Lock delay: ").append(lockAcquireDurationMillis).append(" ms)");
			}
		}

		if (executionDurationMillis > 0) {
			if (!firstEntry) {
				sb.append(", ");
			}
			firstEntry = false;

			if (asyncExecution) {
				sb.append("Async execution: ");
			} else {
				sb.append("Sync execution: ");
			}
			sb.append(executionDurationMillis).append(" ms");
		}
		return sb.toString();
	}

	/////

	/**
	 * This is run on the main thread to prepare a new task execution.
	 */
	protected abstract void prepare();

	/**
	 * The execution of the task.
	 * <p>
	 * This is potentially (but not necessarily) run asynchronously.
	 */
	protected abstract void execute();

	/**
	 * This is run on the main thread after the task has been executed.
	 */
	protected abstract void syncCallback();
}
