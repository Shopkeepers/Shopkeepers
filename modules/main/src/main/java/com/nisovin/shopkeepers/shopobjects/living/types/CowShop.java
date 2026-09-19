package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
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
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class CowShop extends BabyableShop<Cow> {

	public static final Property<Cow.Variant> VARIANT = new BasicProperty<Cow.Variant>()
			.dataKeyAccessor("variant", KeyedSerializers.forRegistry(Cow.Variant.class))
			.defaultValue(Cow.Variant.TEMPERATE)
			.build();

	private final PropertyValue<Cow.Variant> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public CowShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<CowShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		variantProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		variantProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyVariant();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public Cow.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Cow.Variant variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(RegistryUtils.cycleKeyed(Cow.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Cow entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVariant(this.getVariant());
	}

	private static final Map<Cow.Variant, Color> VARIANT_EDITOR_ITEM_COLORS = Map.ofEntries(
			Map.entry(Cow.Variant.TEMPERATE, Color.fromRGB(133, 105, 73)),
			Map.entry(Cow.Variant.WARM, Color.fromRGB(128, 54, 36)),
			Map.entry(Cow.Variant.COLD, Color.fromRGB(183, 108, 46))
	);

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		var color = VARIANT_EDITOR_ITEM_COLORS.getOrDefault(this.getVariant(), Color.BLACK);
		ItemUtils.setLeatherColor(iconItem, color);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCowVariant,
				Messages.buttonCowVariantLore
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
