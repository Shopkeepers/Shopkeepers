package com.nisovin.shopkeepers.api.user;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Represents a player that is known to the Shopkeepers plugin.
 */
public interface User {

	/**
	 * Checks whether this {@link User} object is still valid.
	 * <p>
	 * The User object is only valid as long as it is managed by the {@link UserManager}. User objects usually become
	 * invalid on plugin shutdowns and reloads.
	 * 
	 * @return <code>true</code> if still valid
	 */
	public boolean isValid();

	/**
	 * Gets the player's unique id.
	 * 
	 * @return the player unique id, not <code>null</code>
	 */
	public UUID getUniqueId();

	/**
	 * Gets the player's last known name.
	 * <p>
	 * If this {@link User} object is still {@link #isValid() valid} and the player is currently online, this matches
	 * the player's current name.
	 * 
	 * @return the player's last known name, or <code>null</code> if not known
	 */
	public String getName();

	/**
	 * Gets the player's display name.
	 * <p>
	 * The display name may not be available (eg. if the player is not online currently).
	 * 
	 * @return the display name, or <code>null</code> if not available
	 */
	public String getDisplayName();

	/**
	 * Gets the player's display name, or if it is not available (eg. if the player is not online currently) gets the
	 * player's {@link #getName() last known name} instead.
	 * 
	 * @return the player's display name, or if not available the player's name, or <code>null</code> if neither is
	 *         available
	 */
	public String getDisplayNameOrName();

	/**
	 * Checks whether the player is currently online.
	 * 
	 * @return <code>true</code> if the player is online
	 */
	public boolean isOnline();

	/**
	 * Gets the {@link Player} if he is currently online.
	 * 
	 * @return the player, or <code>null</code> if the player is not online
	 */
	public Player getPlayer();

	/**
	 * Gets the {@link OfflinePlayer}.
	 * 
	 * @return the offline player, never <code>null</code> (even if the player does not exist or has never played on the
	 *         server before)
	 */
	public OfflinePlayer getOfflinePlayer();

	/**
	 * Gets a String representation of this {@link User} which is more user-friendly compared to {@link #toString()}.
	 * <p>
	 * The String representation contains the user's {@link #getUniqueId() unique id} and {@link #getName() last known
	 * name} (if it is available).
	 * 
	 * @return a more user-friendly String representation
	 */
	public String toPrettyString();
}
