package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Zombie;
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

// TODO Use BabyableShop as base once there is a common interface for this inside Bukkit.
public class ZombieShop<E extends Zombie> extends SKLivingShopObject<E> {

	public static final Property<Boolean> BABY = new BasicProperty<Boolean>()
			.dataKeyAccessor("baby", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> babyProperty = new PropertyValue<>(BABY)
			.onValueChanged(Unsafe.initialized(this)::applyBaby)
			.build(properties);

	public ZombieShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<? extends ZombieShop<E>> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		babyProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		babyProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyBaby();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getBabyEditorButton());
		return editorButtons;
	}

	// BABY

	public boolean isBaby() {
		return babyProperty.getValue();
	}

	public void setBaby(boolean baby) {
		babyProperty.setValue(baby);
	}

	public void cycleBaby() {
		this.setBaby(!this.isBaby());
	}

	private void applyBaby() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return; // Not spawned

		if (this.isBaby()) {
			entity.setBaby();
		} else {
			entity.setAdult();
			// TODO: MC-9568: Growing up mobs get moved. Might be fixed.
			this.teleportBack();
		}
	}

	private ItemStack getBabyEditorItem() {
		ItemStack iconItem = new ItemStack(Material.EGG);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonBaby, Messages.buttonBabyLore);
		return iconItem;
	}

	private Button getBabyEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getBabyEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleBaby();
				return true;
			}
		};
	}
}
