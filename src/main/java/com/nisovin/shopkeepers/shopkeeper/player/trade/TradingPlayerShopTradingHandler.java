package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected TradingPlayerShopTradingHandler(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// Find offer:
		TradeOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove result items from container contents:
		UnmodifiableItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (ItemUtils.removeItems(newContainerContents, resultItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain the required items.");
			return false;
		}

		// Add the traded items to the container contents:
		if (!this.addItems(newContainerContents, tradingRecipe.getItem1(), tradeData.offeredItem1)
				|| !this.addItems(newContainerContents, tradingRecipe.getItem2(), tradeData.offeredItem2)) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
			this.onInsufficientStorageSpace(tradeData);
			return false;
		}
		return true;
	}

	// We differentiate between the required and the offered items here, because the items that the trading player uses
	// in the trade might be slightly different to the items that are required according to the trading recipe. But
	// depending on Minecraft's item comparison rules and our settings, the items of the trading player might still be
	// accepted for the trade.
	// Returns false, if not all items could be added to the contents.
	private boolean addItems(ItemStack[] contents, UnmodifiableItemStack requiredItem, ItemStack offeredItem) {
		if (ItemUtils.isEmpty(requiredItem)) return true;
		int amountAfterTaxes = this.getAmountAfterTaxes(requiredItem.getAmount());
		if (amountAfterTaxes > 0) {
			if (ItemUtils.addItems(contents, offeredItem, amountAfterTaxes) != 0) {
				// Could not add all items to the contents:
				return false;
			}
		}
		return true;
	}
}
