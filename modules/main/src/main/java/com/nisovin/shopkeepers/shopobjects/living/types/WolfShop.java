package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Wolf;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.RegistryUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.KeyedSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class WolfShop extends SittableShop<Wolf> {

	public static final Property<Boolean> ANGRY = new BasicProperty<Boolean>()
			.dataKeyAccessor("angry", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	public static final Property<@Nullable DyeColor> COLLAR_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("collarColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build();

	public static final Property<Wolf.Variant> VARIANT = new BasicProperty<Wolf.Variant>()
			.dataKeyAccessor("wolfVariant", KeyedSerializers.forRegistry(Wolf.Variant.class))
			.defaultValue(Wolf.Variant.PALE)
			.build();

	private final PropertyValue<Boolean> angryProperty = new PropertyValue<>(ANGRY)
			.onValueChanged(Unsafe.initialized(this)::applyAngry)
			.build(properties);
	private final PropertyValue<@Nullable DyeColor> collarColorProperty = new PropertyValue<>(COLLAR_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCollarColor)
			.build(properties);
	private final PropertyValue<Wolf.Variant> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public WolfShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<WolfShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		angryProperty.load(shopObjectData);
		collarColorProperty.load(shopObjectData);
		variantProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		angryProperty.save(shopObjectData);
		collarColorProperty.save(shopObjectData);
		variantProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyAngry();
		this.applyCollarColor();
		this.applyVariant();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getAngryEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	@Override
	public void onTick() {
		super.onTick();

		// The angry state gets reset when the AngerTime runs out, so we reset it periodically:
		this.applyAngry();
	}

	// ANGRY

	public boolean isAngry() {
		return angryProperty.getValue();
	}

	public void setAngry(boolean angry) {
		angryProperty.setValue(angry);
	}

	public void cycleAngry() {
		this.setAngry(!this.isAngry());
	}

	private void applyAngry() {
		Wolf entity = this.getEntity();
		if (entity == null) return; // Not spawned

		// Only marks the wolf as angry for a random duration. We therefore apply this state again
		// every shopkeeper tick.
		entity.setAngry(this.isAngry());
	}

	private ItemStack getAngryEditorItem() {
		Material iconItemType = this.isAngry() ? Material.RED_WOOL : Material.WHITE_WOOL;
		ItemStack iconItem = new ItemStack(iconItemType);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonWolfAngry,
				Messages.buttonWolfAngryLore
		);
		return iconItem;
	}

	private Button getAngryEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getAngryEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleAngry();
				return true;
			}
		};
	}

	// COLLAR COLOR

	public @Nullable DyeColor getCollarColor() {
		return collarColorProperty.getValue();
	}

	public void setCollarColor(@Nullable DyeColor collarColor) {
		collarColorProperty.setValue(collarColor);
	}

	public void cycleCollarColor(boolean backwards) {
		this.setCollarColor(
				EnumUtils.cycleEnumConstantNullable(
						DyeColor.class,
						this.getCollarColor(),
						backwards
				)
		);
	}

	private void applyCollarColor() {
		Wolf entity = this.getEntity();
		if (entity == null) return; // Not spawned

		DyeColor collarColor = this.getCollarColor();
		if (collarColor == null) {
			// No collar / untamed:
			entity.setTamed(false);
		} else {
			entity.setTamed(true); // Only tamed cats will show the collar
			entity.setCollarColor(collarColor);
		}
	}

	private ItemStack getCollarColorEditorItem() {
		DyeColor collarColor = this.getCollarColor();
		ItemStack iconItem;
		if (collarColor == null) {
			iconItem = new ItemStack(Material.BARRIER);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(collarColor));
		}
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonCollarColor,
				Messages.buttonCollarColorLore
		);
		return iconItem;
	}

	private Button getCollarColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getCollarColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleCollarColor(backwards);
				return true;
			}
		};
	}

	// VARIANT

	public Wolf.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Wolf.Variant variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(RegistryUtils.cycleKeyed(Wolf.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Wolf entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getVariant().toString()) {
		case "minecraft:spotted":
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		case "minecraft:snowy":
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case "minecraft:black":
			ItemUtils.setLeatherColor(iconItem, DyeColor.BLACK.getColor());
			break;
		case "minecraft:ashen":
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(140, 144, 167));
			break;
		case "minecraft:rusty":
			// Default brown color.
			break;
		case "minecraft:woods":
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(88, 68, 34));
			break;
		case "minecraft:chestnut":
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(159, 119, 115));
			break;
		case "minecraft:striped":
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(202, 182, 114));
			break;
		case "minecraft:pale":
		default:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(220, 220, 220));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonWolfVariant,
				Messages.buttonWolfVariantLore
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
}
