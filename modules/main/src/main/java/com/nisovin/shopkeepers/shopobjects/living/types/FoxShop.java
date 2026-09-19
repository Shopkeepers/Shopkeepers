package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Fox;
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
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class FoxShop extends SittableShop<Fox> {

	public static final Property<Fox.Type> FOX_TYPE = new BasicProperty<Fox.Type>()
			.dataKeyAccessor("foxType", EnumSerializers.lenient(Fox.Type.class))
			.defaultValue(Fox.Type.RED)
			.build();

	public static final Property<Boolean> SLEEPING = new BasicProperty<Boolean>()
			.dataKeyAccessor("sleeping", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	public static final Property<Boolean> CROUCHING = new BasicProperty<Boolean>()
			.dataKeyAccessor("crouching", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Fox.Type> foxTypeProperty = new PropertyValue<>(FOX_TYPE)
			.onValueChanged(Unsafe.initialized(this)::applyFoxType)
			.build(properties);
	private final PropertyValue<Boolean> sleepingProperty = new PropertyValue<>(SLEEPING)
			.onValueChanged(Unsafe.initialized(this)::applySleeping)
			.build(properties);
	private final PropertyValue<Boolean> crouchingProperty = new PropertyValue<>(CROUCHING)
			.onValueChanged(Unsafe.initialized(this)::applyCrouching)
			.build(properties);

	public FoxShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<FoxShop> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		foxTypeProperty.load(shopObjectData);
		sleepingProperty.load(shopObjectData);
		crouchingProperty.load(shopObjectData);
		// Incompatible with each other:
		if (this.isSleeping() && this.isCrouching()) {
			this.setCrouching(false);
		}
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		foxTypeProperty.save(shopObjectData);
		sleepingProperty.save(shopObjectData);
		crouchingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyFoxType();
		this.applySleeping();
		this.applyCrouching();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getFoxTypeEditorButton());
		editorButtons.add(this.getSleepingEditorButton());
		editorButtons.add(this.getCrouchingEditorButton());
		return editorButtons;
	}

	// FOX TYPE

	public Fox.Type getFoxType() {
		return foxTypeProperty.getValue();
	}

	public void setFoxType(Fox.Type foxType) {
		foxTypeProperty.setValue(foxType);
	}

	public void cycleFoxType(boolean backwards) {
		this.setFoxType(EnumUtils.cycleEnumConstant(Fox.Type.class, this.getFoxType(), backwards));
	}

	private void applyFoxType() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setFoxType(this.getFoxType());
	}

	private ItemStack getFoxTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getFoxType()) {
		case SNOW:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case RED:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonFoxVariant,
				Messages.buttonFoxVariantLore
		);
		return iconItem;
	}

	private Button getFoxTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getFoxTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				boolean backwards = clickEvent.isRightClick();
				cycleFoxType(backwards);
				return true;
			}
		};
	}

	// SLEEPING

	public boolean isSleeping() {
		return sleepingProperty.getValue();
	}

	public void setSleeping(boolean sleeping) {
		sleepingProperty.setValue(sleeping);
		// Incompatible with crouching:
		if (sleeping && this.isCrouching()) {
			this.setCrouching(false);
		}
	}

	public void cycleSleeping() {
		this.setSleeping(!this.isSleeping());
	}

	private void applySleeping() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setSleeping(this.isSleeping());
	}

	private ItemStack getSleepingEditorItem() {
		Material iconItemType = this.isSleeping() ? Material.GREEN_BED : Material.RED_BED;
		ItemStack iconItem = new ItemStack(iconItemType);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonFoxSleeping,
				Messages.buttonFoxSleepingLore
		);
		return iconItem;
	}

	private Button getSleepingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getSleepingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleSleeping();
				this.updateAllIcons(); // Required if crouching got disabled
				return true;
			}

			@Override
			protected boolean isUpdateIconOnActionSuccess() {
				// The action already updates all icons.
				return false;
			}
		};
	}

	// CROUCHING

	public boolean isCrouching() {
		return crouchingProperty.getValue();
	}

	public void setCrouching(boolean crouching) {
		crouchingProperty.setValue(crouching);
		// Incompatible with sleeping:
		if (crouching && this.isSleeping()) {
			this.setSleeping(false);
		}
	}

	public void cycleCrouching() {
		this.setCrouching(!this.isCrouching());
	}

	private void applyCrouching() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setCrouching(this.isCrouching());
	}

	private ItemStack getCrouchingEditorItem() {
		Material iconItemType = this.isCrouching() ? Material.GREEN_CARPET : Material.RED_CARPET;
		ItemStack iconItem = new ItemStack(iconItemType);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonFoxCrouching,
				Messages.buttonFoxCrouchingLore
		);
		return iconItem;
	}

	private Button getCrouchingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getCrouchingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleCrouching();
				this.updateAllIcons(); // Required if sleeping got disabled
				return true;
			}

			@Override
			protected boolean isUpdateIconOnActionSuccess() {
				// The action already updates all icons.
				return false;
			}
		};
	}
}
