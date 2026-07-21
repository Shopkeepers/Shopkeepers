package com.nisovin.shopkeepers.api.shopkeeper.container;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information about a container that is used by a shopkeeper to store its stock and/or earnings in.
 * <p>
 * A shop container has its own world, which is usually, but not necessarily, the same as the
 * shopkeeper's world.
 * <p>
 * The container block does not necessarily have to be a chest, but could also be another type of
 * supported container. The container block might not exist in the world currently, e.g. if the
 * block was broken.
 * <p>
 * This may be a snapshot of a shop container's properties at a particular time. Do not assume that
 * this information is dynamically updated. For example, any dynamic changes to the container type
 * might not be reflected by this object.
 * <p>
 * Do not compare shop containers by their object identity. Instead, compare them by their location.
 */
public interface ShopContainer {

	/**
	 * Gets the name of the world the container is located in.
	 * 
	 * @return the container's world name, not <code>null</code> or empty
	 */
	public String getWorldName();

	/**
	 * Gets the container's x coordinate.
	 * 
	 * @return the container's x coordinate
	 */
	public int getX();

	/**
	 * Gets the container's y coordinate.
	 * 
	 * @return the container's y coordinate
	 */
	public int getY();

	/**
	 * Gets the container's z coordinate.
	 * 
	 * @return the container's z coordinate
	 */
	public int getZ();

	/**
	 * Gets the container's block.
	 * <p>
	 * The block might not actually be a valid container type currently (for example if something
	 * has broken or changed the type of the block in the meantime).
	 * 
	 * @return the container's block, or <code>null</code> if the container's world is not loaded
	 *         currently
	 */
	public @Nullable Block getBlock();

	/**
	 * Gets the {@link Inventory} of this container's block.
	 * 
	 * @return the container inventory, or <code>null</code> if the block is missing or not a
	 *         supported container currently
	 */
	public @Nullable Inventory getInventory();

	/**
	 * Attempts to open the container inventory for the specified player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if the inventory view was successfully opened
	 */
	public boolean openInventoryView(Player player);

	/**
	 * Gets the {@link ShopContainerType type} of this container.
	 * 
	 * @return the container type, not <code>null</code>
	 */
	public ShopContainerType getType();
}
