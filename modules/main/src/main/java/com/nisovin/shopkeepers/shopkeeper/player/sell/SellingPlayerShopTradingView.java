package com.nisovin.shopkeepers.shopkeeper.player.sell;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.currency.CurrencyInventoryUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTaxUtils;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingView;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;

public class SellingPlayerShopTradingView extends PlayerShopTradingView {

	/**
	 * The offer corresponding to the currently processed trade.
	 */
	private @Nullable PriceOffer currentOffer = null;

	protected SellingPlayerShopTradingView(
			SellingPlayerShopTradingViewProvider provider,
			Player player,
			UIState uiState
	) {
		super(provider, player, uiState);
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeperNonNull() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeperNonNull();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Get offer for this type of item:
		UnmodifiableItemStack soldItem = tradingRecipe.getResultItem();
		PriceOffer offer = shopkeeper.getOffer(soldItem);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					"Could not find the offer corresponding to the trading recipe!"
			);
			return false;
		}

		// Validate the found offer:
		int expectedSoldItemAmount = offer.getItem().getAmount();
		if (expectedSoldItemAmount != soldItem.getAmount()) {
			// Unexpected, because the recipe was created based on this offer.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade("The offer does not match the trading recipe!");
			return false;
		}

		this.currentOffer = offer;

		return true;
	}

	@Override
	protected boolean finalTradePreparation(Trade trade) {
		if (!super.finalTradePreparation(trade)) return false;

		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		PriceOffer offer = Unsafe.assertNonNull(this.currentOffer);

		// Remove the result items from the stock containers:
		// Note: We always use the configured result item here, ignoring any modifications to the
		// "result" item during the trade event. The trading player will still receive the modified
		// result item.
		UnmodifiableItemStack soldItem = tradingRecipe.getResultItem();
		if (InventoryUtils.removeItems(this.stockContents, soldItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade("The shop's containers do not contain the required items.");
			return false;
		}

		// Add the earnings to the earnings containers:
		// Note: We always use the configured currency items here, ignoring any modifications to the
		// "received" items during the subsequent trade event.
		int amountAfterTaxes = PlayerShopTaxUtils.getAmountAfterTaxes(offer.getPrice());
		if (CurrencyInventoryUtils.addCurrency(this.earningsContents, amountAfterTaxes) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade("The shop's containers cannot hold the traded items.");
			return false;
		}

		return true;
	}

	@Override
	protected void onTradeOver(TradingContext tradingContext) {
		super.onTradeOver(tradingContext);

		// Reset trade related state:
		this.currentOffer = null;
	}
}
