package com.nisovin.shopkeepers.tradelog.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.tradelog.TradeLogStorageType;
import com.nisovin.shopkeepers.tradelog.TradeLogUtils;
import com.nisovin.shopkeepers.tradelog.TradeLogger;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.SingletonTask;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Retry;
import com.nisovin.shopkeepers.util.java.ThrowableUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.java.VoidCallable;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Base class for {@link TradeLogger}s with a single concurrent writer. Trades are buffered and
 * periodically persisted in batches.
 * <p>
 * If any initial setup is required, override {@link #preSetup()}, {@link #asyncSetup()} and
 * {@link #postSetup()} accordingly.
 */
public abstract class AbstractSingleWriterTradeLogger implements TradeLogger {

	private static final int DELAYED_SAVE_TICKS = 600; // 30 seconds

	private static final int SAVE_MAX_ATTEMPTS = 20;
	private static final long SAVE_RETRY_DELAY_MILLIS = 25L;
	private static final long SAVE_ERROR_MSG_THROTTLE_MILLIS = TimeUnit.MINUTES.toMillis(5);

	protected final Plugin plugin;
	protected final TradeLogStorageType storageType;
	protected final String logPrefix;

	private final SetupTask setupTask;
	private boolean setupCompleted = false;

	private boolean enabled = true;

	private List<TradeRecord> pending = new ArrayList<>();
	private final SaveTask saveTask;
	private @Nullable BukkitTask delayedSaveTask = null;
	// This is reset to the current configuration value prior to every save. This ensures that the
	// value of this setting remains constant during the save and does not differ for the items of
	// the trades that are being saved as part of the same batch.
	private boolean logItemMetadata;

	public AbstractSingleWriterTradeLogger(Plugin plugin, TradeLogStorageType storageType) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.storageType = storageType;
		this.logPrefix = storageType.toString() + " trade log: ";
		this.setupTask = new SetupTask(plugin);
		this.saveTask = new SaveTask(plugin);
	}

	@Override
	public final void setup() {
		if (setupCompleted) return;
		if (setupTask.isRunning()) return;

		setupTask.run();
	}

	/**
	 * Override this to perform any setup that needs to happen on the server's main thread before
	 * {@link #asyncSetup()} is invoked.
	 */
	protected void preSetup() {
	}

	/**
	 * Override this to perform any asynchronous setup.
	 * <p>
	 * If the setup fails, consider calling {@link #disable(String)} during {@link #postSetup()}.
	 * <p>
	 * In certain circumstances, such as the plugin shutting down again before the setup has been
	 * completed, this might also be called on the main thread.
	 */
	protected void asyncSetup() {
	}

	/**
	 * Override this to perform any setup that needs to happen on the server's main thread after
	 * {@link #asyncSetup()} completes.
	 */
	protected void postSetup() {
	}

	private class SetupTask extends SingletonTask {

		private SetupTask(Plugin plugin) {
			super(plugin);
		}

		private class InternalAsyncTask extends SingletonTask.InternalAsyncTask {
		}

		private class InternalSyncCallbackTask extends SingletonTask.InternalSyncCallbackTask {
		}

		@Override
		protected InternalAsyncTask createInternalAsyncTask() {
			return new InternalAsyncTask();
		}

		@Override
		protected InternalSyncCallbackTask createInternalSyncCallbackTask() {
			return new InternalSyncCallbackTask();
		}

		@Override
		protected void prepare() {
			preSetup();
		}

		@Override
		protected void execute() {
			asyncSetup();
		}

		@Override
		protected void syncCallback() {
			postSetup();
			setupCompleted = true;

			// Save any pending trades that were buffered while the setup was in progress:
			savePending();
		}
	}

	/**
	 * Call this to disable this trade logger, e.g. if the setup has failed. Trades won't be logged.
	 * 
	 * @param reason
	 *            the reason
	 */
	protected final void disable(String reason) {
		if (!SchedulerUtils.isMainThread()) {
			throw new IllegalStateException("This must be called from the server's main thread!");
		}

		Log.severe(logPrefix + "Disabled (trades won't be logged)! Reason: " + reason);
		enabled = false;
		this.cancelDelayedSave();
		pending.clear();
	}

	@Override
	public void logTrade(TradeRecord trade) {
		if (!enabled) return;

		pending.add(trade);

		// It is likely for there to be additional trades to log in the immediate future. In order
		// to reduce IO overhead, we do not trigger a save right away, but buffer the incoming trade
		// records over a short period of time.
		this.savePendingDelayed();
	}

	@Override
	public void flush() {
		setupTask.awaitExecutions();
		this.savePending();
		saveTask.awaitExecutions();
	}

	private boolean hasPending() {
		return !pending.isEmpty();
	}

	private void savePendingDelayed() {
		if (!setupCompleted) {
			// Any pending trades are saved once the setup completes.
			return;
		}

		if (!this.hasPending()) {
			// There are no pending trades to save:
			return;
		}

		if (delayedSaveTask != null) {
			// There is already a delayed save in progress:
			return;
		}

		delayedSaveTask = SchedulerUtils.runTaskLaterOrOmit(
				plugin,
				new DelayedSaveTask(),
				DELAYED_SAVE_TICKS
		);
	}

	private class DelayedSaveTask implements Runnable {
		@Override
		public void run() {
			delayedSaveTask = null;
			savePending();
		}
	}

	private void cancelDelayedSave() {
		if (delayedSaveTask != null) {
			delayedSaveTask.cancel();
			delayedSaveTask = null;
		}
	}

	private void savePending() {
		assert setupCompleted;
		if (!this.hasPending()) {
			// There are no pending trades to save:
			return;
		}

		saveTask.run(); // Usually async, but may be sync during plugin disable
	}

	private class SaveTask extends SingletonTask {

		private List<TradeRecord> saving = new ArrayList<>();
		private @Nullable SaveContext saveContext = null;
		private boolean saveSucceeded = false;
		private long lastSaveErrorMsgMillis = 0L;

		private SaveTask(Plugin plugin) {
			super(plugin);
		}

		private class InternalAsyncTask extends SingletonTask.InternalAsyncTask {
		}

		private class InternalSyncCallbackTask extends SingletonTask.InternalSyncCallbackTask {
		}

		@Override
		protected InternalAsyncTask createInternalAsyncTask() {
			return new InternalAsyncTask();
		}

		@Override
		protected InternalSyncCallbackTask createInternalSyncCallbackTask() {
			return new InternalSyncCallbackTask();
		}

		@Override
		protected void prepare() {
			// Stop any active delayed save task:
			cancelDelayedSave();

			// Reset local logItemMetadata setting:
			logItemMetadata = Settings.logItemMetadata;

			// Swap the pending and saving lists of trades:
			assert saving.isEmpty();
			List<TradeRecord> temp = saving;
			saving = pending;
			pending = temp;

			// Setup new SaveContext:
			assert saveContext == null;
			this.saveContext = new SaveContext(saving);
		}

		@Override
		protected void execute() {
			SaveContext saveContext = Unsafe.assertNonNull(this.saveContext);
			saveSucceeded = writeTradesWithRetry(saveContext);
			assert saveSucceeded ? !saveContext.hasUnsavedTrades() : saveContext.hasUnsavedTrades();
		}

		@Override
		protected void syncCallback() {
			SaveContext saveContext = Unsafe.assertNonNull(this.saveContext);

			this.printDebugInfo();

			if (!saveSucceeded) {
				// Save failed:

				// Add the unsaved trades to the front of the pending trades:
				pending.addAll(0, saveContext.getUnsavedTrades());

				// Attempt the save again after a short delay:
				// However, during the final save attempt during plugin disable, this is skipped and
				// data might be lost.
				savePendingDelayed();

				// Inform admins about the issue (throttled to once every x minutes):
				long nowMillis = System.currentTimeMillis();
				if (Math.abs(nowMillis - lastSaveErrorMsgMillis) > SAVE_ERROR_MSG_THROTTLE_MILLIS) {
					lastSaveErrorMsgMillis = nowMillis;
					String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED
							+ logPrefix + "Failed to log trades!"
							+ " Please check the server logs and look into the issue!";
					for (Player player : Bukkit.getOnlinePlayers()) {
						assert player != null;
						if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.ADMIN_PERMISSION)) {
							player.sendMessage(errorMsg);
						}
					}
				}
			}

			// Reset:
			this.saveContext = null;
			saving.clear();
		}

		private void printDebugInfo() {
			Log.debug(() -> {
				SaveContext saveContext = Unsafe.assertNonNull(this.saveContext);

				StringBuilder sb = new StringBuilder();
				sb.append("Logged trades to the ");
				sb.append(storageType);
				sb.append(" trade log (");

				// Number of logged trade records:
				sb.append(saving.size()).append(" records");

				// Number of trade records that we failed to log:
				if (saveContext.hasUnsavedTrades()) {
					sb.append(", ")
							.append(saveContext.getUnsavedTrades().size())
							.append(" failed to log");
				}

				// Timing summary:
				sb.append("): ");
				sb.append(this.getExecutionTimingString());

				// Failure indicator:
				if (!saveSucceeded) {
					if (saveContext.getUnsavedTrades().size() == saving.size()) {
						sb.append(" -- Logging failed!");
					} else {
						sb.append(" -- Logging partially failed!");
					}
				}
				return sb.toString();
			});
		}
	}

	/**
	 * The context for a particular invocation of the save task to persist a batch of trade records.
	 */
	protected static class SaveContext {

		private final List<? extends TradeRecord> trades;
		private int nextUnsaved = 0;

		private SaveContext(List<? extends TradeRecord> trades) {
			assert trades != null && !CollectionUtils.containsNull(trades);
			this.trades = trades;
		}

		/**
		 * Checks if there are any remaining trades in this batch that need to be persisted.
		 * 
		 * @return <code>true</code> if there are unsaved trades for the current batch
		 */
		public boolean hasUnsavedTrades() {
			return (nextUnsaved < trades.size());
		}

		/**
		 * Gets the next unsaved {@link TradeRecord} of this batch.
		 * <p>
		 * Call {@link #onTradeSuccessfullySaved()} once the trade record has been successfully
		 * persisted to move the cursor forward.
		 * 
		 * @return the next trade record to persist, or <code>null</code> if there are no more
		 *         trades to persist in this batch
		 */
		public @Nullable TradeRecord getNextUnsavedTrade() {
			if (!this.hasUnsavedTrades()) return null;
			return trades.get(nextUnsaved);
		}

		// May return a sublist view:
		private List<? extends TradeRecord> getUnsavedTrades() {
			if (!this.hasUnsavedTrades()) {
				return Collections.emptyList();
			} else {
				return trades.subList(nextUnsaved, trades.size());
			}
		}

		/**
		 * This must be called after successfully persisting the {@link TradeRecord} returned by
		 * {@link #getNextUnsavedTrade()}.
		 */
		public void onTradeSuccessfullySaved() {
			nextUnsaved++;
		}
	}

	/**
	 * Gets a compact (one line) string representation of the item's metadata.
	 * 
	 * @param itemStack
	 *            the item
	 * @return the item's metadata, or an empty string if {@link Settings#logItemMetadata} is
	 *         <code>false</code>.
	 */
	protected String getItemMetadata(UnmodifiableItemStack itemStack) {
		assert itemStack != null;
		if (!logItemMetadata) return ""; // Disabled

		return TradeLogUtils.getItemMetadata(itemStack);
	}

	// May be invoked asynchronously.
	// Returns true on success.
	private boolean writeTradesWithRetry(SaveContext saveContext) {
		try {
			Retry.retry((VoidCallable) () -> {
				this.writeTrades(saveContext);
			}, SAVE_MAX_ATTEMPTS, (attemptNumber, exception, retry) -> {
				// Trade logging failed:
				assert exception != null;
				// Don't spam with errors and stacktraces: Only print them once for the first failed
				// saving attempt (and again for the last failed attempt), and otherwise log a
				// compact description of the issue:
				String errorMsg = logPrefix + "Failed to log trades (attempt " + attemptNumber + ")";
				if (attemptNumber == 1) {
					Log.severe(errorMsg, exception);
				} else {
					String issue = ThrowableUtils.getDescription(exception);
					Log.severe(errorMsg + ": " + issue);
				}

				// Try again after a small delay:
				if (retry) {
					try {
						Thread.sleep(SAVE_RETRY_DELAY_MILLIS);
					} catch (InterruptedException e) {
						// Restore the interrupt status for anyone interested in it, but otherwise
						// ignore the interrupt here, because we prefer to keep retrying to still
						// save the data to disk after all:
						Thread.currentThread().interrupt();
					}
				}
			});
			return true;
		} catch (Exception e) {
			Log.severe(logPrefix + "Failed to log trades! Data might have been lost! :(", e);
			return false;
		}
	}

	/**
	 * Persists the remaining unsaved trades of the given {@link SaveContext}.
	 * <p>
	 * Implementation notes:
	 * <ul>
	 * <li>Reliably persist all trades.
	 * <li>Persist trades in the order in which they occurred.
	 * <li>Persist individual trades atomically, i.e. not partially, intertwined, or duplicated
	 * (e.g. if we retry failed log attempts).
	 * <li>Usually invoked asynchronously, but may be invoked on the server's main thread, for
	 * example during plugin shutdown.
	 * </ul>
	 * <p>
	 * If the saving of a trade record fails, i.e. if this method throws an exception, this method
	 * may be invoked again several times after short delays with the same {@link SaveContext} in
	 * order to retry the saving of the remaining trade records of the batch. If the saving still
	 * cannot be completed after several retries, the failed save attempt is logged and admins are
	 * informed. If the plugin is currently shutting down, the data for any unsaved trades is lost.
	 * Otherwise, if the plugin is not shutting down, we re-attempt the saving of the unsaved trade
	 * records as part of the next batch.
	 * 
	 * @param saveContext
	 *            the save context
	 * @throws Exception
	 *             if the saving fails
	 */
	protected abstract void writeTrades(SaveContext saveContext) throws Exception;
}
