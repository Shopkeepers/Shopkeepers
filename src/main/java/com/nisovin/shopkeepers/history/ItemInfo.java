package com.nisovin.shopkeepers.history;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Representing serialized (and potentially invalid) item data.
 */
public class ItemInfo {

	// Note: For performance reasons, the serialization and deserialization of ItemStack data may happen asynchronously

	// Note: To avoid unnecessary ItemStack copying, this does not copy the passed ItemStack currently.
	public static ItemInfo newItemInfo(ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return null;
		return new ItemInfo(itemStack);
	}

	// this can happen asynchronously:
	private static String serializeItemData(ItemStack itemStack) {
		assert itemStack != null;
		String itemData = null;
		if (!Settings.async().tradingHistoryOmitItemData) {
			itemData = ItemUtils.serializeItemMeta(itemStack.getItemMeta());
		}
		return itemData;
	}

	private final String itemType;
	private final int amount;
	// if an ItemStack is given, its data may get serialized lazily (possibly asynchronously):
	private Optional<String> itemData = null; // can contain null
	private Optional<ItemStack> itemStackCache = null; // cached deserialized item

	private ItemInfo(ItemStack itemStack) {
		this(itemStack.getType().name(), itemStack.getAmount(), null, Optional.of(itemStack));
		assert itemStack != null;
	}

	private ItemInfo(String itemType, int amount, Optional<String> itemData, Optional<ItemStack> itemStack) {
		Validate.notEmpty(itemType, "Item type is empty!");
		Validate.isTrue(amount > 0, "Amount has to be positive!");
		this.itemType = itemType;
		this.amount = amount;
		this.itemData = itemData;
		this.itemStackCache = itemStack;
		assert itemData != null || itemStack != null;
	}

	public ItemInfo(String itemType, String itemData, int amount) {
		this(itemType, amount, Optional.ofNullable(itemData), null);
	}

	/**
	 * @return the serialized item type, not <code>null</code>
	 */
	public String getItemType() {
		return itemType;
	}

	/**
	 * @return amount
	 */
	public int getAmount() {
		return amount;
	}

	/**
	 * @return the serialized item data, can be <code>null</code>
	 */
	public String getItemData() {
		// check if item data got already serialized:
		if (itemData == null) {
			// get ItemStack from cache and serialize its data:
			assert itemStackCache != null;
			ItemStack itemStack = this.getItemStack();
			assert itemStack != null;
			String serializedItemData = serializeItemData(itemStack); // can be null
			itemData = Optional.ofNullable(serializedItemData);
		}
		assert itemData != null;
		return itemData.orElse(null); // item data content can be null
	}

	/**
	 * Gets the deserialized ItemStack.
	 * 
	 * @return the deserialized ItemStack, or <code>null</code> if deserialization failed
	 */
	public ItemStack getItemStack() {
		// check cache:
		if (itemStackCache == null) {
			// deserialize and setup cache:
			assert itemData != null;
			ItemStack itemStack = null;
			Material type = Material.matchMaterial(itemType);
			if (type == null) {
				Log.severe("Could not deserialize ItemStack! Unknown item type: " + itemType);
			} else {
				String itemDataString = this.getItemData(); // can be null
				ItemMeta itemMeta = ItemUtils.deserializeItemMeta(itemDataString); // can be null
				itemStack = new ItemStack(type);
				itemStack.setAmount(amount);
				itemStack.setItemMeta(itemMeta);
			}
			// cache ItemStack:
			this.itemStackCache = Optional.ofNullable(itemStack);
		}
		assert itemStackCache != null;
		return itemStackCache.orElse(null); // can be null if deserialization failed earlier
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ItemInfo [itemType=");
		builder.append(itemType);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", itemData=");
		builder.append(itemData);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + itemType.hashCode();
		result = prime * result + amount;
		result = prime * result + ((itemData == null) ? 0 : itemData.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ItemInfo)) return false;
		ItemInfo other = (ItemInfo) obj;
		if (amount != other.amount) return false;
		if (!itemType.equals(other.itemType)) return false;
		if (itemData == null) {
			if (other.itemData != null) return false;
		} else if (!itemData.equals(other.itemData)) return false;
		return true;
	}
}
