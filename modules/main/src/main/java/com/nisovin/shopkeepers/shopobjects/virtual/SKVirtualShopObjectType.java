package com.nisovin.shopkeepers.shopobjects.virtual;

import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

// TODO Not yet used.
public final class SKVirtualShopObjectType extends AbstractShopObjectType<SKVirtualShopObject>
		implements VirtualShopObjectType<SKVirtualShopObject> {

	private final VirtualShops virtualShops;

	public SKVirtualShopObjectType(VirtualShops virtualShops) {
		super("virtual", Collections.emptyList(), "shopkeeper.virtual", SKVirtualShopObject.class);
		this.virtualShops = virtualShops;
	}

	@Override
	public boolean isEnabled() {
		return false; // TODO Add setting
	}

	@Override
	public String getDisplayName() {
		return "virtual"; // TODO Message setting
	}

	@Override
	public boolean mustBeSpawned() {
		return false; // Does not need to be spawned and despawned
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		return true; // Does not use any spawn location
	}

	@Override
	public SKVirtualShopObject createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		return new SKVirtualShopObject(virtualShops, shopkeeper, creationData);
	}
}
