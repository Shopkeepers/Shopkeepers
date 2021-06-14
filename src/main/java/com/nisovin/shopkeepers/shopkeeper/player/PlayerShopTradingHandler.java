package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class PlayerShopTradingHandler extends TradingHandler {

	// State related to the currently handled trade:
	protected Inventory containerInventory = null;
	protected ItemStack[] newContainerContents = null;

	protected PlayerShopTradingHandler(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.TRADING(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		PlayerShopkeeper shopkeeper = this.getShopkeeper();

		// Stop opening if trading shall be prevented while the owner is offline:
		if (Settings.preventTradingWhileOwnerIsOnline && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null) {
				if (!silent) {
					Log.debug(() -> "Blocked trade window opening for " + player.getName() + ", because the shop owner is online.");
					TextUtils.sendMessage(player, Messages.cannotTradeWhileOwnerOnline, "owner", ownerPlayer.getName());
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Implementation note: If the trade is aborted due to insufficient storage space, this has to call
	 * {@link #onInsufficientStorageSpace(TradeData)}.
	 */
	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		AbstractPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;

		// No trading with own shop:
		if (Settings.preventTradingWithOwnShop && shopkeeper.isOwner(tradingPlayer)
				&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithOwnShop);
			this.debugPreventedTrade(tradingPlayer, "Trading with the own shop is not allowed.");
			return false;
		}

		// No trading while shop owner is online:
		if (Settings.preventTradingWhileOwnerIsOnline) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null && !shopkeeper.isOwner(tradingPlayer)
					&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWhileOwnerOnline, "owner", ownerPlayer.getName());
				this.debugPreventedTrade(tradingPlayer, "Trading is not allowed while the shop owner is online.");
				return false;
			}
		}

		// Check for the shop's container:
		Inventory containerInventory = shopkeeper.getContainerInventory();
		if (containerInventory == null) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithShopMissingContainer, "owner", shopkeeper.getOwnerName());
			this.debugPreventedTrade(tradingPlayer, "The shop's container is missing.");
			return false;
		}

		// Setup common state information for handling this trade:
		this.containerInventory = containerInventory;
		this.newContainerContents = containerInventory.getContents();

		return true;
	}

	/**
	 * This is called by when a trade attempt fails due to insufficient storage space.
	 * 
	 * @param tradeData
	 *            the trade data
	 */
	protected void onInsufficientStorageSpace(TradeData tradeData) {
		// TODO Inform the shop owner
	}

	@Override
	protected void onTradeApplied(TradeData tradeData) {
		super.onTradeApplied(tradeData);

		// Apply container content changes:
		if (containerInventory != null && newContainerContents != null) {
			containerInventory.setContents(newContainerContents);
		}

		// Reset trade related state information:
		this.resetTradeState();
	}

	@Override
	protected void onTradeAborted(TradeData tradeData) {
		super.onTradeAborted(tradeData);
		this.resetTradeState();
	}

	protected void resetTradeState() {
		containerInventory = null;
		newContainerContents = null;
	}
}
