package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ChestedHorseShop<E extends ChestedHorse> extends BabyableShop<E> {

	private final Property<Boolean> carryingChestProperty = new BooleanProperty(shopkeeper, "carryingChest", false);

	public ChestedHorseShop(LivingShops livingShops, SKLivingShopObjectType<? extends ChestedHorseShop<E>> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		carryingChestProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		carryingChestProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applyCarryingChest(entity);
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
		shopkeeper.markDirty();
		this.applyCarryingChest(this.getEntity()); // Null if not spawned
	}

	public void cycleCarryingChest() {
		this.setCarryingChest(!this.isCarryingChest());
	}

	private void applyCarryingChest(E entity) {
		if (entity == null) return;
		entity.setCarryingChest(this.isCarryingChest());
	}

	private ItemStack getCarryingChestEditorItem() {
		ItemStack iconItem = new ItemStack(Material.CHEST);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonCarryingChest, Messages.buttonCarryingChestLore);
		return iconItem;
	}

	private Button getCarryingChestEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getCarryingChestEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCarryingChest();
				return true;
			}
		};
	}
}
