package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;

/**
 * Utility functions related to materials, items and inventories.
 */
public final class ItemUtils {

	private ItemUtils() {
	}

	// shared YAML config that gets reused
	private static final ThreadLocal<YamlConfiguration> YAML = ThreadLocal.withInitial(() -> new YamlConfiguration());
	public static int MAX_STACK_SIZE = 64;

	// material utilities:

	public static boolean isChest(Material material) {
		return material == Material.CHEST || material == Material.TRAPPED_CHEST;
	}

	public static boolean isSign(Material material) {
		if (material == null) return false;
		return material.data == org.bukkit.block.data.type.Sign.class || material.data == org.bukkit.block.data.type.WallSign.class;
	}

	// itemstack utilities:

	public static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
	}

	public static ItemStack getNullIfEmpty(ItemStack item) {
		return isEmpty(item) ? null : item;
	}

	public static ItemStack cloneOrNullIfEmpty(ItemStack item) {
		return isEmpty(item) ? null : item.clone();
	}

	/**
	 * Creates a clone of the given {@link ItemStack} with amount <code>1</code>.
	 * 
	 * @param item
	 *            the item to get a normalized version of
	 * @return the normalized item
	 */
	public static ItemStack getNormalizedItem(ItemStack item) {
		if (item == null) return null;
		ItemStack normalizedClone = item.clone();
		normalizedClone.setAmount(1);
		return normalizedClone;
	}

	public static int trimItemAmount(ItemStack item, int amount) {
		return trimItemAmount(item.getType(), amount);
	}

	// trims the amount between 1 and the item's max-stack-size
	public static int trimItemAmount(Material itemType, int amount) {
		return MathUtils.trim(amount, 1, itemType.getMaxStackSize());
	}

	public static ItemStack createItemStack(Material type, int amount, String displayName, List<String> lore) {
		// TODO return null in case of type AIR?
		ItemStack itemStack = new ItemStack(type, amount);
		return ItemUtils.setItemStackNameAndLore(itemStack, displayName, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String displayName, List<String> lore) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			if (displayName != null) {
				meta.setDisplayName(displayName);
			}
			if (lore != null) {
				meta.setLore(lore);
			}
			item.setItemMeta(meta);
		}
		return item;
	}

	// null to remove display name
	public static ItemStack setItemStackName(ItemStack itemStack, String displayName) {
		if (itemStack == null) return null;
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) return itemStack;
		if (displayName == null && !itemMeta.hasDisplayName()) {
			return itemStack;
		}
		itemMeta.setDisplayName(displayName); // null will clear the display name
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static String getPrettyMaterialName(Material material) {
		if (material == null) return null;
		return getPrettyMaterialName(material.name());
	}

	public static String getPrettyMaterialName(String materialName) {
		if (materialName == null) return null;
		String prettyName = materialName.replace('_', ' ');
		prettyName = StringUtils.capitalizeAll(prettyName.toLowerCase(Locale.ROOT));
		return prettyName;
	}

	public static String getItemStackName(ItemStack itemStack) {
		if (itemStack == null) return null;
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) return null;
		if (!itemMeta.hasDisplayName()) return null;
		return itemMeta.getDisplayName();
	}

	public static ItemStack setLocalizedName(ItemStack item, String locName) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setLocalizedName(locName);
			item.setItemMeta(meta);
		}
		return item;
	}

	public static ItemStack setLeatherColor(ItemStack leatherArmorItem, Color color) {
		if (leatherArmorItem == null) return null;
		ItemMeta meta = leatherArmorItem.getItemMeta();
		if (meta instanceof LeatherArmorMeta) {
			LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
			leatherMeta.setColor(color);
			leatherArmorItem.setItemMeta(leatherMeta);
		}
		return leatherArmorItem;
	}

	// TODO this can be removed once bukkit provides a non-deprecated mapping itself
	public static Material getWoolType(DyeColor dyeColor) {
		switch (dyeColor) {
		case ORANGE:
			return Material.ORANGE_WOOL;
		case MAGENTA:
			return Material.MAGENTA_WOOL;
		case LIGHT_BLUE:
			return Material.LIGHT_BLUE_WOOL;
		case YELLOW:
			return Material.YELLOW_WOOL;
		case LIME:
			return Material.LIME_WOOL;
		case PINK:
			return Material.PINK_WOOL;
		case GRAY:
			return Material.GRAY_WOOL;
		case LIGHT_GRAY:
			return Material.LIGHT_GRAY_WOOL;
		case CYAN:
			return Material.CYAN_WOOL;
		case PURPLE:
			return Material.PURPLE_WOOL;
		case BLUE:
			return Material.BLUE_WOOL;
		case BROWN:
			return Material.BROWN_WOOL;
		case GREEN:
			return Material.GREEN_WOOL;
		case RED:
			return Material.RED_WOOL;
		case BLACK:
			return Material.BLACK_WOOL;
		case WHITE:
		default:
			return Material.WHITE_WOOL;
		}
	}

	public static Material getCarpetType(DyeColor dyeColor) {
		switch (dyeColor) {
		case ORANGE:
			return Material.ORANGE_CARPET;
		case MAGENTA:
			return Material.MAGENTA_CARPET;
		case LIGHT_BLUE:
			return Material.LIGHT_BLUE_CARPET;
		case YELLOW:
			return Material.YELLOW_CARPET;
		case LIME:
			return Material.LIME_CARPET;
		case PINK:
			return Material.PINK_CARPET;
		case GRAY:
			return Material.GRAY_CARPET;
		case LIGHT_GRAY:
			return Material.LIGHT_GRAY_CARPET;
		case CYAN:
			return Material.CYAN_CARPET;
		case PURPLE:
			return Material.PURPLE_CARPET;
		case BLUE:
			return Material.BLUE_CARPET;
		case BROWN:
			return Material.BROWN_CARPET;
		case GREEN:
			return Material.GREEN_CARPET;
		case RED:
			return Material.RED_CARPET;
		case BLACK:
			return Material.BLACK_CARPET;
		case WHITE:
		default:
			return Material.WHITE_CARPET;
		}
	}

	public static boolean isDamageable(ItemStack itemStack) {
		if (itemStack == null) return false;
		return isDamageable(itemStack.getType());
	}

	public static boolean isDamageable(Material type) {
		return (type.getMaxDurability() > 0);
	}

	public static int getDurability(ItemStack itemStack) {
		assert itemStack != null;
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (!(itemMeta instanceof Damageable)) return 0; // also checks for null ItemMeta
		return ((Damageable) itemMeta).getDamage();
	}

	public static String getSimpleItemInfo(ItemStack item) {
		if (item == null) return "empty";
		StringBuilder sb = new StringBuilder();
		sb.append(item.getAmount()).append('x').append(item.getType());
		return sb.toString();
	}

	public static String getSimpleRecipeInfo(TradingRecipe recipe) {
		if (recipe == null) return "none";
		StringBuilder sb = new StringBuilder();
		sb.append("[item1=").append(getSimpleItemInfo(recipe.getItem1()))
				.append(", item2=").append(getSimpleItemInfo(recipe.getItem2()))
				.append(", result=").append(getSimpleItemInfo(recipe.getResultItem())).append("]");
		return sb.toString();
	}

	/**
	 * Same as {@link ItemStack#isSimilar(ItemStack)}, but taking into account that both given ItemStacks might be
	 * <code>null</code>.
	 * 
	 * @param item1
	 *            an itemstack
	 * @param item2
	 *            another itemstack
	 * @return <code>true</code> if the given item stacks are both <code>null</code> or similar
	 */
	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (item1 == null) return (item2 == null);
		return item1.isSimilar(item2);
	}

	/**
	 * Checks if the given item matches the specified attributes.
	 * 
	 * @param item
	 *            the item
	 * @param type
	 *            the item type
	 * @param displayName
	 *            the displayName, or <code>null</code> or empty to ignore it
	 * @param lore
	 *            the item lore, or <code>null</code> or empty to ignore it
	 * @return <code>true</code> if the item has similar attributes
	 */
	public static boolean isSimilar(ItemStack item, Material type, String displayName, List<String> lore) {
		if (item == null) return false;
		if (item.getType() != type) return false;

		boolean checkDisplayName = (displayName != null && !displayName.isEmpty());
		boolean checkLore = (lore != null && !lore.isEmpty());
		if (!checkDisplayName && !checkLore) return true;

		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta == null) return false;

		// compare display name:
		if (checkDisplayName) {
			if (!itemMeta.hasDisplayName() || !displayName.equals(itemMeta.getDisplayName())) {
				return false;
			}
		}

		// compare lore:
		if (checkLore) {
			if (!itemMeta.hasLore() || !lore.equals(itemMeta.getLore())) {
				return false;
			}
		}

		return true;
	}

	// ITEM DATA MATCHING

	public static boolean matchesData(ItemStack item, ItemStack data) {
		return matchesData(item, data, false); // not matching partial lists
	}

	// same type and contains data
	public static boolean matchesData(ItemStack item, ItemStack data, boolean matchPartialLists) {
		if (item == data) return true;
		if (data == null) return true;
		if (item == null) return false;
		// compare item types:
		if (item.getType() != data.getType()) return false;

		// check if meta data is contained in item:
		return matchesData(item.getItemMeta(), data.getItemMeta(), matchPartialLists);
	}

	public static boolean matchesData(ItemStack item, Material dataType, Map<String, Object> data, boolean matchPartialLists) {
		assert dataType != null;
		if (item == null) return false;
		if (item.getType() != dataType) return false;
		if (data == null || data.isEmpty()) return true;
		return matchesData(item.getItemMeta(), data, matchPartialLists);
	}

	public static boolean matchesData(ItemMeta itemMetaData, ItemMeta dataMetaData) {
		return matchesData(itemMetaData, dataMetaData, false); // not matching partial lists
	}

	// Checks if the meta data contains the other given meta data.
	// Similar to minecraft's nbt data matching (trading does not match partial lists, but data specified in commands
	// does), but there are a few differences: Minecraft requires explicitly specified empty lists to perfectly match in
	// all cases, and some data is treated as list in Minecraft but as map in Bukkit (eg. enchantments). But the
	// behavior is the same if not matching partial lists.
	public static boolean matchesData(ItemMeta itemMetaData, ItemMeta dataMetaData, boolean matchPartialLists) {
		if (itemMetaData == dataMetaData) return true;
		if (dataMetaData == null) return true;
		if (itemMetaData == null) return false;

		// TODO maybe there is a better way of doing this in the future..
		Map<String, Object> itemMetaDataMap = itemMetaData.serialize();
		Map<String, Object> dataMetaDataMap = dataMetaData.serialize();
		return matchesData(itemMetaDataMap, dataMetaDataMap, matchPartialLists);
	}

	public static boolean matchesData(ItemMeta itemMetaData, Map<String, Object> data, boolean matchPartialLists) {
		if (data == null || data.isEmpty()) return true;
		if (itemMetaData == null) return false;
		Map<String, Object> itemMetaDataMap = itemMetaData.serialize();
		return matchesData(itemMetaDataMap, data, matchPartialLists);
	}

	public static boolean matchesData(Map<String, Object> itemData, Map<String, Object> data, boolean matchPartialLists) {
		return _matchesData(itemData, data, matchPartialLists);
	}

	private static boolean _matchesData(Object target, Object data, boolean matchPartialLists) {
		if (target == data) return true;
		if (data == null) return true;
		if (target == null) return false;

		// check if map contains given data:
		if (data instanceof Map) {
			if (!(target instanceof Map)) return false;
			Map<?, ?> targetMap = (Map<?, ?>) target;
			Map<?, ?> dataMap = (Map<?, ?>) data;
			for (Entry<?, ?> entry : dataMap.entrySet()) {
				Object targetValue = targetMap.get(entry.getKey());
				if (!_matchesData(targetValue, entry.getValue(), matchPartialLists)) {
					return false;
				}
			}
			return true;
		}

		// check if list contains given data:
		if (matchPartialLists && data instanceof List) {
			if (!(target instanceof List)) return false;
			List<?> targetList = (List<?>) target;
			List<?> dataList = (List<?>) data;
			// if empty list is explicitly specified, then target list has to be empty as well:
			/*if (dataList.isEmpty()) {
				return targetList.isEmpty();
			}*/
			// Avoid loop (TODO: only works if dataList doesn't contain duplicate entries):
			if (dataList.size() > targetList.size()) {
				return false;
			}
			for (Object dataEntry : dataList) {
				boolean dataContained = false;
				for (Object targetEntry : targetList) {
					if (_matchesData(targetEntry, dataEntry, matchPartialLists)) {
						dataContained = true;
						break;
					}
				}
				if (!dataContained) {
					return false;
				}
			}
			return true;
		}

		// check if objects are equal:
		return data.equals(target);
	}

	//

	/**
	 * Increases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the itemstack's amount ends up to be at most {@link ItemStack#getMaxStackSize()}, and that
	 * empty itemstacks are represented by <code>null</code>.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @param amountToIncrease
	 *            the amount to increase, can be negative to decrease
	 * @return the resulting item, or <code>null</code> if the item ends up being empty
	 */
	public static ItemStack increaseItemAmount(ItemStack itemStack, int amountToIncrease) {
		if (isEmpty(itemStack)) return null;
		int newAmount = Math.min(itemStack.getAmount() + amountToIncrease, itemStack.getMaxStackSize());
		if (newAmount <= 0) return null;
		itemStack.setAmount(newAmount);
		return itemStack;
	}

	/**
	 * Decreases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the itemstack's amount ends up to be at most {@link ItemStack#getMaxStackSize()}, and that
	 * empty itemstacks are represented by <code>null</code>.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @param amountToDescrease
	 *            the amount to decrease, can be negative to increase
	 * @return the resulting item, or <code>null</code> if the item ends up being empty
	 */
	public static ItemStack descreaseItemAmount(ItemStack itemStack, int amountToDescrease) {
		return increaseItemAmount(itemStack, -amountToDescrease);
	}

	/**
	 * Gets an itemstack's amount and returns <code>0</code> for empty itemstacks.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @return the itemstack's amount, or <code>0</code> if the itemstack is empty
	 */
	public static int getItemStackAmount(ItemStack itemStack) {
		return (isEmpty(itemStack) ? 0 : itemStack.getAmount());
	}

	// inventory utilities:

	public static List<ItemCount> countItems(ItemStack[] contents, Filter<ItemStack> filter) {
		List<ItemCount> itemCounts = new ArrayList<>();
		if (contents == null) return itemCounts;
		for (ItemStack item : contents) {
			if (isEmpty(item)) continue;
			if (filter != null && !filter.accept(item)) continue;

			// check if we already have a counter for this type of item:
			ItemCount itemCount = ItemCount.findSimilar(itemCounts, item);
			if (itemCount != null) {
				// increase item count:
				itemCount.addAmount(item.getAmount());
			} else {
				// add new item entry:
				itemCounts.add(new ItemCount(item, item.getAmount()));
			}
		}
		return itemCounts;
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items matching the specified
	 * {@link ItemData}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemData
	 *            the item data to match, <code>null</code> will not match any item
	 * @param amount
	 *            the amount of items to look for
	 * @return <code>true</code> if the at least specified amount of matching items was found
	 */
	public static boolean containsAtLeast(ItemStack[] contents, ItemData itemData, int amount) {
		if (contents == null) return false;
		if (itemData == null) return false; // consider null to match no item here
		int remainingAmount = amount;
		for (ItemStack itemStack : contents) {
			if (!itemData.matches(itemStack)) continue;
			int currentAmount = itemStack.getAmount() - remainingAmount;
			if (currentAmount >= 0) {
				return true;
			} else {
				remainingAmount = -currentAmount;
			}
		}
		return false;
	}

	/**
	 * Removes the specified amount of items which match the specified {@link ItemData} from the given contents.
	 * 
	 * @param contents
	 *            the contents
	 * @param itemData
	 *            the item data to match, <code>null</code> will not match any item
	 * @param amount
	 *            the amount of matching items to remove
	 * @return the amount of items that couldn't be removed (<code>0</code> on full success)
	 */
	public static int removeItems(ItemStack[] contents, ItemData itemData, int amount) {
		if (contents == null) return amount;
		if (itemData == null) return amount;
		int remainingAmount = amount;
		for (int slotId = 0; slotId < contents.length; slotId++) {
			ItemStack itemStack = contents[slotId];
			if (!itemData.matches(itemStack)) continue;
			int newAmount = itemStack.getAmount() - remainingAmount;
			if (newAmount > 0) {
				itemStack.setAmount(newAmount);
				break;
			} else {
				contents[slotId] = null;
				remainingAmount = -newAmount;
				if (remainingAmount == 0) break;
			}
		}
		return remainingAmount;
	}

	/**
	 * Adds the given {@link ItemStack} to the given contents.
	 * 
	 * <p>
	 * This will first try to fill similar partial {@link ItemStack}s in the contents up to the item's max stack size.
	 * Afterwards it will insert the remaining amount into empty slots, splitting at the item's max stack size.
	 * <p>
	 * This does not modify the original item stacks in the given array. If it has to modify the amount of an item
	 * stack, it first replaces it with a copy. So in case those item stacks are mirroring changes to their minecraft
	 * counterpart, those don't get affected directly.<br>
	 * The item being added gets copied as well before it gets inserted in an empty slot.
	 * 
	 * @param contents
	 *            the contents to add the given {@link ItemStack} to
	 * @param item
	 *            the {@link ItemStack} to add
	 * @return the amount of items which couldn't be added (<code>0</code> on full success)
	 */
	public static int addItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		// search for partially fitting item stacks:
		int maxStackSize = item.getMaxStackSize();
		int size = contents.length;
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];

			// slot empty? - skip, because we are currently filling existing item stacks up
			if (isEmpty(slotItem)) continue;

			// slot already full?
			int slotAmount = slotItem.getAmount();
			if (slotAmount >= maxStackSize) continue;

			if (slotItem.isSimilar(item)) {
				// copy itemstack, so we don't modify the original itemstack:
				slotItem = slotItem.clone();
				contents[slot] = slotItem;

				int newAmount = slotAmount + amount;
				if (newAmount <= maxStackSize) {
					// remaining amount did fully fit into this stack:
					slotItem.setAmount(newAmount);
					return 0;
				} else {
					// did not fully fit:
					slotItem.setAmount(maxStackSize);
					amount -= (maxStackSize - slotAmount);
					assert amount != 0;
				}
			}
		}

		// we have items remaining:
		assert amount > 0;

		// search for empty slots:
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];
			if (isEmpty(slotItem)) {
				// found empty slot:
				if (amount > maxStackSize) {
					// add full stack:
					ItemStack stack = item.clone();
					stack.setAmount(maxStackSize);
					contents[slot] = stack;
					amount -= maxStackSize;
				} else {
					// completely fits:
					ItemStack stack = item.clone(); // create a copy, just in case
					stack.setAmount(amount); // stack of remaining amount
					contents[slot] = stack;
					return 0;
				}
			}
		}

		// not all items did fit into the inventory:
		return amount;
	}

	/**
	 * Removes the given {@link ItemStack} from the given contents.
	 * 
	 * <p>
	 * If the amount of the given {@link ItemStack} is {@link Integer#MAX_VALUE}, then all similar items are being
	 * removed from the contents.<br>
	 * This does not modify the original item stacks. If it has to modify the amount of an item stack, it first replaces
	 * it with a copy. So in case those item stacks are mirroring changes to their minecraft counterpart, those don't
	 * get affected directly.
	 * </p>
	 * 
	 * @param contents
	 *            the contents to remove the given {@link ItemStack} from
	 * @param item
	 *            the {@link ItemStack} to remove from the given contents
	 * @return the amount of items which couldn't be removed (<code>0</code> on full success)
	 */
	public static int removeItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		boolean removeAll = (amount == Integer.MAX_VALUE);
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack slotItem = contents[slot];
			if (slotItem == null) continue;
			if (item.isSimilar(slotItem)) {
				if (removeAll) {
					contents[slot] = null;
				} else {
					int newAmount = slotItem.getAmount() - amount;
					if (newAmount > 0) {
						// copy itemstack, so we don't modify the original itemstack:
						slotItem = slotItem.clone();
						contents[slot] = slotItem;
						slotItem.setAmount(newAmount);
						// all items were removed:
						return 0;
					} else {
						contents[slot] = null;
						amount = -newAmount;
						if (amount == 0) {
							// all items were removed:
							return 0;
						}
					}
				}
			}
		}

		if (removeAll) return 0;
		return amount;
	}

	public static void updateInventoryLater(Player player) {
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> player.updateInventory());
	}

	// DE-/SERIALIZATION

	private static final String ITEM_KEY = "item";
	private static final String META_KEY = "meta";

	public static String serializeItem(ItemStack itemStack) throws Exception {
		if (itemStack == null) return null;
		YamlConfiguration yaml = YAML.get(); // shared yaml config
		yaml.set(ITEM_KEY, itemStack);
		String dataString = null;
		try {
			dataString = yaml.saveToString();
		} catch (Exception e) { // just in case something goes wrong
			Log.severe("Error during serialization of ItemStack!", e);
		}
		yaml.set(ITEM_KEY, null); // clear shared yaml config again
		return dataString;
	}

	public static ItemStack deserializeItem(String itemStackData) throws Exception {
		if (StringUtils.isEmpty(itemStackData)) return null;
		YamlConfiguration yaml = YAML.get(); // shared yaml config
		try {
			yaml.loadFromString(itemStackData);
		} catch (Exception e) {
			Log.severe("Error during deserialization of ItemStack!", e);
		}
		ItemStack itemStack = yaml.getItemStack(ITEM_KEY);
		yaml.set(ITEM_KEY, null); // clear shared yaml config again
		return itemStack;
	}

	public static String serializeItemMeta(ItemMeta itemMeta) {
		if (itemMeta == null || Bukkit.getItemFactory().equals(itemMeta, null)) {
			return null;
		}
		YamlConfiguration yaml = YAML.get(); // shared yaml config
		yaml.set(META_KEY, itemMeta);
		String dataString = null;
		try {
			dataString = yaml.saveToString();
		} catch (Exception e) { // just in case something goes wrong
			Log.severe("Error during serialization of ItemMeta!", e);
		}
		yaml.set(META_KEY, null); // clear shared yaml config again
		return dataString;
	}

	public static ItemMeta deserializeItemMeta(String itemMetaData) {
		if (StringUtils.isEmpty(itemMetaData)) return null;
		YamlConfiguration yaml = YAML.get(); // shared yaml config
		try {
			yaml.loadFromString(itemMetaData);
		} catch (Exception e) {
			Log.severe("Error during deserialization of ItemMeta!", e);
		}
		ItemMeta itemMeta = yaml.getSerializable(META_KEY, ItemMeta.class, null);
		yaml.set(META_KEY, null); // clear shared yaml config again
		return itemMeta;
	}
}
