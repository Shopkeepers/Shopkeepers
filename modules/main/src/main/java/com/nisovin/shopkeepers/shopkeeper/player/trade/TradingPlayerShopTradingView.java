package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTaxUtils;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingView;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;

public class TradingPlayerShopTradingView extends PlayerShopTradingView {

	protected TradingPlayerShopTradingView(
			TradingPlayerShopTradingViewProvider provider,
			Player player,
			UIState uiState
	) {
		super(provider, player, uiState);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeperNonNull() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeperNonNull();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Find offer:
		TradeOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade("Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		return true;
	}

	@Override
	protected boolean finalTradePreparation(Trade trade) {
		if (!super.finalTradePreparation(trade)) return false;

		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Remove the result items from the stock containers:
		// Note: We always use the configured result item here, ignoring any modifications to the
		// "result" item during the trade event. The trading player will still receive the modified
		// result item.
		UnmodifiableItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (InventoryUtils.removeItems(this.stockContents, resultItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade("The shop's containers do not contain the required items.");
			return false;
		}

		// Add the received items to the earnings containers, taking modifications during the trade
		// event into account:
		// Note: Even if the received items were not altered by any plugins, depending on the used
		// item comparison logic and settings, the items that the trading player offered might
		// slightly differ the required items, but still be accepted.
		ShopkeeperTradeEvent tradeEvent = Unsafe.assertNonNull(trade.getTradeEvent());
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
}
