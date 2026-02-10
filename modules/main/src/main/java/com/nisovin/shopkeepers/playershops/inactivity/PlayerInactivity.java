package com.nisovin.shopkeepers.playershops.inactivity;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.bukkit.Ticks;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Handles the removal of shops that are owned by inactive players.
 */
public class PlayerInactivity {

	private final SKShopkeepersPlugin plugin;
	private final DeleteInactivePlayerShopsTask task;

	public PlayerInactivity(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.task = new DeleteInactivePlayerShopsTask();
	}

	public void onEnable() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return; // Feature is disabled

		// Delete inactive player shops, once shortly after plugin startup, and then periodically:
		task.start();
	}

	public void onDisable() {
		task.stop();
	}

	/**
	 * This task periodically triggers the detection and removal of shops that are owned by inactive
	 * players.
	 * <p>
	 * The task is also run shortly after being started.
	 * <p>
	 * Since we measure player inactivity in granularity of days, and since the checking for
	 * inactive players is relatively performance-intensive, we run this task very infrequently. It
	 * is also not required that this task runs exactly in the specified interval, which is
	 * unlikely, because server lag can noticeably influence the exact interval duration. The
	 * primary purpose of this task is to account for servers that keep running for very long
	 * durations.
	 */
	private final class DeleteInactivePlayerShopsTask implements Runnable {

		// ~4 hours (can be noticeably longer if the server lags)
		private static final long INTERVAL_TICKS = Ticks.PER_SECOND * 60 * 60 * 4L;

		private @Nullable WrappedTask task = null;

		public DeleteInactivePlayerShopsTask() {
			Validate.notNull(plugin, "plugin is null");
		}

		public void start() {
			this.stop(); // Stop the task if it is already running

			// The task runs once shortly after start, and then periodically in large intervals:
			task = SchedulerUtils.runAsyncTaskTimerOrOmit(this, 5L, INTERVAL_TICKS);
		}

		public void stop() {
			if (task != null) {
				task.cancel();
				task = null;
			}
		}

		@Override
		public void run() {
			deleteShopsOfInactivePlayers();
		}
	}

	// TODO Also add a command to manually detect and then optionally delete inactive player shops?
	public void deleteShopsOfInactivePlayers() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return; // Feature is disabled
		new DeleteShopsOfInactivePlayersProcedure(plugin).start();
	}
}
