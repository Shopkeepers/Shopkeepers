package com.nisovin.shopkeepers.currency;

import java.util.Collections;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utility functions for adding and removing {@link Currency currency} to and from inventory
 * contents.
 */
public final class CurrencyInventoryUtils {

	/**
	 * Checks if the given contents contain any {@link Currencies#match(ItemStack) currency} items.
	 * 
	 * @param contents
	 *            the contents to search through, not <code>null</code>
	 * @return <code>true</code> if the contents contain currency
	 */
	public static boolean containsCurrency(@ReadOnly @Nullable ItemStack @ReadOnly [] contents) {
		Validate.notNull(contents, "contents is null");
		for (ItemStack itemStack : contents) {
			if (itemStack == null) continue;
			if (Currencies.match(itemStack) != null) return true;
		}
		return false;
	}

	/**
	 * Counts the total {@link Currency currency} value contained in the given contents.
	 * 
	 * @param contents
	 *            the contents to count the currency in, not <code>null</code>
	 * @return the total currency value
	 */
	public static int countCurrency(@ReadOnly @Nullable ItemStack @ReadOnly [] contents) {
		Validate.notNull(contents, "contents is null");
		int totalCurrency = 0;
		for (ItemStack itemStack : contents) {
			if (itemStack == null) continue;

			Currency currency = Currencies.match(itemStack);
			if (currency != null) {
				totalCurrency += (itemStack.getAmount() * currency.getValue());
			}
		}
		return totalCurrency;
	}

	/**
	 * Adds the specified amount of currency to the given contents.
	 * 
	 * @param contents
	 *            the contents to add the currency to, not <code>null</code>
	 * @param amount
	 *            the currency value to add
	 * @return the remaining currency value that could not be added, <code>0</code> on complete
	 *         success
	 */
	public static int addCurrency(@ReadOnly @Nullable ItemStack @ReadWrite [] contents, int amount) {
		Validate.notNull(contents, "contents is null");
		if (amount <= 0) return 0;

		int remaining = amount;
		// TODO Always store the currency in the most compressed form possible, regardless of
		// 'highCurrencyMinCost'?
		if (Currencies.isHighCurrencyEnabled() && remaining > Settings.highCurrencyMinCost) {
			Currency highCurrency = Currencies.getHigh();
			// Note: This rounds down, so the remaining amount cannot end up negative after
			// subtracting the high currency value.
			int highCurrencyAmount = (remaining / highCurrency.getValue());
			if (highCurrencyAmount > 0) {
				ItemStack currencyItems = Currencies.getHigh().getItemData().createItemStack(highCurrencyAmount);
				int remainingHighCurrency = InventoryUtils.addItems(contents, currencyItems);
				assert remainingHighCurrency >= 0 && remainingHighCurrency <= highCurrencyAmount;
				remaining -= (highCurrencyAmount - remainingHighCurrency) * highCurrency.getValue();
				assert remaining >= 0;
				if (remaining <= 0) return 0;
			}
		}

		ItemStack currencyItems = Currencies.getBase().getItemData().createItemStack(remaining);
		int remainingItems = InventoryUtils.addItems(contents, currencyItems);

		// Return the remaining currency value that could not be added:
		return remainingItems * Currencies.getBase().getValue();
	}

	/**
	 * Adds the specified amount of currency to the given list of contents.
	 * <p>
	 * This first tries to add the currency to contents that already
	 * {@link #containsCurrency(ItemStack[]) contain currency}, and then to the remaining contents.
	 * Within each contents array, {@link #addCurrency(ItemStack[], int)} is used.
	 * 
	 * @param contentsList
	 *            the list of contents, not <code>null</code>
	 * @param amount
	 *            the currency value to add
	 * @return the remaining currency value that could not be added, <code>0</code> on complete
	 *         success
	 */
	public static int addCurrency(
			@ReadOnly List<@ReadOnly @Nullable ItemStack @ReadWrite []> contentsList,
			int amount
	) {
		Validate.notNull(contentsList, "contentsList is null");
		if (amount <= 0) return 0;

		int remaining = amount;
		// First pass: Contents that already contain currency. Second pass: The rest.
		for (int pass = 0; pass < 2; pass++) {
			for (var contents : contentsList) {
				if (remaining <= 0) return 0;

				// Skip if either:
				// - containsCurrency and pass==1 (already handled in pass 0)
				// - !containsCurrency and pass==0 (skipped in pass 0)
				boolean containsCurrency = containsCurrency(contents);
				if (containsCurrency ? (pass != 0) : (pass == 0)) {
					continue;
				}

				remaining = addCurrency(contents, remaining);
			}
		}

		return remaining;
	}

	// TODO Simplify this? Maybe by separating into different, general utility functions.
	// TODO Support iterating in reverse order, for nicer looking contents?
	/**
	 * Removes the specified amount of currency from the given contents.
	 * <p>
	 * If the currency cannot be removed exactly (e.g. because only a larger currency denomination
	 * is available), the change is added back into the contents.
	 * 
	 * @param contents
	 *            the contents to remove the currency from, not <code>null</code>
	 * @param amount
	 *            the currency value to remove
	 * @return the remaining currency value that could not be removed (<code>0</code> on complete
	 *         success), or a negative value if too much currency was removed and the change did not
	 *         fit back into the contents
	 */
	public static int removeCurrency(@ReadOnly @Nullable ItemStack @ReadWrite [] contents, int amount) {
		Validate.notNull(contents, "contents is null");
		Validate.isTrue(amount >= 0, "amount cannot be negative");
		if (amount == 0) return 0;

		return removeCurrency(Collections.singletonList(contents), amount);
	}

	/**
	 * Removes the specified amount of currency from the given list of contents.
	 * <p>
	 * If the currency cannot be removed exactly (e.g. because only a larger currency denomination
	 * is available), the change is added back into the contents.
	 * 
	 * @param contentsList
	 *            the list of contents, not <code>null</code>
	 * @param amount
	 *            the currency value to remove
	 * @return the remaining currency value that could not be removed (<code>0</code> on complete
	 *         success), or a negative value if too much currency was removed and the change did not
	 *         fit back into one of the contents
	 */
	public static int removeCurrency(
			@ReadOnly List<@ReadOnly @Nullable ItemStack @ReadWrite []> contentsList,
			int amount
	) {
		Validate.notNull(contentsList, "contentsList is null");
		Validate.isTrue(amount >= 0, "amount cannot be negative");

		if (amount == 0) return 0;
		int remaining = amount;

		// First pass: Remove as much low currency as available from partial stacks.
		// Second pass: Remove as much low currency as available from full stacks.
		Currency baseCurrency = Currencies.getBase();
		for (int pass = 0; pass < 2; pass++) {
			for (var contents : contentsList) {
				for (int slot = 0; slot < contents.length; slot++) {
					ItemStack itemStack = contents[slot];
					if (!baseCurrency.getItemData().matches(itemStack)) continue;
					assert itemStack != null;

					// Second pass, or the ItemStack is a partial one:
					int itemAmount = itemStack.getAmount();
					if (pass == 1 || itemAmount < itemStack.getMaxStackSize()) {
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

			if (remaining == 0) break;
		}
		if (remaining == 0) return 0;

		if (!Currencies.isHighCurrencyEnabled()) {
			// We couldn't remove all currency:
			return remaining;
		}

		Currency highCurrency = Currencies.getHigh();
		int remainingHigh = (int) Math.ceil((double) remaining / highCurrency.getValue());
		// We rounded the high currency up, so if this is negative now, it represents the remaining
		// change which needs to be added back:
		remaining -= (remainingHigh * highCurrency.getValue());
		assert remaining <= 0;

		// First pass: Remove high currency from partial stacks.
		// Second pass: Remove high currency from full stacks.
		for (int pass = 0; pass < 2; pass++) {
			for (var contents : contentsList) {
				for (int slot = 0; slot < contents.length; slot++) {
					ItemStack itemStack = contents[slot];
					if (!highCurrency.getItemData().matches(itemStack)) continue;
					assert itemStack != null;

					// Second pass, or the ItemStack is a partial one:
					int itemAmount = itemStack.getAmount();
					if (pass == 1 || itemAmount < itemStack.getMaxStackSize()) {
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

			if (remainingHigh == 0) break;
		}

		remaining += (remainingHigh * highCurrency.getValue());
		if (remaining >= 0) {
			return remaining;
		}
		assert remaining < 0; // We have some change left
		remaining = -remaining; // The change is now represented as positive value

		// Add the remaining change into empty slots (all partial slots have already been cleared
		// above):
		// TODO This could probably be replaced with addCurrencies, but would then no longer use the
		// skip-partial-stacks-check optimization).
		int maxStackSize = baseCurrency.getMaxStackSize();
		for (var contents : contentsList) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!ItemUtils.isEmpty(itemStack)) continue;

				int stackSize = Math.min(remaining, maxStackSize);
				contents[slot] = baseCurrency.getItemData().createItemStack(stackSize);
				remaining -= stackSize;
				if (remaining == 0) break;
			}

			if (remaining == 0) break;
		}

		// We removed too much, represent as negative value:
		remaining = -remaining;
		return remaining;
	}

	private CurrencyInventoryUtils() {
	}
}
