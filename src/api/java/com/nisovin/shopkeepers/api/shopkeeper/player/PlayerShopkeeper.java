package com.nisovin.shopkeepers.api.shopkeeper.player;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.user.User;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest and will deposit earnings
 * back into that chest.
 */
public interface PlayerShopkeeper extends Shopkeeper {

	// TODO check all reference since this could previously return null if the player is not online
	/**
	 * Gets the owner of this shop.
	 * 
	 * @return the owner, not <code>null</code>
	 */
	public User getOwner();

	/**
	 * Sets the owner of this shop.
	 * 
	 * @param newOwner
	 *            the new owner, not <code>null</code>
	 */
	public void setOwner(User newOwner);

	/**
	 * Checks if the player with the specified unique id owns this shop.
	 * 
	 * @param playerId
	 *            the player's unique id
	 * @return <code>true</code> if the player owns this shop
	 */
	public boolean isOwner(UUID playerId);

	/**
	 * Checks if the player specified by the given {@link User} owns this shop.
	 * 
	 * @param user
	 *            the user
	 * @return <code>true</code> if the player owns this shop
	 */
	public boolean isOwner(User user);

	/**
	 * Checks if the given player owns this shop.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the player owns this shop
	 */
	public boolean isOwner(OfflinePlayer player);

	/**
	 * Gets a String representation of the owner.
	 * <p>
	 * This contains the owner's unique id, as well as his last known name (if available).
	 * 
	 * @return a String representation of the owner
	 */
	public String getOwnerString();

	/**
	 * Checks whether this shopkeeper is for hire.
	 * <p>
	 * The shopkeeper is for hire if a hire cost has been specified.
	 * 
	 * @return <code>true</code> if this shopkeeper is for hire
	 */
	public boolean isForHire();

	/**
	 * Sets this shopkeeper for hire using the given hire cost.
	 * 
	 * @param hireCost
	 *            the hire cost item, or <code>null</code> or empty to disable hiring for this shopkeeper
	 */
	public void setForHire(ItemStack hireCost);

	/**
	 * Gets the hiring cost of this shopkeeper.
	 * 
	 * @return a copy of the hiring cost item, or <code>null</code> if this shop is not for hire
	 */
	public ItemStack getHireCost();

	/**
	 * Gets the chest's x coordinate.
	 * 
	 * @return the chest's x coordinate
	 */
	public int getChestX();

	/**
	 * Gets the chest's y coordinate.
	 * 
	 * @return the chest's y coordinate
	 */
	public int getChestY();

	/**
	 * Gets the chest's z coordinate.
	 * 
	 * @return the chest's z coordinate.
	 */
	public int getChestZ();

	public void setChest(int chestX, int chestY, int chestZ);

	public Block getChest();

	public int getCurrencyInChest();

	// SHOPKEEPER UIs - shortcuts for common UI types:

	/**
	 * Attempts to open the hiring interface of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type doesn't support hiring (ex. admin shops).
	 * 
	 * @param player
	 *            the player requesting the hiring interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openHireWindow(Player player);

	/**
	 * Attempts to open the chest inventory of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the chest inventory window
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openChestWindow(Player player);
}
