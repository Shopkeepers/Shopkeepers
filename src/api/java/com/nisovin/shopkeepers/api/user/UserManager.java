package com.nisovin.shopkeepers.api.user;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Provides and caches {@link User} objects.
 * <p>
 * Users represent the subset of players that are currently known to the Shopkeepers plugin. The {@link UserManager}
 * keeps track of users for:
 * <ul>
 * <li>Currently online players (may only include players which have successfully {@link PlayerJoinEvent joined} the
 * server).
 * <li>Players referred to by currently loaded shops, such as shop owners.
 * <li>Users which have been recently accessed (eg. via {@link #getOrCreateUser(UUID)}) and are still in use.
 * </ul>
 */
public interface UserManager {

	/**
	 * Gets the {@link UserManager currently cached} {@link User} for the specified unique player id.
	 * 
	 * @param playerId
	 *            the player's unique id
	 * @return the user, or <code>null</code> if the user is not cached currently
	 */
	public User getUser(UUID playerId);

	/**
	 * Gets the {@link UserManager currently cached} {@link User} for the specified player.
	 * <p>
	 * This is provided as a convenience and simply delegates to {@link #getUser(UUID)} using the player's unique id.
	 * 
	 * @param player
	 *            the player
	 * @return the user, or <code>null</code> if the user is not cached currently
	 */
	public User getUser(OfflinePlayer player);

	/**
	 * Gets the {@link User} for the specified unique player id which the caller of this method expects to be cached
	 * currently.
	 * <p>
	 * For example, we guarantee that the {@link User Users} for {@link UserManager online players} are always available
	 * inside the cache. However, unlike {@link #getUser(UUID)}, this performs some additional validation and throws an
	 * (unchecked) exception if the specified {@link User} is not actually cached currently.
	 * <p>
	 * Only use this method if you actually expect the {@link User} to be cached currently. Any exception thrown by this
	 * method should be an indication for a programming error or missed state on the caller's side.
	 * 
	 * @param playerId
	 *            the player's unique id
	 * @return the user, not <code>null</code>
	 */
	public User getAssertedUser(UUID playerId);

	/**
	 * Gets the {@link User} for the specified player which the caller of this method expects to be cached currently.
	 * <p>
	 * This is provided as a convenience and simply delegates to {@link #getAssertedUser(UUID)} using the player's
	 * unique id.
	 * 
	 * @param player
	 *            the player
	 * @return the user, not <code>null</code>
	 */
	public User getAssertedUser(OfflinePlayer player);

	/**
	 * Gets or creates the {@link User} for the specified unique player id.
	 * <p>
	 * This may need to asynchronously load the data for the user.
	 * 
	 * @param playerId
	 *            the player's unique id
	 * @return the task which provides the user, never <code>null</code>
	 */
	public CompletableFuture<? extends User> getOrCreateUser(UUID playerId);

	/**
	 * Gets or creates the {@link User} for the specified player.
	 * <p>
	 * This may need to asynchronously load the data for the user.
	 * <p>
	 * This is provided as a convenience and simply delegates to {@link #getOrCreateUser(UUID)} using the player's
	 * unique id.
	 * 
	 * @param player
	 *            the player
	 * @return the task which provides the user, never <code>null</code>
	 */
	public CompletableFuture<? extends User> getOrCreateUser(OfflinePlayer player);

	/**
	 * Gets or creates the {@link User} for the specified unique player id.
	 * <p>
	 * Unlike {@link #getOrCreateUser(UUID)} this may block while loading the data for the user!
	 * 
	 * @param playerId
	 *            the player's unique id
	 * @return the user, never <code>null</code>
	 */
	public User getOrCreateUserImmediately(UUID playerId);

	/**
	 * Gets or creates the {@link User} for the specified player.
	 * <p>
	 * Unlike {@link #getOrCreateUser(UUID)} this may block while loading the data for the user!
	 * <p>
	 * This is provided as a convenience and simply delegates to {@link #getOrCreateUserImmediately(UUID)} using the
	 * player's unique id.
	 * 
	 * @param player
	 *            the player
	 * @return the user, never <code>null</code>
	 */
	public User getOrCreateUserImmediately(OfflinePlayer player);

	/**
	 * Gets all {@link UserManager currently cached} {@link User Users}.
	 * 
	 * @return an unmodifiable view on the currently cached users
	 */
	public Collection<? extends User> getAllUsers();

	/**
	 * Searches for {@link UserManager currently cached} users whose names match the specified name.
	 * <p>
	 * There can exist multiple users for the same name (eg. when taking display names into account or if players
	 * changed their name and their last known name got never updated).
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching users as required.
	 * 
	 * @param playerName
	 *            the player name
	 * @param checkDisplayNames
	 *            whether to also compare with the display names of online users
	 * @return a stream over the matching users
	 */
	public Stream<? extends User> getUsers(String playerName, boolean checkDisplayNames);

	/**
	 * Searches for {@link UserManager currently cached} users whose unique ids start with the specified prefix.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching users as required.
	 * 
	 * @param uuidPrefix
	 *            the player uuid prefix
	 * @return a stream over the matching users
	 */
	public Stream<? extends User> getUsersByUUIDPrefix(String uuidPrefix);

	/**
	 * Searches for {@link UserManager currently cached} users whose names start with the specified prefix.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching users as required.
	 * 
	 * @param namePrefix
	 *            the player name prefix
	 * @param checkDisplayNames
	 *            whether to also compare with the display names of online users
	 * @return stream over the matching users
	 */
	public Stream<? extends User> getUsersByNamePrefix(String namePrefix, boolean checkDisplayNames);
}
