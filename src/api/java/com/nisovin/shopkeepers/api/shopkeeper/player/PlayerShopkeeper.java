package com.nisovin.shopkeepers.api.shopkeeper.player;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest and will deposit earnings
 * back into that chest.
 */
public interface PlayerShopkeeper extends Shopkeeper {

	/**
	 * Sets the owner of this shop.
	 * 
	 * @param player
	 *            the owner of this shop
	 */
	public void setOwner(Player player);

	public void setOwner(UUID ownerUUID, String ownerName);

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owner's player uuid, not <code>null</code>
	 */
	public UUID getOwnerUUID();

	// TODO remove?
	/**
	 * Gets the last known name of the player who owns this shop.
	 * 
	 * @return the owner's last known name, not <code>null</code>
	 */
	public String getOwnerName();

	/**
	 * Gets a string representation of the owning player.
	 * <p>
	 * This contains the owner's last known name as well as his uuid.
	 * 
	 * @return a string representing the the owner
	 */
	public String getOwnerString();

	/**
	 * Checks if the given owner is owning this shop.
	 * 
	 * @param player
	 *            the player to check
	 * @return <code>true</code> if the given player owns this shop
	 */
	public boolean isOwner(Player player);

	/**
	 * Gets the owner of this shop IF he is online.
	 * 
	 * @return the owner of this shop, or <code>null</code> if the owner is offline
	 */
	public Player getOwner();

	/**
	 * Gets the uuids of all trusted players.
	 * <p>
	 * Trusted players are able to access the shop's chest and (with a few exceptions) edit the shop.
	 * 
	 * @return an unmodifiable view on the uuids of all trusted players
	 */
	public Collection<UUID> getTrustedPlayers();

	/**
	 * Adds the given player uuid to the collection of trusted players.
	 * <p>
	 * This has no effect if the player is already trusted.
	 * 
	 * @param playerUUID
	 *            the player's uuid
	 */
	public void addTrustedPlayer(UUID playerUUID);

	/**
	 * Removes the given player uuid from the collection of trusted players.
	 * <p>
	 * This has no effect if the player is already untrusted.
	 * 
	 * @param playerUUID
	 *            the player's uuid
	 */
	public void removeTrustedPlayer(UUID playerUUID);

	/**
	 * Checks if the given player is trusted.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the player is trusted
	 */
	public boolean isTrusted(Player player);

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
