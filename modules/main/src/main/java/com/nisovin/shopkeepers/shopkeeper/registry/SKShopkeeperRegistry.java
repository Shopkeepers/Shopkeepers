package com.nisovin.shopkeepers.shopkeeper.registry;

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

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.activation.ShopkeeperChunkActivator;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.ShopkeeperChunkMap.ChangeListener;
import com.nisovin.shopkeepers.shopkeeper.spawning.ShopkeeperSpawner;
import com.nisovin.shopkeepers.shopkeeper.ticking.ShopkeeperTicker;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.block.BlockShopObjectIds;
import com.nisovin.shopkeepers.shopobjects.entity.EntityShopObjectIds;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKShopkeeperRegistry implements ShopkeeperRegistry {

	private final SKShopkeepersPlugin plugin;

	// All shopkeepers:
	private final Map<UUID, AbstractShopkeeper> shopkeepersByUUID = new LinkedHashMap<>();
	private final Collection<? extends AbstractShopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersByUUID.values());
	private final Map<Integer, AbstractShopkeeper> shopkeepersById = new HashMap<>();

	// TODO Shopkeepers by name TreeMap to speedup name lookups and prefix matching?
	// TODO TreeMaps for shopkeeper owners by name and uuid to speedup prefix matching?

	// Virtual shopkeepers:
	// Set: Allows for fast removal.
	private final Set<AbstractShopkeeper> virtualShopkeepers = new LinkedHashSet<>();
	private final Collection<? extends AbstractShopkeeper> virtualShopkeepersView = Collections.unmodifiableCollection(virtualShopkeepers);

	private final ShopkeeperChunkMap chunkMap;
	private final ChangeListener chunkMapChangeListener = new ChangeListener() {
		@Override
		public void onShopkeeperAdded(
				AbstractShopkeeper shopkeeper,
				ChunkShopkeepers chunkShopkeepers
		) {
		}

		@Override
		public void onShopkeeperRemoved(
				AbstractShopkeeper shopkeeper,
				ChunkShopkeepers chunkShopkeepers
		) {
		}

		@Override
		public void onWorldAdded(WorldShopkeepers worldShopkeepers) {
		}

		@Override
		public void onWorldRemoved(WorldShopkeepers worldShopkeepers) {
			Unsafe.assertNonNull(shopkeeperSpawner);
			shopkeeperSpawner.onShopkeeperWorldRemoved(worldShopkeepers.getWorldName());
		}

		@Override
		public void onChunkAdded(ChunkShopkeepers chunkShopkeepers) {
			// Also immediately set up the chunk activator's chunk data, but do not yet trigger
			// shopkeeper activation (ticking, spawning, etc.). This ensures that all queries
			// involving active chunks provide a consistent view.
			Unsafe.assertNonNull(chunkActivator);
			chunkActivator.onShopkeeperChunkAdded(chunkShopkeepers.getChunkCoords());
		}

		@Override
		public void onChunkRemoved(ChunkShopkeepers chunkShopkeepers) {
			Unsafe.assertNonNull(chunkActivator);
			chunkActivator.onShopkeeperChunkRemoved(chunkShopkeepers.getChunkCoords());
		}
	};

	// Player shopkeepers:
	private int playerShopCount = 0;
	// Note: Already unmodifiable.
	private final Set<? extends AbstractPlayerShopkeeper> allPlayerShopkeepersView = new AbstractSet<AbstractPlayerShopkeeper>() {
		@Override
		public Iterator<AbstractPlayerShopkeeper> iterator() {
			if (this.isEmpty()) {
				return Collections.emptyIterator();
			}
			return Unsafe.initialized(SKShopkeeperRegistry.this).getAllShopkeepers().stream()
					.filter(shopkeeper -> shopkeeper instanceof PlayerShopkeeper)
					.<AbstractPlayerShopkeeper>map(shopkeeper -> (AbstractPlayerShopkeeper) shopkeeper)
					.iterator();
		}

		@Override
		public int size() {
			return playerShopCount;
		}
	};

	private final ShopObjectRegistry shopObjectRegistry = new ShopObjectRegistry();
	private final ShopkeeperTicker shopkeeperTicker;
	private final ShopkeeperSpawner shopkeeperSpawner;
	private final ShopkeeperChunkActivator chunkActivator;
	private final ActiveChunkQueries activeChunkQueries;

	public SKShopkeeperRegistry(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.chunkMap = new ShopkeeperChunkMap(chunkMapChangeListener);
		this.shopkeeperTicker = new ShopkeeperTicker(plugin);
		this.shopkeeperSpawner = new ShopkeeperSpawner(plugin, Unsafe.initialized(this));
		this.chunkActivator = new ShopkeeperChunkActivator(
				plugin,
				Unsafe.initialized(this),
				shopkeeperTicker,
				shopkeeperSpawner
		);
		this.activeChunkQueries = new ActiveChunkQueries(chunkMap, chunkActivator);
	}

	public void onEnable() {
		shopObjectRegistry.onEnable();
		chunkActivator.onEnable();
		shopkeeperSpawner.onEnable();
		shopkeeperTicker.onEnable();
	}

	public void onDisable() {
		// Unload all shopkeepers:
		this.unloadAllShopkeepers();
		assert this.getAllShopkeepers().isEmpty();

		// Reset all (just in case):
		this.ensureEmpty();

		shopkeeperTicker.onDisable();
		shopkeeperSpawner.onDisable();
		chunkActivator.onDisable();
		shopObjectRegistry.onDisable();
	}

	private void ensureEmpty() {
		if (!shopkeepersByUUID.isEmpty() || !shopkeepersById.isEmpty()
				|| !virtualShopkeepers.isEmpty() || playerShopCount != 0) {
			Log.warning("Some shopkeepers were not properly unregistered!");
			shopkeepersByUUID.clear();
			shopkeepersById.clear();
			virtualShopkeepers.clear();
			playerShopCount = 0;
		}
		chunkMap.ensureEmpty();
	}

	public ShopkeeperSpawner getShopkeeperSpawner() {
		return shopkeeperSpawner;
	}

	public ShopkeeperChunkActivator getChunkActivator() {
		return chunkActivator;
	}

	// SHOPKEEPER CREATION

	private SKShopkeeperStorage getShopkeeperStorage() {
		return plugin.getShopkeeperStorage();
	}

	@Override
	public AbstractShopkeeper createShopkeeper(
			ShopCreationData creationData
	) throws ShopkeeperCreateException {
		Validate.notNull(creationData, "creationData is null");
		ShopType<?> shopType = creationData.getShopType();
		assert shopType != null;
		Validate.isTrue(shopType instanceof AbstractShopType,
				"shopType is not of type AbstractShopType, but: " + shopType.getClass().getName());
		AbstractShopType<?> abstractShopType = (AbstractShopType<?>) shopType;

		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		int id = shopkeeperStorage.getNextShopkeeperId();

		AbstractShopkeeper shopkeeper = abstractShopType.createShopkeeper(id, creationData);
		assert shopkeeper != null;

		// Validate shopkeeper ids:
		try {
			this.validateUnusedShopkeeperIds(shopkeeper);
		} catch (RuntimeException e) {
			throw new ShopkeeperCreateException(e.getMessage(), e);
		}

		// Success:

		// Add the shopkeeper to the registry and spawn it:
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.CREATED);
		return shopkeeper;
	}

	/**
	 * Recreates a shopkeeper by loading its previously saved data from the given
	 * {@link ShopkeeperData}.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data
	 * @return the loaded shopkeeper, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the shopkeeper data could not be loaded
	 */
	// Internal method: This is only supposed to be called by the built-in storage currently. If the
	// data comes from any other source, the storage would need to be made aware of the shopkeeper
	// (e.g. by marking the shopkeeper as dirty). Otherwise, certain operations (such as checking if
	// a certain shopkeeper id is already in use) would no longer work as expected.
	public AbstractShopkeeper loadShopkeeper(
			ShopkeeperData shopkeeperData
	) throws InvalidDataException {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");

		AbstractShopType<?> shopType = shopkeeperData.get(AbstractShopkeeper.SHOP_TYPE);
		assert shopType != null;

		AbstractShopkeeper shopkeeper = shopType.loadShopkeeper(shopkeeperData);
		assert shopkeeper != null;

		// Validate shopkeeper ids:
		try {
			this.validateUnusedShopkeeperIds(shopkeeper);
		} catch (RuntimeException e) {
			throw new InvalidDataException(e.getMessage(), e);
		}

		// Success:

		// Add the shopkeeper to the registry and spawn it:
		this.addShopkeeper(shopkeeper, ShopkeeperAddedEvent.Cause.LOADED);
		return shopkeeper;
	}

	private void validateUnusedShopkeeperIds(Shopkeeper shopkeeper) {
		Validate.isTrue(this.getShopkeeperById(shopkeeper.getId()) == null,
				() -> "There already exists a shopkeeper with the same id: " + shopkeeper.getId());
		Validate.isTrue(this.getShopkeeperByUniqueId(shopkeeper.getUniqueId()) == null,
				() -> "There already exists a shopkeeper with the same unique id: "
						+ shopkeeper.getUniqueId());
	}

	// ADD / REMOVE SHOPKEEPER

	private void addShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperAddedEvent.Cause cause) {
		assert shopkeeper != null && !shopkeeper.isValid();
		assert !shopkeepersByUUID.containsKey(shopkeeper.getUniqueId());
		assert !shopkeepersById.containsKey(shopkeeper.getId());

		// Store by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		int shopkeeperId = shopkeeper.getId();
		shopkeepersByUUID.put(shopkeeperUniqueId, shopkeeper);
		shopkeepersById.put(shopkeeperId, shopkeeper);

		// Inform the storage about the used up id:
		SKShopkeeperStorage shopkeeperStorage = this.getShopkeeperStorage();
		shopkeeperStorage.onShopkeeperIdUsed(shopkeeperId);

		// Add shopkeeper to chunk-based storage:
		if (shopkeeper.isVirtual()) {
			virtualShopkeepers.add(shopkeeper);
		} else {
			chunkMap.addShopkeeper(shopkeeper);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount++;
		}

		// Log a warning if either the shop type or the shop object type is disabled. The shopkeeper
		// is still added (so containers are still protected), but it might not get spawned, and
		// there is no guarantee that the shop still works as expected. Admins are advised to either
		// delete the shopkeeper, or change its object type to something else.
		AbstractShopType<?> shopType = shopkeeper.getType();
		if (!shopType.isEnabled()) {
			Log.warning(shopkeeper.getLogPrefix() + "Shop type '" + shopType.getIdentifier()
					+ "' is disabled! Consider deleting this shopkeeper.");
		}
		AbstractShopObjectType<?> shopObjectType = shopkeeper.getShopObject().getType();
		if (!shopObjectType.isEnabled()) {
			Log.warning(shopkeeper.getLogPrefix() + "Object type '" + shopObjectType.getIdentifier()
					+ "' is disabled! Consider changing the object type.");
		}

		// Inform shopkeeper:
		// If the shop object handles spawning itself, and the shop object is already spawned, this
		// might register the already spawned shop object.
		shopkeeper.informAdded(cause);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperAddedEvent(shopkeeper, cause));
		if (!shopkeeper.isValid()) {
			// The shopkeeper has already been removed again.
			return;
		}

		// If necessary, activate the shopkeeper (start ticking, spawn, etc.):
		chunkActivator.checkShopkeeperActivation(shopkeeper);
	}

	private void removeShopkeeper(
			AbstractShopkeeper shopkeeper,
			ShopkeeperRemoveEvent.Cause cause
	) {
		assert shopkeeper != null && shopkeeper.isValid() && cause != null;

		Location loc = shopkeeper.getLocation();
		if (loc != null) {
			SchedulerUtils.runOnMainThreadOrOmit(loc, () -> {
				doRemoveShopkeeper(shopkeeper, cause);
			});
		} else {
			doRemoveShopkeeper(shopkeeper, cause);
		}


	}

	private void doRemoveShopkeeper(AbstractShopkeeper shopkeeper, ShopkeeperRemoveEvent.Cause cause) {
		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperRemoveEvent(shopkeeper, cause));

		if (!shopkeeper.isValid()) {
			Log.warning(shopkeeper.getLogPrefix()
					+ "Aborting removal, because already removed during ShopkeeperRemoveEvent!");
			return;
		}

		// Delayed closing of all active UI sessions:
		// TODO Views might want/need to handle the UI closing immediately here (e.g. to save UI
		// state and apply shopkeeper changes).
		shopkeeper.abortUISessionsDelayed();

		// If necessary, deactivate the shopkeeper (stop ticking, despawn, etc.):
		chunkActivator.deactivateShopkeeper(shopkeeper);

		// Inform shopkeeper:
		// If the shop object handles spawning itself, this is expected to unregister any currently
		// spawned shop object.
		shopkeeper.informRemoval(cause);

		// Verify that the shop object is no longer registered:
		if (shopObjectRegistry.isRegistered(shopkeeper)) {
			Log.warning(shopkeeper.getLogPrefix() + "Shop object of type '"
					+ shopkeeper.getShopObject().getType().getIdentifier()
					+ "' did not unregister itself during shopkeeper removal!");
		}

		// Remove shopkeeper by unique id and session id:
		UUID shopkeeperUniqueId = shopkeeper.getUniqueId();
		shopkeepersByUUID.remove(shopkeeperUniqueId);
		shopkeepersById.remove(shopkeeper.getId());

		// Remove shopkeeper from chunk-based storage:
		if (shopkeeper.isVirtual()) {
			virtualShopkeepers.remove(shopkeeper);
		} else {
			chunkMap.removeShopkeeper(shopkeeper);
		}

		// Update player shop count:
		if (shopkeeper instanceof PlayerShopkeeper) {
			playerShopCount--;
		}

		if (cause == ShopkeeperRemoveEvent.Cause.DELETE) {
			// Remove shopkeeper from storage:
			this.getShopkeeperStorage().deleteShopkeeper(shopkeeper);
		}
	}

	// This is not expected to be called for invalid or virtual shopkeepers.
	public void onShopkeeperMoved(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(shopkeeper.isValid(), "shopkeeper is not valid");
		Validate.isTrue(!shopkeeper.isVirtual(), "shopkeeper is virtual");

		ChunkCoords oldChunk = Unsafe.assertNonNull(shopkeeper.getLastChunkCoords());

		// Update the shopkeeper's location inside the chunk map:
		if (!chunkMap.moveShopkeeper(shopkeeper)) {
			// The shopkeeper's chunk did not change.
			return;
		}

		// Inform chunk activator:
		chunkActivator.onShopkeeperMoved(shopkeeper, oldChunk);
	}

	private void unloadShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null && shopkeeper.isValid();
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.UNLOAD);
	}

	public void unloadAllShopkeepers() {
		// Note: One optimization idea is to clear the shopkeeper spawn queue here immediately,
		// instead of removing shopkeepers one by one during shopkeeper removals. However, we don't
		// expect this to actually provide much benefit, as the spawn queue is usually not very full
		// anyway: The spawn queue is intentionally not used in situations in which it could fill up
		// a lot (reloads, world save respawns, etc.) in order to not create a backlog that would
		// result in players waiting very long for shopkeepers to respawn. The same applies when
		// deleting all shopkeepers.
		new ArrayList<>(this.getAllShopkeepers()).forEach(this::unloadShopkeeper);
	}

	public void deleteShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(shopkeeper.isValid(), "shopkeeper is invalid");
		this.removeShopkeeper(shopkeeper, ShopkeeperRemoveEvent.Cause.DELETE);
	}

	public void deleteAllShopkeepers() {
		new ArrayList<>(this.getAllShopkeepers()).forEach(this::deleteShopkeeper);
	}

	///// QUERYING

	@Override
	public Collection<? extends AbstractShopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getVirtualShopkeepers() {
		return virtualShopkeepersView;
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId) {
		return shopkeepersByUUID.get(shopkeeperUniqueId);
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeperById(int shopkeeperId) {
		return shopkeepersById.get(shopkeeperId);
	}

	// PLAYER SHOPS

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getAllPlayerShopkeepers() {
		return allPlayerShopkeepersView;
	}

	@Override
	public Collection<? extends AbstractPlayerShopkeeper> getPlayerShopkeepersByOwner(
			UUID ownerUUID
	) {
		Validate.notNull(ownerUUID, "ownerUUID is null");
		// TODO Improve? Maybe keep an index of player shops? Or even index by owner?
		// Note: Already unmodifiable.
		return new AbstractSet<AbstractPlayerShopkeeper>() {
			private Stream<? extends AbstractPlayerShopkeeper> createStream() {
				return allPlayerShopkeepersView.stream()
						.filter(shopkeeper -> shopkeeper.getOwnerUUID().equals(ownerUUID));
			}

			@Override
			public Iterator<AbstractPlayerShopkeeper> iterator() {
				if (allPlayerShopkeepersView.isEmpty()) {
					// There are no player shops at all:
					return Collections.emptyIterator();
				}
				return Unsafe.cast(this.createStream().iterator());
			}

			@Override
			public int size() {
				if (allPlayerShopkeepersView.isEmpty()) {
					// There are no player shops at all:
					return 0;
				}
				return this.createStream().mapToInt(shopkeeper -> 1).sum();
			}
		};
	}

	// BY NAME

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByName(String shopName) {
		String normalizedShopName = StringUtils.normalize(TextUtils.stripColor(shopName));
		if (StringUtils.isEmpty(normalizedShopName)) return Stream.empty();

		// TODO Improve via (Tree)Map?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // Can be empty
			if (shopkeeperName.isEmpty()) return false; // Has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			// Include shopkeeper if name matches:
			return (shopkeeperName.equals(normalizedShopName));
		});
	}

	@Override
	public Stream<? extends AbstractShopkeeper> getShopkeepersByNamePrefix(
			String shopNamePrefix
	) {
		String normalizedShopNamePrefix = StringUtils.normalize(TextUtils.stripColor(shopNamePrefix));
		if (StringUtils.isEmpty(normalizedShopNamePrefix)) return Stream.empty();

		// TODO Improve via TreeMap?
		return this.getAllShopkeepers().stream().filter(shopkeeper -> {
			String shopkeeperName = shopkeeper.getName(); // Can be empty
			if (shopkeeperName.isEmpty()) return false; // Has no name, filter

			shopkeeperName = TextUtils.stripColor(shopkeeperName);
			shopkeeperName = StringUtils.normalize(shopkeeperName);
			// Include shopkeeper if name matches:
			return shopkeeperName.startsWith(normalizedShopNamePrefix);
		});
	}

	// BY WORLD

	@Override
	public Collection<? extends String> getWorldsWithShopkeepers() {
		return chunkMap.getWorldsWithShopkeepers();
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInWorld(String worldName) {
		WorldShopkeepers worldShopkeepers = chunkMap.getWorldShopkeepers(worldName);
		if (worldShopkeepers == null) return Collections.emptySet();
		return worldShopkeepers.getShopkeepers();
	}

	@Override
	public Map<? extends ChunkCoords, ? extends Collection<? extends AbstractShopkeeper>> getShopkeepersByChunks(
			String worldName
	) {
		WorldShopkeepers worldShopkeepers = chunkMap.getWorldShopkeepers(worldName);
		if (worldShopkeepers == null) {
			return Collections.emptyMap();
		}
		return worldShopkeepers.getShopkeepersByChunk();
	}

	// ACTIVE CHUNKS

	@Override
	public Collection<? extends ChunkCoords> getActiveChunks(String worldName) {
		return activeChunkQueries.getActiveChunks(worldName);
	}

	@Override
	public boolean isChunkActive(ChunkCoords chunkCoords) {
		return chunkActivator.isChunkActive(chunkCoords);
	}

	// Note: This are the shopkeepers in active chunks. The shopkeepers might not necessarily be
	// spawned yet.
	@Override
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers() {
		return activeChunkQueries.getShopkeepersInActiveChunks();
	}

	@Override
	public Collection<? extends AbstractShopkeeper> getActiveShopkeepers(String worldName) {
		return activeChunkQueries.getShopkeepersInActiveChunks(worldName);
	}

	// BY CHUNK

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersInChunk(
			ChunkCoords chunkCoords
	) {
		Validate.notNull(chunkCoords, "chunkCoords is null");
		ChunkShopkeepers chunkShopkeepers = chunkMap.getChunkShopkeepers(chunkCoords);
		if (chunkShopkeepers == null) return Collections.emptySet();
		return chunkShopkeepers.getShopkeepers();
	}

	public Collection<? extends AbstractShopkeeper> getShopkeepersInChunkSnapshot(
			ChunkCoords chunkCoords
	) {
		Validate.notNull(chunkCoords, "chunkCoords is null");
		ChunkShopkeepers chunkShopkeepers = chunkMap.getChunkShopkeepers(chunkCoords);
		if (chunkShopkeepers == null) return Collections.emptySet();
		return chunkShopkeepers.getShopkeepersSnapshot();
	}

	// BY LOCATION

	@Override
	public Collection<? extends AbstractShopkeeper> getShopkeepersAtLocation(Location location) {
		World world = LocationUtils.getWorld(location);
		String worldName = world.getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
		ChunkCoords chunkCoords = ChunkCoords.fromBlock(worldName, x, z);
		this.getShopkeepersInChunk(chunkCoords).forEach(shopkeeper -> {
			assert worldName.equals(shopkeeper.getWorldName());
			if (shopkeeper.getX() == x && shopkeeper.getY() == y && shopkeeper.getZ() == z) {
				shopkeepers.add(shopkeeper);
			}
		});
		return shopkeepers;
	}

	// BY SHOP OBJECT

	public ShopObjectRegistry getShopObjectRegistry() {
		return shopObjectRegistry;
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeperByEntity(Entity entity) {
		Validate.notNull(entity, "entity is null");
		Object objectId = EntityShopObjectIds.getObjectId(entity);
		return shopObjectRegistry.getShopkeeperByObjectId(objectId);
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return (this.getShopkeeperByEntity(entity) != null);
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeperByBlock(Block block) {
		Validate.notNull(block, "block is null");
		return getShopkeeperByBlock(
				block.getWorld().getName(),
				block.getX(),
				block.getY(),
				block.getZ()
		);
	}

	@Override
	public @Nullable AbstractShopkeeper getShopkeeperByBlock(String worldName, int x, int y, int z) {
		Object objectId = BlockShopObjectIds.getSharedObjectId(worldName, x, y, z);
		return shopObjectRegistry.getShopkeeperByObjectId(objectId);
	}

	@Override
	public boolean isShopkeeper(Block block) {
		return (this.getShopkeeperByBlock(block) != null);
	}
}
