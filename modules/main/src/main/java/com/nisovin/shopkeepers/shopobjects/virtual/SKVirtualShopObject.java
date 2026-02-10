package com.nisovin.shopkeepers.shopobjects.virtual;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;

// TODO Not yet used.
public class SKVirtualShopObject extends AbstractShopObject implements VirtualShopObject {

	protected final VirtualShops virtualShops;

	protected SKVirtualShopObject(
			VirtualShops virtualShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(shopkeeper, creationData);
		this.virtualShops = virtualShops;
	}

	@Override
	public SKVirtualShopObjectType getType() {
		return virtualShops.getSignShopObjectType();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
	}

	// ACTIVATION

	@Override
	public boolean isSpawned() {
		return false; // Virtual shops are not present in any world
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public @Nullable String getId() {
		return null;
	}

	@Override
	public boolean spawn() {
		return false;
	}

	@Override
	public void despawn() {
	}

	@Override
	public @Nullable Location getLocation() {
		return null;
	}

	@Override
	public CompletableFuture<Boolean> move() {
		return CompletableFuture.completedFuture(false);
	}

	// TICKING

	@Override
	public void onTick() {
		super.onTick();
	}

	@Override
	public @Nullable Location getTickVisualizationParticleLocation() {
		return null;
	}

	// NAMING

	@Override
	public void setName(@Nullable String name) {
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	// EDITOR ACTIONS

	@Override
	public List<Button> createEditorButtons() {
		return super.createEditorButtons();
	}
}
