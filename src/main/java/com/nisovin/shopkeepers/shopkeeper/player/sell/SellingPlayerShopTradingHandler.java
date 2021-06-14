package com.nisovin.shopkeepers.shopkeeper.player.sell;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class SellingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected SellingPlayerShopTradingHandler(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// Get offer for this type of item:
		UnmodifiableItemStack soldItem = tradingRecipe.getResultItem();
		PriceOffer offer = shopkeeper.getOffer(soldItem);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		// Validate the found offer:
		int expectedSoldItemAmount = offer.getItem().getAmount();
		if (expectedSoldItemAmount != soldItem.getAmount()) {
			// Unexpected, because the recipe was created based on this offer.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "The offer does not match the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove result items from container contents:
		if (ItemUtils.removeItems(newContainerContents, soldItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain the required items.");
			return false;
		}

		// Add earnings to container contents:
		// TODO Maybe add the actual items the trading player gave, instead of creating new currency items?
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
			// TODO Always store the currency in the most compressed form possible, regardless of
			// 'highCurrencyMinCost'?
			int remaining = amountAfterTaxes;
			if (Settings.isHighCurrencyEnabled() && remaining > Settings.highCurrencyMinCost) {
				int highCurrencyAmount = (remaining / Settings.highCurrencyValue);
				if (highCurrencyAmount > 0) {
					int remainingHighCurrency = ItemUtils.addItems(newContainerContents, Settings.createHighCurrencyItem(highCurrencyAmount));
					remaining -= ((highCurrencyAmount - remainingHighCurrency) * Settings.highCurrencyValue);
				}
			}
			if (remaining > 0) {
				if (ItemUtils.addItems(newContainerContents, Settings.createCurrencyItem(remaining)) != 0) {
					TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
					this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
					this.onInsufficientStorageSpace(tradeData);
					return false;
				}
			}
		}
		return true;
	}
}
