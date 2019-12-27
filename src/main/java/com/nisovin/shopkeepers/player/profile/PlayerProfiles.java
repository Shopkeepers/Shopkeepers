package com.nisovin.shopkeepers.player.profile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.nisovin.shopkeepers.player.profile.storage.PlayerStorage;

/**
 * Provides access to the {@link PlayerStorage}.
 */
public interface PlayerProfiles {

	/**
	 * Fetches the {@link PlayerProfile} for the specified player uuid.
	 * 
	 * @param playerUUID
	 *            the player's uuid
	 * @return the task that provides the player profile, or <code>null</code> if no data was found for the specified
	 *         player
	 */
	public CompletableFuture<PlayerProfile> getProfile(UUID playerUUID);

	/**
	 * Fetches the {@link PlayerProfile} for players with the specified name.
	 * <p>
	 * There can exist multiple profiles for different players with the same name (eg. if players changed their name and
	 * their last known name never got updated).
	 * 
	 * @param playerName
	 *            the player's name
	 * @return the task that provides the player profiles, or an empty list if no data was found for the specified
	 *         player name
	 */
	public CompletableFuture<List<PlayerProfile>> getProfiles(String playerName);

	/**
	 * Updates (or creates) the player's profile.
	 * <p>
	 * This is blocking.
	 * 
	 * @param profile
	 *            the player profile
	 * @return the task
	 */
	public CompletableFuture<Void> updateProfile(PlayerProfile profile);

	/**
	 * Attempts to remove the profile of the specified player.
	 * <p>
	 * This may fail if the profile is still referenced by some other storage component.
	 * 
	 * @param playerUUID
	 *            the player's unique id
	 * @return the task
	 */
	public CompletableFuture<Void> removeProfile(UUID playerUUID);

	/**
	 * Gets the number of existing player profiles.
	 * 
	 * @return the task that provides the number of existing player profiles
	 */
	public CompletableFuture<Integer> getPlayerCount();
}
