package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.ChestedHorse;
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
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ChestedHorseShop<E extends ChestedHorse> extends AbstractHorseShop<E> {

	public static final Property<Boolean> CARRYING_CHEST = new BasicProperty<Boolean>()
			.dataKeyAccessor("carryingChest", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> carryingChestProperty = new PropertyValue<>(CARRYING_CHEST)
			.onValueChanged(Unsafe.initialized(this)::applyCarryingChest)
			.build(properties);

	public ChestedHorseShop(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<? extends ChestedHorseShop<E>> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(context, shopObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		carryingChestProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		carryingChestProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyCarryingChest();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getCarryingChestEditorButton());
		return editorButtons;
	}

	// CARRYING CHEST

	public boolean isCarryingChest() {
		return carryingChestProperty.getValue();
	}

	public void setCarryingChest(boolean carryingChest) {
		carryingChestProperty.setValue(carryingChest);
	}

	public void cycleCarryingChest() {
		this.setCarryingChest(!this.isCarryingChest());
	}

	private void applyCarryingChest() {
		@Nullable E entity = this.getEntity();
		if (entity == null) return; // Not spawned

		entity.setCarryingChest(this.isCarryingChest());
	}

	private ItemStack getCarryingChestEditorItem() {
		ItemStack iconItem = new ItemStack(Material.CHEST);
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCarryingChest,
				Messages.buttonCarryingChestLore
		);
		return iconItem;
	}

	private Button getCarryingChestEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				return getCarryingChestEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				cycleCarryingChest();
				return true;
			}
		};
	}
}
