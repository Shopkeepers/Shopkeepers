package com.nisovin.shopkeepers.shopkeeper.spawning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.activation.ShopkeeperChunkActivator;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopkeeper.spawning.ShopkeeperSpawnState.State;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.util.taskqueue.TaskQueueStatistics;

/**
 * Responsible for the spawning of shopkeepers.
 */
public class ShopkeeperSpawner {

	// Note: We assume that the shopkeeper entities spawned by us are either stationary, or marked
	// as non-persistent, or both. Because otherwise, if they get teleported into another chunk, or
	// even another world, and we don't update the location of the corresponding shopkeeper
	// immediately, we would need to check for them during chunk unloads, world unloads, and world
	// saves (which we currently don't) in order to remove them again before they get saved to disk.
	// All shopkeeper entities spawned by us are marked as non-persistent, and are usually also
	// stationary, unless some other plugin moves them.

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;
	private final WorldSaveDespawner worldSaveDespawner;
	private final ShopkeeperSpawnerWorldListener listener;

	// A queue that prevents performance drops caused by the spawning of too many shopkeepers at the
	// same time (e.g. when a newly activated chunk contains many shopkeepers).
	// However, in order to avoid that players have to wait for shopkeepers to spawn, there are some
	// situations in which we spawn shopkeepers immediately instead of adding them to the queue.
	// This includes: When a shopkeeper is newly created, when shopkeepers are loaded (i.e. on
	// plugin reloads), and when shopkeepers are respawned after world saves. In the latter two
	// cases, a potentially large number of shopkeepers is expected to be spawned at the same time.
	// Due to its limited throughput, the queue would not be able to deal with this sudden peak
	// appropriately.
	// However, since these situations are associated with a certain performance impact anyway, we
	// prefer to spawn all affected shopkeepers immediately, instead of causing confusion due to
	// players having to wait for shopkeepers to respawn.
	private final ShopkeeperSpawnQueue spawnQueue;

	// World entries are lazily added: They might not be added immediately when a shopkeeper is
	// added to a world.
	// In order to track which worlds are currently being saved, we automatically add a new world
	// entry whenever a world is saved, even if the world does not contain any shopkeepers yet.
	// World entries are removed again once the world has been unloaded and the last shopkeeper has
	// been removed.
	private final Map<String, WorldData> worlds = new HashMap<>();

	public ShopkeeperSpawner(SKShopkeepersPlugin plugin, SKShopkeeperRegistry shopkeeperRegistry) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(shopkeeperRegistry, "shopkeeperRegistry is null");
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.worldSaveDespawner = new WorldSaveDespawner(
				Unsafe.initialized(this),
				plugin,
				shopkeeperRegistry
		);
		this.listener = new ShopkeeperSpawnerWorldListener(
				Unsafe.initialized(this),
				worldSaveDespawner
		);
		this.spawnQueue = new ShopkeeperSpawnQueue(
				plugin,
				Unsafe.initialized(this)::doSpawnShopkeeper
		);
	}

	public void onEnable() {
		// Start the spawn queue:
		spawnQueue.start();

		Bukkit.getPluginManager().registerEvents(listener, plugin);

		SchedulerUtils.runAsyncTaskLaterOrOmit(new CheckUnspawnableShopkeepersTask(), 5L);
	}

	private class CheckUnspawnableShopkeepersTask implements Runnable {
		@Override
		public void run() {
			// Check for shopkeepers that failed to spawn during their last spawn attempt:
			checkUnspawnableShopkeepers(Settings.deleteUnspawnableShopkeepers, false);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(listener);

		// Shutdown the spawn queue:
		spawnQueue.shutdown();

		// We don't expect the plugin to be disabled or reloaded during world saves. Otherwise, if
		// the plugin is reloaded, the shopkeepers might get immediately respawned while the world
		// save is still in progress.
		// However, during normal server shutdowns, the worlds might get saved and the plugin
		// disabled before our respawn tasks are run.
		// Cancel all pending world save respawn tasks:
		worlds.values().forEach(WorldData::cleanUp);

		// Remove all cached world data:
		worlds.clear();
	}

	// DATA

	// Returns null if there is no data for the specified world.
	@Nullable
	WorldData getWorldData(String worldName) {
		assert worldName != null;
		return worlds.get(worldName);
	}

	WorldData getOrCreateWorldData(String worldName) {
		assert worldName != null;
		WorldData worldData = worlds.computeIfAbsent(worldName, WorldData::new);
		assert worldData != null;
		return worldData;
	}

	private @Nullable WorldData removeWorldData(String worldName) {
		assert worldName != null;
		WorldData worldData = worlds.remove(worldName);
		if (worldData != null) {
			worldData.cleanUp();
		}
		return worldData;
	}

	// Called by SKShopkeeperRegistry when the last shopkeeper was removed from a world.
	public void onShopkeeperWorldRemoved(String worldName) {
		assert worldName != null;
		// We only clean up the world data if the world is currently unloaded, because otherwise we
		// still want to keep track whether the world is currently being saved.
		// Note: If the last shopkeeper is removed while the world is currently being unloaded, we
		// assume that this occurs before our world-unload event handler is invoked (which listens
		// on MONITOR priority).
		if (Bukkit.getWorld(worldName) == null) {
			this.removeWorldData(worldName);
		}
	}

	// WORLD EVENTS

	void onWorldUnload(World world) {
		assert world != null;
		// If the world contains no shopkeepers, remove any currently stored data for the world.
		String worldName = world.getName();
		if (shopkeeperRegistry.getShopkeepersInWorld(worldName).isEmpty()) {
			// This will also cancel any currently pending world save respawn task.
			this.removeWorldData(worldName);
		}
	}

	// SHOPKEEPER SPAWN STATE

	private static final Predicate<AbstractShopkeeper> IS_SPAWNING = ShopkeeperSpawner::isSpawning;
	private static final Predicate<AbstractShopkeeper> IS_DESPAWNING = ShopkeeperSpawner::isDespawning;

	private static State getSpawnState(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		return shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class).getState();
	}

	private static boolean isSpawning(AbstractShopkeeper shopkeeper) {
		return (getSpawnState(shopkeeper) == State.SPAWNING);
	}

	private static boolean isDespawning(AbstractShopkeeper shopkeeper) {
		return (getSpawnState(shopkeeper) == State.DESPAWNING);
	}

	// Sets the shopkeeper's spawn state, but first performs any necessary cleanup depending on its
	// previous spawn state. This is called whenever the shopkeeper is requested to be spawned or
	// despawned.
	private void updateSpawnState(AbstractShopkeeper shopkeeper, State newState) {
		assert shopkeeper != null;
		ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);

		// Remove the shopkeeper from the spawn queue, if necessary:
		if (spawnState.getState() == State.QUEUED) {
			spawnQueue.remove(shopkeeper);
		}

		// Set new spawn state:
		spawnState.setState(newState);
	}

	// SHOPKEEPER SPAWNING

	public void spawnShopkeeperImmediately(AbstractShopkeeper shopkeeper) {
		// In order to not have players wait for newly created shopkeepers, teleported shopkeepers,
		// or loaded shopkeepers after plugin/storage reloads, we don't use the spawn queue in those
		// cases, but spawn the shopkeeper immediately.
		this.spawnShopkeeper(shopkeeper, true);
	}

	/**
	 * Spawns the given shopkeeper, if necessary.
	 * <p>
	 * This is called in various situations:
	 * <ul>
	 * <li>Called by {@link ShopkeeperChunkActivator} when a newly added shopkeeper has been added
	 * to an active chunk.
	 * <li>Called by {@link ShopkeeperChunkActivator} when a shopkeeper has been moved from an
	 * inactive to an active chunk.
	 * <li>Called by {@link #onShopkeeperMoved(AbstractShopkeeper, ChunkCoords, boolean)} when a
	 * previously active but pending to be (re-)spawned shopkeeper (the shopkeeper might be queued
	 * to be spawned, or it might wait for a world save respawn) is moved into an active chunk of a
	 * world without a pending world save respawn. The shopkeeper is meant to immediately (re-)spawn
	 * in this situation.
	 * <li>Called by {@link ShopkeeperChunkActivator} when the shopkeepers of a chunk are activated.
	 * <li>Called by {@link ShopkeeperSpawner} when the shopkeepers are respawned after a world
	 * save.
	 * </ul>
	 * <p>
	 * Spawn requests for {@link AbstractShopkeeper#isActive() inactive} shopkeepers, and shop
	 * objects that handle their spawning {@link AbstractShopObjectType#mustBeSpawned() themselves}
	 * (this also includes virtual shop objects), are silently ignored.
	 * <p>
	 * The shopkeeper might already be spawned.
	 * <p>
	 * If necessary (e.g. if the shopkeeper is already spawned but its new world is currently being
	 * saved), this may also despawn the given shopkeeper and register a pending respawn.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>, has to be valid
	 * @param spawnImmediately
	 *            <code>true</code> to spawn the shopkeeper immediately, <code>false</code> to
	 *            instead add it to the spawn queue
	 * @return {@link SpawnResult} indicating the result
	 */
	private SpawnResult spawnShopkeeper(AbstractShopkeeper shopkeeper, boolean spawnImmediately) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(shopkeeper.isValid(), "shopkeeper is invalid");

		// Ignore shop objects that handle their spawning themselves:
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		AbstractShopObjectType<?> shopObjectType = shopObject.getType();
		if (!shopObjectType.mustBeSpawned()) {
			return SpawnResult.IGNORED;
		}

		// Ignore spawn requests for inactive shopkeepers:
		if (!shopkeeper.isActive()) {
			return SpawnResult.IGNORED_INACTIVE;
		}

		boolean alreadySpawned = shopObject.isSpawned();

		String worldName = Unsafe.assertNonNull(shopkeeper.getWorldName());
		WorldData worldData = this.getOrCreateWorldData(worldName);
		assert worldData != null;
		if (worldData.isWorldSaveRespawnPending() && shopObjectType.mustDespawnDuringWorldSave()) {
			SpawnResult result;
			if (alreadySpawned) {
				Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLocatedLogPrefix()
						+ "Despawn due to pending respawn after world save.");
				// Note: This also updates the shopkeeper's spawn state.
				this.doDespawnShopkeeper(shopkeeper);
				result = SpawnResult.DESPAWNED_AND_AWAITING_WORLD_SAVE_RESPAWN;
			} else {
				Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLocatedLogPrefix()
						+ "Skipping spawning due to pending respawn after world save.");
				result = SpawnResult.AWAITING_WORLD_SAVE_RESPAWN;
			}

			// Update the shopkeeper's spawn state:
			// If the shopkeeper was previously queued to be spawned (for example when it has been
			// moved from one world to another), this removes the shopkeeper from the spawn queue
			// and lets the world save respawn handle its spawning.
			this.updateSpawnState(shopkeeper, State.PENDING_WORLD_SAVE_RESPAWN);
			return result;
		}

		// Ignore if the shopkeeper is already spawned:
		// Note: We intentionally ignore here whether the shopkeeper is queued to be spawned,
		// because when this method is called we might want to immediately spawn the shopkeeper
		// anyway.
		if (alreadySpawned) {
			// Update the shopkeeper's spawn state, just in case the shopkeeper has been spawned
			// externally by some other component.
			this.updateSpawnState(shopkeeper, State.SPAWNED);
			return SpawnResult.ALREADY_SPAWNED;
		}

		if (spawnImmediately) {
			// This also updates the shopkeeper's spawn state if necessary (remove from spawn queue,
			// etc.):
			if (this.doSpawnShopkeeper(shopkeeper)) {
				return SpawnResult.SPAWNED;
			} else {
				return SpawnResult.SPAWNING_FAILED;
			}
		} else {
			// Update the shopkeeper's spawn state (e.g. remove it from the queue if it is already
			// queued, or reset its respawn-pending state, e.g. if the shopkeeper was previously
			// located in a currently saving world):
			this.updateSpawnState(shopkeeper, State.DESPAWNED);

			// Add the shopkeeper to the spawn queue (this also updates its spawn state):
			spawnQueue.add(shopkeeper);
			return SpawnResult.QUEUED;
		}
	}

	// The shopkeeper might already be marked as inactive when this is called. However, it is still
	// marked as valid.
	public void despawnShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(shopkeeper.isValid(), "shopkeeper is invalid");

		// Ignore shop objects that handle their spawning themselves:
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		if (!shopObject.getType().mustBeSpawned()) return;

		// Despawn the shopkeeper:
		this.doDespawnShopkeeper(shopkeeper);
	}

	// Called by ShopkeeperChunkActivator when a shopkeeper has been moved from one chunk to
	// another, after the shopkeeper's activation state has been updated.
	// activationStateChanged: True if the shopkeeper's activation state has changed.
	public void onShopkeeperMoved(
			AbstractShopkeeper shopkeeper,
			ChunkCoords oldChunkCoords,
			boolean activationStateChanged
	) {
		assert shopkeeper != null && oldChunkCoords != null;
		// Goals:
		// 1) When a previously active shopkeeper that is pending to be respawned due to a world
		// save is moved into an active chunk of a world that is not being saved currently, we need
		// to immediately respawn the shopkeeper.
		// 2) When a previously active shopkeeper that was not pending to be respawned is moved into
		// an active chunk of a world that is currently being saved, we need to despawn the
		// shopkeeper and wait for the new world's pending respawn.
		// 3) If the shopkeeper was previously queued to be spawned, but is then moved somewhere, we
		// want to spawn it immediately. This ensures that when a the shopkeeper is teleported near
		// a player, the player does not need to wait for the shopkeeper to appear. This applies
		// even if the old and new worlds are the same. To handle this case, we cannot ignore
		// shopkeepers here even if they are not affected by world saves.

		// Ignore if the shopkeeper's activation state has changed:
		// The activation change has already triggered either a spawn or a despawn of the
		// shopkeeper, which take the shopkeeper's and the world's current pending respawn states
		// into account.
		if (activationStateChanged) return;

		// Ignore if the shopkeeper's activation has not changed, and the shopkeeper is currently
		// inactive: I.e. the shopkeeper was previously inactive, and does not need to be spawned in
		// its new chunk either.
		if (!shopkeeper.isActive()) return;

		// The shopkeeper moved from one active chunk to another. It is meant to be spawned
		// immediately (e.g. if it was previously only queued to be spawned). But if the new world
		// is currently being saved, it might actually need to be despawned first and then wait for
		// the world save respawn. This method takes care of these aspects:
		this.spawnShopkeeper(shopkeeper, true);
	}

	// Returns true on success.
	private boolean doSpawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		AbstractShopObjectType<?> shopObjectType = shopObject.getType();
		assert shopObjectType.mustBeSpawned();

		// Reset the shopkeeper's spawn state:
		this.updateSpawnState(shopkeeper, State.DESPAWNED);

		// TODO Handle dynamic disabling of shop object types by despawning the corresponding shop
		// objects.
		if (!shopObjectType.isEnabled()) {
			Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLogPrefix()
					+ "Object type '" + shopObjectType.getIdentifier()
					+ "' is disabled. Skipping spawning.");
			return false;
		}

		// Set the new spawn state:
		ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);
		spawnState.setState(State.SPAWNED);

		boolean spawned = false;
		try {
			// This is expected to also register the spawned shop object:
			// This has no effect if the shopkeeper is already spawned.
			spawned = shopObject.spawn();
		} catch (Throwable e) {
			Log.severe(shopkeeper.getLogPrefix() + "Error during spawning!", e);
		}
		if (spawned) {
			// Validation:
			Object objectId = shopObject.getId();
			if (objectId == null) {
				Log.warning(shopkeeper.getLogPrefix()
						+ "Successfully spawned, but provides no object id!");
			} else if (!objectId.equals(shopObject.getLastId())) {
				Log.warning(shopkeeper.getLogPrefix()
						+ "Successfully spawned, but object not registered!");
			}
			return true;
		} else {
			// Reset the shopkeeper's spawn state:
			spawnState.setState(State.DESPAWNED);

			// Due to an open Spigot 1.17 issue, entities report as 'invalid' after being spawned
			// during chunk loads. The shopkeepers plugin then assumes that the spawning failed. In
			// order to not spam with warnings, this warning has been replaced with a debug output
			// for now.
			// TODO Replace this with a warning again once the underlying issue has been resolved in
			// Spigot.
			Log.debug(shopkeeper.getLocatedLogPrefix() + "Spawning failed!");
			return false;
		}
	}

	// This has no effect if the shopkeeper is not spawned or pending to be spawned currently.
	// This also removes the shopkeeper from the spawn queue.
	private void doDespawnShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		assert shopObject.getType().mustBeSpawned();

		// Reset the shopkeeper's spawn state:
		this.updateSpawnState(shopkeeper, State.DESPAWNED);

		// Despawn the shop object:
		try {
			shopObject.despawn();
		} catch (Throwable e) {
			Log.severe(shopkeeper.getLogPrefix() + "Error during despawning!", e);
		}
	}

	// CHUNK SHOPKEEPERS

	public TaskQueueStatistics getSpawnQueueStatistics() {
		return spawnQueue;
	}

	public void spawnChunkShopkeepers(
			ChunkCoords chunkCoords,
			String spawnReason,
			Predicate<? super AbstractShopkeeper> filter,
			boolean spawnImmediately
	) {
		Collection<? extends AbstractShopkeeper> shopkeepers = shopkeeperRegistry.getShopkeepersInChunkSnapshot(chunkCoords);
		this.spawnChunkShopkeepers(chunkCoords, spawnReason, shopkeepers, filter, spawnImmediately);
	}

	// This only spawns shopkeepers if the chunk is currently active.
	public void spawnChunkShopkeepers(
			ChunkCoords chunkCoords,
			String spawnReason,
			Collection<? extends AbstractShopkeeper> shopkeepers,
			Predicate<? super AbstractShopkeeper> filter,
			boolean spawnImmediately
	) {
		assert chunkCoords != null && spawnReason != null && shopkeepers != null && filter != null;
		if (shopkeepers.isEmpty()) return;
		if (!shopkeeperRegistry.isChunkActive(chunkCoords)) return;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + shopkeepers.size() + " shopkeepers in chunk "
						+ TextUtils.getChunkString(chunkCoords)
						+ (spawnReason.isEmpty() ? "" : " (" + spawnReason + ")")
		);

		// Mark the shopkeepers as 'currently-spawning':
		shopkeepers.forEach(shopkeeper -> {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();
			// Ignore shop objects that handle their spawning themselves:
			if (!objectType.mustBeSpawned()) return;

			// Ignore shopkeepers that are not affected by this spawn request:
			if (!filter.test(shopkeeper)) return;

			// This has no noticeable effect if the shopkeeper was already in the spawning state.
			// This aborts any currently pending despawning.
			this.updateSpawnState(shopkeeper, State.SPAWNING);
		});

		int spawned = 0;
		int awaitingWorldSaveRespawn = 0;
		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();

			// Ignore shop objects that handle their spawning themselves:
			if (!objectType.mustBeSpawned()) continue;

			// Ignore shopkeepers that are not affected by this spawn request:
			if (!filter.test(shopkeeper)) continue;

			ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);

			// Skip if something else has reset the shopkeeper's currently-spawning state in the
			// meantime (e.g. if it has already been spawned or despawned in the meantime):
			if (spawnState.getState() != State.SPAWNING) {
				Log.debug(() -> shopkeeper.getLogPrefix()
						+ "  Skipping spawning because superseded by another spawn or despawn request.");
				continue;
			}

			// Note: We can assume that the chunk is still active and that the shopkeeper has not
			// moved to another chunk in the meantime, because otherwise its 'currently-spawning'
			// state would have been reset. Even if the shopkeeper moved to another chunk, and was
			// then subsequently marked as 'currently-spawning' again, this state would have been
			// reset again, because any subsequently triggered spawning is handled before the
			// control returns to the current spawning.
			assert chunkCoords.equals(shopkeeper.getLastChunkCoords());
			assert shopkeeper.isActive();

			// Spawn the shopkeeper:
			// This also updates the shopkeeper's spawn state.
			SpawnResult result = this.spawnShopkeeper(shopkeeper, spawnImmediately);
			switch (result) {
			case SPAWNED:
			case QUEUED:
				spawned++;
				break;
			case AWAITING_WORLD_SAVE_RESPAWN:
			case DESPAWNED_AND_AWAITING_WORLD_SAVE_RESPAWN:
				awaitingWorldSaveRespawn++;
				break;
			default:
				break;
			}

			// Check if the shopkeeper has been marked as dirty:
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		int spawnedFinal = spawned;
		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "  Actually spawned: " + spawnedFinal + (spawnImmediately ? "" : " (queued)"));

		if (awaitingWorldSaveRespawn > 0) {
			int awaitingWorldSaveRespawnFinal = awaitingWorldSaveRespawn;
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "  Skipped due to a pending respawn after world save: "
							+ awaitingWorldSaveRespawnFinal
			);
		}

		// If dirty, trigger a delayed save:
		if (dirty) {
			plugin.getShopkeeperStorage().saveDelayed();
		}
	}

	public void despawnChunkShopkeepers(
			ChunkCoords chunkCoords,
			String despawnReason,
			Predicate<? super AbstractShopkeeper> filter,
			@Nullable Consumer<? super AbstractShopkeeper> onDespawned
	) {
		Collection<? extends AbstractShopkeeper> shopkeepers = shopkeeperRegistry.getShopkeepersInChunkSnapshot(chunkCoords);
		this.despawnChunkShopkeepers(chunkCoords, despawnReason, shopkeepers, filter, onDespawned);
	}

	// The chunk might already be marked as inactive when this is called, but this can also be
	// called for currently active chunks.
	public void despawnChunkShopkeepers(
			ChunkCoords chunkCoords,
			String despawnReason,
			Collection<? extends AbstractShopkeeper> shopkeepers,
			Predicate<? super AbstractShopkeeper> filter,
			@Nullable Consumer<? super AbstractShopkeeper> onDespawned
	) {
		assert chunkCoords != null && despawnReason != null && shopkeepers != null && filter != null;
		if (shopkeepers.isEmpty()) return;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + shopkeepers.size() + " shopkeepers in chunk "
						+ TextUtils.getChunkString(chunkCoords)
						+ (despawnReason.isEmpty() ? "" : " (" + despawnReason + ")")
		);

		// Mark the shopkeepers as 'currently-despawning':
		shopkeepers.forEach(shopkeeper -> {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();
			// Ignore shop objects that handle their spawning themselves:
			if (!objectType.mustBeSpawned()) return;

			// Ignore shopkeepers that are not affected by this despawn request:
			if (!filter.test(shopkeeper)) return;

			// This has no noticeable effect if the shopkeeper was already in the despawning state.
			// This aborts any currently pending spawning.
			this.updateSpawnState(shopkeeper, State.DESPAWNING);
		});

		boolean initialChunkActivationState = shopkeeperRegistry.isChunkActive(chunkCoords);
		int despawned = 0;
		boolean dirty = false;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			AbstractShopObject shopObject = shopkeeper.getShopObject();
			AbstractShopObjectType<?> objectType = shopObject.getType();

			// Ignore shop objects that handle their spawning themselves:
			if (!objectType.mustBeSpawned()) continue;

			// Ignore shopkeepers that are not affected by this despawn request:
			if (!filter.test(shopkeeper)) continue;

			ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);

			// Skip if something else has reset the shopkeeper's currently-despawning state in the
			// meantime (e.g. if it has already been spawned or despawned in the meantime):
			if (spawnState.getState() != State.DESPAWNING) {
				Log.debug(() -> shopkeeper.getLogPrefix()
						+ "  Skipping despawning because superseded by another spawn or despawn request.");
				continue;
			}

			// Note: We can assume that the shopkeeper has not moved to another chunk in the
			// meantime, and that the activation state of the chunk and shopkeeper are still the
			// same as when we started the despawning, because otherwise its 'currently-despawning'
			// state would have been reset. Even if the shopkeeper moved to another chunk, and was
			// then subsequently marked as 'currently-despawning' again, this state would have been
			// reset again, because any subsequently triggered despawning is handled before the
			// control returns to the current despawning.
			assert chunkCoords.equals(shopkeeper.getLastChunkCoords());
			assert initialChunkActivationState == shopkeeper.isActive();

			// Despawn the shopkeeper:
			// This also resets the shopkeeper's spawn state.
			this.despawnShopkeeper(shopkeeper);
			despawned++;
			assert spawnState.getState() == State.DESPAWNED;

			if (onDespawned != null) {
				onDespawned.accept(shopkeeper);
			}

			// Check if the shopkeeper has been marked as dirty:
			if (shopkeeper.isDirty()) {
				dirty = true;
			}
		}

		int actuallyDespawned = despawned;
		Log.debug(DebugOptions.shopkeeperActivation, () -> "  Actually despawned: "
				+ actuallyDespawned);

		// If dirty, trigger a delayed save:
		if (dirty) {
			plugin.getShopkeeperStorage().saveDelayed();
		}
	}

	// WORLD SHOPKEEPERS

	void spawnShopkeepersInWorld(
			String worldName,
			String spawnReason,
			Predicate<? super AbstractShopkeeper> shopkeeperFilter,
			boolean spawnImmediately
	) {
		assert worldName != null && spawnReason != null && shopkeeperFilter != null;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Spawning " + shopkeeperRegistry.getShopkeepersInWorld(worldName).size()
						+ " shopkeepers in world '" + worldName + "'"
						+ (spawnReason.isEmpty() ? "" : " (" + spawnReason + ")")
		);

		// The shopkeeper chunk map can change while we iterate over these chunks. We therefore need
		// to create a snapshot of these chunks first.
		List<? extends ChunkCoords> chunks = new ArrayList<>(shopkeeperRegistry.getShopkeepersByChunks(worldName).keySet());

		// Mark all shopkeepers as 'spawning' up front, so that we can detect and account for spawn
		// state changes that happen in the meantime:
		chunks.forEach(chunkCoords -> {
			if (!shopkeeperRegistry.isChunkActive(chunkCoords)) return;

			Collection<? extends AbstractShopkeeper> chunkShopkeepers = shopkeeperRegistry.getShopkeepersInChunk(chunkCoords);
			chunkShopkeepers.forEach(shopkeeper -> {
				if (!shopkeeperFilter.test(shopkeeper)) return;

				this.updateSpawnState(shopkeeper, State.SPAWNING);
			});
		});

		// We only spawn the shopkeepers that are still marked as 'spawning' once we process them:
		Predicate<? super AbstractShopkeeper> newShopkeeperFilter = IS_SPAWNING.and(shopkeeperFilter);
		assert newShopkeeperFilter != null;

		chunks.forEach(chunkCoords -> {
			// This only spawns the shopkeepers if the chunk is still active. We don't determine the
			// active chunks up front, because the chunks can get deactivated while we iterate them,
			// so we would need to check here anyway if the chunks are still active at the time we
			// process it.
			// Also note: If the chunk became inactive in the meantime, or some shopkeepers were
			// removed, the previously set spawn states of the shopkeepers have already been reset
			// again. Any shopkeepers that have been newly added to the chunk in the meantime are
			// ignored in the following, since their spawn states will not be 'spawning'.
			this.spawnChunkShopkeepers(
					chunkCoords,
					spawnReason,
					newShopkeeperFilter,
					spawnImmediately
			);
		});
	}

	void despawnShopkeepersInWorld(
			String worldName,
			String despawnReason,
			Predicate<? super AbstractShopkeeper> shopkeeperFilter,
			@Nullable Consumer<? super AbstractShopkeeper> onDespawned
	) {
		assert worldName != null && despawnReason != null && shopkeeperFilter != null;

		Log.debug(DebugOptions.shopkeeperActivation,
				() -> "Despawning " + shopkeeperRegistry.getShopkeepersInWorld(worldName).size()
						+ " shopkeepers in world '" + worldName + "'"
						+ (despawnReason.isEmpty() ? "" : " (" + despawnReason + ")")
		);

		// The shopkeeper chunk map can change while we iterate over these chunks. We therefore need
		// to create a snapshot of these chunks first.
		List<? extends ChunkCoords> chunks = new ArrayList<>(
				shopkeeperRegistry.getShopkeepersByChunks(worldName).keySet()
		);

		// Mark all shopkeepers as 'despawning' up front, so that we can detect and account for
		// spawn state changes that happen in the meantime:
		chunks.forEach(chunkCoords -> {
			if (!shopkeeperRegistry.isChunkActive(chunkCoords)) return;

			Collection<? extends AbstractShopkeeper> chunkShopkeepers = shopkeeperRegistry.getShopkeepersInChunk(chunkCoords);
			chunkShopkeepers.forEach(shopkeeper -> {
				if (!shopkeeperFilter.test(shopkeeper)) return;

				this.updateSpawnState(shopkeeper, State.DESPAWNING);
			});
		});

		// We only despawn the shopkeepers that are still marked as 'despawning' once we process
		// them:
		Predicate<? super AbstractShopkeeper> newShopkeeperFilter = IS_DESPAWNING.and(shopkeeperFilter);
		assert newShopkeeperFilter != null;

		chunks.forEach(chunkCoords -> {
			// We don't determine the active chunks up front, because the chunks can get deactivated
			// while we iterate them, so we would need to check here anyway if the chunks are still
			// active at the time we process them.
			// Also note: If the chunk became inactive in the meantime, or some shopkeepers were
			// removed, the previously set spawn states of the shopkeepers have already been reset
			// again. Any shopkeepers that have been newly added to the chunk in the meantime are
			// ignored in the following, since their spawn states will not be 'despawning'.
			if (!shopkeeperRegistry.isChunkActive(chunkCoords)) return;
			this.despawnChunkShopkeepers(
					chunkCoords,
					despawnReason,
					newShopkeeperFilter,
					onDespawned
			);
		});
	}

	// ANALYSIS

	/**
	 * Checks for and optionally warns about or deletes shopkeepers that failed to spawn the last
	 * time we tried to spawn them.
	 * <p>
	 * This only handles shopkeepers that are spawned by us (see
	 * {@link AbstractShopObjectType#mustBeSpawned()}.
	 * 
	 * @param deleteUnspawnableShopkeepers
	 *            <code>true</code> to also delete any found shopkeepers that failed to spawn
	 * @param silent
	 *            <code>true</code> to not log warnings about any found unspawnable shopkeepers
	 * @return the found unspawnable shopkeepers
	 */
	public List<Shopkeeper> checkUnspawnableShopkeepers(boolean deleteUnspawnableShopkeepers, boolean silent) {
		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> unspawnableShopkeepers = new ArrayList<>();
		shopkeeperRegistry.getAllShopkeepers().forEach(shopkeeper -> {
			var shopObject = shopkeeper.getShopObject();
			// Skip shopkeepers that are not spawned by us:
			if (!shopObject.getType().mustBeSpawned()) {
				return;
			}

			if (!shopObject.isLastSpawnFailed()) {
				return;
			}

			unspawnableShopkeepers.add(shopkeeper);
			if (!silent) {
				Log.warning(shopkeeper.getLogPrefix() + "The last spawn attempt failed.");
			}
		});

		if (!unspawnableShopkeepers.isEmpty()) {
			if (deleteUnspawnableShopkeepers) {
				// Delete those shopkeepers:
				for (Shopkeeper shopkeeper : unspawnableShopkeepers) {
					shopkeeper.delete(); // runs synchronously
				}

				// Save:
				plugin.getShopkeeperStorage().save();

				if (!silent) {
					Log.warning("Deleted " + unspawnableShopkeepers.size()
							+ " shopkeepers that previously failed to spawn!");
				}
			} else {
				// Only log a warning:
				if (!silent) {
					Log.warning("Found " + unspawnableShopkeepers.size() + " shopkeepers that "
							+ "previously failed to spawn! Either enable the setting "
							+ "'delete-unspawnable-shopkeepers' inside the config, or use the "
							+ "command '/shopkeepers deleteUnspawnableShopkeepers' to "
							+ "automatically delete these shopkeepers and get rid of these "
							+ "warnings.");
				}
			}
		}
		return unspawnableShopkeepers;
	}
}
