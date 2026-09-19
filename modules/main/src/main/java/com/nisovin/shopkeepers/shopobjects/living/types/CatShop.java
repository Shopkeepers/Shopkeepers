package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Cat;
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
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class CatShop extends SittableShop<Cat> {

	public static final Property<Cat.Type> CAT_TYPE = new BasicProperty<Cat.Type>()
			.dataKeyAccessor("catType", KeyedSerializers.forRegistry(Cat.Type.class))
			.defaultValue(Cat.Type.TABBY)
			.build();

	public static final Property<@Nullable DyeColor> COLLAR_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("collarColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build();

	private final PropertyValue<Cat.Type> catTypeProperty = new PropertyValue<>(CAT_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applyCatType)
			.build(properties);
	private final PropertyValue<@Nullable DyeColor> collarColorProperty = new PropertyValue<>(COLLAR_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCollarColor)
			.build(properties);

	public CatShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<CatShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		catTypeProperty.load(shopObjectData);
		collarColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		catTypeProperty.save(shopObjectData);
		collarColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyCatType();
		this.applyCollarColor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getCatTypeEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// CAT TYPE

	public Cat.Type getCatType() {
		return catTypeProperty.getValue();
	}

	public void setCatType(Cat.Type catType) {
		catTypeProperty.setValue(catType);
	}

	public void cycleCatType(boolean backwards) {
		this.setCatType(RegistryUtils.cycleKeyed(Cat.Type.class, this.getCatType(), backwards));
	}

	private void applyCatType() {
		Cat entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setCatType(this.getCatType());
	}

	private static final Map<Cat.Type, Color> CAT_TYPE_EDITOR_ITEM_COLORS = Map.ofEntries(
			Map.entry(Cat.Type.TABBY, Color.BLACK.mixColors(Color.ORANGE)),
			Map.entry(Cat.Type.ALL_BLACK, Color.BLACK),
			Map.entry(Cat.Type.BLACK, Color.BLACK.mixDyes(DyeColor.GRAY)),
			Map.entry(Cat.Type.BRITISH_SHORTHAIR, Color.SILVER),
			Map.entry(Cat.Type.CALICO, Color.ORANGE.mixDyes(DyeColor.BROWN)),
			Map.entry(Cat.Type.JELLIE, Color.GRAY),
			Map.entry(Cat.Type.PERSIAN, Color.WHITE.mixDyes(DyeColor.ORANGE)),
			Map.entry(Cat.Type.RAGDOLL, Color.WHITE.mixDyes(DyeColor.BROWN)),
			Map.entry(Cat.Type.RED, Color.ORANGE),
			Map.entry(Cat.Type.SIAMESE, Color.GRAY.mixDyes(DyeColor.BROWN)),
			Map.entry(Cat.Type.WHITE, Color.WHITE)
	);

	private ItemStack getCatTypeEditorItem() {
		var catType = this.getCatType();
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		var color = CAT_TYPE_EDITOR_ITEM_COLORS.getOrDefault(catType, Color.PURPLE);
		ItemUtils.setLeatherColor(iconItem, color);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCatVariant,
				Messages.buttonCatVariantLore
		);
		return iconItem;
	}

	private Button getCatTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleCatType(backwards);
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
		Cat entity = this.getEntity();
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
		ItemUtils.setDisplayNameAndLore(iconItem,
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
}
