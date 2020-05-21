package com.nisovin.shopkeepers.api.profile;

import java.time.Instant;
import java.util.UUID;

/**
 * General information about a player.
 */
public interface PlayerProfile {

	/**
	 * Gets the player's unique id.
	 * 
	 * @return the player's unique id
	 */
	public UUID getUniqueId();

	/**
	 * Gets the player's last known name.
	 * <p>
	 * This matches the player's current name, if the player is currently online and this profile is up-to-date.
	 * 
	 * @return the player's last known name
	 */
	public String getName();

	/**
	 * Gets the timestamp of when the player was first seen on the server.
	 * 
	 * @return the timestamp of when the player was first seen on the server
	 */
	public Instant getFirstSeen();

	/**
	 * Gets the timestamp of when the player was last seen on the server.
	 * <p>
	 * If the player is currently online, this only matches the current time if this profile is up-to-date (i.e. if it
	 * got created just now).
	 * 
	 * @return the timestamp of when the player was last seen on the server
	 */
	public Instant getLastSeen();

	/**
	 * Creates a <b>new</b> {@link PlayerProfile} that merges the information of this profile with the player
	 * information currently available on this server.
	 * <p>
	 * This may update the player name and the first-seen and last-seen timestamps.
	 * 
	 * @return the new, updated player profile
	 */
	public PlayerProfile update();
}
