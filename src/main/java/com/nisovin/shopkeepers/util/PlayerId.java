package com.nisovin.shopkeepers.util;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Represents the information which may be used to identify a player.
 * <p>
 * By default this includes:
 * <ul>
 * <li>The player's unique id (required, non-changing).
 * <li>The player's name (optional).
 * <li>The player's display name (optional).
 * </ul>
 * <p>
 * There is no guarantee whether any of the optional {@link PlayerId} attributes are immutable or not. Implementations
 * may for instance decide to dynamically update or freshly fetch the data whenever requested.
 * <p>
 * {@link PlayerId} objects may get cached, but no assumptions should be made regarding this. There may exist multiple
 * {@link PlayerId} objects for the same player in memory at the same time, possibly even with different values for the
 * optional attributes. The comparison of {@link PlayerId} objects only takes the {@link PlayerId#getUniqueId() unique
 * id} into account.
 */
public interface PlayerId {

	/**
	 * Gets the player's unique id.
	 * 
	 * @return the unique id, not <code>null</code>
	 */
	public UUID getUniqueId();

	/**
	 * Gets the player's name.
	 * <p>
	 * The name may not be available, or outdated (eg. only represent the player's last known name).
	 * 
	 * @return the name, or <code>null</code> if not available
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
	 * Gets the player's display name, or if it is not available, gets the player's name instead.
	 * 
	 * @return the player's display name, or name, or <code>null</code> if neither is available
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
	 * Gets a String representation of the player which is more compact and user-friendly compared to
	 * {@link #toString()}.
	 * <p>
	 * The String representation contains the player's {@link #getUniqueId() unique id} and {@link #getName() name} (if
	 * it is available). This may for example be used for debugging purposes.
	 * 
	 * @return the String representation
	 */
	public String toPrettyString();
}
