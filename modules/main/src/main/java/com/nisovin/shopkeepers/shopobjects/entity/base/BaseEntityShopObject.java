package com.nisovin.shopkeepers.shopobjects.entity.base;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.debug.events.DebugListener;
import com.nisovin.shopkeepers.debug.events.EventDebugListener;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.bukkit.Ticks;
import com.nisovin.shopkeepers.util.bukkit.WorldUtils;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Extension of {@link AbstractEntityShopObject} with additional common entity spawning and setup
 * logic.
 * <p>
 * The corresponding {@link #getType() shop object type} is expected to inherit from
 * {@link BaseEntityShopObjectType}.
 *
 * @param <E>
 *            the entity type
 */
public abstract class BaseEntityShopObject<E extends Entity>
		extends AbstractEntityShopObject implements EntityShopObject {

	/**
	 * We check from slightly below the top of the spawn block (= offset) in a range of up to one
	 * block below the spawn block (= range) for a location to spawn the shopkeeper entity at.
	 */
	protected static final double SPAWN_LOCATION_OFFSET = 0.98D;
	protected static final double SPAWN_LOCATION_RANGE = 2.0D;
	/**
	 * Flying mobs can be placed one block above the ground without falling down.
	 */
	protected static final double SPAWN_LOCATION_RANGE_FLYING = 1.0D;

	protected static final int CHECK_PERIOD_SECONDS = 10;
	protected static final int CHECK_PERIOD_TICKS = Ticks.PER_SECOND * CHECK_PERIOD_SECONDS;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_PERIOD_SECONDS + 1
	);
	// If the entity could not be respawned this amount of times, we throttle its tick rate (i.e.
	// the rate at which we attempt to respawn it):
	protected static final int MAX_RESPAWN_ATTEMPTS = 5;
	protected static final int THROTTLED_CHECK_PERIOD_SECONDS = 60;

	private static final Location sharedLocation = new Location(null, 0, 0, 0);

	protected final BaseEntityShopObjectCreationContext context;
	private final BaseEntityShopObjectType<?> shopObjectType;

	private volatile @Nullable E entity;
	private @Nullable Location lastSpawnLocation = null;
	private int respawnAttempts = 0;
	private boolean debuggingSpawn = false;
	// Shared among all base entity shopkeepers to prevent spam:
	private static long lastSpawnDebugMillis = 0L;
	private static final long SPAWN_DEBUG_THROTTLE_MILLIS = TimeUnit.MINUTES.toMillis(5);

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final int checkingOffset = nextCheckingOffset.getAndIncrement();
	private final RateLimiter checkLimiter = new RateLimiter(CHECK_PERIOD_SECONDS, checkingOffset);
	private boolean skipRespawnAttemptsIfPeaceful = false;

	protected BaseEntityShopObject(
			BaseEntityShopObjectCreationContext context,
			BaseEntityShopObjectType<?> shopObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(shopkeeper, creationData);
		this.context = context;
		this.shopObjectType = shopObjectType;
	}

	protected BaseEntityShopObjectCreationContext getContext() {
		return context;
	}

	@Override
	public BaseEntityShopObjectType<?> getType() {
		return shopObjectType;
	}

	public final EntityType getEntityType() {
		return shopObjectType.getEntityType();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
	}

	// ITEM UPDATES

	@Override
	public int updateItems(String logPrefix, @ReadWrite ShopObjectData shopObjectData) {
		int updatedItems = super.updateItems(logPrefix, shopObjectData);
		return updatedItems;
	}

	// ACTIVATION

	@Override
	public @Nullable E getEntity() {
		return entity;
	}

	private @Nullable Location getSpawnLocation() {
		Location spawnLocation = shopkeeper.getLocation();
		if (spawnLocation == null) return null; // World not loaded

		spawnLocation.add(0.5D, 0.0D, 0.5D); // Center of block

		if (this.shallAdjustSpawnLocation()) {
			this.adjustSpawnLocation(spawnLocation);
		}

		return spawnLocation;
	}

	protected boolean shallAdjustSpawnLocation() {
		return true;
	}

	// Shopkeepers might be located 1 block above passable (especially in the past) or non-full
	// blocks. In order to not have these shopkeepers hover but stand on the ground, we determine
	// the exact spawn location their entity would fall to within the range of up to 1 block below
	// their spawn block.
	// This also applies with gravity disabled, and even if the block below their spawn block is air
	// now: Passable blocks like grass or non-full blocks like carpets or slabs might have been
	// broken since the shopkeeper was created. We still want to place the shopkeeper nicely on the
	// ground in those cases.
	private void adjustSpawnLocation(Location spawnLocation) {
		// The entity may be able to stand on certain types of fluids:
		Set<? extends Material> collidableFluids = EntityUtils.getCollidableFluids(
				this.getEntityType()
		);
		// However, if the spawn location is inside a fluid (i.e. underwater or inside of lava), we
		// ignore this aspect (i.e. the entity sinks to the ground even if it can usually stand on
		// top of the liquid).
		// We don't check the spawn block itself but the block above in order to also spawn entities
		// that are in shallow liquids on top of the liquid.
		if (!collidableFluids.isEmpty()) {
			World world = Unsafe.assertNonNull(spawnLocation.getWorld());
			Block blockAbove = world.getBlockAt(
					shopkeeper.getX(),
					shopkeeper.getY() + 1,
					shopkeeper.getZ()
			);
			if (blockAbove.isLiquid()) {
				collidableFluids = Collections.emptySet();
			}
		}

		// We check for collisions from the top of the block:
		spawnLocation.add(0.0D, SPAWN_LOCATION_OFFSET, 0.0D);

		var spawnLocationRange = EntityUtils.canFly(this.getEntityType())
				? SPAWN_LOCATION_RANGE_FLYING
				: SPAWN_LOCATION_RANGE;
		double distanceToGround = WorldUtils.getCollisionDistanceToGround(
				spawnLocation,
				spawnLocationRange,
				collidableFluids
		);

		if (distanceToGround == spawnLocationRange) {
			// No collision within the checked range: Remove the initial offset from the spawn
			// location again.
			distanceToGround = SPAWN_LOCATION_OFFSET;
		}

		// Adjust the spawn location:
		spawnLocation.add(0.0D, -distanceToGround, 0.0D);
	}

	/**
	 * Any preparation that needs to be done before spawning. Might only allow limited operations.
	 * 
	 * @param entity
	 *            the entity about to be spawned
	 */
	protected void prepareEntity(@NonNull E entity) {
		// Assign metadata for easy identification by other plugins:
		ShopkeeperMetadata.apply(entity);

		// Don't save the entity to the world data:
		entity.setPersistent(false);

		// Apply name (if it has/uses one):
		this.applyName(entity, shopkeeper.getName());

		// Any version-specific preparation:
		Compat.getProvider().prepareEntity(entity);
	}

	/**
	 * Any clean up that needs to happen for the entity. The entity might not be fully setup yet.
	 */
	protected void cleanUpEntity(@Nullable Runnable callback) {
		Entity entity = Unsafe.assertNonNull(this.entity);

		SchedulerUtils.runTaskOrOmit(entity, () -> {
			// Disable AI:
			this.cleanupAI();

			// Remove metadata again:
			ShopkeeperMetadata.remove(entity);

			// Remove the entity (if it hasn't been removed already):
			if (!entity.isDead()) {
				entity.remove();
			}

			this.entity = null;

			if (callback != null) {
				callback.run();
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean spawn() {
		if (entity != null) {
			return true; // Already spawned
		}

		// Prepare spawn location:
		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) {
			this.onSpawnFailed();
			return false; // World not loaded
		}
		World world = Unsafe.assertNonNull(spawnLocation.getWorld());

		// Spawn entity:
		// TODO Check if the block is passable before spawning there?
		EntityType entityType = this.getEntityType();
		Class<? extends Entity> entityClass = Unsafe.assertNonNull(entityType.getEntityClass());
		// Note: We expect this type of entity to be spawnable, and not result in an
		// IllegalArgumentException.
		// We disable the spawn data randomization here (prevents for example baby zombies from
		// spawning or mounting nearby chicken):
		// TODO With this, some of the setup below might no longer be needed.
		this.entity = (E) world.spawn(spawnLocation, entityClass, false, entity -> {
			assert entity != null;
			// Note: This callback is run after the entity has been prepared (this includes the
			// creation of random equipment and the random spawning of passengers) and right before
			// the entity gets added to the world (which triggers the corresponding
			// CreatureSpawnEvent).

			// Debugging entity spawning:
			if (entity.isDead()) {
				Log.debug("Spawning shopkeeper entity is dead already!");
			}

			// Prepare entity, before it gets spawned:
			prepareEntity((E) entity);

			// Try to bypass entity-spawn blocking plugins (right before this specific entity is
			// about to get spawned):
			context.baseEntityShops.forceEntitySpawn(spawnLocation, entityType);
		});
		E entity = this.entity;
		assert entity != null;

		boolean success = this.isActive();
		if (success) {
			// Remember the spawn location:
			this.lastSpawnLocation = spawnLocation;

			// Further setup entity after it was successfully spawned:
			// Some entities randomly spawn with passengers:
			for (Entity passenger : entity.getPassengers()) {
				passenger.remove();
			}
			// Some entities might automatically mount on nearby entities (like baby zombies on
			// chicken):
			entity.eject();

			// This is also required so that certain Minecraft behaviors (e.g. the panic behavior of
			// nearby villagers) ignore the shopkeeper entities. Otherwise, the shopkeeper entities
			// can be abused for mob farms (e.g. villages spawn more iron golems when villagers are
			// in panic due to nearby hostile mob shopkeepers).
			entity.setInvulnerable(true);

			// Any version-specific setup:
			Compat.getProvider().setupSpawnedEntity(entity);

			// Overwrite AI:
			this.overwriteAI();
			// Register the shop object for our custom AI processing:
			context.baseEntityShops.getEntityAI().addShopObject(this);

			// Apply sub-type:
			this.onSpawn();

			// Reset all state related to respawn throttling:
			respawnAttempts = 0;
			this.resetTickRate();
			skipRespawnAttemptsIfPeaceful = false;

			// Inform about the object id change:
			this.onIdChanged();

			this.onSpawnSucceeded();
		} else {
			// Failure:
			this.onSpawnFailed();

			// Debug, if not already debugging and cooldown is over:
			boolean debug = (Settings.debug && !debuggingSpawn && entity.isDead()
					&& (System.currentTimeMillis() - lastSpawnDebugMillis) > SPAWN_DEBUG_THROTTLE_MILLIS
					&& ChunkCoords.isChunkLoaded(entity.getLocation()));

			// Due to an open Spigot 1.17 issue, entities report as 'invalid' after being spawned
			// during chunk loads. In order to not spam with warnings, this warning has been
			// replaced with a debug output for now.
			// TODO Replace this with a warning again once the underlying issue has been resolved in
			// Spigot.
			Log.debug("Failed to spawn shopkeeper entity: Entity dead: " + entity.isDead()
					+ ", entity valid: " + entity.isValid()
					+ ", chunk loaded: " + ChunkCoords.isChunkLoaded(entity.getLocation())
					+ ", debug -> " + debug);

			// Reset the entity:
			this.cleanUpEntity(null);

			// Debug entity spawning:
			if (debug) {
				// Print chunk's entity counts:
				EntityUtils.printEntityCounts(spawnLocation.getChunk());

				// Try again and log event activity:
				debuggingSpawn = true;
				lastSpawnDebugMillis = System.currentTimeMillis();
				Log.info("Trying again and logging event activity ..");

				// Log all events occurring during spawning, and their registered listeners:
				DebugListener debugListener = DebugListener.register(true, true);

				// Log creature spawn handling:
				EventDebugListener<EntitySpawnEvent> spawnListener = new EventDebugListener<>(
						EntitySpawnEvent.class,
						(priority, event) -> {
							Entity spawnedEntity = event.getEntity();
							Log.info("  EntitySpawnEvent (" + priority + "): "
									+ "cancelled: " + event.isCancelled()
									+ ", dead: " + spawnedEntity.isDead()
									+ ", valid: " + spawnedEntity.isValid()
									+ ", chunk loaded: "
									+ ChunkCoords.isChunkLoaded(spawnedEntity.getLocation())
							);
						}
				);

				// Try to spawn the entity again:
				success = this.spawn();

				// Unregister listeners again:
				debugListener.unregister();
				spawnListener.unregister();
				debuggingSpawn = false;
				Log.info(".. Done. Successful: " + success);
			}
		}

		return success;
	}

	/**
	 * This method is called right after the entity was spawned.
	 * <p>
	 * It can be used to apply additional mob type specific setup.
	 */
	protected void onSpawn() {
		assert this.getEntity() != null;
	}

	protected void overwriteAI() {
		E entity = Unsafe.assertNonNull(this.entity);

		if (Settings.silenceShopEntities) {
			entity.setSilent(true);
		}

		if (Settings.disableGravity) {
			this.setNoGravity(entity);
			// When gravity is disabled, we may also be able to disable collisions / the pushing of
			// mobs via the noclip flag. However, this might not properly work for Vex, since they
			// disable their noclip again after their movement.
			// TODO Still required? Bukkit's setCollidable API might actually work now.
			// But this might also provide a small performance benefit.
			Compat.getProvider().setNoclip(entity);
		}
	}

	protected final void setNoGravity(E entity) {
		entity.setGravity(false);

		// Making sure that Spigot's entity activation range does not keep this entity ticking,
		// because it assumes that it is currently falling:
		Compat.getProvider().setOnGround(entity, true);
	}

	protected void cleanupAI() {
		// Disable AI:
		context.baseEntityShops.getEntityAI().removeShopObject(this);
	}

	@Override
	public void despawn() {
		if (entity == null) return;

		// Clean up entity:
		this.cleanUpEntity(() -> {
			lastSpawnLocation = null;

			// Inform about the object id change:
			this.onIdChanged();
		});
	}

	@Override
	public CompletableFuture<Boolean> move() {
		Entity entity = this.entity;
		if (entity == null) return CompletableFuture.completedFuture(false); // Ignore if not spawned

		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) return CompletableFuture.completedFuture(false);

		this.lastSpawnLocation = spawnLocation;
		CompletableFuture<Boolean> teleportSuccess = SKShopkeepersPlugin.getInstance().getForcingEntityTeleporter()
				.teleport(entity, spawnLocation);

		// Inform the AI system:
		context.baseEntityShops.getEntityAI().updateLocation(this);

		return teleportSuccess;
	}

	// TICKING

	@Override
	public void onTick() {
		super.onTick();
		if (checkLimiter.request()) {
			if (this.isSpawningScheduled()) {
				Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLogPrefix()
						+ "Spawning is scheduled. Skipping entity check.");
				return;
			}

			this.check();

			// Indicate ticking activity for visualization:
			this.indicateTickActivity();
		}
	}

	private boolean isTickRateThrottled() {
		return (checkLimiter.getThreshold() == THROTTLED_CHECK_PERIOD_SECONDS);
	}

	private void throttleTickRate() {
		if (this.isTickRateThrottled()) return; // Already throttled

		Log.debug("Throttling tick rate");
		checkLimiter.setThreshold(THROTTLED_CHECK_PERIOD_SECONDS);
		checkLimiter.setRemainingThreshold(THROTTLED_CHECK_PERIOD_SECONDS + checkingOffset);
	}

	private void resetTickRate() {
		checkLimiter.setThreshold(CHECK_PERIOD_SECONDS);
		checkLimiter.setRemainingThreshold(checkingOffset);
	}

	private void check() {
		if (!this.isActive()) {
			this.checkInactive();
		} else {
			this.checkActive();
		}
	}

	/**
	 * Periodic actions performed when the spawned entity is found to be inactive.
	 */
	protected void checkInactive() {
		this.respawnInactiveEntity();
	}

	/**
	 * Periodic actions performed when the spawned entity is found to still be active.
	 */
	protected void checkActive() {
		this.teleportBackIfMoved();
	}

	// True if the entity was respawned.
	private boolean respawnInactiveEntity() {
		assert !this.isActive();
		if (skipRespawnAttemptsIfPeaceful) {
			// Null if the world is not loaded:
			Location shopkeeperLocation = shopkeeper.getLocation();
			if (shopkeeperLocation != null
					&& LocationUtils.getWorld(shopkeeperLocation).getDifficulty() == Difficulty.PEACEFUL) {
				Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLocatedLogPrefix()
						+ this.getEntityType() + " is missing. "
						+ "Skipping respawn attempt due to peaceful difficulty.");
				return false;
			} else {
				skipRespawnAttemptsIfPeaceful = false;
			}
		}
		assert !skipRespawnAttemptsIfPeaceful;

		@Nullable E entity = this.entity;
		if (entity != null) {
			Location entityLocation = entity.getLocation();
			if (ChunkCoords.isSameChunk(shopkeeper.getLocation(), entityLocation)) {
				// Check if the entity was removed due to the world's difficulty:
				if (entity.isDead()
						&& EntityUtils.isRemovedOnPeacefulDifficulty(this.getEntityType())
						&& LocationUtils.getWorld(entityLocation).getDifficulty() == Difficulty.PEACEFUL) {
					skipRespawnAttemptsIfPeaceful = true;
					// This is a warning in order to inform server admins about the issue right
					// away.
					// This is only logged once per affected shopkeeper and then skipped until the
					// difficulty is changed.
					Log.warning(shopkeeper.getLocatedLogPrefix() + this.getEntityType()
							+ " was removed due to the world's difficulty being set to peaceful."
							+ " Respawn attempts are skipped until the difficulty is changed.");
					// No return here because we still need to clean up the old entity.
				} else {
					// The entity has been removed (e.g. by another plugin), or the chunk silently
					// unloaded (without a corresponding ChunkUnloadEvent):
					Log.debug(() -> shopkeeper.getLocatedLogPrefix() + this.getEntityType() +
							" was removed. Maybe by another plugin, or the chunk was silently "
							+ "unloaded. (dead: " + entity.isDead() + ", valid: " + entity.isValid()
							+ ", chunk loaded: " + ChunkCoords.isChunkLoaded(entityLocation) + ")");
				}
			} // Else: The entity might have moved into a chunk that was then unloaded.

			// Despawn (i.e. cleanup) the previously spawned but no longer active entity:
			this.despawn();

			if (skipRespawnAttemptsIfPeaceful) {
				return false;
			}
		}

		Log.debug(() -> shopkeeper.getLocatedLogPrefix() + this.getEntityType()
				+ " is missing. Attempting respawn.");

		boolean spawned = this.spawn(); // This will load the chunk if necessary
		if (!spawned) {
			// TODO Maybe add a setting to remove shopkeeper if it can't be spawned a certain amount
			// of times?
			Log.debug("  Respawn failed");
			respawnAttempts += 1;
			if (respawnAttempts >= MAX_RESPAWN_ATTEMPTS) {
				// Throttle the rate at which we attempt to respawn the entity:
				this.throttleTickRate();
			}
		} // Else: respawnAttempts and tick rate got reset.
		return spawned;
	}

	// This is not only relevant when gravity is enabled, but also to react to other plugins
	// teleporting shopkeeper entities around or enabling their AI again.
	private void teleportBackIfMoved() {
		assert this.isActive();
		E entity = Unsafe.assertNonNull(this.entity);
		// Note: Comparing the entity's current location with the last spawn location (instead of
		// freshly calculating the 'intended' spawn location) not only provides a small performance
		// benefit, but also ensures that shopkeeper mobs don't start to fall if the block below
		// them is broken and gravity is disabled (which is confusing when gravity is supposed to be
		// disabled).
		// However, to account for shopkeepers that have previously been placed above passable or
		// non-full blocks, which might also have been broken since then, we still place the
		// shopkeeper mob up to one block below their location when they are respawned (we just
		// don't move them there dynamically during this check). If the mob is supposed to
		// dynamically move when the block below it is broken, gravity needs to be enabled.
		Location entityLoc = Unsafe.assertNonNull(entity.getLocation(sharedLocation));
		Location lastSpawnLocation = Unsafe.assertNonNull(this.lastSpawnLocation);
		// This also account for the worlds being different:
		if (LocationUtils.getDistanceSquared(entityLoc, lastSpawnLocation) > 0.2D) {
			// The squared distance 0.2 triggers for distances slightly below 0.5. Since we spawn
			// the entity at the center of the spawn block, this ensures that we teleport it back
			// into place whenever it changes its block.
			// Teleport back:
			Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLocatedLogPrefix()
					+ "Entity moved (" + TextUtils.getLocationString(entityLoc)
					+ "). Teleporting back.");
			// We freshly determine a potentially new spawn location:
			// The previous spawn location might no longer be ideal. For example, if the shopkeeper
			// previously spawned slightly below its actual spawn location (due to there missing
			// some block), players might want to reset the shopkeeper's location by letting it fall
			// due to gravity and then placing a block below its actual spawn location for the
			// shopkeeper to now be able to stand on.
			// Non-null: This is only called for shopkeepers in active chunks, i.e. loaded worlds.
			Location spawnLocation = Unsafe.assertNonNull(this.getSpawnLocation());
			spawnLocation.setYaw(entityLoc.getYaw());
			spawnLocation.setPitch(entityLoc.getPitch());
			this.lastSpawnLocation = spawnLocation;

			SKShopkeepersPlugin.getInstance().getForcingEntityTeleporter()
					.teleport(entity, spawnLocation);

			this.overwriteAI();
		}
		sharedLocation.setWorld(null); // Reset
	}

	public void teleportBack() {
		@Nullable E entity = this.getEntity(); // Null if not spawned
		if (entity == null) return;

		Location lastSpawnLocation = Unsafe.assertNonNull(this.lastSpawnLocation);
		Location entityLoc = entity.getLocation();
		lastSpawnLocation.setYaw(entityLoc.getYaw());
		lastSpawnLocation.setPitch(entityLoc.getPitch());

		SKShopkeepersPlugin.getInstance().getForcingEntityTeleporter()
				.teleport(entity, lastSpawnLocation);
	}

	// AI

	/**
	 * This is called whenever the AI of the entity is ticked, while it is in range of players.
	 * <p>
	 * The tick rate is defined by {@link Settings#entityBehaviorTickPeriod}. The AI is also ticked
	 * while the entity is currently falling.
	 */
	public void tickAI() {
		Entity entity = this.getEntity();
		if (entity == null) return; // Unexpected

		// Nothing by default.
	}

	// NAMING

	@Override
	public void setName(@Nullable String name) {
		@Nullable E entity = this.entity;
		if (entity == null) return;
		this.applyName(entity, name);
	}

	protected void applyName(@NonNull E entity, @Nullable String name) {
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			String preparedName = this.prepareName(Messages.nameplatePrefix + name);
			// Set entity name plate:
			entity.setCustomName(preparedName);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// Remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	@Override
	public @Nullable String getName() {
		@Nullable E entity = this.entity;
		if (entity == null) return null;
		return entity.getCustomName();
	}

	// EDITOR ACTIONS

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		return editorButtons;
	}
}
