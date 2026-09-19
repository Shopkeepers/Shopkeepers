package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Pig;
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
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class PigShop extends BabyableShop<Pig> {

	public static final Property<Boolean> SADDLE = new BasicProperty<Boolean>()
			.dataKeyAccessor("saddle", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	public static final Property<Pig.Variant> VARIANT = new BasicProperty<Pig.Variant>()
			.dataKeyAccessor("variant", KeyedSerializers.forRegistry(Pig.Variant.class))
			.defaultValue(Pig.Variant.TEMPERATE)
			.build();

	private final PropertyValue<Boolean> saddleProperty = new PropertyValue<>(SADDLE)
			.onValueChanged(Unsafe.initialized(this)::applySaddle)
			.build(properties);

	private final PropertyValue<Pig.Variant> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public PigShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<PigShop> shopObjectType,
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
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		saddleProperty.save(shopObjectData);
		variantProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applySaddle();
		this.applyVariant();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSaddleEditorButton());
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
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
		Pig entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setSaddle(this.hasSaddle());
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

	public Pig.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Pig.Variant variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(RegistryUtils.cycleKeyed(Pig.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Pig entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVariant(this.getVariant());
	}

	private static final Map<Pig.Variant, Color> VARIANT_EDITOR_ITEM_COLORS = Map.ofEntries(
			Map.entry(Pig.Variant.TEMPERATE, Color.fromRGB(252, 183, 179)),
			Map.entry(Pig.Variant.WARM, Color.fromRGB(203, 114, 56)),
			Map.entry(Pig.Variant.COLD, Color.fromRGB(226, 201, 148))
	);

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		var color = VARIANT_EDITOR_ITEM_COLORS.getOrDefault(this.getVariant(), Color.BLACK);
		ItemUtils.setLeatherColor(iconItem, color);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonPigVariant,
				Messages.buttonPigVariantLore
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
