package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerCreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.SKShopContainer;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.dependencies.towny.TownyDependency;
import com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.playershops.PlayerShopsLimit;
import com.nisovin.shopkeepers.shopcreation.ContainerSelection;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperCreation;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.interaction.InteractionUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractPlayerShopType<T extends AbstractPlayerShopkeeper>
		extends AbstractShopType<T> implements PlayerShopType<T> {

	protected AbstractPlayerShopType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<T> shopkeeperType
	) {
		super(identifier, aliases, permission, shopkeeperType);
	}

	@Override
	protected void validateCreationData(ShopCreationData shopCreationData) {
		super.validateCreationData(shopCreationData);
		Validate.isTrue(shopCreationData instanceof PlayerShopCreationData,
				() -> "shopCreationData is not of type " + PlayerShopCreationData.class.getName()
						+ ", but: " + shopCreationData.getClass().getName());
	}

	@Override
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData shopCreationData) {
		// Shop creation data gets validated before this is invoked:
		assert shopCreationData instanceof PlayerShopCreationData;
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;

		// The creator, not null here:
		Player creator = Unsafe.assertNonNull(shopCreationData.getCreator());

		// Validate the container block:
		Block containerBlock = playerShopCreationData.getShopContainer();
		if (!ShopContainers.isSupportedContainer(containerBlock.getType())) {
			// The block is not / no longer a supported type of container:
			if (ItemUtils.isContainer(containerBlock.getType())) {
				TextUtils.sendMessage(creator, Messages.unsupportedContainer);
			} else {
				TextUtils.sendMessage(creator, Messages.invalidContainer);
			}
			return false;
		}

		ShopkeeperCreation shopkeeperCreation = SKShopkeepersPlugin.getInstance().getShopkeeperCreation();
		ContainerSelection containerSelection = shopkeeperCreation.getContainerSelection();
		if (!containerSelection.validateContainer(creator, containerBlock)) {
			return false;
		}

		int maxShopsLimit = PlayerShopsLimit.getMaxShopsLimit(creator);
		// Call event:
		PlayerCreatePlayerShopkeeperEvent createEvent = new PlayerCreatePlayerShopkeeperEvent(
				shopCreationData,
				maxShopsLimit
		);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("PlayerShopkeeperCreateEvent was cancelled!");
			return false;
		} else {
			maxShopsLimit = createEvent.getMaxShopsLimit();
		}

		// Check the max shops limit:
		if (maxShopsLimit != Integer.MAX_VALUE) {
			ShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();
			int count = shopkeeperRegistry.getPlayerShopkeepersByOwner(creator.getUniqueId()).size();
			if (count >= maxShopsLimit) {
				TextUtils.sendMessage(creator, Messages.tooManyShops);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace blockFace,
			@Nullable ShopCreationData shopCreationData,
			@Nullable AbstractShopkeeper shopkeeper
	) {
		if (!super.validateSpawnLocation(player, spawnLocation, blockFace, shopCreationData, shopkeeper)) {
			return false;
		}

		if (spawnLocation == null) return true; // Nothing to validate

		// Check that all shop containers are within the configured maximum distance:
		// This also catches cases in which players try to place or move the shopkeeper to a world
		// different to its container(s).
		double maxContainerDistanceSq = Settings.maxContainerDistance * Settings.maxContainerDistance;
		if (shopCreationData != null) {
			// Check if the selected shop container is too far away:
			assert shopCreationData instanceof PlayerShopCreationData;
			PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;
			BlockLocation containerLocation = BlockLocation.of(playerShopCreationData.getShopContainer());
			if (containerLocation.getBlockCenterDistanceSquared(spawnLocation) > maxContainerDistanceSq) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.containerTooFarAway);
				}
				return false;
			}
		} else if (shopkeeper != null) {
			// All of the shop's containers must be within range of the new location:
			assert shopkeeper instanceof AbstractPlayerShopkeeper;
			AbstractPlayerShopkeeper playerShopkeeper = (AbstractPlayerShopkeeper) shopkeeper;
			for (ShopContainer container : playerShopkeeper.getContainers()) {
				BlockLocation containerLocation = ((SKShopContainer) container).getLocation();
				if (containerLocation.getBlockCenterDistanceSquared(spawnLocation) > maxContainerDistanceSq) {
					if (player != null) {
						TextUtils.sendMessage(player, Messages.containerTooFarAway);
					}
					return false;
				}
			}
		}

		// Check WorldGuard restrictions:
		if (Settings.enableWorldGuardRestrictions) {
			if (!WorldGuardDependency.isShopAllowed(player, spawnLocation)) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.restrictedArea);
				}
				return false;
			}
		}

		// Check Towny restrictions:
		if (Settings.enableTownyRestrictions) {
			if (!TownyDependency.isCommercialArea(spawnLocation)) {
				if (player != null) {
					TextUtils.sendMessage(player, Messages.restrictedArea);
				}
				return false;
			}
		}

		// Check if the player can interact with the spawn location block:
		if (Settings.checkSpawnLocationInteractionResult && player != null) {
			var spawnLocationBlock = spawnLocation.getBlock();
			if (!InteractionUtils.checkBlockInteract(player, spawnLocationBlock, true)) {
				TextUtils.sendMessage(player, Messages.restrictedArea);
				return false;
			}
		}

		return true;
	}
}
