package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.TropicalFish;
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
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class TropicalFishShop extends SKLivingShopObject<TropicalFish> {

	public static final Property<TropicalFish.Pattern> PATTERN = new BasicProperty<TropicalFish.Pattern>()
			.dataKeyAccessor("pattern", EnumSerializers.lenient(TropicalFish.Pattern.class))
			.defaultValue(TropicalFish.Pattern.KOB)
			.build();
	public static final Property<DyeColor> BODY_COLOR = new BasicProperty<DyeColor>()
			.dataKeyAccessor("bodyColor", EnumSerializers.lenient(DyeColor.class))
			.defaultValue(DyeColor.WHITE)
			.build();
	public static final Property<DyeColor> PATTERN_COLOR = new BasicProperty<DyeColor>()
			.dataKeyAccessor("patternColor", EnumSerializers.lenient(DyeColor.class))
			.defaultValue(DyeColor.WHITE)
			.build();

	private final PropertyValue<TropicalFish.Pattern> patternProperty = new PropertyValue<>(PATTERN)
			.onValueChanged(Unsafe.initialized(this)::applyPattern)
			.build(properties);
	private final PropertyValue<DyeColor> bodyColorProperty = new PropertyValue<>(BODY_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyBodyColor)
			.build(properties);
	private final PropertyValue<DyeColor> patternColorProperty = new PropertyValue<>(PATTERN_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyPatternColor)
			.build(properties);

	public TropicalFishShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<TropicalFishShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		patternProperty.load(shopObjectData);
		bodyColorProperty.load(shopObjectData);
		patternColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		patternProperty.save(shopObjectData);
		bodyColorProperty.save(shopObjectData);
		patternColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyPattern();
		this.applyBodyColor();
		this.applyPatternColor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getPatternEditorButton());
		editorButtons.add(this.getBodyColorEditorButton());
		editorButtons.add(this.getPatternColorEditorButton());
		return editorButtons;
	}

	// PATTERN

	public TropicalFish.Pattern getPattern() {
		return patternProperty.getValue();
	}

	public void setPattern(TropicalFish.Pattern pattern) {
		patternProperty.setValue(pattern);
	}

	public void cyclePattern(boolean backwards) {
		this.setPattern(
				EnumUtils.cycleEnumConstant(
						TropicalFish.Pattern.class,
						this.getPattern(),
						backwards
				)
		);
	}

	private void applyPattern() {
		TropicalFish entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setPattern(this.getPattern());
	}

	private ItemStack getPatternEditorItem() {
		ItemStack iconItem = new ItemStack(Material.TROPICAL_FISH);
		// TODO Provide translations for the pattern name?
		String patternName = EnumUtils.formatEnumName(this.getPattern().name());
		String displayName = StringUtils.replaceArguments(
				Messages.buttonTropicalFishPattern,
				"pattern", patternName
		);
		List<String> lore = StringUtils.replaceArguments(
				Messages.buttonTropicalFishPatternLore,
				"pattern", patternName
		);
		ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private Button getPatternEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getPatternEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cyclePattern(backwards);
				return true;
			}
		};
	}

	// BODY COLOR

	public DyeColor getBodyColor() {
		return bodyColorProperty.getValue();
	}

	public void setBodyColor(DyeColor color) {
		bodyColorProperty.setValue(color);
	}

	public void cycleBodyColor(boolean backwards) {
		this.setBodyColor(
				EnumUtils.cycleEnumConstant(DyeColor.class, this.getBodyColor(), backwards)
		);
	}

	private void applyBodyColor() {
		TropicalFish entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setBodyColor(this.getBodyColor());
	}

	private ItemStack getBodyColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(this.getBodyColor()));
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonTropicalFishBodyColor,
				Messages.buttonTropicalFishBodyColorLore
		);
		return iconItem;
	}

	private Button getBodyColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getBodyColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleBodyColor(backwards);
				return true;
			}
		};
	}

	// PATTERN COLOR

	public DyeColor getPatternColor() {
		return patternColorProperty.getValue();
	}

	public void setPatternColor(DyeColor color) {
		patternColorProperty.setValue(color);
	}

	public void cyclePatternColor(boolean backwards) {
		this.setPatternColor(
				EnumUtils.cycleEnumConstant(DyeColor.class, this.getPatternColor(), backwards)
		);
	}

	private void applyPatternColor() {
		TropicalFish entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setPatternColor(this.getPatternColor());
	}

	private ItemStack getPatternColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(this.getPatternColor()));
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonTropicalFishPatternColor,
				Messages.buttonTropicalFishPatternColorLore
		);
		return iconItem;
	}

	private Button getPatternColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getPatternColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cyclePatternColor(backwards);
				return true;
			}
		};
	}
}
