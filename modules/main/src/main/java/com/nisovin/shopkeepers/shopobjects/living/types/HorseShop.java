package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Horse;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopEquipment;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.compat.MC_1_21_11;
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

public class HorseShop extends AbstractHorseShop<Horse> {

	public enum HorseArmor {

		LEATHER(Material.LEATHER_HORSE_ARMOR),
		IRON(Material.IRON_HORSE_ARMOR),
		GOLD(Material.GOLDEN_HORSE_ARMOR),
		DIAMOND(Material.DIAMOND_HORSE_ARMOR),
		NETHERITE(MC_1_21_11.NETHERITE_HORSE_ARMOR);

		private final @Nullable Material material;

		private HorseArmor(@Nullable Material material) {
			this.material = material;
		}

		public @Nullable Material getMaterial() {
			return material;
		}

		public boolean isEnabled() {
			return material != null;
		}
	}

	public static final Property<Horse.Color> COLOR = new BasicProperty<Horse.Color>()
			.dataKeyAccessor("color", EnumSerializers.lenient(Horse.Color.class))
			.defaultValue(Horse.Color.BROWN)
			.build();

	public static final Property<Horse.Style> STYLE = new BasicProperty<Horse.Style>()
			.dataKeyAccessor("style", EnumSerializers.lenient(Horse.Style.class))
			.defaultValue(Horse.Style.NONE)
			.build();

	public static final Property<@Nullable HorseArmor> ARMOR = new BasicProperty<@Nullable HorseArmor>()
			.dataKeyAccessor("armor", EnumSerializers.lenient(HorseArmor.class))
			.nullable() // Null indicates 'no armor'
			.defaultValue(null)
			.build();

	private final PropertyValue<Horse.Color> colorProperty = new PropertyValue<>(COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyColor)
			.build(properties);
	private final PropertyValue<Horse.Style> styleProperty = new PropertyValue<>(STYLE)
			.onValueChanged(Unsafe.initialized(this)::applyStyle)
			.build(properties);
	private final PropertyValue<@Nullable HorseArmor> armorProperty = new PropertyValue<>(ARMOR)
			.onValueChanged(Unsafe.initialized(this)::applyArmor)
			.build(properties);

	public HorseShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<HorseShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		styleProperty.load(shopObjectData);
		armorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		colorProperty.save(shopObjectData);
		styleProperty.save(shopObjectData);
		armorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applyStyle();
		this.applyArmor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getStyleEditorButton());
		editorButtons.add(this.getArmorEditorButton());
		return editorButtons;
	}

	// EQUIPMENT

	@Override
	protected void onEquipmentChanged() {
		super.onEquipmentChanged();

		// If the body slot is now empty, apply the armor instead:
		this.applyArmor();
	}

	// COLOR

	public Horse.Color getColor() {
		return colorProperty.getValue();
	}

	public void setColor(Horse.Color color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(Horse.Color.class, this.getColor(), backwards));
	}

	private void applyColor() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getColor()) {
		case BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(31, 31, 31));
			break;
		case BROWN:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(54, 25, 8));
			break;
		case CHESTNUT:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(110, 59, 38));
			break;
		case CREAMY:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(98, 65, 28));
			break;
		case DARK_BROWN:
			ItemUtils.setLeatherColor(iconItem, Color.fromRGB(39, 21, 13));
			break;
		case GRAY:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case WHITE:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonHorseColor,
				Messages.buttonHorseColorLore
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

	// STYLE

	public Horse.Style getStyle() {
		return styleProperty.getValue();
	}

	public void setStyle(Horse.Style style) {
		styleProperty.setValue(style);
	}

	public void cycleStyle(boolean backwards) {
		this.setStyle(EnumUtils.cycleEnumConstant(Horse.Style.class, this.getStyle(), backwards));
	}

	private void applyStyle() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setStyle(this.getStyle());
	}

	private ItemStack getStyleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.WHITE_BANNER);
		BannerMeta itemMeta = Unsafe.castNonNull(iconItem.getItemMeta());
		itemMeta.addPattern(new Pattern(DyeColor.BROWN, PatternType.CURLY_BORDER));
		itemMeta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_BOTTOM));
		itemMeta.addPattern(new Pattern(DyeColor.BROWN, PatternType.TRIANGLES_TOP));
		iconItem.setItemMeta(itemMeta);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonHorseStyle,
				Messages.buttonHorseStyleLore
		);
		return iconItem;
	}

	private Button getStyleEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getStyleEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleStyle(backwards);
				return true;
			}
		};
	}

	// ARMOR

	public @Nullable HorseArmor getArmor() {
		return armorProperty.getValue();
	}

	public void setArmor(@Nullable HorseArmor armor) {
		armorProperty.setValue(armor);
	}

	public void cycleArmor(boolean backwards) {
		this.setArmor(
				EnumUtils.cycleEnumConstantNullable(
						HorseArmor.class,
						this.getArmor(),
						backwards,
						horseArmor -> horseArmor == null || horseArmor.isEnabled()
				)
		);
	}

	private void applyArmor() {
		Horse entity = this.getEntity();
		if (entity == null) return; // Not spawned

		// The armor uses the body equipment slot. If a non-empty equipment item is set, e.g. via
		// the equipment editor, the equipment takes precedence.
		if (EquipmentUtils.EQUIPMENT_SLOT_BODY.isPresent()) {
			LivingShopEquipment shopEquipment = this.getEquipment();
			@Nullable UnmodifiableItemStack bodyItem = shopEquipment.getItem(EquipmentUtils.EQUIPMENT_SLOT_BODY.get());
			if (!ItemUtils.isEmpty(bodyItem)) {
				return;
			}
		}

		HorseArmor armor = this.getArmor();
		var armorItem = (armor == null || !armor.isEnabled()) ? null
				: new ItemStack(Unsafe.assertNonNull(armor.getMaterial()));
		entity.getInventory().setArmor(armorItem);
	}

	private ItemStack getArmorEditorItem() {
		HorseArmor armor = this.getArmor();
		var iconType = armor == null || !armor.isEnabled() ? Material.BARRIER
				: Unsafe.assertNonNull(armor.getMaterial());
		ItemStack iconItem = new ItemStack(iconType);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonHorseArmor,
				Messages.buttonHorseArmorLore
		);
		return iconItem;
	}

	private Button getArmorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getArmorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleArmor(backwards);
				return true;
			}
		};
	}
}
