package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Utilities related to player shop taxes.
 */
public final class PlayerShopTaxUtils {

	/**
	 * Applies taxes to the specified amount (currency value or item count).
	 * <p>
	 * Note: Depending on the configuration, the amount can end up 0.
	 * 
	 * @param amount
	 *            the original amount before taxes
	 * @return the amount after taxes, i.e. a value >= 0 and <= amount.
	 */
	public static int getAmountAfterTaxes(int amount) {
		assert amount >= 0;
		if (Settings.taxRate == 0) return amount;

		int taxes;
		if (Settings.taxRoundUp) {
			taxes = (int) Math.ceil(amount * (Settings.taxRate / 100.0D));
		} else {
			taxes = (int) Math.floor(amount * (Settings.taxRate / 100.0D));
		}

		return Math.max(0, Math.min(amount - taxes, amount));
	}

	/**
	 * Applies taxes to the given item stack and adds the remaining items to the given list of
	 * inventory contents.
	 * 
	 * @param contentsList
	 *            the list of contents, not <code>null</code>
	 * @param itemStack
	 *            the item stack to add, not <code>null</code>
	 * @return the amount of items that could not be added, or <code>0</code> if all items were
	 *         added
	 */
	public static int addItemsAfterTaxes(
			@ReadOnly List<@ReadOnly @Nullable ItemStack @ReadWrite []> contentsList,
			@Nullable UnmodifiableItemStack itemStack
	) {
		if (ItemUtils.isEmpty(itemStack)) return 0;
		assert itemStack != null;

		int amountAfterTaxes = getAmountAfterTaxes(itemStack.getAmount());
		if (amountAfterTaxes <= 0) return 0;

		return InventoryUtils.addItems(contentsList, itemStack, amountAfterTaxes);
	}

	private PlayerShopTaxUtils() {
	}
}
