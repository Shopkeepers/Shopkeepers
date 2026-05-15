package com.nisovin.shopkeepers.shopkeeper.player;

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

public class PlayerShopTradingView extends TradingView {

	// State related to the currently handled trade:
	protected @Nullable Inventory containerInventory = null;
	protected @Nullable ItemStack @Nullable [] newContainerContents = null;

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

		// Check for the shop's container:
		Inventory containerInventory = shopkeeper.getContainerInventory();
		if (containerInventory == null) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithShopMissingContainer,
					"owner", shopkeeper.getOwnerName()
			);
			this.debugPreventedTrade("The shop's container is missing.");
			return false;
		}

		// Setup common state information for handling this trade:
		this.containerInventory = containerInventory;
		this.newContainerContents = Unsafe.cast(containerInventory.getContents());

		return true;
	}

	@Override
	protected void onTradeApplied(Trade trade) {
		super.onTradeApplied(trade);

		// Apply container content changes:
		if (containerInventory != null && newContainerContents != null) {
			containerInventory.setContents(Unsafe.castNonNull(newContainerContents));
		}
	}

	@Override
	protected void onTradeOver(TradingContext tradingContext) {
		super.onTradeOver(tradingContext);

		// Reset trade related state:
		containerInventory = null;
		newContainerContents = null;
	}
}
