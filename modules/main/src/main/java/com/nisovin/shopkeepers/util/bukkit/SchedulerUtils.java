package com.nisovin.shopkeepers.util.bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Scheduler related utilities.
 */
public final class SchedulerUtils {

	/**
	 * Creates an {@link Executor} that executes tasks on the server's main thread using
	 * {@link #runOnMainThreadOrOmit(Location, Runnable)}.
	 * <p>
	 * If the thread registering the task is already the server's main thread, the task is run
	 * immediately. Otherwise, it is scheduled using the {@link BukkitScheduler}. If the plugin is
	 * not enabled at the time of task registration, the task is omitted.
	 *
	 * @return the executor
	 */
	public static WrappedExecutor createSyncExecutor() {
		return (location, runnable) -> runOnMainThreadOrOmit(location, runnable);
	}

	/**
	 * Creates an {@link Executor} that executes tasks using
	 * {@link #runAsyncTaskOrOmit(Runnable)}.
	 *
	 * @return the executor
	 */
	public static Executor createAsyncExecutor() {
		return (runnable) -> runAsyncTaskOrOmit(runnable);
	}

	public static int getActiveAsyncTasks(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		return getFoliaLib().getScheduler().getAllTasks().size();
	}

	private static void validatePluginTask(Runnable task) {
		Validate.notNull(task, "task is null");
	}

	/**
	 * Checks if the current thread is the server's main thread.
	 *
	 * @return <code>true</code> if currently running on the main thread
	 */
	public static boolean isMainThread(Location location) {
		return getFoliaLib().getScheduler().isOwnedByCurrentRegion(location);
	}

	public static boolean isGlobalThread() {
		return getFoliaLib().getScheduler().isGlobalTickThread();
	}

	/**
	 * Schedules the given task to be run on the primary thread if required.
	 * <p>
	 * If the current thread is already the primary thread, the task will be run immediately.
	 * Otherwise, it attempts to schedule the task to run on the server's primary thread. However,
	 * if the plugin is disabled, the task won't be scheduled.
	 *
	 * @param task
	 *            the task, not <code>null</code>
	 * @return <code>true</code> if the task was run or successfully scheduled to be run,
	 *         <code>false</code> otherwise
	 */
	public static boolean runOnMainThreadOrOmit(Location location, Runnable task) {
		validatePluginTask(task);
		if (isMainThread(location)) {
			task.run();
			return true;
		} else {
			return (runTaskOrOmit(location, task) != null);
		}
	}

	public static @Nullable WrappedTask runTaskOrOmit(Entity entity, Runnable task) {
		return runTaskLaterOrOmit(entity.getLocation(), task, 0L);
	}

	public static @Nullable WrappedTask runTaskOrOmit(Location location, Runnable task) {
		return runTaskLaterOrOmit(location, task, 0L);
	}

	public static @Nullable WrappedTask runTaskLaterOrOmit(
			Entity entity,
			Runnable task,
			long delay) {
		return runTaskLaterOrOmit(entity.getLocation(), task, delay);
	}

	public static @Nullable WrappedTask runTaskLaterOrOmit(
			Location location,
			Runnable task,
			long delay) {
		validatePluginTask(task);
		// Tasks can only be registered while enabled:
		try {
			return getFoliaLib().getScheduler().runAtLocationLater(location, task, delay);
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
		return null;
	}

	public static void runTaskTimerOrOmit(Entity entity, Consumer<WrappedTask> task, long delay, long period) {
		runTaskTimerOrOmit(entity.getLocation(), task, delay, period);
	}

	public static void runTaskTimerOrOmit(
			Location location,
			Consumer<@NonNull WrappedTask> task,
			long delay,
			long period
	) {
		Validate.notNull(task, "task is null");
		// Tasks can only be registered while enabled:
		try {
			getFoliaLib().getScheduler().runAtLocationTimer(location, task, delay, period);
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
	}

	public static @Nullable WrappedTask runAsyncTaskOrOmit(Runnable task) {
		return runAsyncTaskLaterOrOmit(task, 0L);
	}

	public static @Nullable WrappedTask runAsyncTaskLaterOrOmit(
			Runnable task,
			long delay
	) {
		validatePluginTask(task);
		// Tasks can only be registered while enabled:
		try {
			return getFoliaLib().getScheduler().runLaterAsync(task, delay);
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
		return null;
	}

	public static @Nullable WrappedTask runAsyncTaskTimerOrOmit(Runnable task, long delay, long period) {
		validatePluginTask(task);
		// Tasks can only be registered while enabled:
		try {
			return getFoliaLib().getScheduler().runTimerAsync(task, delay, period);
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
		return null;
	}



	public static @NonNull CompletableFuture<@NonNull Void> runTaskGloballyOrOmit(Runnable task) {
		validatePluginTask(task);
		// Tasks can only be registered while enabled:
		try {
			@SuppressWarnings("nullness")
			CompletableFuture<Void> result = getFoliaLib().getScheduler().runNextTick(t -> task.run());
			return result;
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
		return CompletableFuture.completedFuture(null);
	}

	public static @NonNull CompletableFuture<@NonNull Void> runTaskLaterGloballyOrOmit(Runnable task, long delay) {
		validatePluginTask(task);
		// Tasks can only be registered while enabled:
		try {
			@SuppressWarnings("nullness")
			CompletableFuture<Void> t = getFoliaLib().getScheduler().runLater(ta -> task.run(), delay);
			return t;
		} catch (IllegalPluginAccessException e) {
			// Couldn't register task: The plugin got disabled just now.
		}
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Awaits the completion of async tasks of the specified plugin.
	 * <p>
	 * If a logger is specified, it will be used to print informational messages suited to the
	 * context of this method being called during disabling of the plugin.
	 *
	 * @param plugin
	 *            the plugin
	 * @param asyncTasksTimeoutSeconds
	 *            the duration to wait for async tasks to finish in seconds (can be <code>0</code>)
	 * @param logger
	 *            the logger used for printing informational messages, can be <code>null</code>
	 * @return the number of remaining async tasks that are still running after waiting for the
	 *         specified duration
	 */
	public static int awaitAsyncTasksCompletion(
			Plugin plugin,
			int asyncTasksTimeoutSeconds,
			@Nullable Logger logger
	) {
		Validate.notNull(plugin, "plugin is null");
		Validate.isTrue(asyncTasksTimeoutSeconds >= 0, "asyncTasksTimeoutSeconds cannot be negative");

		int activeAsyncTasks = getActiveAsyncTasks(plugin);
		if (activeAsyncTasks > 0 && asyncTasksTimeoutSeconds > 0) {
			if (logger != null) {
				logger.info("Waiting up to " + asyncTasksTimeoutSeconds + " seconds for "
						+ activeAsyncTasks + " remaining async tasks to finish ...");
			}

			final long asyncTasksTimeoutMillis = TimeUnit.SECONDS.toMillis(asyncTasksTimeoutSeconds);
			final long waitStartNanos = System.nanoTime();
			long waitDurationMillis = 0L;
			do {
				// Periodically check again:
				try {
					Thread.sleep(25L);
				} catch (InterruptedException e) {
					// Ignore, but reset interrupt flag:
					Thread.currentThread().interrupt();
				}
				// Update the number of active async task before breaking from loop:
				activeAsyncTasks = getActiveAsyncTasks(plugin);

				// Update waiting duration and compare to timeout:
				waitDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStartNanos);
				if (waitDurationMillis > asyncTasksTimeoutMillis) {
					// Timeout reached, abort waiting..
					break;
				}
			} while (activeAsyncTasks > 0);

			if (waitDurationMillis > 1 && logger != null) {
				logger.info("Waited " + waitDurationMillis + " ms for async tasks to finish.");
			}
		}

		if (activeAsyncTasks > 0 && logger != null) {
			// Severe, since this can potentially result in data loss, depending on what the tasks
			// are doing:
			logger.severe("There are still " + activeAsyncTasks
					+ " remaining async tasks active! Disabling anyway now.");
		}
		return activeAsyncTasks;
	}


	private static FoliaLib getFoliaLib() {
		return SKShopkeepersPlugin.getInstance().getFoliaLib();
	}

	private SchedulerUtils() {
	}
}