package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Chicken;
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

public class ChickenShop extends BabyableShop<Chicken> {

	public static final Property<Chicken.Variant> VARIANT = new BasicProperty<Chicken.Variant>()
			.dataKeyAccessor("variant", KeyedSerializers.forRegistry(Chicken.Variant.class))
			.defaultValue(Chicken.Variant.TEMPERATE)
			.build();

	private final PropertyValue<Chicken.Variant> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public ChickenShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<ChickenShop> shopObjectType,
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

	public Chicken.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Chicken.Variant variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(RegistryUtils.cycleKeyed(
				Chicken.Variant.class,
				this.getVariant(),
				backwards
		));
	}

	private void applyVariant() {
		Chicken entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVariant(this.getVariant());
	}

	private static final Map<Chicken.Variant, Color> VARIANT_EDITOR_ITEM_COLORS = Map.ofEntries(
			Map.entry(Chicken.Variant.TEMPERATE, Color.WHITE),
			Map.entry(Chicken.Variant.WARM, Color.fromRGB(230, 178, 100)),
			Map.entry(Chicken.Variant.COLD, Color.fromRGB(116, 106, 125))
	);

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		var color = VARIANT_EDITOR_ITEM_COLORS.getOrDefault(this.getVariant(), Color.WHITE);
		ItemUtils.setLeatherColor(iconItem, color);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonChickenVariant,
				Messages.buttonChickenVariantLore
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
