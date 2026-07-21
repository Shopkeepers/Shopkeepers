package com.nisovin.shopkeepers.shopcreation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.interaction.InteractionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ContainerSelection {

	private static final int MAX_TRACKED_CONTAINERS = 5;

	// This BlockLocation object is reused for lookups. It does not need to be reset after every
	// use.
	private static final MutableBlockLocation sharedBlockLocation = new MutableBlockLocation();

	private final ShopkeepersPlugin plugin;
	private final ProtectedContainers protectedContainers;

	private final RecentlyPlacedContainersListener containersListener = new RecentlyPlacedContainersListener(
			Unsafe.initialized(this)
	);

	// By player id:
	private final Map<UUID, Deque<BlockLocation>> recentlyPlacedContainers = new HashMap<>();
	private final Map<UUID, Block> selectedContainer = new HashMap<>();

	public ContainerSelection(ShopkeepersPlugin plugin, ProtectedContainers protectedContainers) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(protectedContainers, "protectedContainers is null");
		this.plugin = plugin;
		this.protectedContainers = protectedContainers;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(containersListener, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(containersListener);
		selectedContainer.clear();
		// Note: The recently placed containers are not cleared here in order to persist them across
		// plugin reloads.
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		UUID playerId = player.getUniqueId();
		selectedContainer.remove(playerId);
		recentlyPlacedContainers.remove(playerId);
	}

	private BlockLocation getSharedKey(Block block) {
		sharedBlockLocation.set(block);
		return sharedBlockLocation;
	}

	public void addRecentlyPlacedContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		UUID playerId = player.getUniqueId();
		Deque<BlockLocation> recentlyPlaced = recentlyPlacedContainers.computeIfAbsent(
				playerId,
				key -> new ArrayDeque<>(MAX_TRACKED_CONTAINERS + 1)
		);
		assert recentlyPlaced != null;
		if (recentlyPlaced.size() == MAX_TRACKED_CONTAINERS) {
			recentlyPlaced.removeFirst();
		}
		recentlyPlaced.addLast(BlockLocation.of(container));
	}

	public boolean isRecentlyPlacedContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		UUID playerId = player.getUniqueId();
		@Nullable Deque<BlockLocation> recentlyPlaced = recentlyPlacedContainers.get(playerId);
		if (recentlyPlaced == null) return false;

		BlockLocation containerLocation = this.getSharedKey(container);
		return recentlyPlaced.contains(containerLocation);
	}

	public void selectContainer(Player player, @Nullable Block container) {
		Validate.notNull(player, "player is null");
		UUID playerId = player.getUniqueId();
		if (container == null) {
			selectedContainer.remove(playerId);
		} else {
			// This is validated once the container is actually used:
			assert ShopContainers.isSupportedContainer(container.getType());
			selectedContainer.put(playerId, container);
		}
	}

	public @Nullable Block getSelectedContainer(Player player) {
		Validate.notNull(player, "player is null");
		UUID playerId = player.getUniqueId();
		return selectedContainer.get(playerId);
	}

	// Checks if the player can use the given container for a player shopkeeper:
	public boolean validateContainer(Player player, Block containerBlock) {
		Validate.notNull(player, "player is null");
		Validate.notNull(containerBlock, "containerBlock is null");
		// Check if the container is already used by some other shopkeeper:
		if (protectedContainers.isContainerProtected(containerBlock, null)) {
			TextUtils.sendMessage(player, Messages.containerAlreadyInUse);
			return false;
		}

		// Check for recently placed:
		if (Settings.requireContainerRecentlyPlaced
				&& !this.isRecentlyPlacedContainer(player, containerBlock)) {
			// Container was not recently placed:
			TextUtils.sendMessage(player, Messages.containerNotPlaced);
			return false;
		}

		// Check if the player can access the container by triggering a dummy interact event:
		if (!InteractionUtils.checkBlockInteract(player, containerBlock, false)) {
			TextUtils.sendMessage(player, Messages.noContainerAccess);
			return false;
		}

		return true;
	}
}
