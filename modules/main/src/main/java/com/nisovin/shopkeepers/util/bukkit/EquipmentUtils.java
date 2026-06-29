package com.nisovin.shopkeepers.util.bukkit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.EnumUtils;

/**
 * Helpers related to entity equipment.
 */
public class EquipmentUtils {

	/**
	 * Checks whether the given entity type supports {@link LivingEntity#getEquipment()}.
	 * 
	 * @param entityType
	 *            the entity type
	 * @return <code>true</code> if the entity type supports {@link LivingEntity#getEquipment()}
	 */
	public static boolean supportsEquipment(EntityType entityType) {
		if (entityType == EntityType.ARMOR_STAND
				|| entityType == EntityType.PLAYER) {
			return true;
		}

		Class<?> entityClass = entityType.getEntityClass();
		if (entityClass == null) return false;

		return Mob.class.isAssignableFrom(entityClass);
	}

	// Added in Bukkit 1.20.5
	public static final Optional<EquipmentSlot> EQUIPMENT_SLOT_BODY;

	// Added in Bukkit 1.21.5
	public static final Optional<EquipmentSlot> EQUIPMENT_SLOT_SADDLE;

	// Common supported equipment slot combinations:
	// Lists for fast iteration and lookup by index. No duplicate or null elements.
	// Element order consistent with EquipmentSlot enum.
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_HANDS_AND_ARMOR;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_HANDS_AND_HEAD;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_HANDS_HEAD_SADDLE;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_MAINHAND_AND_HEAD;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_HANDS;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_MAINHAND;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_HEAD;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_BODY;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_SADDLE;
	public static final List<? extends EquipmentSlot> EQUIPMENT_SLOTS_BODY_AND_SADDLE;

	static {
		@Nullable EquipmentSlot bodySlot = EnumUtils.valueOf(EquipmentSlot.class, "BODY");
		EQUIPMENT_SLOT_BODY = Optional.ofNullable(bodySlot);

		@Nullable EquipmentSlot saddleSlot = EnumUtils.valueOf(EquipmentSlot.class, "SADDLE");
		EQUIPMENT_SLOT_SADDLE = Optional.ofNullable(saddleSlot);

		EQUIPMENT_SLOTS = Collections.unmodifiableList(Arrays.asList(EquipmentSlot.values()));

		EQUIPMENT_SLOTS_HANDS_AND_ARMOR = Collections.unmodifiableList(Arrays.asList(
				EquipmentSlot.HAND,
				EquipmentSlot.OFF_HAND,
				EquipmentSlot.FEET,
				EquipmentSlot.LEGS,
				EquipmentSlot.CHEST,
				EquipmentSlot.HEAD
		));

		EQUIPMENT_SLOTS_HANDS_AND_HEAD = Collections.unmodifiableList(Arrays.asList(
				EquipmentSlot.HAND,
				EquipmentSlot.OFF_HAND,
				EquipmentSlot.HEAD
		));

		EQUIPMENT_SLOTS_HANDS_HEAD_SADDLE = Collections.unmodifiableList(Unsafe.cast(Arrays.asList(
				EquipmentSlot.HAND,
				EquipmentSlot.OFF_HAND,
				EquipmentSlot.HEAD,
				saddleSlot
		).stream().filter(x -> x != null).toList()));

		EQUIPMENT_SLOTS_MAINHAND_AND_HEAD = Collections.unmodifiableList(Arrays.asList(
				EquipmentSlot.HAND,
				EquipmentSlot.HEAD
		));

		EQUIPMENT_SLOTS_HANDS = Collections.unmodifiableList(Arrays.asList(
				EquipmentSlot.HAND,
				EquipmentSlot.OFF_HAND
		));

		EQUIPMENT_SLOTS_MAINHAND = Collections.singletonList(EquipmentSlot.HAND);

		EQUIPMENT_SLOTS_HEAD = Collections.singletonList(EquipmentSlot.HEAD);

		// Added in MC 1.20.5:
		EQUIPMENT_SLOTS_BODY = bodySlot == null ? Collections.emptyList()
				: Collections.singletonList(Unsafe.assertNonNull(bodySlot));

		// Added in MC 1.21.5:
		EQUIPMENT_SLOTS_SADDLE = saddleSlot == null ? Collections.emptyList()
				: Collections.singletonList(Unsafe.assertNonNull(saddleSlot));
		EQUIPMENT_SLOTS_BODY_AND_SADDLE = Collections.unmodifiableList(Unsafe.cast(Arrays.asList(
				bodySlot,
				saddleSlot
		).stream().filter(x -> x != null).toList()));
	}

	/**
	 * Gets the {@link EquipmentSlot}s that entities of the specified type support, i.e. that affect
	 * their visual appearance when a (supported) item is equipped in these slots.
	 * 
	 * @param entityType
	 *            the entity type
	 * @return an unmodifiable view on the supported equipment slots, not <code>null</code>, can be
	 *         empty
	 */
	public static List<? extends EquipmentSlot> getSupportedEquipmentSlots(EntityType entityType) {
		switch (entityType.name()) {
		case "PIG":
		case "STRIDER":
		case "SKELETON_HORSE":
		case "ZOMBIE_HORSE":
		case "MULE":
		case "DONKEY":
		case "CAMEL":
		case "CAMEL_HUSK":
			// MC 1.21.5: Saddle is now an equipment slot.
			return EQUIPMENT_SLOTS_SADDLE;
		// MC 1.21.5: Saddle is now an equipment slot.
		// Body: Horse armor, Nautilus armor (EquipmentSlot added in Bukkit 1.20.5)
		case "HORSE":
		case "NAUTILUS":
		case "ZOMBIE_NAUTILUS":
			return EQUIPMENT_SLOTS_BODY_AND_SADDLE;
		case "PLAYER":
		case "ARMOR_STAND":
		case "MANNEQUIN":
		case "ZOMBIE":
		case "ZOMBIE_VILLAGER":
		case "DROWNED":
		case "HUSK":
		case "GIANT":
		case "SKELETON":
		case "WITHER_SKELETON":
		case "STRAY":
		case "PARCHED":
		case "BOGGED":
		case "PIGLIN":
		case "PIGLIN_BRUTE":
		case "ZOMBIFIED_PIGLIN":
			return EQUIPMENT_SLOTS_HANDS_AND_ARMOR;
		case "PILLAGER": // Head: Only certain items are rendered
			return EQUIPMENT_SLOTS_HANDS_AND_HEAD;
		case "VILLAGER": // Head: Only certain items are rendered
		case "WANDERING_TRADER": // Head: Only certain items are rendered
			// The main hand item item is only visible when chasing a target.
			// "Johnny" is a separate property, without influence on the visibility of the axe.
		case "VINDICATOR": // Head: Only certain items
			return EQUIPMENT_SLOTS_MAINHAND_AND_HEAD;
		case "VEX":
		case "ALLAY":
			return EQUIPMENT_SLOTS_HANDS;
		case "FOX":
		case "DOLPHIN":
		case "WITCH":
			return EQUIPMENT_SLOTS_MAINHAND;
		case "EVOKER": // Head: Only certain items are rendered
		case "ILLUSIONER": // Head: Only certain items are rendered
			return EQUIPMENT_SLOTS_HEAD;
		// Body: Carpet (EquipmentSlot added in Bukkit 1.20.5). Does not support saddle.
		case "LLAMA":
		case "TRADER_LLAMA":
		case "WOLF": // Body: Wolf armor MC 1.20.5
			return EQUIPMENT_SLOTS_BODY;
		case "HAPPY_GHAST": // Body: Colored harness MC 1.21.6
			return EQUIPMENT_SLOTS_BODY;
		case "COPPER_GOLEM": // MC 1.21.10
			// Main and off hand: Overlaps, or only one is rendered.
			// Head: Some items are rendered with a weird offset.
			// Saddle: Only certain items are rendered.
			return EQUIPMENT_SLOTS_HANDS_HEAD_SADDLE;
		case "SULFUR_CUBE": // MC 26.2
			return EQUIPMENT_SLOTS_BODY; // Body: Equipped item
		default:
			return Collections.emptyList();
		}

		/* Notes on other mob properties:
		 * - Snow golem: Pumpkin head is a separate property.
		 * - Mule: Chest is a separate property.
		 * - Donkey: Chest is a separate property.
		 * - Enderman: Carried block is a separate property.
		 */
	}

	/**
	 * Checks whether the given entity type supports a saddle (and the saddle has a visual effect).
	 * 
	 * @param entityType
	 *            the entity type
	 * @return <code>true</code> if the entity type supports a saddle
	 */
	public static boolean supportsSaddle(EntityType entityType) {
		switch (entityType.name()) {
		case "PIG":
		case "STRIDER":
		case "NAUTILUS":
		case "ZOMBIE_NAUTILUS":
			// AbstractHorse:
		case "HORSE":
		case "SKELETON_HORSE":
		case "ZOMBIE_HORSE":
		case "MULE":
		case "DONKEY":
		case "CAMEL":
		case "CAMEL_HUSK":
			return true;
		// Llama extends AbstractHorse, but the saddle is not displayed:
		case "LLAMA":
		case "TRADER_LLAMA":
		default:
			return false;
		}
	}

	private EquipmentUtils() {
	}
}
