package com.nisovin.shopkeepers.container.protection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.BlockUtils;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Handles container protection.
 * <p>
 * <b>Protected containers:</b><br>
 * A container directly used by a player shopkeeper is 'directly protected'. Any adjacent chests (N,
 * W, S, E) that form a double chest with a directly protected chest are protected as well.
 * <p>
 * <b>Bypass:</b><br>
 * The owner and members of a shop corresponding to a protected container, and players with the
 * bypass permission, are not affected by the listed protections. Also, certain protections can be
 * disabled via config settings.
 * <p>
 * <b>Hired shopkeepers:</b><br>
 * For hired shopkeepers, i.e. shops that retain a hire cost item, the shop members can access the
 * container, but are otherwise affected by the same protections as other players. I.e. they cannot
 * destroy or build next to the container of their hired shopkeeper.
 * <p>
 * <b>Protections:</b><br>
 * <ul>
 * <li>Protected containers cannot be accessed.
 * <li>Protected containers cannot be broken or destroyed by explosions.
 * <li>Protected containers don't transfer items, e.g. into or from hoppers, droppers, etc.
 * <li>Chests cannot be placed adjacent to a directly protected chest, if they would connect to it
 * and form a double chest.
 * <li>Hoppers and droppers cannot be placed adjacent to a protected container, if they would be
 * able to receive or inject items.
 * <li>Rails cannot be placed below a protected container (to prevent hopper carts stealing items).
 * </ul>
 * Note that there is no protection for the following cases:
 * <ul>
 * <li>Even though rails cannot be placed below protected containers, hopper carts are still able to
 * be placed / pushed to end up below protected containers. The item movement protection will
 * however still prevent items from being extracted from the container.
 * <li>Adjacent unconnected chests are not protected. It should however not be possible to access
 * any protected adjacent chest by that.
 * <li>Adjacent or chains of droppers and hoppers are not protected. So if item movement is enabled
 * in the config and the shop owner places those connected to his shop container, other players will
 * be able to access (or break) them.
 * </ul>
 */
public class ProtectedContainers {

	// Does not need to be reset after every use.
	private static final MutableBlockLocation sharedBlockLocation = new MutableBlockLocation();

	private final SKShopkeepersPlugin plugin;
	private final ContainerProtectionListener containerProtectionListener = new ContainerProtectionListener(Unsafe.initialized(this));
	private final InventoryMoveItemListener inventoryMoveItemListener = new InventoryMoveItemListener(Unsafe.initialized(this));
	private final Map<BlockLocation, List<AbstractPlayerShopkeeper>> protectedContainers = new HashMap<>();

	public ProtectedContainers(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		if (Settings.protectContainers) {
			Bukkit.getPluginManager().registerEvents(containerProtectionListener, plugin);
			if (Settings.preventItemMovement) {
				Bukkit.getPluginManager().registerEvents(inventoryMoveItemListener, plugin);
			}
		}
	}

	public void disable() {
		// Cleanup:
		HandlerList.unregisterAll(containerProtectionListener);
		HandlerList.unregisterAll(inventoryMoveItemListener);
		protectedContainers.clear();
	}

	// Protected containers:

	private BlockLocation getSharedKey(String worldName, int x, int y, int z) {
		sharedBlockLocation.set(worldName, x, y, z);
		return sharedBlockLocation;
	}

	public void addContainer(BlockLocation location, AbstractPlayerShopkeeper shopkeeper) {
		Validate.notNull(location, "location is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");
		List<AbstractPlayerShopkeeper> shopkeepers = protectedContainers.computeIfAbsent(
				location.immutable(),
				// There is usually at most one shopkeeper using the same container:
				key -> new ArrayList<>(1)
		);
		assert shopkeepers != null;
		shopkeepers.add(shopkeeper);
	}

	public void removeContainer(BlockLocation location, AbstractPlayerShopkeeper shopkeeper) {
		Validate.notNull(location, "location is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");
		// This operation either updates the value inside the Map, or removes it. It does not insert
		// a new entry for the passed key. We can therefore safely use the given location, without
		// first creating an immutable copy of it.
		protectedContainers.computeIfPresent(location, (key, shopkeepers) -> {
			shopkeepers.remove(shopkeeper);
			if (shopkeepers.isEmpty()) {
				// TODO Requires uncheckedNull due to Checker Framework limitation
				return Unsafe.uncheckedNull(); // Removes the mapping
			} else {
				return shopkeepers; // Keeps the updated mapping
			}
		});
	}

	// Gets the shopkeepers that are directly using the container at the specified location:
	private @Nullable List<? extends AbstractPlayerShopkeeper> _getShopkeepers(
			String worldName,
			int x,
			int y,
			int z
	) {
		BlockLocation key = this.getSharedKey(worldName, x, y, z);
		return protectedContainers.get(key);
	}

	// Gets the shopkeepers that are directly using the specified container block:
	private @Nullable List<? extends AbstractPlayerShopkeeper> _getShopkeepers(Block block) {
		return this._getShopkeepers(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	// Gets the shopkeepers that are directly using the container at the specified location:
	public List<? extends PlayerShopkeeper> getShopkeepers(
			String worldName,
			int x,
			int y,
			int z
	) {
		List<? extends PlayerShopkeeper> shopkeepers = this._getShopkeepers(worldName, x, y, z);
		if (shopkeepers != null) {
			return Collections.unmodifiableList(shopkeepers);
		} else {
			return Collections.emptyList();
		}
	}

	// Gets the shopkeepers that are directly using the specified container block:
	public List<? extends PlayerShopkeeper> getShopkeepers(Block block) {
		return this.getShopkeepers(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	// Gets the shopkeepers that use the container at the given location (directly or by a connected
	// chest):
	public List<? extends PlayerShopkeeper> getShopkeepersUsingContainer(Block containerBlock) {
		return this.getShopkeepersUsingContainer(containerBlock, new ArrayList<>());
	}

	// Gets the shopkeepers that use the container at the given location (directly or by a connected
	// chest), and adds them to the provided list:
	private List<? extends AbstractPlayerShopkeeper> getShopkeepersUsingContainer(
			Block containerBlock,
			List<AbstractPlayerShopkeeper> results
	) {
		Validate.notNull(containerBlock, "containerBlock is null!");
		Validate.notNull(results, "results is null!");

		// Check if the block is directly used by shopkeepers:
		List<? extends AbstractPlayerShopkeeper> shopkeepers = this._getShopkeepers(containerBlock);
		if (shopkeepers != null) {
			assert !shopkeepers.isEmpty();
			results.addAll(shopkeepers);
		}

		// If the block is a chest, check for a connected chest:
		@Nullable Block connectedChest = BlockUtils.getConnectedChestBlock(containerBlock);
		if (connectedChest != null) {
			// Note: In case of block data inconsistency (i.e. connected chest missing or not
			// mutually connected), we consider the block to be connected (and by that protected)
			// anyway, because such inconsistencies might also occur during the handling of block
			// placements.
			// Minecraft determines double chests by this consistency criteria: Same chest type,
			// same facing, opposite chest type (opposite connected block faces).
			shopkeepers = this._getShopkeepers(connectedChest);
			if (shopkeepers != null) {
				results.addAll(shopkeepers);
			}
		}

		return results;
	}

	// Container protection checks:

	// Gets reused by the container protection checks:
	private final List<AbstractPlayerShopkeeper> tempResultsList = new ArrayList<>();

	/**
	 * Checks if the given container block is protected.
	 * <p>
	 * The block is protected if either:
	 * <ul>
	 * <li>The container block is directly used by a shopkeeper.
	 * <li>The block is a chest that is connected to another chest block (forms a double chest)
	 * which is directly used by a shopkeeper.
	 * </ul>
	 * <p>
	 * Optionally, this takes shop container access for the specified player into account.
	 * 
	 * @param containerBlock
	 *            the container block (the block might not actually be a container anymore though)
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for
	 *            protection without taking shop container access into account
	 * @return <code>true</code> if the block is protected
	 */
	public boolean isContainerProtected(Block containerBlock, @Nullable Player player) {
		return !this.checkProtection(containerBlock, player, ContainerProtectionCheck.DEFAULT)
				.isAllowed();
	}

	// Does not check whether the given block actually is a supported type of container.
	private ContainerProtectionResult checkProtection(
			Block containerBlock,
			@Nullable Player player,
			ContainerProtectionCheck check
	) {
		Validate.notNull(containerBlock, "containerBlock is null!");
		Validate.notNull(check, "check is null");

		this.getShopkeepersUsingContainer(containerBlock, tempResultsList);
		ContainerProtectionResult result = this.checkProtection(tempResultsList, player, check);

		// Cleanup temporary results list:
		tempResultsList.clear();
		return result;
	}

	// shopkeepers: The shopkeepers that use the container.
	private ContainerProtectionResult checkProtection(
			List<? extends AbstractPlayerShopkeeper> shopkeepers,
			@Nullable Player player,
			ContainerProtectionCheck check
	) {
		if (shopkeepers.isEmpty()) {
			// No protection found:
			return ContainerProtectionResult.ALLOWED;
		}

		// Protection found. Check if the player is affected by it:
		if (player == null) {
			return ContainerProtectionResult.PROTECTED;
		}

		// We always allow shop members to access their shop container (regardless of other
		// shopkeepers using the same container):
		boolean containerAccess = false;
		// For hireable shops, the shop members can access the container, but they are not allowed
		// to break the container or place blocks next to it (ContainerProtectionCheck.DEFAULT).
		boolean hireable = false;
		for (AbstractPlayerShopkeeper shopkeeper : shopkeepers) {
			if (!containerAccess
					&& shopkeeper.hasAccessLevel(player, DefaultPlayerShopAccessLevels.CONTAINER())) {
				containerAccess = true;
			}

			if (shopkeeper.isHireable()) {
				hireable = true;
			}
		}

		if (containerAccess && (check == ContainerProtectionCheck.ACCESS || !hireable)) {
			return ContainerProtectionResult.ALLOWED;
		}

		// The bypass permission bypasses both kinds of protection:
		if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			return ContainerProtectionResult.ALLOWED;
		}

		// The player would usually have access to the shop, but fails the protection check because
		// we are not checking for access-only and the shop is hireable. -> PROTECTED_HIREABLE.
		return containerAccess
				? ContainerProtectionResult.PROTECTED_HIREABLE
				: ContainerProtectionResult.PROTECTED;
	}

	/**
	 * Checks if the given block is a protected shop container.
	 * <p>
	 * This checks if the specified block actually is a supported type of shop container. This does
	 * not take any players into account, such as shop owners or players with the bypass permission.
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the block is a protected container
	 */
	public boolean isProtectedContainer(Block block) {
		return !this.checkContainerProtection(block, null).isAllowed();
	}

	/**
	 * Checks the container protection of the given block.
	 * <p>
	 * This checks if the specified block actually is a supported type of shop container currently,
	 * and optionally takes the shop container access of the specified player into account.
	 * 
	 * @param block
	 *            the block
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for
	 *            protection without taking shop container access into account
	 * @return the protection result, not <code>null</code>
	 * @see #checkContainerProtection(Block, Player, ContainerProtectionCheck)
	 */
	public ContainerProtectionResult checkContainerProtection(
			Block block,
			@Nullable Player player
	) {
		return this.checkContainerProtection(block, player, ContainerProtectionCheck.DEFAULT);
	}

	/**
	 * Checks the container protection of the given block.
	 * <p>
	 * This checks if the specified block actually is a supported type of shop container currently,
	 * and optionally takes the shop container access of the specified player into account.
	 * 
	 * @param block
	 *            the block
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for
	 *            protection without taking shop container access into account
	 * @param check
	 *            the kind of action to check the protection for
	 * @return the protection result, not <code>null</code>
	 */
	public ContainerProtectionResult checkContainerProtection(
			Block block,
			@Nullable Player player,
			ContainerProtectionCheck check
	) {
		Validate.notNull(block, "block is null");
		if (!ShopContainers.isSupportedContainer(block.getType())) {
			return ContainerProtectionResult.ALLOWED;
		}

		return this.checkProtection(block, player, check);
	}
}
