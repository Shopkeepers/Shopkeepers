package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
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
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ArmorStandShop extends SKLivingShopObject<ArmorStand> {

	public static final Property<Boolean> BASE_PLATE = new BasicProperty<Boolean>()
			.dataKeyAccessor("basePlate", BooleanSerializers.STRICT)
			.useDefaultIfMissing()
			.defaultValue(true)
			.build();

	public static final Property<Boolean> SHOW_ARMS = new BasicProperty<Boolean>()
			.dataKeyAccessor("showArms", BooleanSerializers.STRICT)
			.useDefaultIfMissing()
			.defaultValue(false)
			.build();

	public static final Property<Boolean> SMALL = new BasicProperty<Boolean>()
			.dataKeyAccessor("small", BooleanSerializers.STRICT)
			.useDefaultIfMissing()
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> basePlateProperty = new PropertyValue<>(BASE_PLATE)
			.onValueChanged(Unsafe.initialized(this)::applyBasePlate)
			.build(properties);

	private final PropertyValue<Boolean> showArmsProperty = new PropertyValue<>(SHOW_ARMS)
			.onValueChanged(Unsafe.initialized(this)::applyShowArms)
			.build(properties);

	private final PropertyValue<Boolean> smallProperty = new PropertyValue<>(SMALL)
			.onValueChanged(Unsafe.initialized(this)::applySmall)
			.build(properties);

	public ArmorStandShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<ArmorStandShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		basePlateProperty.load(shopObjectData);
		showArmsProperty.load(shopObjectData);
		smallProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		basePlateProperty.save(shopObjectData);
		showArmsProperty.save(shopObjectData);
		smallProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyBasePlate();
		this.applyShowArms();
		this.applySmall();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getBasePlateEditorButton());
		editorButtons.add(this.getShowArmsEditorButton());
		editorButtons.add(this.getSmallEditorButton());
		return editorButtons;
	}

	// BASE PLATE

	public boolean hasBasePlate() {
		return basePlateProperty.getValue();
	}

	public void setBasePlate(boolean basePlate) {
		basePlateProperty.setValue(basePlate);
	}

	public void cycleBasePlate(boolean backwards) {
		this.setBasePlate(!this.hasBasePlate());
	}

	private void applyBasePlate() {
		ArmorStand entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setBasePlate(this.hasBasePlate());
	}

	private ItemStack getBasePlateEditorItem() {
		ItemStack iconItem = new ItemStack(this.hasBasePlate()
				? Material.HEAVY_WEIGHTED_PRESSURE_PLATE
				: Material.STONE_PRESSURE_PLATE
		);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonArmorStandBasePlate,
				Messages.buttonArmorStandBasePlateLore
		);
		return iconItem;
	}

	private Button getBasePlateEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getBasePlateEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleBasePlate(backwards);
				return true;
			}
		};
	}

	// SHOW ARMS

	public boolean isShowArms() {
		return showArmsProperty.getValue();
	}

	public void setShowArms(boolean showArms) {
		showArmsProperty.setValue(showArms);
	}

	public void cycleShowArms(boolean backwards) {
		this.setShowArms(!this.isShowArms());
	}

	private void applyShowArms() {
		ArmorStand entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setArms(this.isShowArms());
	}

	private ItemStack getShowArmsEditorItem() {
		ItemStack iconItem = new ItemStack(this.isShowArms()
				? Material.WOODEN_SWORD
				: Material.STICK
		);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonArmorStandShowArms,
				Messages.buttonArmorStandShowArmsLore
		);
		return iconItem;
	}

	private Button getShowArmsEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getShowArmsEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleShowArms(backwards);
				return true;
			}
		};
	}

	// SMALL

	public boolean isSmall() {
		return smallProperty.getValue();
	}

	public void setSmall(boolean small) {
		smallProperty.setValue(small);
	}

	public void cycleSmall(boolean backwards) {
		this.setSmall(!this.isSmall());
	}

	private void applySmall() {
		ArmorStand entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setSmall(this.isSmall());
	}

	private ItemStack getSmallEditorItem() {
		ItemStack iconItem = new ItemStack(this.isSmall()
				? Material.OAK_SLAB
				: Material.OAK_PLANKS
		);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonArmorStandSmall,
				Messages.buttonArmorStandSmallLore
		);
		return iconItem;
	}

	private Button getSmallEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getSmallEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleSmall(backwards);
				return true;
			}
		};
	}
}
