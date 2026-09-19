package com.nisovin.shopkeepers.shopobjects;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.ShopObjectRegistry;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractShopObjectType<T extends AbstractShopObject>
		extends AbstractSelectableType implements ShopObjectType<T> {

	private final Class<@NonNull T> shopObjectClass;

	protected AbstractShopObjectType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<@NonNull T> shopObjectClass
	) {
		super(identifier, aliases, permission);
		Validate.notNull(shopObjectClass, "shopObjectClass is null");
		this.shopObjectClass = shopObjectClass;
	}

	/**
	 * Gets the concrete and most specific class of the shop objects that are created by this
	 * {@link ShopObjectType}.
	 * 
	 * @return the concrete shop object class, not <code>null</code>
	 */
	public final Class<@NonNull T> getShopObjectClass() {
		return shopObjectClass;
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Messages.selectedShopObjectType,
				"type", this.getDisplayName()
		);
	}

	/**
	 * Whether the spawning and despawning of shop objects of this type is managed by the
	 * Shopkeepers plugin.
	 * <p>
	 * Some types of shop objects may handle the spawning and despawning of their shop objects
	 * themselves. If this returns <code>true</code>, the shop objects will be
	 * {@link AbstractShopObject#spawn() spawned} and {@link AbstractShopObject#despawn() despawned}
	 * by the Shopkeepers plugin in reaction to {@link ShopkeeperRegistry#getActiveChunks(String)
	 * chunk activations} and deactivations.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if the spawning and despawning of shop objects of this type is
	 *         managed by the Shopkeepers plugin
	 */
	public abstract boolean mustBeSpawned();

	/**
	 * Whether shop objects of this type shall be despawned right before world saves and respawned
	 * afterwards.
	 * <p>
	 * The return value of this method is only used if {@link #mustBeSpawned()} returns
	 * <code>true</code>.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if shop objects of this type get despawned during world saves
	 */
	public boolean mustDespawnDuringWorldSave() {
		return this.mustBeSpawned();
	}

	@Override
	public final boolean isValidSpawnLocation(
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		return this.validateSpawnLocation(null, spawnLocation, attachedBlockFace);
	}

	/**
	 * Checks if this type of shop object can be spawned at the specified location.
	 * <p>
	 * Unlike {@link #isValidSpawnLocation(Location, BlockFace)}, this may send feedback to the
	 * given player.
	 * 
	 * @param player
	 *            the player who is trying to place the shopkeeper, or <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param attachedBlockFace
	 *            the block face against which to spawn the object, or <code>null</code> if unknown
	 * @return <code>true</code> if the shop object can be spawned at the given location
	 */
	public boolean validateSpawnLocation(
			@Nullable Player player,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		// TODO Check the actual object size?
		if (spawnLocation == null || !spawnLocation.isWorldLoaded()) {
			if (player != null) {
				TextUtils.sendMessage(player, Messages.missingSpawnLocation);
			}
			return false;
		}
		return true;
	}

	/**
	 * Creates a shop object of this type.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param creationData
	 *            the used shop creation data, can be <code>null</code> (e.g. if the shopkeeper gets
	 *            loaded)
	 * @return the shop object
	 */
	public abstract @NonNull T createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	);

	/**
	 * Gets the {@link Shopkeeper} for the given object id that uses a {@link ShopObject} of this
	 * type.
	 * <p>
	 * This behaves like {@link ShopObjectRegistry#getShopkeeperByObjectId(Object)}, but
	 * additionally verifies that the found shopkeeper uses a shop object of this type.
	 * 
	 * @param objectId
	 *            the object id
	 * @return the shopkeeper, or <code>null</code> if no corresponding shopkeeper is found, or if
	 *         the found shopkeeper is not using this type of shop object
	 */
	protected final @Nullable AbstractShopkeeper getShopkeeperByObjectId(Object objectId) {
		ShopObjectRegistry shopObjectRegistry = SKShopkeepersPlugin.getInstance()
				.getShopkeeperRegistry()
				.getShopObjectRegistry();
		AbstractShopkeeper shopkeeper = shopObjectRegistry.getShopkeeperByObjectId(objectId);
		if (shopkeeper != null && shopkeeper.getShopObject().getType() == this) {
			return shopkeeper;
		} else {
			return null;
		}
	}
}
