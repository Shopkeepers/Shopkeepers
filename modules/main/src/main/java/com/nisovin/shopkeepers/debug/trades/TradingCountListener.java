package com.nisovin.shopkeepers.debug.trades;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import java.util.concurrent.CompletableFuture;

/**
 * Tries to accurately detect individual trades handled by Minecraft by listening to corresponding
 * changes of the TRADED_WITH_VILLAGER statistic for a short period of time right after a player has
 * clicked inside the merchant inventory view.
 * <p>
 * This cannot be used for handling or canceling the trades, because at that point the trading and
 * player inventory was already (at least partly) modified. It can however be useful for debugging
 * purposes.
 * <p>
 * Since shopkeepers handles its trades on its own, only trades with vanilla villagers get detected
 * here. This can currently be used to debug and compare vanilla trading behavior with Shopkeeper's
 * trading behavior.
 */
public class TradingCountListener implements Listener {

	private final ShopkeepersPlugin plugin;
	private final Runnable stopListeningAction = () -> {
		stopListeningTask = null;
		Unsafe.initialized(this).stopListeningForTrades();
	};

	private @Nullable Player tradingPlayer = null;
	private int tradeCounter = 0;
	private @Nullable CompletableFuture<Void> stopListeningTask = null;

	public TradingCountListener(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Ensure that our statistic increment event handler is always executed first:
		EventUtils.enforceExecuteFirst(
				PlayerStatisticIncrementEvent.class,
				EventPriority.LOWEST,
				this
		);
	}

	private void startListeningForTrades(Player tradingPlayer) {
		this.stopListeningForTrades();
		Log.debug("Listening for non-shopkeeper trades of player " + tradingPlayer.getName()
				+ " ...");
		this.tradingPlayer = tradingPlayer;
		this.stopListeningTask = SchedulerUtils.runTaskGloballyOrOmit(stopListeningAction);
	}

	private void stopListeningForTrades() {
		if (tradingPlayer == null) return;
		Log.debug(".. Stopped listening for non-shopkeeper trades.");
		tradingPlayer = null;
		tradeCounter = 0;
		if (stopListeningTask != null) {
			stopListeningTask.cancel(true);
			stopListeningTask = null;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onInventoryClick(InventoryClickEvent event) {
		if (!Settings.debug) return;
		if (event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if (!(event.getInventory() instanceof MerchantInventory)) return;
		Player player = (Player) event.getWhoClicked();
		UISession uiSession = plugin.getUIRegistry().getUISession(player);
		UIType uiType = (uiSession != null) ? uiSession.getUIType() : null;
		// Ignore if trading with a shopkeeper, which handles trades on its own:
		if (uiType == DefaultUITypes.TRADING()) return;

		MerchantInventory inventory = (MerchantInventory) event.getInventory();
		ItemStack resultItem = inventory.getItem(2);
		if (ItemUtils.isEmpty(resultItem)) {
			return; // No trade available, ignoring
		}
		TradingRecipe activeRecipe = MerchantUtils.getActiveTradingRecipe(inventory);
		if (activeRecipe == null) {
			return; // No active recipe found, ignoring
		}

		// Start detecting trades:
		this.startListeningForTrades(player);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
		if (!Settings.debug) return;
		if (event.getStatistic() != Statistic.TRADED_WITH_VILLAGER) return;
		Player player = event.getPlayer();
		if (!player.equals(tradingPlayer)) return;

		// Sanity checks:
		int delta = (event.getNewValue() - event.getPreviousValue());
		if (delta != 1) {
			Log.debug(() -> "Non-shopkeeper trade detection: "
					+ "Expected trading statistic change of 1, but got " + delta);
			return;
		}
		Inventory inventory = player.getOpenInventory().getTopInventory();
		if (!(inventory instanceof MerchantInventory)) {
			Log.debug(() -> "Non-shopkeeper trade detection: "
					+ "Expected open merchant inventory, but got " + inventory.getType());
			return;
		}

		MerchantInventory merchantInventory = (MerchantInventory) inventory;
		// Find the recipe Minecraft is using for the trade (the active recipe gets updated after
		// the statistic change):
		TradingRecipe activeRecipe = MerchantUtils.getActiveTradingRecipe(merchantInventory);
		if (activeRecipe == null) {
			Log.debug("Non-shopkeeper trade detection: Could not find the active trading recipe.");
			return;
		}

		tradeCounter++;
		Log.debug(() -> "Detected non-shopkeeper trade (#" + tradeCounter + "): "
				+ ItemUtils.getSimpleRecipeInfo(activeRecipe));
	}
}
