package com.nisovin.shopkeepers.util.taskqueue;

import java.util.ArrayDeque;
import java.util.Queue;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link BukkitScheduler} task that processes a queue of work units.
 * <p>
 * A {@link TaskQueue} has two main characteristics: The rate at which the task executes and can
 * therefore process pending work units, and how many of these work units it processes per
 * execution. The chosen execution rate and the number of processed work units per execution are a
 * balance between the following considerations:
 * <ul>
 * <li>The number of work units processed per execution has to remain low enough to avoid
 * performance drops.
 * <li>More frequent execution rates have lower latencies for work units to be processed, balance
 * the load across more ticks, and can process more work units per time unit while still avoiding
 * performance drops. However, they are also associated with have a higher general performance
 * overhead (more processing done by the scheduler, more task invocations, the queue is checked more
 * often, etc.).
 * <li>Frequently canceling and restarting the task is also associated with a notable overhead,
 * which is why we keep the task running, even when there are no pending work units. A high
 * execution rate therefore produces more overhead in comparison to the actual useful work being
 * done when the queue of pending work units is empty most of the time.
 * <li>The queue should have a high enough throughput so that it can cope with the rate and peaks at
 * which new work units are typically produced, without causing disruptions due to other components
 * or users having to wait for these work units to be processed.
 * </ul>
 *
 * @param <T>
 *            the type of work units
 */
public abstract class TaskQueue<@NonNull T> implements TaskQueueStatistics {

	// Note: In comparison to a linked list, ArrayDeque is more cache friendly, requires one less
	// reference lookup to retrieve elements, and produces less objects and therefore garbage
	// collection overhead.
	// However, in order to more efficiently remove work units from the queue again (ArrayDeque has
	// to move elements when removing an element from the middle), an idea has been to set the
	// element to null and then ignore null elements when processing work units. Since ArrayDeque
	// doesn't support null elements, this would either require our own implementation of
	// ArrayDeque, or we would have to store separate 'Entry' objects which then hold the references
	// to the work units. To not wastefully create and throw away these Entry objects, a second
	// ArrayDeque could be used as cache for unused Entry objects.
	// However, this would negate some of the benefits of using an ArrayDeque in the first place:
	// It's less cache friendly (the Entry objects require their own memory and may be distributed
	// across the memory), and adding, removing, and retrieving elements is slightly more costly.
	// This has not yet been benchmarked (TODO), but taking the following additional considerations
	// into account (specific to how we use this queue though), we simply keep using ArrayDeque as
	// it is for now:
	// - Accessing (and searching through) elements is cache friendly.
	// - Moving array elements is highly optimized.
	// - We expect the queue to remain relatively small in size, so moving elements is probably not
	// that costly.
	// - We expect actual removals to occur relatively infrequently (whereas checking if the queue
	// contains an element that needs to be removed may occur comparatively more often).

	private final Plugin plugin;
	private final int taskPeriodTicks;
	private final int workUnitsPerExecution;
	private final Queue<@NonNull T> pending = new ArrayDeque<>();
	private int maxPending = 0;
	private @Nullable WrappedTask task = null;

	/**
	 * Creates a new {@link TaskQueue}.
	 * 
	 * @param plugin
	 *            the plugin, not <code>null</code>
	 * @param taskPeriodTicks
	 *            the period ticks of the task processing work units
	 * @param workUnitsPerExecution
	 *            the number of work units that are processed per task execution
	 */
	public TaskQueue(Plugin plugin, int taskPeriodTicks, int workUnitsPerExecution) {
		Validate.notNull(plugin, "plugin is null");
		Validate.isTrue(taskPeriodTicks > 0, "taskPeriodTicks has to be positive");
		Validate.isTrue(workUnitsPerExecution > 0, "workUnitsPerExecution has to be positive");
		this.plugin = plugin;
		this.taskPeriodTicks = taskPeriodTicks;
		this.workUnitsPerExecution = workUnitsPerExecution;
	}

	/**
	 * This has to be called during plugin startup.
	 * <p>
	 * This starts the task that processes pending work units.
	 */
	public void start() {
		this.startTask();
	}

	/**
	 * This has to be called on plugin shutdown.
	 * <p>
	 * This stops the task and clears the queue of pending work units without processing them.
	 */
	public void shutdown() {
		// Invoke removal callbacks for all pending work units:
		pending.forEach(this::onRemoval);
		pending.clear();
		this.stopTask();
		maxPending = 0;
	}

	// WORK UNITS

	/**
	 * Adds a new work unit to the queue.
	 * 
	 * @param workUnit
	 *            the work unit, not <code>null</code>
	 */
	public void add(@NonNull T workUnit) {
		assert workUnit != null; // Also checked by queue already
		pending.add(workUnit);

		// Update max pending:
		int size = pending.size();
		if (size > maxPending) {
			maxPending = size;
		}

		// Callback for subclasses:
		this.onAdded(workUnit);
	}

	/**
	 * This callback is invoked whenever a new work unity has been added to the queue.
	 * 
	 * @param workUnit
	 *            the work unit, not <code>null</code>
	 */
	protected void onAdded(@NonNull T workUnit) {
	}

	/**
	 * Removes the given work unit from the queue if the queue contains it.
	 * 
	 * @param workUnit
	 *            the work unit, not <code>null</code>
	 */
	public void remove(@NonNull T workUnit) {
		assert workUnit != null; // Also checked by queue already
		if (pending.remove(workUnit)) {
			// Callback for subclasses:
			this.onRemoval(workUnit);
		}
	}

	/**
	 * This callback is invoked whenever a work unity is or has been removed from the queue without
	 * being processed.
	 * 
	 * @param workUnit
	 *            the work unit, not <code>null</code>
	 */
	protected void onRemoval(@NonNull T workUnit) {
	}

	// STATISTICS

	@Override
	public int getPendingCount() {
		return pending.size();
	}

	@Override
	public int getMaxPendingCount() {
		return maxPending;
	}

	// TASK

	private void startTask() {
		// Skip if the task is already running:
		if (task != null) {
			return;
		}

		// Start new task:
		task = SchedulerUtils.runAsyncTaskTimerOrOmit(this.createTask(), 1, taskPeriodTicks);
	}

	private void stopTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	/**
	 * Creates the task object that processes pending work units.
	 * <p>
	 * This method can be overridden to produce a named task, which is easier to identify in
	 * timings. However, the returned task has to invoke the task produced by this class (i.e. its
	 * super class).
	 * 
	 * @return the task
	 */
	protected Runnable createTask() {
		return this::execute;
	}

	private void execute() {
		// Skip the whole loop if there are no pending work units:
		Queue<@NonNull T> queue = pending;
		if (queue.isEmpty()) {
			return;
		}

		int localWorkUnitsPerExecution = workUnitsPerExecution;
		for (int i = 0; i < localWorkUnitsPerExecution; ++i) {
			@Nullable T workUnit = queue.poll();
			if (workUnit == null) {
				// The queue is empty:
				return;
			}

			// Process the work unit:
			this.process(workUnit);
		}
	}

	/**
	 * Process the given work unit.
	 * 
	 * @param workUnit
	 *            the work unit, not <code>null</code>
	 */
	protected abstract void process(@NonNull T workUnit);
}
