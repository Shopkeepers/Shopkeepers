package com.nisovin.shopkeepers.shopobjects.virtual;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;

// TODO not yet used
public class SKVirtualShopObject extends AbstractShopObject implements VirtualShopObject {

	protected final VirtualShops virtualShops;

	protected SKVirtualShopObject(VirtualShops virtualShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.virtualShops = virtualShops;
	}

	@Override
	public SKVirtualShopObjectType getType() {
		return virtualShops.getSignShopObjectType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
	}

	// ACTIVATION

	@Override
	public boolean isActive() {
		return true; // always active
	}

	@Override
	public String getId() {
		return this.getType().createObjectId(shopkeeper);
	}

	@Override
	public boolean needsSpawning() {
		return false; // does not need to be spawned and despawned
	}

	@Override
	public boolean spawn() {
		return false;
	}

	@Override
	public void despawn() {
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public boolean check() {
		return false; // everything alright
	}

	@Override
	public void onShopkeeperOwnerChanged() {
	}

	// NAMING

	@Override
	public void setName(String name) {
	}

	@Override
	public String getName() {
		return null;
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<EditorHandler.Button>();
		editorButtons.addAll(super.getEditorButtons());
		return editorButtons;
	}
}
