package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.ui.trading.TradingView;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class PlayerShopTradingView extends TradingView {

	// Snapshot of a single container's inventory contents during the currently handled trade:
	protected static final class TradeContainer {

		// No ShopContainer reference: ShopContainers with overlapping inventories are combined into
		// a single TradeContainer.
		private final Inventory inventory;
		private final @Nullable ItemStack[] contents;
		// Whether the contents have already been added to the stock or earnings contents:
		private boolean isStock = false;
		private boolean isEarnings = false;

		private TradeContainer(Inventory inventory, @Nullable ItemStack[] contents) {
			this.inventory = inventory;
			this.contents = contents;
		}
	}

	// State related to the currently handled trade:
	protected final List<TradeContainer> tradeContainers = new ArrayList<>();
	protected final List<@Nullable ItemStack[]> stockContents = new ArrayList<>();
	protected final List<@Nullable ItemStack[]> earningsContents = new ArrayList<>();

	protected PlayerShopTradingView(
			PlayerShopTradingViewProvider provider,
			Player player,
			UIState uiState
	) {
		super(provider, player, uiState);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeperNonNull() {
		return (AbstractPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		var shopkeeper = this.getShopkeeperNonNull();
		Player tradingPlayer = trade.getTradingPlayer();

		// No trading with own shops:
		// This setting also applies to other shop members.
		if (Settings.preventTradingWithOwnShop
				&& shopkeeper.isMember(tradingPlayer)
				&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithOwnShop);
			this.debugPreventedTrade("Trading with the own shop is not allowed.");
			return false;
		}

		// No trading while any shop member is online:
		if (Settings.preventTradingWhileMemberIsOnline) {
			@Nullable Player memberPlayer = shopkeeper.getFirstOnlineMember();
			if (memberPlayer != null
					&& !shopkeeper.isMember(tradingPlayer)
					&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWhileMemberOnline,
						"member", Unsafe.assertNonNull(memberPlayer.getName())
				);
				this.debugPreventedTrade("Trading is not allowed while a shop member is online: "
						+ Unsafe.assertNonNull(memberPlayer.getName()));
				return false;
			}
		}

		// Setup common state information for handling this trade:
		// Collect the shop's containers, skipping any that are currently missing or invalid:
		assert tradeContainers.isEmpty();
		assert stockContents.isEmpty();
		assert earningsContents.isEmpty();

		for (var container : shopkeeper.getContainers()) {
			Inventory containerInventory = container.getInventory();
			if (containerInventory == null) continue;

			// Shop containers that share their inventory (e.g. both sides of a double chest) are
			// only collected once. Otherwise, applying the contents of one snapshot would overwrite
			// the changes to the other snapshot.
			// The shared inventory combines the types (stock or earnings) of these shop containers.
			TradeContainer tradeContainer = this.getTradeContainer(containerInventory);
			if (tradeContainer == null) {
				tradeContainer = new TradeContainer(
						containerInventory,
						containerInventory.getContents()
				);
				tradeContainers.add(tradeContainer);
			} else {
				Log.debug(() -> this.getContext().getLogPrefix() + "Container at "
						+ TextUtils.getLocationString(container.getLocation())
						+ " shares its inventory with another shop container.");
			}

			// Pre-build the lists of stock and earnings container contents:
			var containerType = container.getType();
			if (containerType.isStock() && !tradeContainer.isStock) {
				tradeContainer.isStock = true;
				stockContents.add(tradeContainer.contents);
			}

			if (containerType.isEarnings() && !tradeContainer.isEarnings) {
				tradeContainer.isEarnings = true;
				earningsContents.add(tradeContainer.contents);
			}
		}

		if (stockContents.isEmpty() || earningsContents.isEmpty()) {
			Log.debug(() -> this.getContext().getLogPrefix() + "Missing containers:"
					+ " Stock: " + stockContents.isEmpty()
					+ " Earnings: " + earningsContents.isEmpty());
		}

		return true;
	}

	// Returns null if there is no trade container for the given inventory yet:
	private @Nullable TradeContainer getTradeContainer(Inventory inventory) {
		for (TradeContainer tradeContainer : tradeContainers) {
			if (InventoryUtils.isSameInventory(tradeContainer.inventory, inventory)) {
				return tradeContainer;
			}
		}
		return null;
	}

	@Override
	protected void onTradeApplied(Trade trade) {
		super.onTradeApplied(trade);

		// Apply the container content changes:
		for (TradeContainer container : tradeContainers) {
			container.inventory.setContents(container.contents);
		}
	}

	@Override
	protected void onTradeOver(TradingContext tradingContext) {
		super.onTradeOver(tradingContext);

		// Reset trade related state:
		tradeContainers.clear();
		stockContents.clear();
		earningsContents.clear();
	}
}
