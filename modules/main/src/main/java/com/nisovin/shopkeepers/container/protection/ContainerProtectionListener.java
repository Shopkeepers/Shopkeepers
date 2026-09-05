package com.nisovin.shopkeepers.container.protection;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Handles container protection. Can be disabled via a config setting.
 */
class ContainerProtectionListener implements Listener {

	private final ProtectedContainers protectedContainers;

	ContainerProtectionListener(ProtectedContainers protectedContainers) {
		this.protectedContainers = protectedContainers;
	}

	/**
	 * Prevents unauthorized opening of protected containers.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = Unsafe.assertNonNull(event.getClickedBlock());
		Player player = event.getPlayer();
		var protectionResult = protectedContainers.checkContainerProtection(
				block,
				player,
				ContainerProtectionCheck.ACCESS
		);
		if (!protectionResult.isAllowed()) {
			// TODO Always allow access to own shop containers, even if cancelled by other plugins?
			Log.debug(() -> "Cancelled container interaction by '" + player.getName() + "' at '"
					+ TextUtils.getLocationString(block) + "': Protected container.");
			TextUtils.sendMessage(player, Messages.cannotInteractShopContainer);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		var protectionResult = protectedContainers.checkContainerProtection(block, player);
		if (!protectionResult.isAllowed()) {
			Log.debug(() -> "Cancelled breaking of container block by '" + player.getName()
					+ "' at '" + TextUtils.getLocationString(block) + "': "
					+ getProtectionDescription(protectionResult) + ".");
			var message = (protectionResult == ContainerProtectionResult.PROTECTED_HIREABLE)
					? Messages.cannotBreakHiredShopContainer
					: Messages.cannotBreakShopContainer;
			TextUtils.sendMessage(player, message);

			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Material type = block.getType();

		if (ItemUtils.isChest(type)) {
			// Note: Unconnected chests can be placed.
			this.checkBlockPlacement(event, "(double) chest", block);
		} else if (type == Material.HOPPER) {
			// Prevent placement of hoppers that could be used to extract or inject items from/into
			// a protected container:
			this.checkBlockPlacement(
					event,
					"hopper",
					block.getRelative(BlockFace.UP),
					this.getFacedBlock(block)
			);
		} else if (type == Material.DROPPER) {
			// Prevent placement of droppers that could be used to inject items into a protected
			// container:
			this.checkBlockPlacement(event, "dropper", this.getFacedBlock(block));
		} else if (ItemUtils.isRail(type)) {
			this.checkBlockPlacement(event, "rail", block.getRelative(BlockFace.UP));
		}
	}

	// Cancels the block placement if any of the specified nearby container blocks is protected.
	// blockName: The name of the placed block, used for the debug output.
	private void checkBlockPlacement(
			BlockPlaceEvent event,
			String blockName,
			@NonNull Block... containerBlocks
	) {
		Player player = event.getPlayer();
		for (Block containerBlock : containerBlocks) {
			var protectionResult = protectedContainers.checkContainerProtection(containerBlock, player);
			if (protectionResult.isAllowed()) continue;

			Block block = event.getBlock();
			Log.debug(() -> "Cancelled placing of " + blockName + " block by '" + player.getName()
					+ "' at '" + TextUtils.getLocationString(block) + "': "
					+ getProtectionDescription(protectionResult) + " nearby.");
			var message = (protectionResult == ContainerProtectionResult.PROTECTED_HIREABLE)
					? Messages.cannotPlaceBlockNearHiredShopContainer
					: Messages.cannotPlaceBlockNearShopContainer;
			TextUtils.sendMessage(player, message);

			event.setCancelled(true);
			return;
		}
	}

	// The description of the given protection, used for the debug output.
	private static String getProtectionDescription(ContainerProtectionResult protectionResult) {
		if (protectionResult == ContainerProtectionResult.PROTECTED_HIREABLE) {
			return "Hireable shop container";
		} else {
			return "Protected container";
		}
	}

	private Block getFacedBlock(Block directionalBlock) {
		Directional directionalData = (Directional) directionalBlock.getBlockData();
		BlockFace facing = directionalData.getFacing();
		return directionalBlock.getRelative(facing);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		List<Block> blockList = event.blockList();
		this.removeProtectedChests(blockList);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		List<Block> blockList = event.blockList();
		this.removeProtectedChests(blockList);
	}

	// Block list has to be modifiable.
	private void removeProtectedChests(List<? extends Block> blockList) {
		blockList.removeIf(protectedContainers::isProtectedContainer);
	}
}
