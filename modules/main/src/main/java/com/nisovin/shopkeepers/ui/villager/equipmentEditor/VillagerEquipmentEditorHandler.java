package com.nisovin.shopkeepers.ui.villager.equipmentEditor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.equipmentEditor.AbstractEquipmentEditorHandler;
import com.nisovin.shopkeepers.ui.villager.VillagerUIHelper;
import com.nisovin.shopkeepers.util.bukkit.EquipmentUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class VillagerEquipmentEditorHandler extends AbstractEquipmentEditorHandler {

	private static Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> getEquipmentMap(
			LivingEntity entity
	) {
		Validate.notNull(entity, "entity is null");

		@Nullable EntityEquipment equipment = entity.getEquipment();
		if (equipment == null) return Collections.emptyMap(); // Unexpected

		Map<EquipmentSlot, UnmodifiableItemStack> equipmentMap = new EnumMap<>(EquipmentSlot.class);
		for (EquipmentSlot slot : EquipmentUtils.EQUIPMENT_SLOTS) {
			@Nullable ItemStack item = equipment.getItem(slot);
			if (ItemUtils.isEmpty(item)) continue;
			assert item != null;

			equipmentMap.put(slot, UnmodifiableItemStack.ofNonNull(item));
		}

		return equipmentMap;
	}

	private final AbstractVillager villager;
	private final EntityEquipment entityEquipment;

	VillagerEquipmentEditorHandler(AbstractVillager villager) {
		super(
				SKDefaultUITypes.VILLAGER_EQUIPMENT_EDITOR(),
				EquipmentUtils.EQUIPMENT_SLOTS,
				getEquipmentMap(villager),
				(slot, item) -> {
				}
		);

		Validate.notNull(villager, "villager is null");
		this.villager = villager;
		this.entityEquipment = Unsafe.assertNonNull(villager.getEquipment());
	}

	@Override
	protected void onEquipmentChanged(UISession uiSession, EquipmentSlot slot, @Nullable UnmodifiableItemStack item) {
		if (!uiSession.getPlayer().isValid()) return;
		if (!VillagerUIHelper.checkVillagerValid(villager, uiSession)) return;

		// This copies the item internally:
		entityEquipment.setItem(slot, ItemUtils.asItemStackOrNull(item));

		super.onEquipmentChanged(uiSession, slot, item);
	}
}
