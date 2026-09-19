package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.sign.HangingSignShopObjectType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.block.base.BaseBlockShops;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public final class SKHangingSignShopObjectType
		extends BaseBlockShopObjectType<SKHangingSignShopObject>
		implements HangingSignShopObjectType<SKHangingSignShopObject> {

	private final BaseBlockShops blockShops;

	public SKHangingSignShopObjectType(BaseBlockShops blockShops) {
		super(
				"hanging-sign",
				Collections.emptyList(),
				"shopkeeper.hanging-sign",
				SKHangingSignShopObject.class
		);
		this.blockShops = blockShops;
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableHangingSignShops;
	}

	@Override
	public String getDisplayName() {
		return Messages.shopObjectTypeHangingSign;
	}

	@Override
	public boolean mustBeSpawned() {
		return true; // Despawn signs on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		if (!super.validateSpawnLocation(player, spawnLocation, attachedBlockFace)) {
			return false;
		}
		assert spawnLocation != null;

		// Block has to be empty:
		Block spawnBlock = spawnLocation.getBlock();
		if (!spawnBlock.isEmpty()) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.spawnBlockNotEmpty);
			}
			return false;
		}

		// UP: Hanging sign
		// Block side: Hanging wall sign
		if (attachedBlockFace == BlockFace.UP
				|| (attachedBlockFace != null && !BlockFaceUtils.isBlockSide(attachedBlockFace))) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.invalidSpawnBlockFace);
			}
			return false;
		}

		return true;
	}

	@Override
	public SKHangingSignShopObject createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		return new SKHangingSignShopObject(blockShops, shopkeeper, creationData);
	}
}
