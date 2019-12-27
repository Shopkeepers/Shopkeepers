package com.nisovin.shopkeepers.player.profile.storage;

import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.storage.StorageException;

/**
 * Stores {@link PlayerProfile player profiles}.
 */
public interface PlayerStorage {

	/**
	 * Fetches the {@link PlayerProfile} for the specified player uuid.
	 * <p>
	 * This is blocking.
	 * 
	 * @param playerUUID
	 *            the player's uuid
	 * @return the player profile, or <code>null</code> if no data was found for the specified player
	 * @throws StorageException
	 *             in case of failure
	 */
	public PlayerProfile getProfile(UUID playerUUID) throws StorageException;

	/**
	 * Fetches the {@link PlayerProfile} for players with the specified name.
	 * <p>
	 * There can exist multiple profiles for different players with the same name (eg. if players changed their name and
	 * their last known name never got updated). The found player profiles will be returned in descending order
	 * according to when the players were last seen.
	 * <p>
	 * This is blocking.
	 * 
	 * @param playerName
	 *            the player's name
	 * @return the player profiles, or an empty list if no data was found for the specified player name
	 * @throws StorageException
	 *             in case of failure
	 */
	public List<PlayerProfile> getProfiles(String playerName) throws StorageException;

	/**
	 * Updates (or creates) the player's profile.
	 * <p>
	 * This is blocking.
	 * 
	 * @param profile
	 *            the player profile
	 * @throws StorageException
	 *             in case of failure
	 */
	public void updateProfile(PlayerProfile profile) throws StorageException;

	/**
	 * Attempts to remove the profile of the specified player.
	 * <p>
	 * This may fail if the profile is still referenced by some other storage component.
	 * <p>
	 * This is blocking.
	 * 
	 * @param playerUUID
	 *            the player's unique id
	 * @throws StorageException
	 *             in case of failure
	 */
	public void removeProfile(UUID playerUUID) throws StorageException;

	/**
	 * Gets the number of existing player profiles.
	 * <p>
	 * This is blocking.
	 * 
	 * @return the number of existing player profiles
	 * @throws StorageException
	 *             in case of failure
	 */
	public int getPlayerCount() throws StorageException;
}
