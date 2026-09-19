package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.entity.Frog;
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

public class FrogShop extends BabyableShop<Frog> {

	public static final Property<Frog.Variant> VARIANT = new BasicProperty<Frog.Variant>()
			.dataKeyAccessor("frogVariant", KeyedSerializers.forRegistry(Frog.Variant.class))
			.defaultValue(Frog.Variant.TEMPERATE)
			.build();

	private final PropertyValue<Frog.Variant> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public FrogShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<FrogShop> shopObjectType,
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

	public Frog.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(Frog.Variant variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(RegistryUtils.cycleKeyed(Frog.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Frog entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setVariant(this.getVariant());
	}

	private static final Map<Frog.Variant, DyeColor> VARIANT_EDITOR_ITEM_COLORS = Map.ofEntries(
			Map.entry(Frog.Variant.TEMPERATE, DyeColor.ORANGE),
			Map.entry(Frog.Variant.WARM, DyeColor.LIGHT_GRAY),
			Map.entry(Frog.Variant.COLD, DyeColor.GREEN)
	);

	private ItemStack getVariantEditorItem() {
		var frogVariant = this.getVariant();
		var dyeColor = VARIANT_EDITOR_ITEM_COLORS.getOrDefault(frogVariant, DyeColor.PURPLE);
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(dyeColor));
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonFrogVariant,
				Messages.buttonFrogVariantLore
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
