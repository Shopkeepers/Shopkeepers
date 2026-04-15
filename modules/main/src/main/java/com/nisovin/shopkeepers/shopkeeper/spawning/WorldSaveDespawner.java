package com.nisovin.shopkeepers.shopkeeper.spawning;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopkeeper.spawning.ShopkeeperSpawnState.State;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Handles the temporary despawning and later respawning of shop objects that need to be despawned
 * during world saves.
 */
class WorldSaveDespawner {

	private static final Predicate<AbstractShopkeeper> IS_DESPAWNED_DURING_WORLD_SAVE = (shopkeeper) -> {
		AbstractShopObjectType<?> objectType = shopkeeper.getShopObject().getType();
		return objectType.mustDespawnDuringWorldSave();
	};

	private final ShopkeeperSpawner spawner;
	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;

	WorldSaveDespawner(
			ShopkeeperSpawner spawner,
			SKShopkeepersPlugin plugin,
			SKShopkeeperRegistry shopkeeperRegistry
	) {
		Validate.notNull(spawner, "spawner is null");
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(shopkeeperRegistry, "shopkeeperRegistry is null");
		this.spawner = spawner;
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
	}

	// WORLD EVENTS

	void onWorldUnload(World world) {
		assert world != null;
		// If there are no shopkeepers in the world, the shopkeeper spawner might already have
		// removed the world data, and thereby also already cancelled the world save respawn task.
		// However, if there are shopkeepers in the world, the world data entry is kept, and we then
		// cancel any currently active world save respawn task here.
		String worldName = world.getName();
		WorldData worldData = spawner.getWorldData(worldName);
		if (worldData == null) return;

		worldData.cancelWorldSaveRespawnTask();
	}

	void onWorldSave(World world) {
		assert world != null;
		String worldName = world.getName();
		// Note: Shopkeepers can be added to the world while the world is being saved. To track
		// whether the world is currently being saved, we start the respawn task even if the world
		// does not yet contain any shopkeepers.
		WorldData worldData = spawner.getOrCreateWorldData(worldName);
		if (worldData.isWorldSaveRespawnPending()) {
			// We are already processing a save of this world.
			Log.debug(DebugOptions.shopkeeperActivation,
					() -> "Ignoring saving of world '" + worldName
							+ "', because another save is already processed for this world."
			);
			return;
		}

		// Set up the world save respawn task:
		// This is done prior to the despawning of shopkeepers, because the presence of this task is
		// also used to indicate that the world is currently being saved.
		new RespawnShopkeepersAfterWorldSaveTask(worldData).start();

		// Note: The chunks stays marked as active during the temporary despawning of the
		// shopkeepers.
		// Note: The shopkeepers remain ticking during the temporary despawning. Since we assume
		// that the world save will be complete on the next tick, this should not be an issue, even
		// if one of the shop objects is ticked and respawns itself before our respawn task is run.
		// However, to prevent this inconsistent respawning responsibility anyway, we set the
		// shopkeeper to state 'world-save-respawn-pending', so that the shop object can skip any
		// respawn attempts while our respawn task is still pending.
		spawner.despawnShopkeepersInWorld(
				worldName,
				"world saving",
				IS_DESPAWNED_DURING_WORLD_SAVE,
				this::setPendingWorldSaveRespawn
		);
	}

	private void setPendingWorldSaveRespawn(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);
		spawnState.setState(State.PENDING_WORLD_SAVE_RESPAWN);
	}

	class RespawnShopkeepersAfterWorldSaveTask implements Runnable {

		private final WorldData worldData;
		private @Nullable CompletableFuture<Void> task;

		RespawnShopkeepersAfterWorldSaveTask(WorldData worldData) {
			assert worldData != null;
			this.worldData = worldData;
		}

		void start() {
			assert !worldData.isWorldSaveRespawnPending();
			this.task = SchedulerUtils.runTaskGloballyOrOmit(this);
			worldData.setWorldSaveRespawnTask(this);
		}

		@Override
		public void run() {
			// Assert: World is still loaded (the task is cancelled on world unload).
			worldData.setWorldSaveRespawnTask(null);

			// In order to not have players wait for shopkeepers to respawn after world saves, we
			// respawn the shopkeepers immediately in this case:
			spawner.spawnShopkeepersInWorld(
					worldData.getWorldName(),
					"world saving finished",
					IS_DESPAWNED_DURING_WORLD_SAVE,
					true
			);
		}

		public void cancel() {
			if (task != null) {
				task.cancel(true);
				task = null;
				worldData.setWorldSaveRespawnTask(null);
				this.onCancelled();
			}
		}

		private void onCancelled() {
			// Reset the 'pending-respawn' state of all affected shop objects inside the world:
			shopkeeperRegistry.getShopkeepersInWorld(worldData.getWorldName()).forEach(shopkeeper -> {
				ShopkeeperSpawnState spawnState = shopkeeper.getComponents().getOrAdd(ShopkeeperSpawnState.class);
				if (spawnState.getState() == State.PENDING_WORLD_SAVE_RESPAWN) {
					assert shopkeeper.getShopObject().getType().mustBeSpawned();
					assert shopkeeper.getShopObject().getType().mustDespawnDuringWorldSave();
					spawnState.setState(State.DESPAWNED);
				}
			});
		}
	}
}
