package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class BlockUtils {

	/**
	 * Gets the block that the given chest block is connected to, i.e. the other half of the double
	 * chest that the given block is part of.
	 * <p>
	 * This is derived from the given block's block data alone: The returned block is not verified
	 * to actually be a chest that is mutually connected to the given block.
	 * 
	 * @param chestBlock
	 *            the chest block, not <code>null</code>
	 * @return the connected block, or <code>null</code> if the given block is not a chest, or if it
	 *         is not connected to another chest
	 */
	public static @Nullable Block getConnectedChestBlock(Block chestBlock) {
		Validate.notNull(chestBlock, "chestBlock is null");
		if (!ItemUtils.isChest(chestBlock.getType())) {
			return null;
		}

		Chest chestData = (Chest) chestBlock.getBlockData();
		BlockFace connectedFace = getConnectedChestBlockFace(chestData);
		if (connectedFace == null) {
			return null;
		}

		// Note: We still return the relative block even in the case of block data inconsistency,
		// i.e. if the relative block is missing, is not a chest, or is not mutually connected.
		return chestBlock.getRelative(connectedFace);
	}

	private static @Nullable BlockFace getConnectedChestBlockFace(Chest chestData) {
		switch (chestData.getFacing()) {
		case NORTH:
			switch (chestData.getType()) {
			case RIGHT:
				return BlockFace.WEST;
			case LEFT:
				return BlockFace.EAST;
			default:
				return null; // Not connected
			}
		case EAST:
			switch (chestData.getType()) {
			case RIGHT:
				return BlockFace.NORTH;
			case LEFT:
				return BlockFace.SOUTH;
			default:
				return null; // Not connected
			}
		case SOUTH:
			switch (chestData.getType()) {
			case RIGHT:
				return BlockFace.EAST;
			case LEFT:
				return BlockFace.WEST;
			default:
				return null; // Not connected
			}
		case WEST:
			switch (chestData.getType()) {
			case RIGHT:
				return BlockFace.SOUTH;
			case LEFT:
				return BlockFace.NORTH;
			default:
				return null; // Not connected
			}
		default:
			return null; // Invalid chest facing
		}
	}

	private BlockUtils() {
	}
}
