package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.EquipmentUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.NamespacedKeySerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.inventory.NautilusArmor;
import com.nisovin.shopkeepers.util.java.EnumUtils;

// Note: Does not support the baby variant even though it implements the Ageable interface.
//TODO Use the ZombieNautilius type once we support 1.21.11+
public class ZombieNautilusShop extends SKLivingShopObject<Tameable> {

	public static final Property<Boolean> SADDLE = new BasicProperty<Boolean>()
			.dataKeyAccessor("saddle", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	// TODO Replace with the actual type once we only support MC 1.21.11+
	public static final Property<NamespacedKey> VARIANT = new BasicProperty<NamespacedKey>()
			.dataKeyAccessor("variant", NamespacedKeySerializers.DEFAULT)
			.defaultValue(NamespacedKey.minecraft("temperate"))
			.build();

	public static final Property<@Nullable NautilusArmor> ARMOR = new BasicProperty<@Nullable NautilusArmor>()
			.dataKeyAccessor("armor", EnumSerializers.lenient(NautilusArmor.class))
			.nullable() // Null indicates 'no armor'
			.defaultValue(null)
			.build();

	private final PropertyValue<Boolean> saddleProperty = new PropertyValue<>(SADDLE)
			.onValueChanged(Unsafe.initialized(this)::applySaddle)
			.build(properties);
	private final PropertyValue<NamespacedKey> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);
	private final PropertyValue<@Nullable NautilusArmor> armorProperty = new PropertyValue<>(ARMOR)
			.onValueChanged(Unsafe.initialized(this)::applyArmor)
			.build(properties);

	public ZombieNautilusShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<ZombieNautilusShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		saddleProperty.load(shopObjectData);
		variantProperty.load(shopObjectData);
		armorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		saddleProperty.save(shopObjectData);
		variantProperty.save(shopObjectData);
		armorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applySaddle();
		this.applyVariant();
		this.applyArmor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSaddleEditorButton());
		editorButtons.add(this.getVariantEditorButton());
		editorButtons.add(this.getArmorEditorButton());
		return editorButtons;
	}

	// EQUIPMENT

	@Override
	protected void onEquipmentChanged() {
		super.onEquipmentChanged();

		// If the body slot is now empty, apply the armor instead:
		this.applyArmor();
	}

	// SADDLE

	public boolean hasSaddle() {
		return saddleProperty.getValue();
	}

	public void setSaddle(boolean saddle) {
		saddleProperty.setValue(saddle);
	}

	public void cycleSaddle() {
		this.setSaddle(!this.hasSaddle());
	}

	private void applySaddle() {
		Tameable entity = this.getEntity();
		if (entity == null) return; // Not spawned

		var saddleItem = this.hasSaddle() ? new ItemStack(Material.SADDLE) : null;
		var equipment = entity.getEquipment();
		assert equipment != null;
		equipment.setItem(EquipmentSlot.SADDLE, saddleItem);
	}

	private ItemStack getSaddleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SADDLE);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonSaddle,
				Messages.buttonSaddleLore
		);
		return iconItem;
	}

	private Button getSaddleEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getSaddleEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleSaddle();
				return true;
			}
		};
	}

	// VARIANT

	public NamespacedKey getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(NamespacedKey variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(Compat.getProvider().cycleZombieNautilusVariant(this.getVariant(), backwards));
	}

	private void applyVariant() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Compat.getProvider().setZombieNautilusVariant(entity, this.getVariant());
	}

	private static final NamespacedKey VARIANT_WARM = NamespacedKey.minecraft("warm");

	private ItemStack getVariantEditorItem() {
		var variant = this.getVariant();
		var itemType = variant.equals(VARIANT_WARM) ? Material.BRAIN_CORAL
				: Material.DEAD_BRAIN_CORAL;
		ItemStack iconItem = new ItemStack(itemType);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonZombieNautilusVariant,
				Messages.buttonZombieNautilusVariantLore
		);
		return iconItem;
	}

	private Button getVariantEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getVariantEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleVariant(backwards);
				return true;
			}
		};
	}

	// ARMOR

	public @Nullable NautilusArmor getArmor() {
		return armorProperty.getValue();
	}

	public void setArmor(@Nullable NautilusArmor armor) {
		armorProperty.setValue(armor);
	}

	public void cycleArmor(boolean backwards) {
		this.setArmor(
				EnumUtils.cycleEnumConstantNullable(
						NautilusArmor.class,
						this.getArmor(),
						backwards,
						horseArmor -> horseArmor == null || horseArmor.isEnabled()
				)
		);
	}

	private void applyArmor() {
		LivingEntity entity = this.getEntity();
		if (entity == null) return; // Not spawned

		// The armor uses the body equipment slot. If a non-empty equipment item is set, e.g. via
		// the equipment editor, the equipment takes precedence.
		if (EquipmentUtils.EQUIPMENT_SLOT_BODY.isPresent()) {
			LivingShopEquipment shopEquipment = this.getEquipment();
			@Nullable UnmodifiableItemStack bodyItem = shopEquipment.getItem(EquipmentUtils.EQUIPMENT_SLOT_BODY.get());
			if (!ItemUtils.isEmpty(bodyItem)) {
				return;
			}
		}

		NautilusArmor armor = this.getArmor();
		var armorItem = (armor == null || !armor.isEnabled()) ? null
				: new ItemStack(Unsafe.assertNonNull(armor.getMaterial()));
		var equipment = entity.getEquipment();
		assert equipment != null;
		equipment.setItem(EquipmentSlot.BODY, armorItem);
	}

	private ItemStack getArmorEditorItem() {
		NautilusArmor armor = this.getArmor();
		var iconType = armor == null || !armor.isEnabled() ? Material.BARRIER
				: Unsafe.assertNonNull(armor.getMaterial());
		ItemStack iconItem = new ItemStack(iconType);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonNautilusArmor,
				Messages.buttonNautilusArmorLore
		);
		return iconItem;
	}

	private Button getArmorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getArmorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleArmor(backwards);
				return true;
			}
		};
	}
}
