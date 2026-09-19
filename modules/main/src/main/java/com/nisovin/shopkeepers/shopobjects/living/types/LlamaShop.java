package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Llama;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectCreationContext;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.EquipmentUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class LlamaShop<E extends Llama> extends ChestedHorseShop<E> {

	public static final Property<Llama.Color> COLOR = new BasicProperty<Llama.Color>()
			.dataKeyAccessor("color", EnumSerializers.lenient(Llama.Color.class))
			.defaultValue(Llama.Color.CREAMY)
			.build();

	public static final Property<@Nullable DyeColor> CARPET_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("carpetColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no carpet'
			.defaultValue(null)
			.build();

	private final PropertyValue<Llama.Color> colorProperty = new PropertyValue<>(COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyColor)
			.build(properties);
	private final PropertyValue<@Nullable DyeColor> carpetColorProperty = new PropertyValue<>(CARPET_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCarpetColor)
			.build(properties);

	public LlamaShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<? extends LlamaShop<E>> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		carpetColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		colorProperty.save(shopObjectData);
		carpetColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applyCarpetColor();

		E entity = Unsafe.assertNonNull(this.getEntity());
		// If this is a trader llama, marking it as 'tamed' disables its delayed despawning. The
		// 'tamed' state should have no visual effect on the llama entity.
		entity.setTamed(true);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getCarpetColorEditorButton());
		return editorButtons;
	}

	// EQUIPMENT

	@Override
	protected void onEquipmentChanged() {
		super.onEquipmentChanged();

		// If the body slot is now empty, apply the carpet instead:
		this.applyCarpetColor();
	}

	// COLOR

	public Llama.Color getColor() {
		return colorProperty.getValue();
	}

	public void setColor(Llama.Color color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Llama.Color.class, this.getColor(), backwards));
	}

	private void applyColor() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getColor()) {
		case BROWN:
			ItemUtils.setLeatherColor(iconItem, DyeColor.BROWN.getColor());
			break;
		case GRAY:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case CREAMY:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.ORANGE));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonLlamaVariant,
				Messages.buttonLlamaVariantLore
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

	// CARPET COLOR

	public @Nullable DyeColor getCarpetColor() {
		return carpetColorProperty.getValue();
	}

	public void setCarpetColor(@Nullable DyeColor carpetColor) {
		carpetColorProperty.setValue(carpetColor);
	}

	public void cycleCarpetColor(boolean backwards) {
		this.setCarpetColor(
				EnumUtils.cycleEnumConstantNullable(
						DyeColor.class,
						this.getCarpetColor(),
						backwards
				)
		);
	}

	private void applyCarpetColor() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return; // Not spawned

		// The carpet uses the body equipment slot. If a non-empty equipment item is set, e.g. via
		// the equipment editor, the equipment takes precedence.
		if (EquipmentUtils.EQUIPMENT_SLOT_BODY.isPresent()) {
			LivingShopEquipment shopEquipment = this.getEquipment();
			@Nullable UnmodifiableItemStack bodyItem = shopEquipment.getItem(EquipmentUtils.EQUIPMENT_SLOT_BODY.get());
			if (!ItemUtils.isEmpty(bodyItem)) {
				return;
			}
		}

		@Nullable ItemStack decor = null;
		DyeColor carpetColor = this.getCarpetColor();
		if (carpetColor != null) {
			decor = new ItemStack(ItemUtils.getCarpetType(carpetColor));
		}

		entity.getInventory().setDecor(decor);
	}

	private ItemStack getCarpetColorEditorItem() {
		DyeColor carpetColor = this.getCarpetColor();
		ItemStack iconItem;
		if (carpetColor != null) {
			iconItem = new ItemStack(ItemUtils.getCarpetType(carpetColor));
		} else {
			iconItem = new ItemStack(Material.BARRIER);
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonLlamaCarpetColor,
				Messages.buttonLlamaCarpetColorLore
		);
		return iconItem;
	}

	private Button getCarpetColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getCarpetColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleCarpetColor(backwards);
				return true;
			}
		};
	}
}
