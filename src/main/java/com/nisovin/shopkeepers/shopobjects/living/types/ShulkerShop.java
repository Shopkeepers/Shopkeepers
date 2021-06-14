package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;

public class ShulkerShop extends SKLivingShopObject<Shulker> {

	private final Property<DyeColor> colorProperty = new EnumProperty<DyeColor>(shopkeeper, DyeColor.class, "color", null) {
		@Override
		public boolean isNullable() {
			return true; // Null indicates default color
		}
	};

	public ShulkerShop(	LivingShops livingShops, SKLivingShopObjectType<ShulkerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		colorProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		colorProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Shulker entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		return editorButtons;
	}

	// COLOR

	public DyeColor getColor() {
		return colorProperty.getValue();
	}

	public void setColor(DyeColor color) {
		colorProperty.setValue(color);
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // Null if not spawned
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getColor(), backwards));
	}

	private void applyColor(Shulker entity) {
		if (entity == null) return;
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		DyeColor color = this.getColor();
		ItemStack iconItem;
		if (color == null) {
			iconItem = new ItemStack(Material.PURPUR_BLOCK);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(color));
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonShulkerColor, Messages.buttonShulkerColorLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleColor(backwards);
				return true;
			}
		};
	}

	// TODO Attached block face (added in late Bukkit 1.16.5)
	// (https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/1c0c52ae4faef8fa3261a9ad0a37cdb39a663237)
	// TODO Open state (Peek) (added in late Bukkit 1.16.5)
	// (https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/6d0965a550ac4c96b90f2d46cc63c7e5d580e8ad)
}
