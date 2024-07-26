package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Cat;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class CatShop extends SittableShop<@NonNull Cat> {

	// TODO MC 1.21: Removed cat type enum. Cat registry is only available in 1.20.4+.
	// Handle as string until we only support 1.20.4+, so that even when running in compatibility
	// mode, and we fail to setup the fallback compat provider for cat types, we still preserve the
	// previously stored cat types (even if we are not able to apply display or cycle them).
	public static final Property<@NonNull String> CAT_TYPE = new BasicProperty<@NonNull String>()
			.dataKeyAccessor("catType", StringSerializers.STRICT_NON_EMPTY)
			.defaultValue("TABBY")
			.build();

	public static final Property<@Nullable DyeColor> COLLAR_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("collarColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build();

	private final PropertyValue<@NonNull String> catTypeProperty = new PropertyValue<>(CAT_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applyCatType)
			.build(properties);
	private final PropertyValue<@Nullable DyeColor> collarColorProperty = new PropertyValue<>(COLLAR_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCollarColor)
			.build(properties);

	public CatShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull CatShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
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
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getCatTypeEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// CAT TYPE

	public String getCatType() {
		return catTypeProperty.getValue();
	}

	public void setCatType(String catType) {
		catTypeProperty.setValue(catType);
	}

	public void cycleCatType(boolean backwards) {
		this.setCatType(NMSManager.getProvider().cycleCatType(this.getCatType(), backwards));
	}

	private void applyCatType() {
		Cat entity = this.getEntity();
		if (entity == null) return; // Not spawned

		Cat.@Nullable Type catType = NMSManager.getProvider().getCatType(this.getCatType());
		if (catType == null) return; // Not supported

		entity.setCatType(catType);
	}

	private ItemStack getCatTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		Cat.@Nullable Type catType = NMSManager.getProvider().getCatType(this.getCatType());
		if (catType == null) catType = Cat.Type.TABBY;
		switch (catType) {
		case TABBY:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixColors(Color.ORANGE));
			break;
		case ALL_BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK);
			break;
		case BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixDyes(DyeColor.GRAY));
			break;
		case BRITISH_SHORTHAIR:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case CALICO:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE.mixDyes(DyeColor.BROWN));
			break;
		case JELLIE:
			ItemUtils.setLeatherColor(iconItem, Color.GRAY);
			break;
		case PERSIAN:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.ORANGE));
			break;
		case RAGDOLL:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.BROWN));
			break;
		case RED:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		case SIAMESE:
			ItemUtils.setLeatherColor(iconItem, Color.GRAY.mixDyes(DyeColor.BROWN));
			break;
		case WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		default:
			// Unknown type:
			ItemUtils.setLeatherColor(iconItem, Color.PURPLE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCatVariant,
				Messages.buttonCatVariantLore
		);
		return iconItem;
	}

	private Button getCatTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
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
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getCollarColorEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleCollarColor(backwards);
				return true;
			}
		};
	}
}
