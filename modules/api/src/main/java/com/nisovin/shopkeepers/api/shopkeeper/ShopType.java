package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.types.SelectableType;

/**
 * Information and logic that is common to all shopkeepers of this shop type.
 * 
 * @param <T>
 *            the type of shopkeeper that is described by this shop type
 */
public interface ShopType<T extends Shopkeeper> extends SelectableType {

	// Override to enforce that each subtype actually specifies a non-default display name.
	@Override
	public abstract String getDisplayName();

	/**
	 * Gets a user-friendly one line description of this shop type.
	 *
	 * @return the description
	 */
	public String getDescription();

	/**
	 * Gets a user-friendly short (but possibly multi-line) description of how to set up this shop
	 * type after creation.
	 *
	 * @return the setup description
	 */
	public String getSetupDescription();

	/**
	 * Gets a user-friendly short (possibly multi-line) description of how to set up the trades for
	 * this shop type.
	 * 
	 * @return the trade setup description
	 */
	public List<? extends String> getTradeSetupDescription();

	/**
	 * Checks if this type of shopkeeper can be spawned at the specified location according to any
	 * shop type specific spawn location validation rules.
	 * <p>
	 * This only checks the validation rules of this shop type. Use
	 * {@link ShopObjectType#isValidSpawnLocation(Location, BlockFace)} to also check whether the
	 * shop object can be spawned at the location.
	 * <p>
	 * If a shopkeeper is specified, its current state is taken into account as well: For player
	 * shopkeepers, this for example checks that all of the shop's containers are within the
	 * configured maximum distance.
	 * 
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the block face against which to spawn the shop object, or <code>null</code> if
	 *            unknown
	 * @param shopkeeper
	 *            the shopkeeper for which the spawn location is checked, or <code>null</code> if
	 *            not available
	 * @return <code>true</code> if the shopkeeper can be spawned at the given location
	 * @throws IllegalArgumentException
	 *             if the given shopkeeper is not of this shop type
	 */
	public boolean isValidSpawnLocation(
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace,
			@Nullable Shopkeeper shopkeeper
	);
}
