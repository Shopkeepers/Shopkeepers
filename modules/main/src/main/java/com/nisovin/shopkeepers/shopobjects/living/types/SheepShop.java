package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Sheep;
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
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class SheepShop extends BabyableShop<Sheep> {

	public static final Property<DyeColor> COLOR = new BasicProperty<DyeColor>()
			.dataKeyAccessor("color", EnumSerializers.lenient(DyeColor.class))
			.defaultValue(DyeColor.WHITE)
			.build();

	public static final Property<Boolean> SHEARED = new BasicProperty<Boolean>()
			.dataKeyAccessor("sheared", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<DyeColor> colorProperty = new PropertyValue<>(COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyColor)
			.build(properties);
	private final PropertyValue<Boolean> shearedProperty = new PropertyValue<>(SHEARED)
			.onValueChanged(Unsafe.initialized(this)::applySheared)
			.build(properties);

	public SheepShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<SheepShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		shearedProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		colorProperty.save(shopObjectData);
		shearedProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applySheared();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getShearedEditorButton());
		return editorButtons;
	}

	// COLOR

	public DyeColor getColor() {
		return colorProperty.getValue();
	}

	public void setColor(DyeColor color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(DyeColor.class, this.getColor(), backwards));
	}

	private void applyColor() {
		Sheep entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(this.getColor()));
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonSheepColor,
				Messages.buttonSheepColorLore
		);
		return iconItem;
	}

	private Button getColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleColor(backwards);
				return true;
			}
		};
	}

	// SHEARED

	public boolean isSheared() {
		return shearedProperty.getValue();
	}

	public void setSheared(boolean sheared) {
		shearedProperty.setValue(sheared);
	}

	public void cycleSheared() {
		this.setSheared(!this.isSheared());
	}

	private void applySheared() {
		Sheep entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setSheared(this.isSheared());
	}

	private ItemStack getShearedEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SHEARS);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonSheepSheared,
				Messages.buttonSheepShearedLore
		);
		return iconItem;
	}

	private Button getShearedEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getShearedEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleSheared();
				return true;
			}
		};
	}
}
