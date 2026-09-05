package com.nisovin.shopkeepers.api.shopkeeper.player;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerType;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopMember;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from one or more
 * containers and deposits its earnings back into them.
 */
public interface PlayerShopkeeper extends Shopkeeper {

	// OWNER

	/**
	 * Sets the owner of this shop.
	 * <p>
	 * Changing the owner to a different player also clears the {@link #getMembers() shop members}.
	 * 
	 * @param player
	 *            the owner of this shop, not <code>null</code>
	 */
	public void setOwner(Player player);

	/**
	 * Sets the owner of this shop.
	 * <p>
	 * Changing the owner to a different player also clears the {@link #getMembers() shop members}.
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

	/**
	 * Gets the timestamp at which the current owner acquired this shop.
	 * <p>
	 * This is the basis of the shop's expiration (if enabled) and is reset when the shop owner
	 * changes, the shop is hired (including when it is hired by the same owner again), or the
	 * expiration is {@link #resetExpiration() reset}.
	 * 
	 * @return the timestamp at which the current owner acquired this shop / the expiration basis,
	 *         not <code>null</code>
	 */
	public Instant getOwnedSince();

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
	 * Checks whether this shopkeeper is currently offered for hire, i.e. it is both
	 * {@link #isHireable() hireable} and has not yet been {@link #setHired() hired}.
	 * <p>
	 * Note: The shopkeeper retains its hire cost item after having been {@link #setHired() hired},
	 * for example to later be able to restore its for-hire state with the same hire cost. I.e. it
	 * remains {@link #isHireable() hireable} but is no longer {@link #isForHire() for hire}.
	 * 
	 * @return <code>true</code> if this shopkeeper is currently offered for hire
	 */
	public boolean isForHire();

	/**
	 * Sets this shopkeeper for hire using the given hire cost item.
	 * <p>
	 * The given item stack is copied before it is stored by the shopkeeper.
	 * 
	 * @param hireCost
	 *            the hire cost item, or <code>null</code> or empty to set this shopkeeper not for
	 *            hire
	 * @see #setForHire(UnmodifiableItemStack)
	 */
	public void setForHire(@Nullable ItemStack hireCost);

	/**
	 * Sets this shopkeeper for hire using the given hire cost item.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored
	 * by the shopkeeper.
	 * <p>
	 * The shop owner is not changed: They keep their access to the shop until another player hires
	 * the shopkeeper. However, the shopkeeper can no longer be traded with while it is for hire and
	 * it does not count towards the player's max-shops limit.
	 * 
	 * @param hireCost
	 *            the hire cost item, or <code>null</code> or empty to set this shopkeeper not for
	 *            hire
	 */
	public void setForHire(@Nullable UnmodifiableItemStack hireCost);

	/**
	 * Sets this shopkeeper for hire using its current (retained) {@link #getHireCost() hire cost
	 * item}.
	 * <p>
	 * This has no effect if this shopkeeper is not {@link #isHireable() hireable}.
	 * 
	 * @see #setForHire(UnmodifiableItemStack)
	 */
	public void setForHire();

	/**
	 * Marks this shopkeeper as hired, i.e. sets it as no longer {@link #isForHire() for hire}, but
	 * preserves its {@link #getHireCost() hire cost} item, i.e. its {@link #isHireable() hireable}
	 * state.
	 * <p>
	 * This also {@link #resetExpiration() resets the shop's expiration} in order to account for the
	 * current owner re-hiring the previously expired shop.
	 * <p>
	 * This has no effect if the shopkeeper is not {@link #isForHire() for hire} currently.
	 */
	public void setHired();

	/**
	 * Gets the hire cost item of this shopkeeper.
	 * <p>
	 * A shopkeeper retains its hire cost item after having been {@link #setHired() hired}, so a
	 * non-empty hire cost item does not imply that the shopkeeper is currently {@link #isForHire()
	 * for hire}.
	 * 
	 * @return an unmodifiable view on the hire cost item, or <code>null</code> if this shopkeeper
	 *         has no hire cost item
	 */
	public @Nullable UnmodifiableItemStack getHireCost();

	/**
	 * Checks whether this shopkeeper has a non-empty {@link #getHireCost() hire cost item}
	 * assigned.
	 * <p>
	 * A shopkeeper retains its hire cost item after having been {@link #setHired() hired}, so this
	 * does not imply that the shopkeeper is currently {@link #isForHire() for hire}.
	 * 
	 * @return <code>true</code> if this shopkeeper has a non-empty hire cost item
	 */
	public boolean isHireable();

	// EXPIRATION

	/**
	 * Gets the timestamp at which this shopkeeper expires.
	 * <p>
	 * This is derived from the configured expiration duration and the time at which the current
	 * owner {@link #getOwnedSince() acquired} this shopkeeper. It is therefore not a fixed
	 * timestamp, but can change when the plugin configuration changes. Shopkeepers that are
	 * currently {@link #isForHire() for hire} do not expire.
	 * <p>
	 * Note: This may recalculate and then persist the shopkeeper's cached expiration, and can
	 * therefore trigger a delayed save of the shopkeeper data.
	 * 
	 * @return the timestamp at which this shopkeeper expires, or <code>null</code> if it does not
	 *         expire
	 */
	public @Nullable Instant getExpiration();

	/**
	 * Resets this shopkeeper's {@link #getExpiration() expiration} by setting its
	 * {@link #getOwnedSince() owned since} timestamp to the current time.
	 * <p>
	 * The current owner thereby gets the full expiration duration again.
	 * <p>
	 * This always updates the owned since timestamp, even if this shopkeeper does not currently
	 * expire, for example while it is {@link #isForHire() for hire}.
	 */
	public void resetExpiration();

	/**
	 * Expires this shopkeeper.
	 * <p>
	 * If this shopkeeper has a {@link #getHireCost() hire cost item}, it is {@link #setForHire()
	 * restored to its for hire state}. Otherwise, it is {@link #delete() deleted}.
	 * <p>
	 * This processes the expiration immediately, without checking whether expiration is enabled,
	 * whether this shopkeeper has actually reached its {@link #getExpiration() expiration}, and
	 * without calling the {@code PlayerShopkeeperExpireEvent}.
	 */
	public void expire();

	// CONTAINERS

	/**
	 * Gets the containers used by this shop.
	 * 
	 * @return an unmodifiable view on the shop's containers, not <code>null</code> but can be empty
	 */
	public Collection<? extends ShopContainer> getContainers();

	/**
	 * Adds a container at the given location to this shop.
	 * <p>
	 * While not enforced in the API, for performance reasons it makes sense for the shop containers
	 * to be near the shopkeeper, so that they are loaded when a player interacts with the shop.
	 * <p>
	 * Note: This is not verified here to avoid loading the specified block's chunk, but avoid
	 * adding the other attached side of a double chest that is already a shop container: Shop
	 * containers automatically consider both sides of double chests, so adding the other side is
	 * redundant.
	 * 
	 * @param worldName
	 *            the container's world name, not <code>null</code> or empty
	 * @param containerX
	 *            the container's x coordinate
	 * @param containerY
	 *            the container's y coordinate
	 * @param containerZ
	 *            the container's z coordinate
	 * @param containerType
	 *            the {@link ShopContainerType}, not <code>null</code>
	 * @return the added container, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the shop already has a container at the given location
	 */
	public ShopContainer addContainer(
			String worldName,
			int containerX,
			int containerY,
			int containerZ,
			ShopContainerType containerType
	);

	/**
	 * Removes the container at the given location from this shop.
	 * <p>
	 * This has no effect if the shop has no container at the given location.
	 * 
	 * @param worldName
	 *            the container's world name, not <code>null</code> or empty
	 * @param containerX
	 *            the container's x coordinate
	 * @param containerY
	 *            the container's y coordinate
	 * @param containerZ
	 *            the container's z coordinate
	 */
	public void removeContainer(
			String worldName,
			int containerX,
			int containerY,
			int containerZ
	);

	/**
	 * Gets the container's x coordinate.
	 * <p>
	 * If the shop has multiple containers, this returns the coordinate of the first container.
	 * 
	 * @return the container's x coordinate, or <code>0</code> if the shop has no containers
	 * @deprecated A shop can have multiple containers. Use {@link #getContainers()} instead.
	 */
	@Deprecated
	public int getContainerX();

	/**
	 * Gets the container's y coordinate.
	 * <p>
	 * If the shop has multiple containers, this returns the coordinate of the first container.
	 * 
	 * @return the container's y coordinate, or <code>0</code> if the shop has no containers
	 * @deprecated A shop can have multiple containers. Use {@link #getContainers()} instead.
	 */
	@Deprecated
	public int getContainerY();

	/**
	 * Gets the container's z coordinate.
	 * <p>
	 * If the shop has multiple containers, this returns the coordinate of the first container.
	 * 
	 * @return the container's z coordinate, or <code>0</code> if the shop has no containers
	 * @deprecated A shop can have multiple containers. Use {@link #getContainers()} instead.
	 */
	@Deprecated
	public int getContainerZ();

	/**
	 * Sets the container's coordinates.
	 * <p>
	 * If the shop has at least one container, this updates the coordinates of its first container,
	 * preserving that container's other properties as well as any other containers. Otherwise, this
	 * adds a new container at the given coordinates of type
	 * {@link DefaultShopContainerTypes#STOCK_AND_EARNINGS()}.
	 * 
	 * @param containerX
	 *            the container's x coordinate
	 * @param containerY
	 *            the container's y coordinate
	 * @param containerZ
	 *            the container's z coordinate
	 * @deprecated A shop can have multiple containers. Use
	 *             {@link #addContainer(String, int, int, int, ShopContainerType)} and
	 *             {@link #removeContainer(String, int, int, int)} instead.
	 */
	@Deprecated
	public void setContainer(int containerX, int containerY, int containerZ);

	/**
	 * Gets the block of the shop's container.
	 * <p>
	 * This does not necessarily have to be a chest, but could also be another type of supported
	 * shop container.
	 * <p>
	 * The block might not actually be a valid container type currently (for example if something
	 * has broken or changed the type of the block in the meantime).
	 * <p>
	 * If the shop has multiple containers, this returns the block of the first container.
	 * 
	 * @return the shop's container block, or <code>null</code> if the shop has no containers or the
	 *         container's world is not loaded currently
	 * @deprecated A shop can have multiple containers. Use {@link #getContainers()} and
	 *             {@link ShopContainer#getBlock()} instead.
	 */
	@Deprecated
	public @Nullable Block getContainer();

	/**
	 * Gets the amount of currency stored inside the shop's stock containers.
	 * <p>
	 * Returns <code>0</code> if the shop has no stock containers or the containers do not exist
	 * currently.
	 * 
	 * @return the amount of currency inside the shop's stock containers
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
	 * <p>
	 * If the shop has multiple containers, this tries to open the inventory view for the first
	 * container.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the inventory view was successfully opened
	 * @deprecated A shop can have multiple containers. Use {@link #getContainers()} and
	 *             {@link ShopContainer#openInventoryView(Player)} instead.
	 */
	@Deprecated
	public boolean openContainerWindow(Player player);

	/**
	 * Attempts to open the shop members editor of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openMembersEditorWindow(Player player);

	/**
	 * Attempts to open the shop containers editor of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openContainersEditorWindow(Player player);
}
