package com.nisovin.shopkeepers.api.shopkeeper.player;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopMember;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a container and
 * will deposit earnings back into that container.
 */
public interface PlayerShopkeeper extends Shopkeeper {

	// OWNER

	/**
	 * Sets the owner of this shop.
	 * 
	 * @param player
	 *            the owner of this shop, not <code>null</code>
	 */
	public void setOwner(Player player);

	/**
	 * Sets the owner of this shop.
	 * 
	 * @param ownerUUID
	 *            the owner's uuid, not <code>null</code>
	 * @param ownerName
	 *            the owner's name, not <code>null</code> or empty
	 */
	public void setOwner(UUID ownerUUID, String ownerName);

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owner's player uuid, not <code>null</code>
	 */
	public UUID getOwnerUUID();

	/**
	 * Gets the last known name of the player who owns this shop.
	 * 
	 * @return the owner's last known name, not <code>null</code>
	 */
	public String getOwnerName();

	/**
	 * Gets a String representation of the owning player.
	 * <p>
	 * This contains the owner's last known name as well as his uuid.
	 * 
	 * @return a String representing the owner
	 */
	public String getOwnerString();

	/**
	 * Checks if the given owner is owning this shop.
	 * 
	 * @param player
	 *            the player to check, not <code>null</code>
	 * @return <code>true</code> if the given player owns this shop
	 */
	public boolean isOwner(Player player);

	/**
	 * Checks if the specified player is the owner of this shop.
	 * 
	 * @param playerUUID
	 *            the player UUID
	 * @return <code>true</code> if the specified player owns this shop
	 */
	public boolean isOwner(UUID playerUUID);

	/**
	 * Gets the owner of this shop IF they are online.
	 * 
	 * @return the owner of this shop, or <code>null</code> if the owner is offline
	 */
	public @Nullable Player getOwner();

	// MEMBERS

	/**
	 * Gets the additional shop members, not including the shop owner.
	 * 
	 * @return an unmodifiable view on the additional shop members, not including the shop owner
	 */
	public Collection<? extends PlayerShopMember> getMembers();

	/**
	 * Adds the specified player as a shop member.
	 * <p>
	 * This has no effect if the shop members feature is disabled.
	 * 
	 * @param playerUUID
	 *            the member's uuid, not <code>null</code>
	 * @param playerName
	 *            the member's name, not <code>null</code> or empty
	 * @param accessLevel
	 *            the {@link PlayerShopAccessLevel}, not
	 *            {@link DefaultPlayerShopAccessLevels#getNone()}
	 * @throws IllegalArgumentException
	 *             if the specified player is already a {@link #isMember(UUID) member} (includes the
	 *             shop owner)
	 */
	public void addMember(UUID playerUUID, String playerName, PlayerShopAccessLevel accessLevel);

	/**
	 * Removes the specified player from the shop members.
	 * <p>
	 * This has no effect if the player is already not a member.
	 * 
	 * @param playerUUID
	 *            the member's uuid
	 * @throws IllegalArgumentException
	 *             if the specified player is the {@link #isOwner(UUID) owner}
	 */
	public void removeMember(UUID playerUUID);

	/**
	 * Gets the {@link PlayerShopMember} for the specified player, if they are a shop member.
	 * <p>
	 * This also returns a value for the shop owner.
	 * 
	 * @param playerUUID
	 *            the player UUID
	 * @return the {@link PlayerShopMember}, or <code>null</code> if the player is not a member
	 */
	public @Nullable PlayerShopMember getMember(UUID playerUUID);

	/**
	 * Checks if the given player is the owner or an additional member of this shop.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if the given player is a member of this shop
	 */
	public boolean isMember(Player player);

	/**
	 * Checks if the specified player is the owner or an additional member of this shop.
	 * 
	 * @param playerUUID
	 *            the player UUID
	 * @return <code>true</code> if the specified player is a member of this shop
	 */
	public boolean isMember(UUID playerUUID);

	/**
	 * Gets the {@link PlayerShopAccessLevel} for the specified player.
	 * <p>
	 * This returns {@link DefaultPlayerShopAccessLevels#getFull()} for the shop owner and
	 * {@link DefaultPlayerShopAccessLevels#getNone()} for players that are not shop members.
	 * 
	 * @param playerUUID
	 *            the player UUID
	 * @return the {@link PlayerShopAccessLevel}
	 */
	public PlayerShopAccessLevel getAccessLevel(UUID playerUUID);

	/**
	 * Updates the {@link PlayerShopAccessLevel} for the specified shop member.
	 * 
	 * @param playerUUID
	 *            the member's uuid, not <code>null</code>
	 * @param accessLevel
	 *            the {@link PlayerShopAccessLevel}, not
	 *            {@link DefaultPlayerShopAccessLevels#getNone()}
	 * @throws IllegalArgumentException
	 *             if the specified player is the {@link #isOwner(UUID) owner} or not a
	 *             {@link #isMember(UUID) member}
	 */
	public void setAccessLevel(UUID playerUUID, PlayerShopAccessLevel accessLevel);

	/**
	 * Checks if the given player has at least the specified {@link PlayerShopAccessLevel}.
	 * <p>
	 * This does not take the {@link ShopkeepersPlugin#BYPASS_PERMISSION} into account.
	 * 
	 * @param player
	 *            the player
	 * @param accessLevel
	 *            the access level to check for
	 * @return <code>true</code> if the given player has the specified access level
	 */
	public boolean hasAccessLevel(Player player, PlayerShopAccessLevel accessLevel);

	/**
	 * Checks if the specified player has at least the specified {@link PlayerShopAccessLevel}.
	 * <p>
	 * This does not take the {@link ShopkeepersPlugin#BYPASS_PERMISSION} into account.
	 * 
	 * @param playerUUID
	 *            the player UUID
	 * @param accessLevel
	 *            the access level to check for
	 * @return <code>true</code> if the specified player has the specified access level
	 */
	public boolean hasAccessLevel(UUID playerUUID, PlayerShopAccessLevel accessLevel);

	// TRADE NOTIFICATIONS

	/**
	 * Checks whether the shop owner is notified about trades of this shopkeeper.
	 * <p>
	 * This property only affects the trade notifications that are sent to the shop owner. It has no
	 * effect on the general trade notifications that may be sent for this shopkeeper to other
	 * players.
	 * 
	 * @return <code>true</code> if the shop owner is notified about trades of this shopkeeper
	 */
	public boolean isNotifyOnTrades();

	/**
	 * Sets whether the shop owner is notified about trades of this shopkeeper.
	 * 
	 * @param notifyOnTrades
	 *            whether to notify the shop owner about trades
	 * @see #isNotifyOnTrades()
	 */
	public void setNotifyOnTrades(boolean notifyOnTrades);

	// HIRING

	/**
	 * Checks whether this shopkeeper is for hire.
	 * <p>
	 * The shopkeeper is for hire if a {@link #getHireCost() hiring cost item} is set.
	 * 
	 * @return <code>true</code> if this shopkeeper is for hire
	 */
	public boolean isForHire();

	/**
	 * Sets this shopkeeper for hire using the given hiring cost item.
	 * <p>
	 * The given item stack is copied before it is stored by the shopkeeper.
	 * 
	 * @param hireCost
	 *            the hiring cost item, or <code>null</code> or empty to set this shopkeeper not for
	 *            hire
	 */
	public void setForHire(@Nullable ItemStack hireCost);

	/**
	 * Sets this shopkeeper for hire using the given hiring cost item.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored
	 * by the shopkeeper.
	 * 
	 * @param hireCost
	 *            the hiring cost item, or <code>null</code> or empty to set this shopkeeper not for
	 *            hire
	 */
	public void setForHire(@Nullable UnmodifiableItemStack hireCost);

	/**
	 * Gets the hiring cost item of this shopkeeper.
	 * 
	 * @return an unmodifiable view on the hiring cost item, or <code>null</code> if this shopkeeper
	 *         is not for hire
	 */
	public @Nullable UnmodifiableItemStack getHireCost();

	// CONTAINERS

	/**
	 * Gets the container's x coordinate.
	 * 
	 * @return the container's x coordinate
	 */
	public int getContainerX();

	/**
	 * Gets the container's y coordinate.
	 * 
	 * @return the container's y coordinate
	 */
	public int getContainerY();

	/**
	 * Gets the container's z coordinate.
	 * 
	 * @return the container's z coordinate.
	 */
	public int getContainerZ();

	/**
	 * Sets the container's coordinates.
	 * 
	 * @param containerX
	 *            the container's x coordinate
	 * @param containerY
	 *            the container's y coordinate
	 * @param containerZ
	 *            the container's z coordinate
	 */
	public void setContainer(int containerX, int containerY, int containerZ);

	/**
	 * Gets the block of the shop's container.
	 * <p>
	 * This does not necessarily have to be a chest, but could also be another type of supported
	 * shop container.
	 * <p>
	 * The block might not actually be a valid container type currently (for example if something
	 * has broken or changed the type of the block in the meantime).
	 * 
	 * @return the shop's container block, or <code>null</code> if the container's world is not
	 *         loaded currently
	 */
	public @Nullable Block getContainer();

	/**
	 * Gets the amount of currency stored inside the shop's container.
	 * <p>
	 * Returns <code>0</code> if the container does not exist currently.
	 * 
	 * @return the amount of currency inside the shop's container
	 */
	public int getCurrencyInContainer();

	// SHOPKEEPER UIs - shortcuts for common UI types:

	/**
	 * Attempts to open the hiring interface of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type does not support hiring (e.g. admin shops).
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openHireWindow(Player player);

	/**
	 * Attempts to open the container inventory of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openContainerWindow(Player player);

	/**
	 * Attempts to open the shop members editor of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openMembersEditorWindow(Player player);
}
