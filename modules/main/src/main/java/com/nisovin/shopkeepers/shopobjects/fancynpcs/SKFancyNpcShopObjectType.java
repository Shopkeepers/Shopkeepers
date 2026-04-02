package com.nisovin.shopkeepers.shopobjects.fancynpcs;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;

public final class SKFancyNpcShopObjectType
		extends AbstractEntityShopObjectType<SKFancyNpcShopObject> {

	private final FancyNpcsShops fancyNpcsShops;

	public SKFancyNpcShopObjectType(FancyNpcsShops fancyNpcsShops) {
		super("fancynpc", Arrays.asList("npc-fancy"), "shopkeeper.fancynpc", SKFancyNpcShopObject.class);
		this.fancyNpcsShops = fancyNpcsShops;
	}

	@Override
	public boolean isEnabled() {
		return fancyNpcsShops.isEnabled();
	}

	@Override
	public String getDisplayName() {
		return "FancyNpc";
	}

	@Override
	public boolean mustBeSpawned() {
		return false; // Spawning and despawning is handled by FancyNpcs.
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player creator,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		if (!super.validateSpawnLocation(creator, spawnLocation, attachedBlockFace)) {
			return false;
		}
		return true;
	}

	@Override
	public SKFancyNpcShopObject createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		return new SKFancyNpcShopObject(fancyNpcsShops, shopkeeper, creationData);
	}
}
