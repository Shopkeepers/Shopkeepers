package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.inventory.EnchantmentUtils;
import com.nisovin.shopkeepers.util.inventory.EnchantmentUtils.EnchantmentEntry;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.inventory.PotionUtils;

/**
 * Helper methods related to placeholder items.
 * <p>
 * In some situations, such as for example when setting up the trades of certain types of player shopkeepers, players
 * can use these placeholder items as substitutes for items they do not have.
 * <p>
 * The display name of the placeholder item specifies the substituted item type. Other properties of the substituted
 * item cannot be specified.
 */
public final class PlaceholderItems {

	/**
	 * Creates a placeholder item.
	 * <p>
	 * If placeholder items are disabled (set to AIR), this returns an empty item stack that is not considered to be a
	 * {@link #isPlaceholderItem(ItemStack) placeholder item}.
	 * 
	 * @param displayName
	 *            the placeholder item's display name
	 * @return the placeholder item, not <code>null</code>
	 */
	public static ItemStack createPlaceholderItem(String displayName) {
		ItemStack item = DerivedSettings.placeholderItemData.createItemStack();
		ItemUtils.setDisplayName(item, displayName);
		return item;
	}

	/**
	 * Checks if the given {@link ItemStack} is a placeholder item (i.e. is not empty and matches
	 * {@link DerivedSettings#placeholderItemData}).
	 * <p>
	 * This does not check if the given placeholder item specifies a {@link #isValidPlaceholderItem(ItemStack) valid
	 * substituted item}.
	 * <p>
	 * Empty item stacks are never considered to be placeholder items.
	 * 
	 * @param itemStack
	 *            the item stack
	 * @return <code>true</code> if the item stack is a placeholder item
	 */
	public static boolean isPlaceholderItem(@ReadOnly ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return false;
		return DerivedSettings.placeholderItemData.matches(itemStack);
	}

	/**
	 * Gets the {@link ItemStack} that is substituted by the given placeholder {@link ItemStack}, if it actually is a
	 * valid placeholder item (i.e. if we can successfully derive its substituted item).
	 * 
	 * @param placeholderItem
	 *            the (potential) placeholder item
	 * @return the substituted item stack, or <code>null</code> if the given item stack is not a valid placeholder
	 */
	public static ItemStack getSubstitutedItem(@ReadOnly ItemStack placeholderItem) {
		if (!isPlaceholderItem(placeholderItem)) return null;

		// Get the display name:
		ItemMeta meta = placeholderItem.getItemMeta();
		assert meta != null;
		String displayName = meta.getDisplayName();
		assert displayName != null; // But can be empty
		if (displayName.isEmpty()) return null;

		// Basic formatting:
		displayName = displayName.trim();
		displayName = ChatColor.stripColor(displayName);

		// Check for a specified material:
		Material material = ItemUtils.parseMaterial(displayName);
		if (material != null) {
			// Validate the material:
			if (material.isLegacy() || material.isAir() || !material.isItem()) {
				return null;
			}

			// We preserve the stack size of the placeholder item stack:
			return new ItemStack(material, placeholderItem.getAmount());
		}

		// Check for a specified enchantment:
		EnchantmentEntry enchantmentEntry = EnchantmentUtils.parseEnchantmentWithLevel(displayName);
		if (enchantmentEntry != null) {
			Enchantment enchantment = enchantmentEntry.getEnchantment();
			int level = enchantmentEntry.getLevel();
			return EnchantmentUtils.createEnchantedBook(enchantment, level);
		}

		// Check for a specified potion item:
		ItemStack potionItem = PotionUtils.parsePotionItem(displayName);
		if (potionItem != null) {
			return potionItem;
		}

		return null;
	}

	/**
	 * Checks if the given {@link ItemStack} is a valid placeholder item, i.e. with valid
	 * {@link #getSubstitutedItem(ItemStack) substituted item}.
	 * 
	 * @param itemStack
	 *            the item stack
	 * @return <code>true</code> if the item stack is a valid placeholder
	 */
	public static boolean isValidPlaceholderItem(@ReadOnly ItemStack itemStack) {
		return (getSubstitutedItem(itemStack) != null);
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder}, this returns the
	 * {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise, this returns the given
	 * {@link ItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or otherwise the given
	 *         item stack itself
	 */
	public static ItemStack replace(@ReadOnly ItemStack itemStack) {
		ItemStack substitutedItem = getSubstitutedItem(itemStack);
		return (substitutedItem != null) ? substitutedItem : itemStack;
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder}, this returns the
	 * {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise, this returns the given
	 * {@link UnmodifiableItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or otherwise the given
	 *         item stack itself
	 */
	public static UnmodifiableItemStack replace(UnmodifiableItemStack itemStack) {
		return UnmodifiableItemStack.of(replace(ItemUtils.asItemStackOrNull(itemStack)));
	}

	private PlaceholderItems() {
	}
}
