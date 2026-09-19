package com.nisovin.shopkeepers.shopcreation;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperPlacement {

	private final ShopkeeperRegistry shopkeeperRegistry;

	public ShopkeeperPlacement(ShopkeeperRegistry shopkeeperRegistry) {
		Validate.notNull(shopkeeperRegistry, "shopkeeperRegistry is null");
		this.shopkeeperRegistry = shopkeeperRegistry;
	}

	public Location determineSpawnLocation(
			Player player,
			Block targetBlock,
			BlockFace targetBlockFace
	) {
		Validate.notNull(player, "player is null");
		Validate.notNull(targetBlock, "targetBlock is null");
		Validate.notNull(targetBlockFace, "targetBlockFace is null");
		// If the target block is passable (and not a liquid, which can only come up as target block
		// when we try to place the shopkeeper on top of water or lava), spawn there, otherwise
		// shift according to target block face:
		// TODO When placing sign shopkeepers it may make more sense to check isSolid here instead
		// of isPassable. At least in some cases, such as placing a sign onto another sign, so that
		// the sign placement is consistent with vanilla sign placement.
		Block spawnBlock;
		if (targetBlock.isPassable() && !targetBlock.isLiquid()) {
			spawnBlock = targetBlock;
		} else {
			spawnBlock = targetBlock.getRelative(targetBlockFace);
		}
		Location spawnLocation = LocationUtils.getBlockCenterLocation(spawnBlock);
		if (targetBlockFace.getModY() == 0 && targetBlockFace != BlockFace.SELF) {
			// Set the yaw of the spawn location to match the direction of the target block face:
			// This is for example required for wall sign shopkeepers, but also allows placing
			// living shopkeepers to be rotated precisely into a specific direction.
			spawnLocation.setYaw(BlockFaceUtils.getYaw(targetBlockFace));
		} else {
			// Face towards the player:
			spawnLocation.setDirection(player.getEyeLocation().subtract(spawnLocation).toVector());
		}
		return spawnLocation;
	}

	/**
	 * Validates the given spawn location.
	 * <p>
	 * If a player is specified, this also sends feedback to the given player.
	 * 
	 * @param player
	 *            the player who is trying to place the shopkeeper, or <code>null</code>
	 * @param shopType
	 *            the shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops, has to provide a
	 *            loaded world if not <code>null</code>
	 * @param blockFace
	 *            the block face, can be <code>null</code> for virtual shops or if not available
	 * @param shopCreationData
	 *            the {@link ShopCreationData} for which the spawn location is validated, or
	 *            <code>null</code> if not available
	 * @param shopkeeper
	 *            the shopkeeper for which the spawn location is validated, or <code>null</code> if
	 *            not available
	 * @return <code>true</code> if the spawn location is valid
	 */
	public boolean validateSpawnLocation(
			@Nullable Player player,
			AbstractShopType<?> shopType,
			AbstractShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace blockFace,
			@Nullable ShopCreationData shopCreationData,
			@Nullable Shopkeeper shopkeeper
	) {
		// Check shop-object type specific validation rules:
		// This is expected to also send feedback to the player if necessary.
		if (!shopObjectType.validateSpawnLocation(player, spawnLocation, blockFace)) {
			return false;
		}

		// Check if the location is already used by another shopkeeper:
		if (spawnLocation != null) {
			if (!shopkeeperRegistry.getShopkeepersAtLocation(spawnLocation).isEmpty()) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.locationAlreadyInUse);
				}
				return false;
			}
		}

		// Check shop-type specific validation rules:
		// This is expected to also send feedback to the player if necessary.
		boolean isSpawnLocationValid = shopType.validateSpawnLocation(
				player,
				spawnLocation,
				blockFace,
				shopCreationData,
				shopkeeper
		);
		if (!isSpawnLocationValid) {
			return false;
		}

		return true;
	}
}
