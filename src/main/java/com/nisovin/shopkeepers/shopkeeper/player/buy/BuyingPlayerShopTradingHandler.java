package com.nisovin.shopkeepers.shopkeeper.player.buy;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected BuyingPlayerShopTradingHandler(SKBuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBuyingPlayerShopkeeper getShopkeeper() {
		return (SKBuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKBuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// Get offer for the bought item:
		UnmodifiableItemStack boughtItem = tradingRecipe.getItem1();
		PriceOffer offer = shopkeeper.getOffer(boughtItem);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		// Validate the found offer:
		int expectedBoughtItemAmount = offer.getItem().getAmount();
		if (expectedBoughtItemAmount > boughtItem.getAmount()) {
			// Unexpected, because the recipe was created based on this offer.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "The offer does not match the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove currency items from container contents:
		int remaining = this.removeCurrency(newContainerContents, offer.getPrice());
		if (remaining > 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientCurrency);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain enough currency.");
			return false;
		} else if (remaining < 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not have enough space to split large currency items.");
			this.onInsufficientStorageSpace(tradeData);
			return false;
		}

		// Add bought items to container contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(expectedBoughtItemAmount);
		if (amountAfterTaxes > 0) {
			// The item the trading player gave might slightly differ from the required item,
			// but is still accepted, depending on the used item comparison logic and settings.
			ItemStack receivedItem = ItemUtils.copyWithAmount(tradeData.offeredItem1, amountAfterTaxes);
			if (ItemUtils.addItems(newContainerContents, receivedItem) != 0) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
				this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
				this.onInsufficientStorageSpace(tradeData);
				return false;
			}
		}
		return true;
	}

	// TODO Simplify this? Maybe by separating into different, general utility functions.
	// TODO Support iterating in reverse order, for nicer looking container contents?
	// Returns the amount of currency that couldn't be removed, 0 on full success, negative if too much was removed.
	protected int removeCurrency(ItemStack[] contents, int amount) {
		Validate.notNull(contents);
		Validate.isTrue(amount >= 0, "Amount cannot be negative!");
		if (amount == 0) return 0;
		int remaining = amount;

		// First pass: Remove as much low currency as available from partial stacks.
		// Second pass: Remove as much low currency as available from full stacks.
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!Settings.isCurrencyItem(itemStack)) continue;

				// Second pass, or the ItemStack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remaining);
					if (newAmount > 0) {
						// Copy the item before modifying it:
						itemStack = itemStack.clone();
						contents[slot] = itemStack;
						itemStack.setAmount(newAmount);
						remaining = 0;
						break;
					} else {
						contents[slot] = null;
						remaining = -newAmount;
						if (newAmount == 0) {
							break;
						}
					}
				}
			}
			if (remaining == 0) break;
		}
		if (remaining == 0) return 0;

		if (!Settings.isHighCurrencyEnabled()) {
			// We couldn't remove all currency:
			return remaining;
		}

		int remainingHigh = (int) Math.ceil((double) remaining / Settings.highCurrencyValue);
		// We rounded the high currency up, so if this is negative now, it represents the remaining change which
		// needs to be added back:
		remaining -= (remainingHigh * Settings.highCurrencyValue);
		assert remaining <= 0;

		// First pass: Remove high currency from partial stacks.
		// Second pass: Remove high currency from full stacks.
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!Settings.isHighCurrencyItem(itemStack)) continue;

				// Second pass, or the ItemStack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remainingHigh);
					if (newAmount > 0) {
						// Copy the item before modifying it:
						itemStack = itemStack.clone();
						contents[slot] = itemStack;
						itemStack.setAmount(newAmount);
						remainingHigh = 0;
						break;
					} else {
						contents[slot] = null;
						remainingHigh = -newAmount;
						if (newAmount == 0) {
							break;
						}
					}
				}
			}
			if (remainingHigh == 0) break;
		}

		remaining += (remainingHigh * Settings.highCurrencyValue);
		if (remaining >= 0) {
			return remaining;
		}
		assert remaining < 0; // We have some change left
		remaining = -remaining; // The change is now represented as positive value

		// Add the remaining change into empty slots (all partial slots have already been cleared above):
		// TODO This could probably be replaced with Utils.addItems
		int maxStackSize = Settings.currencyItem.getType().getMaxStackSize();
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack itemStack = contents[slot];
			if (!ItemUtils.isEmpty(itemStack)) continue;

			int stackSize = Math.min(remaining, maxStackSize);
			contents[slot] = Settings.createCurrencyItem(stackSize);
			remaining -= stackSize;
			if (remaining == 0) break;
		}
		// We removed too much, represent as negative value:
		remaining = -remaining;
		return remaining;
	}
}
