package com.nisovin.shopkeepers.user;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.user.UserManager;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PlayerUtils;
import com.nisovin.shopkeepers.util.StreamUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.WeakValuesCache;
import com.nisovin.shopkeepers.util.WeakValuesCache.RemovalListener;

public class SKUserManager implements UserManager {

	private static final String LOG_PREFIX = "[UserCache] ";

	private final SKShopkeepersPlugin plugin;
	private final PlayerJoinQuitListener playerJoinQuitListener = new PlayerJoinQuitListener(this);

	// Users are cached as long as they are referenced somewhere (eg. inside shopkeepers, or by API clients):
	private final WeakValuesCache<UUID, SKUser> userCache = new WeakValuesCache<>();
	private final RemovalListener<UUID> cacheRemovalListener = new RemovalListener<UUID>() {
		@Override
		public void onEntryRemoved(UUID userId) {
			// We don't expect the cache to still contain an entry for the the user id (i.e. we don't expect users to
			// get replaced inside the cache):
			assert !userCache.containsKey(userId);
			Log.debug(Settings.DebugOptions.userCache, () -> LOG_PREFIX + "User has been removed: " + userId);

			// Cleanup uuid and name mappings:
			removeUUIDStringMapping(userId);
			removeNameMapping(userId);
		}
	};

	// Strong references for all online users:
	private final Map<UUID, SKUser> onlineUsers = new HashMap<>();

	// Mappings by UUID string and (normalized) name: Allows for fast lookups and completions of partial uuid/name
	// inputs (eg. for use in commands).
	// Weak values: Do not prevent the users from getting GC'ed and thereby removed from the cache. We cleanup these
	// mappings once the users are removed from the cache.
	private final NavigableMap<String, WeakReference<SKUser>> byUUIDString = new TreeMap<>();
	// byName and toName mappings: Only store mappings for users with known name.
	private final NavigableMap<String, List<WeakReference<SKUser>>> byName = new TreeMap<>();
	// UUID to (normalized) name mapping: Allows for reverse lookup. Required to cleanup the byName mapping after the
	// User object has been GC'ed.
	private final Map<UUID, String> toName = new HashMap<>();

	public SKUserManager(SKShopkeepersPlugin plugin) {
		assert plugin != null;
		this.plugin = plugin;
	}

	public void onEnable() {
		userCache.registerRemovalListener(cacheRemovalListener);
		Bukkit.getPluginManager().registerEvents(playerJoinQuitListener, plugin);

		// Add entries for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.addOnlineUser(player);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(playerJoinQuitListener);

		// Clear all online user references:
		onlineUsers.clear();

		// Clear the user cache. This also triggers cleanup for any previously cached users.
		userCache.clear();
		userCache.unregisterRemovalListener(cacheRemovalListener);
		// Note: We do not need to mark removed User objects as 'invalid', because SKUser#isValid queries the
		// UserManager to check if it is still valid.
	}

	@Override
	public SKUser getUser(UUID playerId) {
		Validate.notNull(playerId, "playerId is null");
		return userCache.get(playerId);
	}

	@Override
	public SKUser getUser(OfflinePlayer player) {
		Validate.notNull(player, "player is null");
		UUID playerId = player.getUniqueId();
		return this.getUser(playerId);
	}

	@Override
	public SKUser getAssertedUser(UUID playerId) {
		SKUser user = this.getUser(playerId);
		Validate.State.notNull(user, () -> "The user for id '" + playerId + "' is not actually cached currently!"
				+ " Missed state or did the player get disconnected unexpectedly?");
		return user;
	}

	@Override
	public User getAssertedUser(OfflinePlayer player) {
		Validate.notNull(player, "player is null");
		UUID playerId = player.getUniqueId();
		return this.getAssertedUser(playerId);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Implementation note:</br>
	 * When creating a new User for a given unique player id, we may load the player's last known name from the player's
	 * data file. Strictly speaking, Bukkit's OfflinePlayer API for doing so might not be thread-safe, because
	 * CraftOfflinePlayer accesses the server's player map (a regular HashMap) to check if the player is online, while
	 * the server might modify the map on the main thread at the same time. Even though we consider the risk for this
	 * actually causing issues to be quite low, we still perform the player data loading synchronously currently.
	 * However, the behavior of this method might change in the future (for example if we decide to perform the player
	 * data loading asynchronously nevertheless, or if we add our own storage for user data).
	 * <p>
	 * However, loading a single user data file should be quick. For instance, the Minecraft server and
	 * CraftBukkit/Spigot are loading the player data file synchronously as well under various circumstances (such as
	 * whenever a player joins the server). Also, we only need to do this once as long as the User remains cached. And
	 * we mostly load users on server startup and plugin reloads when we load the Shopkeepers, which needs to happen
	 * synchronously anyways and in which case the performance hit is acceptable. The remaining cases are limited to
	 * other plugins accessing uncached Users by unique id, for which the usecases are limited since we already cache
	 * the users for all online players. Our own commands are mostly limited to players that are already cached (online
	 * players, shop owners, etc.) and therefore do not require lookups of offline players by name or uuid.
	 */
	@Override
	public CompletableFuture<SKUser> getOrCreateUser(UUID playerId) {
		// This call also validates the playerId:
		return CompletableFuture.completedFuture(this.getOrCreateUserImmediately(playerId));
	}

	@Override
	public CompletableFuture<SKUser> getOrCreateUser(OfflinePlayer player) {
		Validate.notNull(player, "player is null");
		return this.getOrCreateUser(player.getUniqueId());
	}

	@Override
	public SKUser getOrCreateUserImmediately(UUID playerId) {
		SKUser user = this.getUser(playerId); // validates playerId
		if (user != null) return user;
		user = this.createAndAddUser(playerId);
		assert user != null;
		return user;
	}

	@Override
	public SKUser getOrCreateUserImmediately(OfflinePlayer player) {
		Validate.notNull(player, "player is null");
		return this.getOrCreateUserImmediately(player.getUniqueId());
	}

	private SKUser createAndAddUser(UUID playerId) {
		assert playerId != null;
		SKUser user = this.createUser(playerId);
		this.addUser(user);
		return user;
	}

	private SKUser createUser(UUID playerId) {
		assert playerId != null;
		// Note: This gets the online player is the player is currently online:
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId); // non-blocking
		// If the player is not online and the name is not available, this may access the player's data file
		// (blocking!, but should be quite fast nevertheless):
		String lastKnownName = offlinePlayer.getName(); // can be null
		return new SKUser(playerId, lastKnownName);
	}

	private void addUser(SKUser user) {
		assert user != null;
		// Add user to cache:
		UUID userId = user.getUniqueId();
		assert this.getUser(userId) == null;
		userCache.put(userId, user);

		// Add user to UUID-String and name mappings:
		this.addUUIDStringMapping(user);
		this.addNameMapping(user);
	}

	// UUID-String mapping:

	private void addUUIDStringMapping(SKUser user) {
		assert user != null;
		UUID userId = user.getUniqueId();
		String normalizedUserIdString = normalizeUUIDString(userId.toString());
		assert !byUUIDString.containsKey(normalizedUserIdString);
		WeakReference<SKUser> weakUserRef = new WeakReference<SKUser>(user);
		byUUIDString.put(normalizedUserIdString, weakUserRef);
	}

	private void removeUUIDStringMapping(UUID userId) {
		assert userId != null;
		String userIdString = userId.toString();
		byUUIDString.remove(userIdString);
	}

	// uuidString might also be an uuid prefix
	private String normalizeUUIDString(String uuidString) {
		if (uuidString == null) return null;
		return uuidString.toLowerCase(Locale.ROOT);
	}

	// Name mapping:

	private void addNameMapping(SKUser user) {
		assert user != null;
		String normalizedName = this.normalizeUserName(user.getName()); // can be null
		this._addNameMapping(user, normalizedName);
	}

	private void _addNameMapping(SKUser user, String normalizedName) {
		assert user != null;
		if (normalizedName != null) {
			// toName mapping:
			UUID userId = user.getUniqueId();
			assert !toName.containsKey(userId);
			toName.put(userId, normalizedName);

			// byName mapping:
			// assert: byName does not contain any user for the same unique id yet.
			List<WeakReference<SKUser>> usersByName = byName.computeIfAbsent(normalizedName, name -> {
				return new ArrayList<>();
			});
			WeakReference<SKUser> weakUserRef = new WeakReference<SKUser>(user);
			usersByName.add(weakUserRef);
		}
	}

	private void removeNameMapping(UUID userId) {
		assert userId != null;
		// toName mapping:
		String normalizedOldName = toName.remove(userId); // can be null
		if (normalizedOldName != null) {
			// byName mapping:
			List<WeakReference<SKUser>> usersByName = byName.get(normalizedOldName);
			if (usersByName != null) {
				usersByName.removeIf(weakUserRef -> {
					SKUser refUser = weakUserRef.get();
					// We also cleanup encountered stale entries right away:
					return (refUser == null || refUser.getUniqueId().equals(userId));
				});
				if (usersByName.isEmpty()) {
					byName.remove(normalizedOldName);
				}
			}
		}
	}

	private void updateNameMapping(SKUser user) {
		assert user != null;
		UUID userId = user.getUniqueId();
		String normalizedOldName = toName.get(userId); // can be null
		String normalizedNewName = this.normalizeUserName(user.getName()); // can be null
		if (Objects.equals(normalizedOldName, normalizedNewName)) return; // name did not change

		// Remove old mapping (if required):
		if (normalizedOldName != null) {
			this.removeNameMapping(userId);
		}
		// Add new mapping (if required):
		this._addNameMapping(user, normalizedNewName);
	}

	// userName might also be a name prefix
	private String normalizeUserName(String userName) {
		return PlayerUtils.normalizePlayerName(userName);
	}

	// displayName might also be a name prefix
	private String normalizeDisplayName(String displayName) {
		return PlayerUtils.normalizeDisplayName(displayName);
	}

	// PLAYER JOINING & QUITTING

	// TODO We currently create users for online players immediately (since we do not need to load any user data for
	// them). If we should ever add our own user data storage and perform the user data loading asynchronously, we
	// should do so within the AsyncPlayerPreLoginEvent in order to not block the main server thread then.
	void onPlayerJoin(Player player) {
		// Check if player is actually still online (some earlier event handler might have already kicked the player
		// again):
		if (!player.isOnline()) return;
		this.addOnlineUser(player);
	}

	// non-blocking
	private SKUser addOnlineUser(Player player) {
		assert player != null && player.isOnline();
		UUID playerId = player.getUniqueId();
		// Note: Non-blocking, since the player is currently online.
		SKUser user = this.getOrCreateUserImmediately(playerId);

		// Add strong reference for online user:
		assert !onlineUsers.containsKey(playerId);
		onlineUsers.put(playerId, user);

		// If we already had the user cached, its name might be outdated:
		user.setName(player.getName());
		this.updateNameMapping(user);
		return user;
	}

	void onPlayerQuit(Player player) {
		UUID playerId = player.getUniqueId();
		onlineUsers.remove(playerId);
	}

	// OTHER QUERIES

	@Override
	public Collection<? extends SKUser> getAllUsers() {
		return userCache.getValues(); // already unmodifiable
	}

	@Override
	public Stream<? extends SKUser> getUsers(String playerName, boolean checkDisplayNames) {
		Validate.notNull(playerName, "playerName is null");
		String normalizedName;
		if (checkDisplayNames) {
			// Full name normalization:
			normalizedName = this.normalizeDisplayName(playerName);
		} else {
			// Lightweight normalization:
			normalizedName = this.normalizeUserName(playerName);
		}

		List<WeakReference<SKUser>> userRefsByName = byName.getOrDefault(normalizedName, Collections.emptyList());
		Stream<SKUser> usersByNameStream = StreamUtils.mapReferents(userRefsByName.stream());
		if (!checkDisplayNames) {
			return usersByNameStream;
		} else {
			// To avoid repeatedly dereferencing the user references, we do it only once up front:
			List<SKUser> usersByName = usersByNameStream.collect(Collectors.toList());
			return Stream.concat(usersByName.stream(), onlineUsers.values().stream().filter(user -> {
				// The usersByName list is expected to be small in size, so searching through it is quick.
				if (usersByName.contains(user)) {
					// The user has already been matched by name.
					return false;
				}
				String normalizedDisplayName = this.normalizeDisplayName(user.getDisplayName()); // can be null
				return normalizedName.equals(normalizedDisplayName);
			}));
		}
	}

	@Override
	public Stream<? extends SKUser> getUsersByUUIDPrefix(String uuidPrefix) {
		Validate.notNull(uuidPrefix, "uuidPrefix is null");
		String normalizedUUIDPrefix = this.normalizeUUIDString(uuidPrefix);
		SortedMap<String, WeakReference<SKUser>> prefixMap = byUUIDString.tailMap(normalizedUUIDPrefix);
		return StreamUtils.mapReferents(prefixMap.values().stream());
	}

	@Override
	public Stream<? extends SKUser> getUsersByNamePrefix(String namePrefix, boolean checkDisplayNames) {
		Validate.notNull(namePrefix, "namePrefix is null");
		String normalizedNamePrefix;
		if (checkDisplayNames) {
			// Full name normalization:
			normalizedNamePrefix = this.normalizeDisplayName(namePrefix);
		} else {
			// Lightweight normalization:
			normalizedNamePrefix = this.normalizeUserName(namePrefix);
		}

		SortedMap<String, List<WeakReference<SKUser>>> prefixMap = byName.tailMap(normalizedNamePrefix);
		Stream<SKUser> usersByNameStream = prefixMap.values().stream().flatMap(refList -> StreamUtils.mapReferents(refList.stream()));
		if (!checkDisplayNames) {
			return usersByNameStream;
		} else {
			// To avoid repeatedly dereferencing the user references, we do it only once up front:
			List<SKUser> usersByName = usersByNameStream.collect(Collectors.toList());
			return Stream.concat(usersByName.stream(), onlineUsers.values().stream().filter(user -> {
				if (usersByName.contains(user)) {
					// The user has already been matched by name.
					return false;
				}
				String normalizedDisplayName = this.normalizeDisplayName(user.getDisplayName()); // can be null
				return (normalizedDisplayName != null && normalizedDisplayName.startsWith(normalizedNamePrefix));
			}));
		}
	}
}
