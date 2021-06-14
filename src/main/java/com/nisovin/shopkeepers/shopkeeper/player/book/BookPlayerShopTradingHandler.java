package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.BookItems;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class BookPlayerShopTradingHandler extends PlayerShopTradingHandler {

	private static final Predicate<ItemStack> WRITABLE_BOOK_MATCHER = ItemUtils.itemsOfType(Material.WRITABLE_BOOK);

	protected BookPlayerShopTradingHandler(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		UnmodifiableItemStack bookItem = tradingRecipe.getResultItem();
		BookMeta bookMeta = BookItems.getBookMeta(bookItem);
		if (bookMeta == null || !BookItems.isCopy(bookMeta)) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "The traded item is no valid book copy!");
			return false;
		}

		String bookTitle = BookItems.getTitle(bookMeta);
		if (bookTitle == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not determine the book title of the traded item!");
			return false;
		}

		// Get the offer for this type of item:
		BookOffer offer = shopkeeper.getOffer(bookTitle);
		if (offer == null) {
			// Unexpected, but this might happen if the trades got modified while the player was trading:
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove a blank book from the container contents:
		if (ItemUtils.removeItems(newContainerContents, WRITABLE_BOOK_MATCHER, 1) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientWritableBooks);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain any writable (book-and-quill) items.");
			return false;
		}

		// Add earnings to container contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
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
