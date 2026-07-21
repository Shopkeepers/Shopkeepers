package com.nisovin.shopkeepers.shopkeeper.player.buy;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
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

public class BuyingPlayerShopTradingView extends PlayerShopTradingView {

	/**
	 * The offer corresponding to the currently processed trade.
	 */
	private @Nullable PriceOffer currentOffer = null;

	protected BuyingPlayerShopTradingView(
			BuyingPlayerShopTradingViewProvider provider,
			Player player,
			UIState uiState
	) {
		super(provider, player, uiState);
	}

	@Override
	public SKBuyingPlayerShopkeeper getShopkeeperNonNull() {
		return (SKBuyingPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKBuyingPlayerShopkeeper shopkeeper = this.getShopkeeperNonNull();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Get offer for the bought item:
		UnmodifiableItemStack boughtItem = tradingRecipe.getItem1();
		PriceOffer offer = shopkeeper.getOffer(boughtItem);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					"Could not find the offer corresponding to the trading recipe!"
			);
			return false;
		}

		// Validate the found offer:
		int expectedBoughtItemAmount = offer.getItem().getAmount();
		if (expectedBoughtItemAmount != boughtItem.getAmount()) {
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
		PriceOffer offer = Unsafe.assertNonNull(this.currentOffer);

		// Remove the currency items from the stock containers:
		// Note: We always use the configured currency items here, ignoring any modifications to the
		// "result" item during the trade event.
		int remaining = CurrencyInventoryUtils.removeCurrency(this.stockContents, offer.getPrice());
		if (remaining > 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientCurrency);
			this.debugPreventedTrade("The shop's containers do not contain enough currency.");
			return false;
		} else if (remaining < 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(
					"The shop's containers do not have enough space to split large currency items."
			);
			return false;
		}

		// Add the bought items to the earnings containers, taking modifications to the trade
		// event's "received" items into account:
		// Note: Even if the received items were not altered by any plugins, depending on the used
		// item comparison logic and settings, the items that the trading player offered might
		// slightly differ the required items, but still be accepted.
		// Note: Event handlers might set a second "received" item even if the original trade only
		// involved a single item stack.
		ShopkeeperTradeEvent tradeEvent = trade.getTradeEvent();
		UnmodifiableItemStack receivedItem1 = tradeEvent.getReceivedItem1();
		UnmodifiableItemStack receivedItem2 = tradeEvent.getReceivedItem2();

		if (PlayerShopTaxUtils.addItemsAfterTaxes(this.earningsContents, receivedItem1) != 0
				|| PlayerShopTaxUtils.addItemsAfterTaxes(this.earningsContents, receivedItem2) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade("The shop's containers cannot hold the received items.");
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
