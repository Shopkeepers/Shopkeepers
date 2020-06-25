package com.nisovin.shopkeepers.shopkeeper;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	private static final class ChunkShopkeepers {

		final WorldShopkeepers worldEntry;
		final ChunkCoords chunkCoords;
		// list instead of set or map: we don't expect there to be excessive amounts of shopkeepers inside a single
		// chunk, so removal from the list should be sufficiently fast
		final List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		// Note: The chunk stays marked as active during the temporary despawning of shopkeepers during world saves.
		boolean active;
		BukkitTask activationTask = null;

		ChunkShopkeepers(WorldShopkeepers worldEntry, ChunkCoords chunkCoords, boolean active) {
			assert worldEntry != null && chunkCoords != null;
			this.worldEntry = worldEntry;
			this.chunkCoords = chunkCoords;
			this.active = active;
		}

		boolean isActivationPending() {
			return (activationTask != null);
		}

		void cancelActivationTask() {
			if (activationTask == null) return; // no activation pending
			activationTask.cancel();
			activationTask = null;
		}

		void cleanUp() {
			this.cancelActivationTask();
		}
	}

	private static final class WorldShopkeepers {

		final String worldName;
		final Map<ChunkCoords, ChunkShopkeepers> shopkeepersByChunk = new HashMap<>();
		// unmodifiable entries:
		final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeeperViewsByChunk = new HashMap<>();
		// unmodifiable map with unmodifiable entries:
		final Map<ChunkCoords, List<AbstractShopkeeper>> shopkeepersByChunkView = Collections.unmodifiableMap(shopkeeperViewsByChunk);
		int shopkeeperCount = 0;
		BukkitTask worldSaveRespawnTask = null;

		// note: already unmodifiable
		final Set<AbstractShopkeeper> shopkeepersView = new AbstractSet<AbstractShopkeeper>() {
			@Override
			public Iterator<AbstractShopkeeper> iterator() {
				return shopkeepersByChunk.values().stream()
						.flatMap(chunkEntry -> chunkEntry.shopkeepers.stream())
						.iterator();
			}

			@Override
			public int size() {
				return shopkeeperCount;
			}
		};
		// note: already unmodifiable
		final Set<ChunkCoords> activeChunksView = new AbstractSet<ChunkCoords>() {
			@Override
			public Iterator<ChunkCoords> iterator() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.map(chunkEntry -> chunkEntry.chunkCoords)
						.iterator();
			}

			@Override
			public int size() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.mapToInt(chunkEntry -> 1)
						.sum();
			}
		};
		// note: already unmodifiable
		final Set<AbstractShopkeeper> shopkeepersInActiveChunksView = new AbstractSet<AbstractShopkeeper>() {
			@Override
			public Iterator<AbstractShopkeeper> iterator() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.flatMap(chunkEntry -> chunkEntry.shopkeepers.stream())
						.iterator();
			}

			@Override
			public int size() {
				return shopkeepersByChunk.values().stream()
						.filter(chunkEntry -> chunkEntry.active)
						.mapToInt(chunkEntry -> chunkEntry.shopkeepers.size())
						.sum();
			}
		};

		WorldShopkeepers(String worldName) {
			assert worldName != null;
			this.worldName = worldName;
		}

		ChunkShopkeepers addShopkeeper(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
			assert shopkeeper != null && chunkCoords != null;
			// assert: world name matches this world entry
			// assert shopkeeper is not yet contained
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			if (chunkEntry == null) {
				// if the chunk is currently loaded, the chunk entry gets initialized as active:
				boolean chunkLoaded = chunkCoords.isChunkLoaded();
				chunkEntry = new ChunkShopkeepers(this, chunkCoords, chunkLoaded);
				shopkeepersByChunk.put(chunkCoords, chunkEntry);
				shopkeeperViewsByChunk.put(chunkCoords, Collections.unmodifiableList(chunkEntry.shopkeepers));
			}
			chunkEntry.shopkeepers.add(shopkeeper);
			shopkeeperCount += 1;
			return chunkEntry;
		}

		ChunkShopkeepers removeShopkeeper(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
			assert shopkeeper != null && chunkCoords != null;
			// assert: world name matches this world entry
			ChunkShopkeepers chunkEntry = shopkeepersByChunk.get(chunkCoords);
			if (chunkEntry == null) return null; // could not find shopkeeper
			if (chunkEntry.shopkeepers.remove(shopkeeper)) {
				shopkeeperCount -= 1;
				if (chunkEntry.shopkeepers.isEmpty()) {
					chunkEntry.cleanUp();
					shopkeepersByChunk.remove(chunkCoords);
					shopkeeperViewsByChunk.remove(chunkCoords);
				}
			}
			return chunkEntry;
		}

		boolean isWorldSaveRespawnPending() {
			return (worldSaveRespawnTask != null);
		}

		void cancelWorldSaveRespawnTask() {
			if (worldSaveRespawnTask == null) return;
			worldSaveRespawnTask.cancel();
			worldSaveRespawnTask = null;
		}

		void cleanUp() {
			this.cancelWorldSaveRespawnTask();
		}
	}

	private static final long CHUNK_ACTIVATION_DELAY_TICKS = 2;

	private final SKShopkeepersPlugin plugin;

	// all shopkeepers:
	private final Map<UUID, AbstractShopkeeper> shopkeepersByUUID = new LinkedHashMap<>();
	private final Collection<AbstractShopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersByUUID.values());
	private final Map<Integer, AbstractShopkeeper> shopkeepersById = new HashMap<>();

	// TODO shopkeepers by name TreeMap to speedup name lookups and prefix matching?
	// TODO TreeMaps for shopkeeper owners by name and uuid to speedup prefix matching?

	// virtual shopkeepers:
	// Set: allows for quick removal
	private final Set<AbstractShopkeeper> virtualShopkeepers = new LinkedHashSet<>();
	private final Collection<AbstractShopkeeper> virtualShopkeepersView = Collections.unmodifiableCollection(virtualShopkeepers);

	// by world name
	private final Map<String, WorldShopkeepers> shopkeepersByWorld = new LinkedHashMap<>();
	private final Set<String> shopkeeperWorldsView = Collections.unmodifiableSet(shopkeepersByWorld.keySet());

	// admin shopkeepers:
	private int adminShopCount = 0;
	// note: already unmodifiable
	private final Set<AbstractAdminShopkeeper> allAdminShopkeepersView = new AbstractSet<AbstractAdminShopkeeper>() {
		@Override
		public Iterator<AbstractAdminShopkeeper> iterator() {
			return getAllShopkeepers().stream()
					.filter(shopkeeper -> shopkeeper instanceof AdminShopkeeper)
					.map(shopkeeper -> (AbstractAdminShopkeeper) shopkeeper)
					.iterator();
		}

		@Override
		public int size() {
			return adminShopCount;
		}
	};

	// player shopkeepers:
	private int playerShopCount = 0;
	// note: already unmodifiable
	private final Set<AbstractPlayerShopkeeper> allPlayerShopkeepersView = new AbstractSet<AbstractPlayerShopkeeper>() {
		@Override
		public Iterator<AbstractPlayerShopkeeper> iterator() {
			return getAllShopkeepers().stream()
					.filter(shopkeeper -> shopkeeper instanceof PlayerShopkeeper)
					.map(shopkeeper -> (AbstractPlayerShopkeeper) shopkeeper)
					.iterator();
		}

		@Override
		public int size() {
			return playerShopCount;
		}
	};

	// TODO this may become out-of-sync if shop objects get despawned or removed independently, problem? potential
	// memory leak?
	// -> gets cleaned up by 'teleporter' task currently which periodically checks all activeShopkeepers entries
	// 'active': with active shop object (ie. after successful spawning)
	private final Map<String, AbstractShopkeeper> activeShopkeepers = new HashMap<>();
	private final Collection<AbstractShopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		// start shopkeeper ticking task:
		this.startShopkeeperTickTask();

		// start teleporter task:
		this.startTeleporterTask();

		// start verifier task:
		if (Settings.enableSpawnVerifier) {
			this.startSpawnVerifierTask();
		}

		Bukkit.getPluginManager().registerEvents(new WorldListener(this), plugin);
	}

	public void onDisable() {
		// unload all shopkeepers:
		this.unloadAllShopkeepers();
		assert this.getAllShopkeepers().isEmpty();

		// reset, clearing (just in case):
		shopkeepersByUUID.clear();
		shopkeepersById.clear();
		shopkeepersByWorld.clear();
		virtualShopkeepers.clear();
		activeShopkeepers.clear();
		adminShopCount = 0;
		playerShopCount = 0;
	}

	// PERIODIC TASKS

	private void startTeleporterTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			List<AbstractShopkeeper> readd = new ArrayList<>();
			Iterator<Map.Entry<String, AbstractShopkeeper>> iter = activeShopkeepers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, AbstractShopkeeper> entry = iter.next();
				AbstractShopkeeper shopkeeper = entry.getValue();
				boolean update = shopkeeper.getShopObject().check();
				if (update) { // TODO remove return boolean and instead compare old with current object id?
					// if the shopkeeper had to be respawned its shop id changed:
					// this removes the entry which was stored with the old shop id and later adds back the
					// shopkeeper with its new id
					readd.add(shopkeeper);
					iter.remove();
				}
			}
			if (!readd.isEmpty()) {
				boolean dirty = false;
				for (AbstractShopkeeper shopkeeper : readd) {
					if (shopkeeper.getShopObject().isActive()) {
						this._activateShopkeeper(shopkeeper);
					}
					if (shopkeeper.isDirty()) dirty = true;
				}

				// shop objects might have been removed or respawned, request a save:
				if (dirty) {
					this.getShopkeeperStorage().save();
				}
			}
		}, 200, 200); // 10 seconds
	}

	// TODO ideally this task should not be required..
	// TODO: actually, the shops already get respawned as part of the teleporter task when missing
	// -> remove this task and the corresponding setting?
	private void startSpawnVerifierTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			int count = 0;
			boolean dirty = false;

			for (WorldShopkeepers worldEntry : shopkeepersByWorld.values()) {
				for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
					if (!chunkEntry.active) continue;

					for (AbstractShopkeeper shopkeeper : chunkEntry.shopkeepers) {
						ShopObject shopObject = shopkeeper.getShopObject();
						if (!shopObject.needsSpawning() || shopObject.isActive()) continue;

						// deactivate by old object id:
						this._deactivateShopkeeper(shopkeeper);
						// respawn:
						boolean spawned = shopObject.spawn();
						if (!spawned) {
							Log.debug(() -> "Spawn verifier: Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
							continue;
						}
						// activate with new object id:
						this._activateShopkeeper(shopkeeper);
						count++;
						if (shopkeeper.isDirty()) dirty = true;
					}
				}
			}

			if (count > 0) {
				int finalCount = count;
				Log.debug(() -> "Spawn verifier: " + finalCount + " shopkeepers respawned");
				if (dirty) {
					this.getShopkeeperStorage().save();
				}
			}
		}, 600, 1200); // 30,60 seconds
	}

	// TICKING

	private void startShopkeeperTickTask() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			// prevents concurrent modification errors by making copies of the iterated data:
			// the copies are required because the worlds, active chunks and chunk shopkeepers may change if shopkeepers
			// get removed or chunks get loaded (which cannot be safely avoided)
			boolean dirty = false;
			for (String worldName : new ArrayList<>(this.getWorldsWithShopkeepers())) {
				for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getShopkeepersInActiveChunks(worldName))) {
					if (!shopkeeper.isValid()) continue; // skip if no longer valid
					shopkeeper.tick();
					if (shopkeeper.isDirty()) {
						dirty = true;
					}
				}
			}
			if (dirty) {
				this.getShopkeeperStorage().save();
			}
		}, 20L, 20L); // 1 second
	}

	// SHOPKEEPER CREATION

	private SKShopkeeperStorage getShopkeeperStorage() {
		return plugin.getShopkeeperStorage();
	}

	@Override
	public AbstractShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		Validate.notNull(creationData, "CreationData is null!");
		AbstractShopType<?> abstractShopType = this.validateShopType(creationData.getShopType());

		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		int id = shopkeeperStorage.getNextShopkeeperId();
		AbstractShopkeeper shopkeeper = abstractShopType.createShopkeeper(id, creationData);
		if (shopkeeper == null) {
			// invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' created null shopkeeper!");
		}

		// validate unique id:
		if (this.getShopkeeperByUniqueId(shopkeeper.getUniqueId()) != null) {
			throw new ShopkeeperCreateException("There is already a shopkeeper existing with this unique id: " + shopkeeper.getUniqueId());
		}

		// success:
		shopkeeperStorage.onShopkeeperIdUsed(id);
		if (shopkeeper.isDirty()) shopkeeperStorage.markDirty();
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.CREATED);
		return shopkeeper;
	}

	@Override
	public AbstractShopkeeper loadShopkeeper(ShopType<?> shopType, int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		AbstractShopType<?> abstractShopType = this.validateShopType(shopType);
		Validate.notNull(configSection, "Missing config section!");
		Validate.isTrue(id >= 1, "Invalid id '" + id + "': Id has to be positive!");
		Validate.isTrue(this.getShopkeeperById(id) == null, "There is already a shopkeeper existing with this id: " + id);

		AbstractShopkeeper shopkeeper = abstractShopType.loadShopkeeper(id, configSection);
		if (shopkeeper == null) {
			// invalid shop type implementation..
			throw new ShopkeeperCreateException("ShopType '" + abstractShopType.getClass().getName() + "' loaded null shopkeeper!");
		}

		// validate unique id:
		if (this.getShopkeeperByUniqueId(shopkeeper.getUniqueId()) != null) {
			throw new ShopkeeperCreateException("There is already a shopkeeper existing with this unique id: " + shopkeeper.getUniqueId());
		}

		// success:
		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		shopkeeperStorage.onShopkeeperIdUsed(id);
		if (shopkeeper.isDirty()) shopkeeperStorage.markDirty();
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.LOADED);
		return shopkeeper;
	}

	private AbstractShopType<?> validateShopType(ShopType<?> shopType) {
		Validate.notNull(shopType, "Missing shop type!");
		Validate.isTrue(shopType instanceof AbstractShopType,
				"Expecting an AbstractShopType, got " + shopType.getClass().getName());
		return (AbstractShopType<?>) shopType;
	}

	// ADD / REMOVE

	private void addShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperAddedEvent.Cause cause) {
		assert shopkeeper != null && !shopkeeper.isValid();
		assert !shopkeepersByUUID.containsKey(shopkeeper.getUniqueId());
		assert !shopkeepersById.containsKey(shopkeeper.getId());

		// store by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.put(shopkeeperUniqueId, shopkeeper);
		shopkeepersById.put(shopkeeper.getId(), shopkeeper);

		ChunkCoords chunkCoords = shopkeeper.getChunkCoords(); // null for virtual shops
		ChunkShopkeepers chunkEntry;
		if (chunkCoords == null) {
			// virtual shopkeeper:
			chunkEntry = null;
			virtualShopkeepers.add(shopkeeper);
		} else {
			// add shopkeeper to chunk:
			chunkEntry = this.addShopkeeperToChunk(shopkeeper, chunkCoords);
		}

		// update player shop count:
		if (shopkeeper instanceof AdminShopkeeper) {
			adminShopCount++;
		} else if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount++;
		}

		// inform shopkeeper:
		shopkeeper.informAdded(cause);

		// call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperAddedEvent(shopkeeper, cause));

		// activate shopkeeper:
		if (!shopkeeper.getShopObject().needsSpawning()) {
			// activate shopkeeper once at registration:
			this._activateShopkeeper(shopkeeper);
		} else if (chunkEntry != null && chunkEntry.active) {
			if (!chunkEntry.worldEntry.isWorldSaveRespawnPending()) {
				// spawn shopkeeper in active chunk:
				this.spawnShopkeeper(shopkeeper);
			} else {
				Log.debug(Settings.DebugOptions.shopkeeperActivation,
						() -> "Skipping spawning of shopkeeper at " + shopkeeper.getPositionString() + " due to pending respawn after world save."
				);
			}
		}
	}

	// only called for non-virtual shopkeepers
	private ChunkShopkeepers addShopkeeperToChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		assert shopkeeper != null && chunkCoords != null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) {
			worldEntry = new WorldShopkeepers(worldName);
			shopkeepersByWorld.put(worldName, worldEntry);
		}
		return worldEntry.addShopkeeper(shopkeeper, chunkCoords); // add to chunk
	}

	private void removeShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperRemoveEvent.Cause cause) {
		assert shopkeeper != null && shopkeeper.isValid() && cause != null;

		// despawn shopkeeper:
		this.despawnShopkeeper(shopkeeper, true);

		// call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperRemoveEvent(shopkeeper, cause));

		// inform shopkeeper:
		shopkeeper.informRemoval(cause);

		// remove shopkeeper by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.remove(shopkeeperUniqueId);
		shopkeepersById.remove(shopkeeper.getId());

		ChunkCoords chunkCoords = shopkeeper.getChunkCoords(); // null for virtual shops
		if (chunkCoords == null) {
			// virtual shopkeeper:
			virtualShopkeepers.remove(shopkeeper);
		} else {
			// remove shopkeeper from chunk:
			this.removeShopkeeperFromChunk(shopkeeper, chunkCoords);
		}

		// update player shop count:
		if (shopkeeper instanceof AdminShopkeeper) {
			adminShopCount--;
		} else if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount--;
		}

		// remove shopkeeper from storage:
		this.getShopkeeperStorage().clearShopkeeperData(shopkeeper);
	}

	// only called for non-virtual shopkeepers
	private void removeShopkeeperFromChunk(AbstractShopkeeper shopkeeper, ChunkCoords chunkCoords) {
		assert shopkeeper != null && chunkCoords != null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // could not find shopkeeper
		worldEntry.removeShopkeeper(shopkeeper, chunkCoords); // remove from chunk
		if (worldEntry.shopkeeperCount <= 0) {
			worldEntry.cleanUp();
			shopkeepersByWorld.remove(worldName);
		}
	}

	private void unloadShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null && shopkeeper.isValid();
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.UNLOAD);
	}

	public void unloadAllShopkeepers() {
		for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getAllShopkeepers())) {
			this.unloadShopkeeper(shopkeeper);
		}
	}

	public void deleteShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "Shopkeeper is null!");
		Validate.isTrue(shopkeeper.isValid(), "Shopkeeper is invalid!");
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.DELETE);
	}

	public void deleteAllShopkeepers() {
		for (AbstractShopkeeper shopkeeper : new ArrayList<>(this.getAllShopkeepers())) {
			this.deleteShopkeeper(shopkeeper);
		}
	}

	// this does not get called for virtual shopkeepers
	public void onShopkeeperMove(AbstractShopkeeper shopkeeper, ChunkCoords oldChunk) {
		assert shopkeeper != null && oldChunk != null;
		ChunkCoords newChunk = shopkeeper.getChunkCoords();
		assert newChunk != null;
		if (!newChunk.equals(oldChunk)) {
			// remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper, oldChunk);

			// add to new chunk:
			this.addShopkeeperToChunk(shopkeeper, newChunk);
		}
	}

	// CHUNK ACTIVATION

	private ChunkShopkeepers getChunkEntry(ChunkCoords chunkCoords) {
		if (chunkCoords == null) return null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return null; // there are no shopkeepers in this world
		return worldEntry.shopkeepersByChunk.get(chunkCoords);
	}

	void onChunkLoad(Chunk chunk) {
		assert chunk != null;
		ChunkCoords chunkCoords = new ChunkCoords(chunk);
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return; // there are no shopkeepers in this chunk

		// chunk is not expected to already be active or pending activation (if chunk loading and unloading events are
		// consistently ordered and correctly handled by us):
		if (chunkEntry.active) {
			Log.debug(Settings.DebugOptions.shopkeeperActivation,
					() -> "Detected chunk load for already active chunk: " + TextUtils.getChunkString(chunkCoords)
			);
			return;
		} else if (chunkEntry.isActivationPending()) {
			Log.debug(Settings.DebugOptions.shopkeeperActivation,
					() -> "Detected chunk load for already pending chunk activation: " + TextUtils.getChunkString(chunkCoords)
			);
			return;
		}

		// defer activation to not activate shopkeepers for only briefly loaded chunks:
		chunkEntry.activationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			assert chunkCoords.isChunkLoaded(); // we stop the task on chunk unloads
			chunkEntry.activationTask = null;
			this.activateChunk(chunkEntry);
		}, CHUNK_ACTIVATION_DELAY_TICKS);
	}

	private void activateChunk(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null && chunkEntry.chunkCoords.isChunkLoaded();
		if (chunkEntry.active) { // already active
			assert !chunkEntry.isActivationPending();
			return;
		}
		chunkEntry.cancelActivationTask(); // stop pending activation if any
		chunkEntry.active = true; // mark chunk active

		// inform shopkeepers:
		for (AbstractShopkeeper shopkeeper : chunkEntry.shopkeepers) {
			shopkeeper.getShopObject().onChunkActivation();
		}

		// spawn shopkeepers:
		this.spawnShopkeepers(chunkEntry, false);
	}

	private void spawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSavingFinished) {
		assert chunkEntry != null && chunkEntry.active;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		if (chunkEntry.worldEntry.isWorldSaveRespawnPending()) {
			Log.debug(Settings.DebugOptions.shopkeeperActivation,
					() -> "Skipping spawning of " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
							+ ": Respawn pending after world save."
			);
			return;
		}

		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Spawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);

		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			// spawn shopkeeper:
			this.spawnShopkeeper(shopkeeper);
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		if (dirty) {
			// save delayed:
			plugin.getShopkeeperStorage().saveDelayed();
		}
	}

	// CHUNK DEACTIVATION

	void onChunkUnload(Chunk chunk) {
		assert chunk != null;
		ChunkCoords chunkCoords = new ChunkCoords(chunk);
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return; // there are no shopkeepers in this chunk

		this.deactivateChunk(chunkEntry);
	}

	private void deactivateChunk(ChunkShopkeepers chunkEntry) {
		assert chunkEntry != null;
		if (!chunkEntry.active) { // already inactive
			chunkEntry.cancelActivationTask(); // stop pending activation if any
			return;
		}
		assert !chunkEntry.isActivationPending();
		chunkEntry.active = false; // mark chunk inactive

		// inform shopkeepers:
		for (AbstractShopkeeper shopkeeper : chunkEntry.shopkeepers) {
			shopkeeper.getShopObject().onChunkDeactivation();
		}

		// despawn shopkeepers:
		this.despawnShopkeepers(chunkEntry, false);
	}

	// chunk might already be marked inactive when this is called
	private void despawnShopkeepers(ChunkShopkeepers chunkEntry, boolean worldSaving) {
		assert chunkEntry != null;
		Collection<? extends AbstractShopkeeper> shopkeepers = chunkEntry.shopkeepers;
		if (shopkeepers.isEmpty()) return;

		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Despawning " + shopkeepers.size() + " shopkeepers in chunk " + TextUtils.getChunkString(chunkEntry.chunkCoords)
						+ (worldSaving ? " (world saving)" : "")
		);

		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			// skip shopkeepers which are kept active all the time:
			if (!shopObject.needsSpawning()) continue;
			// if world save: skip shopkeepers that do not need to be despawned:
			if (worldSaving && !shopObject.despawnDuringWorldSaves()) {
				continue;
			}

			// despawn shopkeeper:
			this.despawnShopkeeper(shopkeeper, false);
		}
	}

	// WORLD LOAD

	// TODO this might not be needed, because chunk entries for loaded chunks get activated automatically when the first
	// shopkeeper gets added
	public void activateShopkeepersInAllWorlds() {
		// activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		for (World world : Bukkit.getWorlds()) {
			this.activateChunks(world);
		}
	}

	void onWorldLoad(World world) {
		assert world != null;
		this.activateChunks(world);
	}

	// activates all loaded chunks of the given world
	private void activateChunks(World world) {
		assert world != null;
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // there are no shopkeepers in this world

		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Spawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldName + "'"
		);

		// activate loaded chunks:
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			// check if already active or activation pending (avoids unnecessary isChunkLoaded calls):
			if (chunkEntry.active || chunkEntry.isActivationPending()) {
				continue;
			}
			if (!chunkEntry.chunkCoords.isChunkLoaded()) {
				continue; // chunk is not loaded
			}
			this.activateChunk(chunkEntry);
		}
	}

	// WORLD UNLOAD

	public void deactivateShopkeepersInAllWorlds() {
		// deactivate (despawn) shopkeepers in all loaded worlds:
		for (World world : Bukkit.getWorlds()) {
			this.deactivateChunks(world);
		}
	}

	void onWorldUnload(World world) {
		assert world != null;
		this.deactivateChunks(world);
	}

	private void deactivateChunks(World world) {
		assert world != null;
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // there are no shopkeepers in this world

		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Despawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldName + "'"
		);

		// cancel world save respawn task:
		worldEntry.cancelWorldSaveRespawnTask();

		// deactivate chunks:
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			this.deactivateChunk(chunkEntry);
		}
	}

	// WORLD SAVE

	void onWorldSave(World world) {
		String worldName = world.getName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return; // there are no shopkeepers in this world

		if (worldEntry.isWorldSaveRespawnPending()) {
			// already despawned the shopkeepers due to another world save just recently
			Log.debug(Settings.DebugOptions.shopkeeperActivation,
					() -> "Detected another world save while shopkeepers were already despawned due to a previous world save: " + worldName
			);
			return;
		}

		this.despawnShopkeepersInWorld(worldEntry, true);
		worldEntry.worldSaveRespawnTask = Bukkit.getScheduler().runTask(plugin, () -> {
			// assert: world is still loaded and world entry is still valid (the task gets cancelled on world unload and
			// world entry cleanup)
			worldEntry.worldSaveRespawnTask = null;
			this.spawnShopkeepersInWorld(worldEntry, true);
		});
	}

	private void despawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSaving) {
		assert worldEntry != null;
		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Despawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSaving ? " (world saving)" : "")
		);
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			if (chunkEntry.active) {
				this.despawnShopkeepers(chunkEntry, worldSaving);
			}
		}
	}

	private void spawnShopkeepersInWorld(WorldShopkeepers worldEntry, boolean worldSavingFinished) {
		assert worldEntry != null;
		Log.debug(Settings.DebugOptions.shopkeeperActivation,
				() -> "Spawning " + worldEntry.shopkeeperCount + " shopkeepers in world '" + worldEntry.worldName + "'"
						+ (worldSavingFinished ? " (world saving finished)" : "")
		);
		for (ChunkShopkeepers chunkEntry : worldEntry.shopkeepersByChunk.values()) {
			if (chunkEntry.active) {
				this.spawnShopkeepers(chunkEntry, worldSavingFinished);
			}
		}
	}

	// SHOPKEEPER ACTIVATION

	private void spawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		ShopObject shopObject = shopkeeper.getShopObject();
		if (!shopObject.needsSpawning()) return;

		boolean activate = false;
		// spawn if not yet active:
		if (!shopObject.isActive()) {
			// deactivate shopkeeper by old shop object id, in case there is one:
			this._deactivateShopkeeper(shopkeeper);

			boolean spawned = shopObject.spawn();
			if (spawned) {
				// activate with new object id:
				activate = true;
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		} else if (this.getActiveShopkeeper(shopObject.getId()) == null) {
			// already active but missing activation, activate with current object id:
			activate = true;
		}
		if (activate) {
			// activate with current object id:
			this._activateShopkeeper(shopkeeper);
		}
	}

	private void despawnShopkeeper(AbstractShopkeeper shopkeeper, boolean closeWindows) {
		assert shopkeeper != null;
		if (closeWindows) {
			// delayed closing of all open windows:
			shopkeeper.closeAllOpenWindows();
		}
		this._deactivateShopkeeper(shopkeeper);
		shopkeeper.getShopObject().despawn();
	}

	// performs some validation before actually activating a shopkeeper:
	// returns false if some validation failed
	private boolean _activateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getShopObject().getId(); // current object id
		if (objectId == null) {
			// currently only null is considered invalid,
			// prints 'null' to log then:
			Log.warning("Detected shopkeeper with invalid object id: " + objectId);
			return false;
		} else if (activeShopkeepers.containsKey(objectId)) {
			Log.warning("Detected shopkeepers with duplicate object id: " + objectId);
			return false;
		} else {
			// deactivate by old id in case there is one:
			this._deactivateShopkeeper(shopkeeper);
			assert shopkeeper.getShopObject().getLastId() == null;

			// activate shopkeeper:
			activeShopkeepers.put(objectId, shopkeeper);
			shopkeeper.getShopObject().setLastId(objectId); // remember object id
			return true;
		}
	}

	private boolean _deactivateShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getShopObject().getLastId(); // can be null
		if (objectId != null) {
			shopkeeper.getShopObject().setLastId(null);
			if (activeShopkeepers.get(objectId) == shopkeeper) {
				activeShopkeepers.remove(objectId);
				return true;
			}
		}
		return false;
	}

	// this can be used if the shopkeeper's object id has changed for some reason
	public void onShopkeeperObjectIdChanged(AbstractShopkeeper shopkeeper) {
		// deactivate by old (last) object id:
		this._deactivateShopkeeper(shopkeeper);
		// re-activate by new (current) object id:
		this._activateShopkeeper(shopkeeper);
	}

	///// QUERYING

	@Override
	public Collection<? extends AbstractShopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public AbstractShopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId) {
		return shopkeepersByUUID.get(shopkeeperUniqueId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperById(int shopkeeperId) {
		return shopkeepersById.get(shopkeeperId);
	}

	// VIRTUAL SHOPS

	@Override
	public Collection<? extends AbstractShopkeeper> getVirtualShopkeepers() {
		return virtualShopkeepersView;
	}

	// ADMIN SHOPS

	@Override
	public Collection<? extends AbstractAdminShopkeeper> getAllAdminShopkeepers() {
		return allAdminShopkeepersView;
	}

	// PLAYER SHOPS

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getAllPlayerShopkeepers() {
		return allPlayerShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getPlayerShopkeepersByOwner(UUID ownerUUID) {
		Validate.notNull(ownerUUID, "Owner UUID is null!");
		// TODO improve? maybe keep an index of player shops? or even index by owner?
		// note: already unmodifiable
		return new AbstractSet<AbstractPlayerShopkeeper>() {
			private Stream<AbstractPlayerShopkeeper> createStream() {
				return getAllShopkeepers().stream()
						.filter(shopkeeper -> shopkeeper instanceof PlayerShopkeeper)
						.map(shopkeeper -> (AbstractPlayerShopkeeper) shopkeeper)
						.filter(shopkeeper -> shopkeeper.isOwner(ownerUUID));
			}

			@Override
			public Iterator<AbstractPlayerShopkeeper> iterator() {
				return this.createStream().iterator();
			}

			@Override
			public int size() {
				return this.createStream().mapToInt(shopkeeper -> 1).sum();
			}
		};
	}

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getPlayerShopkeepersByOwner(User owner) {
		Validate.notNull(owner, "owner is null");
		return this.getPlayerShopkeepersByOwner(owner.getUniqueId());
	}

	// BY NAME

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByName(String shopName) {
		String normalizedShopName = StringUtils.normalize(TextUtils.stripColor(shopName));
		if (StringUtils.isEmpty(normalizedShopName)) return Stream.empty();

		// TODO improve via (Tree)Map?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // can be empty
			if (shopkeeperName.isEmpty()) return false; // has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			return (shopkeeperName.equals(normalizedShopName)); // include shopkeeper if name matches
		});
	}

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByNamePrefix(String shopNamePrefix) {
		String normalizedShopNamePrefix = StringUtils.normalize(TextUtils.stripColor(shopNamePrefix));
		if (StringUtils.isEmpty(normalizedShopNamePrefix)) return Stream.empty();

		// TODO improve via TreeMap?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // can be empty
			if (shopkeeperName.isEmpty()) return false; // has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			return (shopkeeperName.startsWith(normalizedShopNamePrefix)); // include shopkeeper if name matches
		});
	}

	// BY WORLD

	@Override
	public Collection<String> getWorldsWithShopkeepers() {
		return shopkeeperWorldsView;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInWorld(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.shopkeepersView;
	}

	@Override
	public Map<ChunkCoords, ? extends Collection<? extends AbstractShopkeeper>> getShopkeepersByChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptyMap();
		return worldEntry.shopkeepersByChunkView;
	}

	// ACTIVE CHUNKS

	@Override
	public Collection<ChunkCoords> getActiveChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.activeChunksView;
	}

	@Override
	public boolean isChunkActive(ChunkCoords chunkCoords) {
		ChunkShopkeepers chunkEntry = this.getChunkEntry(chunkCoords);
		if (chunkEntry == null) return false;
		return chunkEntry.active;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInActiveChunks(String worldName) {
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		return worldEntry.shopkeepersInActiveChunksView;
	}

	// BY CHUNK

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords) {
		Validate.notNull(chunkCoords, "ChunkCoords is null!");
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldEntry = shopkeepersByWorld.get(worldName);
		if (worldEntry == null) return Collections.emptySet();
		List<AbstractShopkeeper> shopkeepersInChunkView = worldEntry.shopkeepersByChunkView.get(chunkCoords);
		if (shopkeepersInChunkView == null) return Collections.emptySet();
		return shopkeepersInChunkView; // unmodifiable already
	}

	// BY LOCATION

	public boolean isShopkeeperAtLocation(Location location) {
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");
		String worldName = world.getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		ChunkCoords chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
		for (AbstractShopkeeper shopkeeper : this.getShopkeepersInChunk(chunkCoords)) {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersAtLocation(Location location) {
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");
		String worldName = world.getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		ChunkCoords chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
		for (AbstractShopkeeper shopkeeper : this.getShopkeepersInChunk(chunkCoords)) {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				shopkeepers.add(shopkeeper);
			}
		}
		return shopkeepers;
	}

	// BY SHOP OBJECT

	@Override
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public AbstractShopkeeper getActiveShopkeeper(String objectId) {
		return activeShopkeepers.get(objectId);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractEntityShopObjectType) {
				String objectId = ((AbstractEntityShopObjectType<?>) shopObjectType).createObjectId(entity);
				AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
				if (shopkeeper != null) return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return (this.getShopkeeperByEntity(entity) != null);
	}

	@Override
	public AbstractShopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		for (ShopObjectType<?> shopObjectType : plugin.getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (shopObjectType instanceof AbstractBlockShopObjectType) {
				String objectId = ((AbstractBlockShopObjectType<?>) shopObjectType).createObjectId(block);
				AbstractShopkeeper shopkeeper = this.getActiveShopkeeper(objectId);
				if (shopkeeper != null) return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeperByBlock(block) != null);
	}
}
