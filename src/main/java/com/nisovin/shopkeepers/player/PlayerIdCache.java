package com.nisovin.shopkeepers.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Stores known player uuids and names.
 * <p>
 * Note: We only store one name per player uuid.
 */
public class PlayerIdCache {

	private static final String LOG_PREFIX = "[PlayerIdCache] ";

	private static class CacheEntry<T> {

		private T value; // can be null
		private int tickets = 0;

		public CacheEntry(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}

		public int getTickets() {
			return tickets;
		}

		public void addTicket() {
			tickets += 1;
		}

		public void removeTicket() {
			tickets -= 1;
		}
	}

	// by UUID string
	private final NavigableMap<String, CacheEntry<PlayerId>> byUUID = new TreeMap<>();
	// by lowercase name; uses the same CacheEntry instances as byUUID; contains at most one entry per uuid
	private final NavigableMap<String, List<CacheEntry<PlayerId>>> byName = new TreeMap<>();

	public PlayerIdCache() {
	}

	/**
	 * Completely clears the cache.
	 */
	public void clear() {
		Log.debug(Settings.DebugOptions.playerIdCache, () -> LOG_PREFIX + "Clearing cache (old size: " + byUUID.size() + ")");
		byUUID.clear();
		byName.clear();
	}

	public void addPlayerId(Player player) {
		Validate.notNull(player, "Player is null!");
		this.addPlayerId(player.getUniqueId(), player.getName(), true);
	}

	public void addPlayerId(UUID playerUUID, String playerName, boolean updateName) {
		// this also validates player uuid and name:
		this.addPlayerId(new PlayerId(playerUUID, playerName), updateName);
	}

	// updateName: whether to update the name in case a PlayerId for this uuid already exists
	public void addPlayerId(PlayerId playerId, boolean updateName) {
		Validate.notNull(playerId, "PlayerId is null!");
		UUID playerUUID = playerId.getUniqueId();
		String uuidKey = playerUUID.toString(); // expected to be lowercase
		String playerName = playerId.getName();

		// get cache entry by uuid string:
		CacheEntry<PlayerId> cacheEntry = byUUID.get(uuidKey);
		if (cacheEntry == null) {
			// add new cache entry:
			Log.debug(Settings.DebugOptions.playerIdCache,
					() -> LOG_PREFIX + "Caching new player id (new size: " + (byUUID.size() + 1) + "): " + playerId.toString()
			);
			cacheEntry = new CacheEntry<>(playerId);
			byUUID.put(uuidKey, cacheEntry);
			cacheEntry.addTicket();
			// continue adding new CacheEntry to byName mapping
		} else {
			// we already have a CacheEntry for this uuid
			PlayerId cachedPlayerId = cacheEntry.getValue();
			Log.debug(Settings.DebugOptions.playerIdCache, () -> LOG_PREFIX + "Adding ticket for cached player id: " + cachedPlayerId);
			cacheEntry.addTicket();

			// assumption: byName mapping already contains the CacheEntry as well
			// but the cached name for the uuid might be different to the given name:
			String cachedPlayerName = cachedPlayerId.getName();
			if (updateName && !cachedPlayerName.equals(playerName)) {
				Log.debug(Settings.DebugOptions.playerIdCache,
						() -> LOG_PREFIX + "Updating cached player name from '" + cachedPlayerName + "' to '" + playerName + "'."
				);
				// update the cached player name (by exchanging the cached PlayerId):
				cacheEntry.setValue(playerId);
				// Note: This also updates the CacheEntry in the byName mapping (because they share the same CacheEntry
				// instances)
			}
			return;
		}

		// cacheEntry is the new CacheEntry. Add it to the byName mapping as well:
		assert cacheEntry != null;
		String playerNameKey = playerName.toLowerCase(Locale.ROOT);
		List<CacheEntry<PlayerId>> byNameEntries = byName.get(playerNameKey);
		if (byNameEntries == null) {
			// add new byName entry:
			// assumption: most cache entries will only have one entry
			byNameEntries = new ArrayList<>(1);
			byName.put(playerNameKey, byNameEntries);
		}
		byNameEntries.add(cacheEntry);
	}

	public void removePlayerId(Player player) {
		Validate.notNull(player, "Player is null!");
		this.removePlayerId(player.getUniqueId(), player.getName());
	}

	public void removePlayerId(UUID playerUUID, String playerName) {
		// this also validates player uuid and name:
		this.removePlayerId(new PlayerId(playerUUID, playerName));
	}

	public void removePlayerId(PlayerId playerId) {
		Validate.notNull(playerId, "PlayerId is null!");
		UUID playerUUID = playerId.getUniqueId();
		String uuidString = playerUUID.toString();

		// get cache entry by uuid string:
		CacheEntry<PlayerId> cacheEntry = byUUID.get(uuidString);
		if (cacheEntry == null) {
			// there is no entry for this uuid cached:
			Log.debug(Settings.DebugOptions.playerIdCache, () -> LOG_PREFIX + "No player id cached for: " + playerId);
			return;
		}

		PlayerId cachedPlayerId = cacheEntry.getValue();
		if (cacheEntry.getTickets() > 1) {
			// cache entry is still valid:
			Log.debug(Settings.DebugOptions.playerIdCache, () -> LOG_PREFIX + "Removing ticket for cached player id: " + cachedPlayerId);
			cacheEntry.removeTicket();
			return;
		}
		Log.debug(Settings.DebugOptions.playerIdCache,
				() -> LOG_PREFIX + "Removing cached player id (new size: " + (byUUID.size() - 1) + "): " + cachedPlayerId.toString()
		);

		// remove cache entry:
		byUUID.remove(uuidString);

		// using the cached player name to find the entry in the byName mapping:
		String cachedPlayerName = cachedPlayerId.getName();
		String playerNameKey = cachedPlayerName.toLowerCase(Locale.ROOT);
		List<CacheEntry<PlayerId>> byNameEntries = byName.get(playerNameKey);
		byNameEntries.remove(cacheEntry);
	}

	public Iterable<PlayerId> getAll() {
		return byUUID.values().stream().map(e -> e.getValue())::iterator;
	}

	public Stream<PlayerId> getByUUIDPrefix(String uuidPrefix) {
		Validate.notNull(uuidPrefix, "UUID prefix is null!");
		SortedMap<String, CacheEntry<PlayerId>> prefixMap = byUUID.tailMap(uuidPrefix.toLowerCase(Locale.ROOT));
		return prefixMap.values().stream().map(e -> e.getValue());
	}

	public Stream<PlayerId> getByNamePrefix(String namePrefix) {
		Validate.notNull(namePrefix, "Name prefix is null!");
		SortedMap<String, List<CacheEntry<PlayerId>>> prefixMap = byName.tailMap(namePrefix.toLowerCase(Locale.ROOT));
		return prefixMap.values().stream().flatMap(list -> list.stream().map(e -> e.getValue()));
	}
}
