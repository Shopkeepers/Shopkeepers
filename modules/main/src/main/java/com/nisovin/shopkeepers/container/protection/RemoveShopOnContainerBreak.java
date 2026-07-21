package com.nisovin.shopkeepers.container.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

public class RemoveShopOnContainerBreak {

	private final SKShopkeepersPlugin plugin;
	private final ProtectedContainers protectedContainers;
	private final RemoveShopOnContainerBreakListener removeShopOnContainerBreakListener;

	public RemoveShopOnContainerBreak(
			SKShopkeepersPlugin plugin,
			ProtectedContainers protectedContainers
	) {
		this.plugin = plugin;
		this.protectedContainers = protectedContainers;
		removeShopOnContainerBreakListener = new RemoveShopOnContainerBreakListener(
				Unsafe.initialized(this)
		);
	}

	public void onEnable() {
		if (Settings.deleteShopkeeperOnBreakContainer) {
			Bukkit.getPluginManager().registerEvents(removeShopOnContainerBreakListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(removeShopOnContainerBreakListener);
	}

	// Handles the breakage of a single block (e.g. a BlockBreakEvent).
	// The specified block still exists in the world, but is expected to be about to be broken.
	// Does not check the delete-shopkeeper-on-break-container setting: This has to be checked by
	// callers beforehand.
	// Triggers a save if a shopkeeper was deleted.
	public void handleBlockBreakage(Block block) {
		if (!ShopContainers.isSupportedContainer(block.getType())) {
			return;
		}

		var shopkeepers = protectedContainers.getShopkeepers(block);
		if (shopkeepers.isEmpty()) return;

		boolean dirty = false;
		// Copy to deal with concurrent modifications:
		for (var shopkeeper : shopkeepers.toArray(new PlayerShopkeeper[0])) {
			assert shopkeeper != null;
			if (this.deleteShopIfContainersBroken(shopkeeper, block)) {
				dirty = true;
			}
		}

		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}

	// Handles the breakage of a collection of blocks (e.g. a BlockExplodeEvent).
	// The specified blocks still exist in the world, but are expected to be about to be broken.
	// Does not check the delete-shopkeeper-on-break-container setting: This has to be checked by
	// callers beforehand.
	// Triggers a save if a shopkeeper was deleted.
	public void handleBlocksBreakage(List<? extends Block> blockList) {
		boolean dirty = false;
		for (Block block : blockList) {
			// Note: We cannot simply call the single-block handleBlockBreakage method here, because
			// since shopkeepers can have multiple containers, and all of which might get broken by
			// the same the explosion, we must consider all broken blocks together when checking if
			// any containers remain for a shopkeeper.

			if (!ShopContainers.isSupportedContainer(block.getType())) continue;

			var shopkeepers = protectedContainers.getShopkeepers(block);
			if (shopkeepers.isEmpty()) continue;

			// Copy to deal with concurrent modifications:
			for (var shopkeeper : shopkeepers.toArray(new PlayerShopkeeper[0])) {
				assert shopkeeper != null;
				if (this.deleteShopIfContainersBroken(shopkeeper, blockList)) {
					dirty = true;
				}
			}
		}

		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}

	/**
	 * Deletes the shopkeeper (and drops the shop creation item, if enabled) if none of its
	 * containers are valid anymore.
	 * <p>
	 * This does not check the "delete-shopkeeper-on-break-container" setting: This has to be
	 * checked by callers beforehand.
	 * <p>
	 * This does not trigger the saving of the shopkeeper storage on its own.
	 * <p>
	 * This is for example used by the periodic shopkeeper container check, when no container blocks
	 * are currently being broken.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @return <code>true</code> if the shopkeeper was deleted
	 */
	public boolean deleteShopIfContainersBroken(PlayerShopkeeper shopkeeper) {
		if (!shopkeeper.isValid()) return false; // Already deleted
		if (this.hasValidContainer(shopkeeper)) {
			// Keep the shop: It still has a valid container.
			// Any broken containers remain in the shop's container list and are shown as "missing"
			// in the editor.
			return false;
		}

		this.deleteShop(shopkeeper, null);
		return true;
	}

	/**
	 * Deletes the shopkeeper (and drops the shop creation item, if enabled) if none of its
	 * containers, except the given broken block, are valid containers anymore.
	 * <p>
	 * The specified block might not be broken yet, but is expected to be about to break.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param brokenBlock
	 *            the (about to be) broken block
	 * @return <code>true</code> if the shopkeeper was deleted
	 */
	private boolean deleteShopIfContainersBroken(PlayerShopkeeper shopkeeper, Block brokenBlock) {
		if (!shopkeeper.isValid()) return false; // Already deleted

		if (this.hasValidContainerExcept(shopkeeper, brokenBlock)) {
			// Keep the shop: It still has a valid container.
			// Any broken containers remain in the shop's container list and are shown as "missing"
			// in the editor.
			return false;
		}

		this.deleteShop(shopkeeper, brokenBlock);
		return true;
	}

	/**
	 * Deletes the shopkeeper (and drops the shop creation item, if enabled) if none of its
	 * containers, except the given broken blocks, are valid containers anymore.
	 * <p>
	 * The specified blocks might not be broken yet, but are expected to be about to break.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param brokenBlocks
	 *            the (about to be) broken blocks
	 * @return <code>true</code> if the shopkeeper was deleted
	 */
	private boolean deleteShopIfContainersBroken(
			PlayerShopkeeper shopkeeper,
			@ReadOnly List<? extends Block> brokenBlocks
	) {
		if (!shopkeeper.isValid()) return false; // Already deleted

		// If the shop is subsequently deleted: Drop the shop creation item at any of the broken
		// blocks that match one of shop container locations (if available). Prefer broken blocks
		// that are actually container types, but fall back to any matching shop container location.
		@Nullable Block brokenContainerBlock = null;
		boolean brokenContainerBlockIsContainer = false;

		for (ShopContainer container : shopkeeper.getContainers()) {
			Block containerBlock = container.getBlock();
			if (containerBlock == null) continue;
			if (brokenBlocks.contains(containerBlock)) {
				// Ignore the container for the "has valid container"-check but remember the broken
				// block location if we subsequently need to delete the shop:
				if (brokenContainerBlock == null || !brokenContainerBlockIsContainer) {
					brokenContainerBlock = containerBlock;
					brokenContainerBlockIsContainer = ShopContainers.isSupportedContainer(containerBlock.getType());
				}
				continue;
			}

			if (ShopContainers.isSupportedContainer(containerBlock.getType())) {
				// Keep the shop: It still has a valid container.
				// Any broken containers remain in the shop's container list and are shown as
				// "missing" in the editor.
				return false;
			}
		}

		this.deleteShop(shopkeeper, brokenContainerBlock);
		return true;
	}

	/**
	 * Checks if the shopkeeper has at least one container that exists in the world.
	 * 
	 * @return <code>true</code> if the shopkeeper has at least one container that exists in the
	 *         world
	 */
	private boolean hasValidContainer(PlayerShopkeeper shopkeeper) {
		return this.hasValidContainerExcept(shopkeeper, (Block) null);
	}

	/**
	 * Checks if the shopkeeper has at least one container that exists in the world, but ignores the
	 * specified block.
	 * <p>
	 * This only excludes the specified block location itself. If the block is a half of a double
	 * chest and the other half is a container of this shopkeeper, this other half's shop container
	 * is not excluded from this check.
	 * 
	 * @param excludedBlock
	 *            the excluded block, or <code>null</code> to not exclude any block location
	 * @return <code>true</code> if the shopkeeper has at least one container (except at the
	 *         specified location) that exists in the world
	 */
	private boolean hasValidContainerExcept(
			PlayerShopkeeper shopkeeper,
			@Nullable Block excludedBlock
	) {
		for (ShopContainer container : shopkeeper.getContainers()) {
			Block containerBlock = container.getBlock();
			if (containerBlock == null) continue;
			if (excludedBlock != null && containerBlock.equals(excludedBlock)) continue;

			if (ShopContainers.isSupportedContainer(containerBlock.getType())) {
				return true;
			}
		}

		return false;
	}

	// brokenBlock: (One of) the last broken block(s) that caused the shopkeeper to be deleted, or
	// null if not available.
	private void deleteShop(PlayerShopkeeper shopkeeper, @Nullable Block brokenBlock) {
		assert shopkeeper.isValid();

		if (Settings.deletingPlayerShopReturnsCreationItem) {
			var dropLocation = this.getShopCreationItemDropLocation(shopkeeper, brokenBlock);
			if (dropLocation != null) {
				ItemStack shopCreationItem = ShopCreationItem.create();
				var world = Unsafe.assertNonNull(dropLocation.getWorld());
				world.dropItemNaturally(dropLocation, shopCreationItem);
			}
		}

		// Note: We do not pass the player responsible for breaking the container here, because we
		// cannot determine the player in all situations anyway (e.g. if a player indirectly breaks
		// the container by causing an explosion).
		shopkeeper.delete();
	}

	// The returned shop creation item is dropped at the location of the last broken container
	// block, or, if not available, at the shop object's / shopkeeper's location.
	private @Nullable Location getShopCreationItemDropLocation(
			PlayerShopkeeper shopkeeper,
			@Nullable Block brokenBlock
	) {
		if (brokenBlock != null) {
			return brokenBlock.getLocation();
		}

		var dropLocation = shopkeeper.getShopObject().getLocation();
		if (dropLocation != null) {
			return dropLocation;
		}

		return shopkeeper.getLocation(); // Can be null
	}
}
