package com.nisovin.shopkeepers.util.inventory;

import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.MinecraftEnumUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Utility functions related to materials and items.
 */
public final class ItemUtils {

	// Note: Since MC 1.20.5, the max stack size is 99 now, but requires a component on the item
	// stack. For our current purposes of this value, 64 is sufficient.
	public static final int MAX_STACK_SIZE_64 = 64;

	// Material utilities:

	/**
	 * Parses the {@link Material} from the given input String.
	 * <p>
	 * This is similar to {@link Material#matchMaterial(String)}, but may have small performance
	 * benefits, or be more lenient in the inputs it accepts.
	 * 
	 * @param input
	 *            the input String
	 * @return the parsed {@link Material}, or <code>null</code> if the material cannot be parsed
	 */
	public static @Nullable Material parseMaterial(@Nullable String input) {
		return MinecraftEnumUtils.parseEnum(Material.class, input);
	}

	/**
	 * Checks if the given material is a container.
	 * 
	 * @param material
	 *            the material
	 * @return <code>true</code> if the material is a container
	 */
	public static boolean isContainer(@Nullable Material material) {
		// TODO This list of container materials needs to be updated with each MC update.
		if (material == null) return false;
		if (isChest(material)) return true; // Includes trapped chest
		if (isShulkerBox(material)) return true;
		switch (material) {
		case BARREL:
		case BREWING_STAND:
		case DISPENSER:
		case DROPPER:
		case HOPPER:
		case FURNACE:
		case BLAST_FURNACE:
		case SMOKER:
		case ENDER_CHEST: // Note: Has no BlockState of type Container.
			return true;
		default:
			return false;
		}
	}

	public static boolean isChest(@Nullable Material material) {
		if (material == null) return false;

		// Chest, trapped chest, copper chests:
		return material.data == org.bukkit.block.data.type.Chest.class;
	}

	public static boolean isShulkerBox(@Nullable Material material) {
		if (material == null) return false;
		switch (material) {
		case SHULKER_BOX:
		case WHITE_SHULKER_BOX:
		case ORANGE_SHULKER_BOX:
		case MAGENTA_SHULKER_BOX:
		case LIGHT_BLUE_SHULKER_BOX:
		case YELLOW_SHULKER_BOX:
		case LIME_SHULKER_BOX:
		case PINK_SHULKER_BOX:
		case GRAY_SHULKER_BOX:
		case LIGHT_GRAY_SHULKER_BOX:
		case CYAN_SHULKER_BOX:
		case PURPLE_SHULKER_BOX:
		case BLUE_SHULKER_BOX:
		case BROWN_SHULKER_BOX:
		case GREEN_SHULKER_BOX:
		case RED_SHULKER_BOX:
		case BLACK_SHULKER_BOX:
			return true;
		default:
			return false;
		}
	}

	public static boolean isSign(@Nullable Material material) {
		if (material == null) return false;
		return material.data == org.bukkit.block.data.type.Sign.class
				|| material.data == org.bukkit.block.data.type.WallSign.class;
	}

	public static boolean isHangingSign(@Nullable Material material) {
		if (material == null) return false;
		return material.data == org.bukkit.block.data.type.HangingSign.class
				|| material.data == org.bukkit.block.data.type.WallHangingSign.class;
	}

	public static boolean isRail(@Nullable Material material) {
		if (material == null) return false;
		switch (material) {
		case RAIL:
		case POWERED_RAIL:
		case DETECTOR_RAIL:
		case ACTIVATOR_RAIL:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Checks if the given {@link Material} is some kind of clickable door block, such as a door,
	 * trap door, or fence gate.
	 * 
	 * @param material
	 *            the material, not <code>null</code>
	 * @return <code>true</code> if the material is a door block
	 */
	public static boolean isClickableDoor(Material material) {
		return material.data == org.bukkit.block.data.type.Door.class
				|| material.data == org.bukkit.block.data.type.TrapDoor.class
				|| material.data == org.bukkit.block.data.type.Gate.class;
	}

	/**
	 * Checks if the given {@link Material} is some kind of clickable switch block, such as a button
	 * or lever.
	 * 
	 * @param material
	 *            the material, not <code>null</code>
	 * @return <code>true</code> if the material is a switch block
	 */
	public static boolean isClickableSwitch(Material material) {
		return material.data == org.bukkit.block.data.type.Switch.class;
	}

	/**
	 * Formats the name of the given {@link Material} to a more user-friendly representation. See
	 * also {@link EnumUtils#formatEnumName(String)}.
	 * 
	 * @param material
	 *            the material, not <code>null</code>
	 * @return the formatted material name
	 */
	public static String formatMaterialName(Material material) {
		Validate.notNull(material, "material is null");
		return EnumUtils.formatEnumName(material.name());
	}

	/**
	 * Formats the {@link Material} name of the given {@link ItemStack} to a more user-friendly
	 * representation. See also {@link #formatMaterialName(Material)}.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @return the formatted material name
	 */
	public static String formatMaterialName(@ReadOnly ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		return formatMaterialName(itemStack.getType());
	}

	// ItemStack utilities:

	/**
	 * Checks if the given {@link ItemStack} is empty.
	 * <p>
	 * The item stack is considered 'empty' if it is <code>null</code>, is of type
	 * {@link Material#AIR}, or its amount is less than or equal to zero.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is empty
	 */
	public static boolean isEmpty(@ReadOnly @Nullable ItemStack itemStack) {
		return itemStack == null
				|| itemStack.getType() == Material.AIR
				|| itemStack.getAmount() <= 0;
	}

	/**
	 * Checks if the given {@link UnmodifiableItemStack} is empty.
	 * <p>
	 * The item stack is considered 'empty' if it is <code>null</code>, is of type
	 * {@link Material#AIR}, or its amount is less than or equal to zero.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is empty
	 */
	public static boolean isEmpty(@Nullable UnmodifiableItemStack itemStack) {
		return isEmpty(asItemStackOrNull(itemStack));
	}

	public static @Nullable ItemStack getNullIfEmpty(@ReadOnly @Nullable ItemStack itemStack) {
		return isEmpty(itemStack) ? null : itemStack;
	}

	public static @Nullable UnmodifiableItemStack getNullIfEmpty(
			@Nullable UnmodifiableItemStack itemStack
	) {
		return isEmpty(itemStack) ? null : itemStack;
	}

	public static ItemStack getOrEmpty(@ReadOnly @Nullable ItemStack itemStack) {
		if (!isEmpty(itemStack)) {
			return Unsafe.assertNonNull(itemStack);
		}
		if (itemStack != null && itemStack.getType() == Material.AIR) {
			return itemStack;
		}
		return new ItemStack(Material.AIR);
	}

	public static @PolyNull UnmodifiableItemStack getFallbackIfNull(
			@Nullable UnmodifiableItemStack item,
			@PolyNull UnmodifiableItemStack fallback
	) {
		if (item == null) return fallback;
		return item;
	}

	public static @Nullable ItemStack cloneOrNullIfEmpty(@ReadOnly @Nullable ItemStack itemStack) {
		if (isEmpty(itemStack)) return null;
		return Unsafe.assertNonNull(itemStack).clone();
	}

	public static @Nullable ItemStack copyOrNullIfEmpty(@Nullable UnmodifiableItemStack itemStack) {
		if (isEmpty(itemStack)) return null;
		return Unsafe.assertNonNull(itemStack).copy();
	}

	public static @PolyNull ItemStack copyOrNull(@PolyNull UnmodifiableItemStack itemStack) {
		if (itemStack == null) return null;
		return itemStack.copy();
	}

	public static @PolyNull UnmodifiableItemStack shallowCopy(
			@PolyNull UnmodifiableItemStack itemStack
	) {
		if (itemStack == null) return null;
		return itemStack.shallowCopy();
	}

	public static @PolyNull UnmodifiableItemStack unmodifiableClone(
			@ReadOnly @PolyNull ItemStack itemStack
	) {
		if (itemStack == null) return null;
		return UnmodifiableItemStack.of(itemStack.clone());
	}

	public static UnmodifiableItemStack nonNullUnmodifiableClone(
			@ReadOnly ItemStack itemStack
	) {
		Validate.notNull(itemStack, "itemStack is null");
		return Unsafe.assertNonNull(unmodifiableClone(itemStack));
	}

	public static @Nullable UnmodifiableItemStack unmodifiableOrNullIfEmpty(
			@ReadOnly @Nullable ItemStack itemStack
	) {
		return UnmodifiableItemStack.of(getNullIfEmpty(itemStack));
	}

	/**
	 * Gets the internal {@link ItemStack} of the given unmodifiable item stack, or
	 * <code>null</code> if the given unmodifiable item stack is <code>null</code>.
	 * <p>
	 * The caller is expected to not modify or expose the returned item stack.
	 * 
	 * @param itemStack
	 *            the unmodifiable item stack
	 * @return the internal item stack, or <code>null</code>
	 * @deprecated Risky. See {@link SKUnmodifiableItemStack#getInternalItemStack()}
	 */
	@Deprecated
	public static @PolyNull ItemStack asItemStackOrNull(@PolyNull UnmodifiableItemStack itemStack) {
		if (itemStack == null) return null;
		return asItemStack(itemStack);
	}

	/**
	 * Gets the internal {@link ItemStack} of the given unmodifiable item stack.
	 * <p>
	 * The caller is expected to not modify or expose the returned item stack.
	 * 
	 * @param itemStack
	 *            the unmodifiable item stack, not <code>null</code>
	 * @return the internal item stack, or <code>null</code>
	 * @deprecated Risky. See {@link SKUnmodifiableItemStack#getInternalItemStack()}
	 */
	@Deprecated
	public static ItemStack asItemStack(UnmodifiableItemStack itemStack) {
		assert itemStack != null;
		return ((SKUnmodifiableItemStack) itemStack).getInternalItemStack();
	}

	/**
	 * Creates a {@link ItemStack#clone() copy} of the given {@link ItemStack} with a stack size of
	 * {@code 1}.
	 * 
	 * @param itemStack
	 *            the item stack to copy, not <code>null</code>
	 * @return the copy
	 */
	public static ItemStack copySingleItem(@ReadOnly ItemStack itemStack) {
		return copyWithAmount(itemStack, 1);
	}

	/**
	 * Creates a {@link ItemStack#clone() copy} of the given {@link ItemStack} with the specified
	 * stack size.
	 * 
	 * @param itemStack
	 *            the item stack to copy, not <code>null</code>
	 * @param amount
	 *            the stack size of the copy
	 * @return the copy
	 */
	public static ItemStack copyWithAmount(@ReadOnly ItemStack itemStack, int amount) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemStack copy = itemStack.clone();
		copy.setAmount(amount);
		return copy;
	}

	public static ItemStack copyWithAmount(UnmodifiableItemStack itemStack, int amount) {
		Validate.notNull(itemStack, "itemStack is null");
		return copyWithAmount(asItemStack(itemStack), amount);
	}

	// Always copies the item stack.
	public static UnmodifiableItemStack unmodifiableCopyWithAmount(
			@ReadOnly ItemStack itemStack,
			int amount
	) {
		Validate.notNull(itemStack, "itemStack is null");
		return UnmodifiableItemStack.ofNonNull(copyWithAmount(itemStack, amount));
	}

	// Returns the same item stack if its amount already matches the target amount.
	public static UnmodifiableItemStack unmodifiableCopyWithAmount(
			UnmodifiableItemStack itemStack,
			int amount
	) {
		Validate.notNull(itemStack, "itemStack is null");
		if (itemStack.getAmount() != amount) {
			return UnmodifiableItemStack.ofNonNull(copyWithAmount(itemStack, amount));
		} else {
			return itemStack;
		}
	}

	// Clamps the amount between 1 and the item stack's max stack size.
	public static int clampAmount(@ReadOnly ItemStack itemStack, int amount) {
		return MathUtils.clamp(amount, 1, itemStack.getMaxStackSize());
	}

	// Clamps the amount between 1 and the item's default max-stack-size.
	public static int clampAmount(Material itemType, int amount) {
		return MathUtils.clamp(amount, 1, itemType.getMaxStackSize());
	}

	/**
	 * Increases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the item stack's amount ends up to be at most
	 * {@link ItemStack#getMaxStackSize()}, and that empty item stacks are represented by
	 * <code>null</code>.
	 * 
	 * @param itemStack
	 *            the item stack, can be empty
	 * @param amountToIncrease
	 *            the amount to increase, can be negative to decrease
	 * @return the resulting item stack, or <code>null</code> if the item stack ends up being empty
	 */
	public static @Nullable ItemStack increaseItemAmount(
			@ReadWrite @Nullable ItemStack itemStack,
			int amountToIncrease
	) {
		if (isEmpty(itemStack)) return null;
		assert itemStack != null;
		int newAmount = Math.min(
				itemStack.getAmount() + amountToIncrease,
				itemStack.getMaxStackSize()
		);
		if (newAmount <= 0) return null;
		itemStack.setAmount(newAmount);
		return itemStack;
	}

	/**
	 * Decreases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the item stack's amount ends up to be at most
	 * {@link ItemStack#getMaxStackSize()}, and that empty item stacks are represented by
	 * <code>null</code>.
	 * 
	 * @param itemStack
	 *            the item stack, can be empty
	 * @param amountToDecrease
	 *            the amount to decrease, can be negative to increase
	 * @return the resulting item, or <code>null</code> if the item ends up being empty
	 */
	public static @Nullable ItemStack decreaseItemAmount(
			@ReadWrite @Nullable ItemStack itemStack,
			int amountToDecrease
	) {
		return increaseItemAmount(itemStack, -amountToDecrease);
	}

	/**
	 * Gets the amount of the given {@link ItemStack}, and returns <code>0</code> if the item stack
	 * is {@link #isEmpty(ItemStack) empty}.
	 * 
	 * @param itemStack
	 *            the item stack, can be empty
	 * @return the item stack's amount, or <code>0</code> if the item stack is empty
	 */
	public static int getItemStackAmount(@ReadOnly @Nullable ItemStack itemStack) {
		if (isEmpty(itemStack)) return 0;
		return Unsafe.assertNonNull(itemStack).getAmount();
	}

	/**
	 * Gets the amount of the given {@link UnmodifiableItemStack}, and returns <code>0</code> if the
	 * item stack is {@link #isEmpty(UnmodifiableItemStack) empty}.
	 * 
	 * @param itemStack
	 *            the item stack, can be empty
	 * @return the item stack's amount, or <code>0</code> if the item stack is empty
	 */
	public static int getItemStackAmount(@Nullable UnmodifiableItemStack itemStack) {
		return getItemStackAmount(asItemStackOrNull(itemStack));
	}

	// The display name and lore are expected to use Minecraft's color codes.
	public static ItemStack createItemStack(
			Material type,
			int amount,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore
	) {
		assert type != null; // Checked by the ItemStack constructor
		assert type.isItem();
		// TODO Return null in case of type AIR?
		ItemStack itemStack = new ItemStack(type, amount);
		return setDisplayNameAndLore(itemStack, displayName, lore);
	}

	// The display name and lore are expected to use Minecraft's color codes.
	public static ItemStack createItemStack(
			ItemData itemData,
			int amount,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore
	) {
		Validate.notNull(itemData, "itemData is null");
		return setDisplayNameAndLore(itemData.createItemStack(amount), displayName, lore);
	}

	// The display name and lore are expected to use Minecraft's color codes.
	// Null arguments keep the previous display name or lore (instead of clearing them).
	public static ItemStack setDisplayNameAndLore(
			@ReadWrite ItemStack itemStack,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore
	) {
		return setItemMeta(itemStack, displayName, lore, null);
	}

	// Replaced in favor of setItemMeta that supports Json text.
	@Deprecated
	public static ItemStack setItemMetaLegacy(
			@ReadWrite ItemStack itemStack,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore,
			@Nullable Integer maxStackSize
	) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemMeta meta = itemStack.getItemMeta();
		if (meta != null) {
			if (displayName != null) {
				meta.setDisplayName(displayName);
			}
			if (lore != null) {
				meta.setLore(Unsafe.cast(lore));
			}
			if (maxStackSize != null) {
				meta.setMaxStackSize(maxStackSize);
			}
			itemStack.setItemMeta(meta);
		}
		return itemStack;
	}

	// Supports Json display name and lore.
	public static ItemStack setItemMeta(
			@ReadWrite ItemStack itemStack,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore,
			@Nullable Integer maxStackSize
	) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemMeta originalMeta = itemStack.getItemMeta();
		if (originalMeta != null) {
			@NonNull ItemMeta newItemMeta = originalMeta;
			if (displayName != null || lore != null) {
				// TODO Workaround for missing component API: ItemMeta supports Json text data
				// during deserialization, so: Serialize the item meta, fill in / replace the text
				// data, and then deserialize the item meta.

				// Modifiable copy of the serialized item meta:
				var serializedMeta = new LinkedHashMap<String, Object>(newItemMeta.serialize());
				if (displayName != null) {
					serializedMeta.put("display-name", displayName);
				}
				if (lore != null) {
					serializedMeta.put("lore", Unsafe.cast(lore));
				}
				var deserializedMeta = ItemSerialization.deserializeItemMeta(serializedMeta);
				if (deserializedMeta != null) {
					newItemMeta = deserializedMeta;
				}
			}

			if (maxStackSize != null) {
				newItemMeta.setMaxStackSize(maxStackSize);
			}

			itemStack.setItemMeta(newItemMeta);
		}
		return itemStack;
	}

	// Supports Json display name and lore.
	public static ItemStack setItemName(
			@ReadWrite ItemStack itemStack,
			@Nullable String itemName
	) {
		Validate.notNull(itemStack, "itemStack is null");
		if (itemName == null) return itemStack;

		ItemMeta originalMeta = itemStack.getItemMeta();
		if (originalMeta != null) {
			@NonNull ItemMeta newItemMeta = originalMeta;
			// TODO Workaround for missing component API: ItemMeta supports Json text data during
			// deserialization, so: Serialize the item meta, fill in / replace the text data, and
			// then deserialize the item meta.

			// Modifiable copy of the serialized item meta:
			var serializedMeta = new LinkedHashMap<String, Object>(newItemMeta.serialize());
			serializedMeta.put("item-name", itemName);
			var deserializedMeta = ItemSerialization.deserializeItemMeta(serializedMeta);
			if (deserializedMeta != null) {
				newItemMeta = deserializedMeta;
			}

			itemStack.setItemMeta(newItemMeta);
		}
		return itemStack;
	}

	public static ItemStack clearDisplayNameAndLore(@ReadWrite ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) return itemStack;

		boolean itemMetaModified = false;
		if (itemMeta.hasDisplayName()) {
			itemMeta.setDisplayName(null);
			itemMetaModified = true;
		}
		if (itemMeta.hasLore()) {
			itemMeta.setLore(null);
			itemMetaModified = true;
		}

		if (itemMetaModified) {
			itemStack.setItemMeta(itemMeta);
		}

		return itemStack;
	}

	// Null to remove display name.
	// The display name is expected to use Minecraft's color codes.
	// Does not support Json text.
	public static ItemStack setDisplayName(
			@ReadWrite ItemStack itemStack,
			@Nullable String displayName
	) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) return itemStack;
		if (displayName == null && !itemMeta.hasDisplayName()) {
			return itemStack;
		}
		itemMeta.setDisplayName(displayName); // Null will clear the display name
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack setUnbreakable(@ReadWrite ItemStack itemStack) {
		Validate.notNull(itemStack, "itemStack is null");
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) return itemStack;

		if (!itemMeta.isUnbreakable()) {
			itemMeta.setUnbreakable(true);
			itemStack.setItemMeta(itemMeta);
		}

		return itemStack;
	}

	// A color of null resets the color to its default.
	public static ItemStack setLeatherColor(
			@ReadWrite ItemStack leatherArmorItem,
			@Nullable Color color
	) {
		Validate.notNull(leatherArmorItem, "leatherArmorItem is null");
		ItemMeta meta = leatherArmorItem.getItemMeta();
		if (meta instanceof LeatherArmorMeta) {
			LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
			leatherMeta.setColor(color);
			leatherArmorItem.setItemMeta(leatherMeta);
		}
		return leatherArmorItem;
	}

	public static String getDisplayNameOrEmpty(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return "";
		return getDisplayNameOrEmpty(itemStack.getItemMeta());
	}

	public static String getDisplayNameOrEmpty(@ReadOnly @Nullable ItemMeta itemMeta) {
		if (itemMeta == null) return "";
		if (!itemMeta.hasDisplayName()) return "";
		return itemMeta.getDisplayName(); // Not null
	}

	public static boolean hasDisplayName(@ReadOnly @Nullable ItemStack itemStack) {
		return !getDisplayNameOrEmpty(itemStack).isEmpty();
	}

	// TODO This can be removed once Bukkit provides a non-deprecated mapping itself.
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

	public static ItemStack createPlayerHead(UUID playerUUID) {
		var itemStack = new ItemStack(Material.PLAYER_HEAD);

		var skullMeta = Unsafe.assertNonNull((SkullMeta) itemStack.getItemMeta());
		try {
			var offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
			skullMeta.setOwningPlayer(offlinePlayer);
		} catch (Exception e) {
			Log.debug("Failed to apply skull meta for player: " + playerUUID, e);
			return itemStack;
		}

		itemStack.setItemMeta(skullMeta);
		return itemStack;
	}

	public static ItemStack createHead(UUID uuid, String name, String skinUrl) {
		var itemStack = new ItemStack(Material.PLAYER_HEAD);

		var profile = Bukkit.createPlayerProfile(uuid, name);
		URL url;
		try {
			url = new URI(skinUrl).toURL();
		} catch (Exception e) {
			Log.debug("Invalid skin URL: " + skinUrl, e);
			return itemStack;
		}

		profile.getTextures().setSkin(url);

		var skullMeta = Unsafe.assertNonNull((SkullMeta) itemStack.getItemMeta());
		try {
			skullMeta.setOwnerProfile(profile);
		} catch (Exception e) {
			Log.debug("Failed to apply skull profile: " + skinUrl, e);
			return itemStack;
		}

		itemStack.setItemMeta(skullMeta);
		return itemStack;
	}

	public static final ItemStack getSkull_MHF_Alex() {
		return createHead(
				UUID.fromString("6ab43178-89fd-4905-97f6-0f67d9d76fd9"),
				"MHF_Alex",
				"http://textures.minecraft.net/texture/63b098967340daac529293c24e04910509b208e7b94563c3ef31dec7b3750"
		);
	}

	public static final ItemStack getSkull_MHF_ArrowLeft() {
		return createHead(
				UUID.fromString("a68f0b64-8d14-4000-a95f4b9ba14f-8df9"),
				"MHF_ArrowLeft",
				"https://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2"
		);
	}

	public static final ItemStack getSkull_MHF_ArrowRight() {
		return createHead(
				UUID.fromString("50c8510b-5ea0-4d60-be9a7d542d6c-d156"),
				"MHF_ArrowRight",
				"https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"
		);
	}

	public static boolean isDamageable(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return false;
		return isDamageable(itemStack.getType());
	}

	public static boolean isDamageable(Material type) {
		return (type.getMaxDurability() > 0);
	}

	public static int getDurability(@ReadOnly @Nullable ItemStack itemStack) {
		// Checking if the item is damageable is cheap in comparison to retrieving the ItemMeta:
		if (!isDamageable(itemStack)) return 0; // Also returns 0 if itemStack is null
		assert itemStack != null;

		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta instanceof Damageable) { // Also checks for null ItemMeta
			return ((Damageable) itemMeta).getDamage();
		} // Else: Unexpected, since we already checked that the item is damageable above.
		return 0;
	}

	public static @Nullable ItemStack ensureBukkitItemStack(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return null;
		if (itemStack.getClass() == ItemStack.class) return itemStack;
		// Similar to CraftItemStack#asBukkitCopy:
		ItemStack bukkitItemStack = new ItemStack(itemStack.getType(), itemStack.getAmount());
		ItemMeta itemMeta = itemStack.getItemMeta();
		// Check if ItemMeta is empty (equivalent to ItemStack#hasItemMeta):
		if (!Bukkit.getItemFactory().equals(itemMeta, null)) {
			bukkitItemStack.setItemMeta(itemMeta);
		}
		return bukkitItemStack;
	}

	public static String getSimpleItemInfo(@Nullable UnmodifiableItemStack item) {
		if (item == null) return "empty";
		StringBuilder sb = new StringBuilder();
		sb.append(item.getAmount()).append('x').append(item.getType());
		return sb.toString();
	}

	public static String getSimpleRecipeInfo(@Nullable TradingRecipe recipe) {
		if (recipe == null) return "none";
		StringBuilder sb = new StringBuilder();
		sb.append("[item1=").append(getSimpleItemInfo(recipe.getItem1()))
				.append(", item2=").append(getSimpleItemInfo(recipe.getItem2()))
				.append(", result=").append(getSimpleItemInfo(recipe.getResultItem()))
				.append("]");
		return sb.toString();
	}

	/**
	 * Same as {@link UnmodifiableItemStack#equals(ItemStack)}, but takes into account that the
	 * given item stacks might both be <code>null</code>.
	 * 
	 * @param unmodifiableItemStack
	 *            an unmodifiable item stack
	 * @param itemStack
	 *            an item stack to compare with
	 * @return <code>true</code> if the item stacks are equal, or both <code>null</code>
	 */
	public static boolean equals(
			@Nullable UnmodifiableItemStack unmodifiableItemStack,
			@ReadOnly @Nullable ItemStack itemStack
	) {
		if (unmodifiableItemStack == null) return (itemStack == null);
		return unmodifiableItemStack.equals(itemStack);
	}

	/**
	 * Same as {@link ItemStack#isSimilar(ItemStack)}, but takes into account that the given item
	 * stacks might both be <code>null</code>.
	 * 
	 * @param item1
	 *            an item stack
	 * @param item2
	 *            another item stack
	 * @return <code>true</code> if the item stacks are similar, or both <code>null</code>
	 */
	public static boolean isSimilar(
			@ReadOnly @Nullable ItemStack item1,
			@ReadOnly @Nullable ItemStack item2
	) {
		if (item1 == null) return (item2 == null);
		return item1.isSimilar(item2);
	}

	/**
	 * Same as {@link UnmodifiableItemStack#isSimilar(UnmodifiableItemStack)}, but takes into
	 * account that the given item stacks might both be <code>null</code>.
	 * 
	 * @param item1
	 *            an item stack
	 * @param item2
	 *            another item stack
	 * @return <code>true</code> if the item stacks are similar, or both <code>null</code>
	 */
	public static boolean isSimilar(
			@Nullable UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2
	) {
		return isSimilar(asItemStackOrNull(item1), asItemStackOrNull(item2));
	}

	/**
	 * Same as {@link UnmodifiableItemStack#isSimilar(ItemStack)}, but takes into account that the
	 * given item stacks might both be <code>null</code>.
	 * 
	 * @param item1
	 *            an item stack
	 * @param item2
	 *            another item stack
	 * @return <code>true</code> if the item stacks are similar, or both <code>null</code>
	 */
	public static boolean isSimilar(
			@Nullable UnmodifiableItemStack item1,
			@ReadOnly @Nullable ItemStack item2
	) {
		return isSimilar(asItemStackOrNull(item1), item2);
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
	public static boolean isSimilar(
			@ReadOnly @Nullable ItemStack item,
			Material type,
			@Nullable String displayName,
			@ReadOnly @Nullable List<? extends String> lore
	) {
		if (item == null) return false;
		if (item.getType() != type) return false;

		boolean checkDisplayName = (displayName != null && !displayName.isEmpty());
		boolean checkLore = (lore != null && !lore.isEmpty());
		if (!checkDisplayName && !checkLore) return true;

		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta == null) return false;

		// Compare display name:
		if (checkDisplayName) {
			assert displayName != null;
			if (!itemMeta.hasDisplayName() || !displayName.equals(itemMeta.getDisplayName())) {
				return false;
			}
		}

		// Compare lore:
		if (checkLore) {
			assert lore != null;
			if (!itemMeta.hasLore() || !lore.equals(itemMeta.getLore())) {
				return false;
			}
		}

		return true;
	}

	// ITEM DATA MATCHING

	public static boolean matchesData(
			@ReadOnly @Nullable ItemStack provided,
			@ReadOnly @Nullable ItemStack required
	) {
		return matchesData(provided, required, true); // Matching partial lists
	}

	public static boolean matchesData(
			@Nullable UnmodifiableItemStack provided,
			@Nullable UnmodifiableItemStack required
	) {
		return matchesData(provided, required, true); // Matching partial lists
	}

	// Same type and contains required data.
	public static boolean matchesData(
			@Nullable UnmodifiableItemStack provided,
			@Nullable UnmodifiableItemStack required,
			boolean matchPartialLists
	) {
		return matchesData(asItemStackOrNull(provided), asItemStackOrNull(required), matchPartialLists);
	}

	// Same type and contains required data.
	public static boolean matchesData(
			@ReadOnly @Nullable ItemStack provided,
			@ReadOnly @Nullable ItemStack required,
			boolean matchPartialLists
	) {
		if (provided == required) return true;
		if (required == null) return true;
		if (provided == null) return false;
		// Compare item types:
		if (provided.getType() != required.getType()) return false;

		// Check if metadata is contained in item:
		return matchesData(
				ItemStackMetaTag.of(provided),
				ItemStackMetaTag.of(required),
				matchPartialLists
		);
	}

	public static boolean matchesData(
			@Nullable UnmodifiableItemStack provided,
			Material requiredType,
			ItemStackMetaTag required,
			boolean matchPartialLists
	) {
		return matchesData(asItemStackOrNull(provided), requiredType, required, matchPartialLists);
	}

	public static boolean matchesData(
			@ReadOnly @Nullable ItemStack provided,
			Material requiredType,
			ItemStackMetaTag required,
			boolean matchPartialLists
	) {
		if (provided == null) return false;
		if (provided.getType() != requiredType) return false;
		if (required.isEmpty()) return true;
		return matchesData(ItemStackMetaTag.of(provided), required, matchPartialLists);
	}

	// Checks if the provided metadata contains the required metadata.
	// Similar to Minecraft's NBT predicate matching. Note: Unlike item comparisons during trading,
	// this matches component data partially.
	public static boolean matchesData(
			ItemStackMetaTag provided,
			ItemStackMetaTag required,
			boolean matchPartialLists
	) {
		return required.matches(provided, matchPartialLists);
	}

	// PREDICATES

	private static final Predicate<@ReadOnly @Nullable ItemStack> EMPTY_ITEMS = ItemUtils::isEmpty;
	private static final Predicate<@ReadOnly @Nullable ItemStack> NON_EMPTY_ITEMS = (itemStack) -> {
		return !isEmpty(itemStack);
	};

	/**
	 * Gets a {@link Predicate} that accepts {@link #isEmpty(ItemStack) empty} {@link ItemStack
	 * ItemStacks}.
	 * 
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> emptyItems() {
		return EMPTY_ITEMS;
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link #isEmpty(ItemStack) non-empty} {@link ItemStack
	 * ItemStacks}.
	 * 
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> nonEmptyItems() {
		return NON_EMPTY_ITEMS;
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link ItemStack ItemStacks} that
	 * {@link ItemData#matches(ItemStack) match} the given {@link ItemData}.
	 * 
	 * @param itemData
	 *            the ItemData, not <code>null</code>
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> matchingItems(ItemData itemData) {
		Validate.notNull(itemData, "itemData is null");
		return (itemStack) -> itemData.matches(itemStack);
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link #isEmpty(ItemStack) non-empty} {@link ItemStack
	 * ItemStacks} that {@link ItemData#matches(ItemStack) match} any of the given {@link ItemData}.
	 * 
	 * @param itemDataList
	 *            the list of ItemData, not <code>null</code> and does not contain <code>null</code>
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> matchingItems(
			@ReadOnly List<? extends ItemData> itemDataList
	) {
		Validate.notNull(itemDataList, "itemDataList is null");
		assert !CollectionUtils.containsNull(itemDataList);
		return (itemStack) -> {
			if (isEmpty(itemStack)) return false;
			for (ItemData itemData : itemDataList) {
				assert itemData != null;
				if (itemData.matches(itemStack)) {
					return true;
				} // Else: Continue.
			}
			return false;
		};
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link ItemStack ItemStacks} that are
	 * {@link ItemStack#isSimilar(ItemStack) similar} to the given {@link ItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> similarItems(
			@ReadOnly ItemStack itemStack
	) {
		Validate.notNull(itemStack, "itemStack is null");
		return (otherItemStack) -> itemStack.isSimilar(otherItemStack);
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link ItemStack ItemStacks} that are
	 * {@link UnmodifiableItemStack#isSimilar(ItemStack) similar} to the given
	 * {@link UnmodifiableItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> similarItems(
			UnmodifiableItemStack itemStack
	) {
		Validate.notNull(itemStack, "itemStack is null");
		return (otherItemStack) -> itemStack.isSimilar(otherItemStack);
	}

	/**
	 * Gets a {@link Predicate} that accepts {@link ItemStack ItemStacks} that are of the specified
	 * {@link Material type}.
	 * 
	 * @param itemType
	 *            the item type, not <code>null</code>
	 * @return the Predicate
	 */
	public static Predicate<@ReadOnly @Nullable ItemStack> itemsOfType(Material itemType) {
		Validate.notNull(itemType, "itemType is null");
		return (itemStack) -> itemStack != null && itemStack.getType() == itemType;
	}

	private ItemUtils() {
	}
}
