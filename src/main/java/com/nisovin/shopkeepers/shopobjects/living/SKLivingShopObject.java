package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Raider;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.DebugListener;
import com.nisovin.shopkeepers.util.EntityUtils;
import com.nisovin.shopkeepers.util.EventDebugListener;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class SKLivingShopObject<E extends LivingEntity> extends AbstractEntityShopObject implements LivingShopObject {

	protected static final double SPAWN_LOCATION_OFFSET = 0.98D;
	protected static final double SPAWN_LOCATION_RANGE = 2.0D;

	protected final LivingShops livingShops;
	private final SKLivingShopObjectType<?> livingObjectType;
	private E entity;
	private int respawnAttempts = 0;
	private boolean debuggingSpawn = false;
	private static long lastSpawnDebugging = 0; // shared among all living shopkeepers to prevent spam

	protected SKLivingShopObject(	LivingShops livingShops, SKLivingShopObjectType<?> livingObjectType,
									AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.livingShops = livingShops;
		this.livingObjectType = livingObjectType;
	}

	@Override
	public SKLivingShopObjectType<?> getType() {
		return livingObjectType;
	}

	@Override
	public EntityType getEntityType() {
		return livingObjectType.getEntityType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		// check for legacy uuid entry:
		if (configSection.contains("uuid")) {
			// mark dirty to remove this entry with the next save:
			shopkeeper.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
	}

	// ACTIVATION

	@Override
	public E getEntity() {
		// is-active check:
		// note: some spigot versions didn't check the isDead flag inside isValid:
		// note: isValid-flag gets set at the tick after handling all queued chunk unloads, so isChunkLoaded check is
		// needed if we check during chunk unloads and the entity in question might be in another chunk than the
		// currently unloaded one
		if (entity != null && !entity.isDead() && entity.isValid() && ChunkCoords.isChunkLoaded(entity.getLocation())) {
			return entity;
		}
		return null;
	}

	@Override
	public boolean isActive() {
		return (this.getEntity() != null);
	}

	@Override
	public String getId() {
		return this.getType().createObjectId(this.getEntity());
	}

	@Override
	public boolean needsSpawning() {
		return true; // despawn shop entities on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean despawnDuringWorldSaves() {
		// spawned entities are non-persistent and therefore already skipped during world saves:
		return false;
	}

	protected void assignShopkeeperMetadata(E entity) {
		entity.setMetadata("shopkeeper", new FixedMetadataValue(ShopkeepersPlugin.getInstance(), true));
	}

	protected void removeShopkeeperMetadata(E entity) {
		entity.removeMetadata("shopkeeper", ShopkeepersPlugin.getInstance());
	}

	// places the entity at the exact location it would fall to, within a range of at most 1 block below the spawn block
	// (because shopkeepers might have been placed 1 block above passable or non-full blocks)
	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world == null) return null; // world not loaded
		Location spawnLocation = new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + SPAWN_LOCATION_OFFSET, shopkeeper.getZ() + 0.5D);
		double distanceToGround = Utils.getCollisionDistanceToGround(spawnLocation, SPAWN_LOCATION_RANGE);
		if (distanceToGround == SPAWN_LOCATION_RANGE) {
			// no collision within the checked range, remove offset from spawn location:
			distanceToGround = SPAWN_LOCATION_OFFSET;
		}
		// adjust spawn location:
		spawnLocation.add(0.0D, -distanceToGround, 0.0D);
		return spawnLocation;
	}

	// Any preparation that needs to be done before spawning. Might only allow limited operations.
	protected void prepareEntity(E entity) {
		// assign metadata for easy identification by other plugins:
		this.assignShopkeeperMetadata(entity);

		// don't save the entity to the world data:
		entity.setPersistent(false);

		// apply name (if it has/uses one):
		this.applyName(entity, shopkeeper.getName());
	}

	// Any clean up that needs to happen for the entity. The entity might not be fully setup yet.
	protected void cleanUpEntity(E entity) {
		// disable AI:
		this.cleanupAI();

		// remove metadata again:
		this.removeShopkeeperMetadata(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean spawn() {
		// check if our current old entity is still valid:
		if (this.isActive()) return true;
		if (entity != null) {
			// perform cleanup before replacing the currently stored entity with a new one:
			this.cleanUpEntity(entity);
			entity = null; // reset
		}

		// prepare spawn location:
		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) {
			return false; // world not loaded
		}
		World world = spawnLocation.getWorld();
		assert world != null;

		// spawn entity:
		// TODO check if the block is passable before spawning there?
		// try to bypass entity-spawn blocking plugins:
		EntityType entityType = this.getEntityType();
		livingShops.forceCreatureSpawn(spawnLocation, entityType);

		entity = (E) world.spawn(spawnLocation, entityType.getEntityClass(), (entity) -> {
			// debugging entity spawning:
			if (entity.isDead()) {
				Log.debug("Spawning shopkeeper entity is dead already!");
			}

			// prepare entity, before it gets spawned:
			prepareEntity((E) entity);
		});

		if (this.isActive()) {
			// further setup entity after it was successfully spawned:
			entity.eject(); // some entities might automatically mount on nearby entities (like baby zombies on chicken)
			entity.setRemoveWhenFarAway(false);
			entity.setCanPickupItems(false);

			// disable breeding:
			if (entity instanceof Ageable) {
				Ageable ageable = ((Ageable) entity);
				ageable.setAdult();
				ageable.setBreed(false);
				ageable.setAgeLock(true);
			}

			// clear equipment:
			EntityEquipment equipment = entity.getEquipment();
			equipment.clear(); // TODO Does not work for foxes yet

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}

			// overwrite AI:
			this.overwriteAI();

			// prevent raider shopkeepers from participating in nearby raids:
			if (entity instanceof Raider) {
				NMSManager.getProvider().setCanJoinRaid((Raider) entity, false);
			}

			// apply sub type:
			this.onSpawn(entity);

			// success:
			return true;
		} else {
			// failure:
			E localEntity = this.entity;
			this.entity = null; // reset

			if (localEntity == null) {
				Log.warning("Failed to spawn shopkeeper entity: Entity is null");
			} else {
				// debug, if not already debugging and cooldown over:
				boolean debug = (Settings.debug && !debuggingSpawn && (System.currentTimeMillis() - lastSpawnDebugging) > (5 * 60 * 1000)
						&& localEntity.isDead() && ChunkCoords.isChunkLoaded(localEntity.getLocation()));

				Log.warning("Failed to spawn shopkeeper entity: Entity dead: " + localEntity.isDead() + ", entity valid: " + localEntity.isValid()
						+ ", chunk loaded: " + ChunkCoords.isChunkLoaded(localEntity.getLocation()) + ", debug -> " + debug);

				// debug entity spawning:
				if (debug) {
					// Print chunk's entity counts:
					EntityUtils.printEntityCounts(spawnLocation.getChunk());

					// Try again and log event activity:
					debuggingSpawn = true;
					lastSpawnDebugging = System.currentTimeMillis();
					Log.info("Trying again and logging event activity ..");

					// log all events occurring during spawning, and their registered listeners:
					DebugListener debugListener = DebugListener.register(true, true);

					// log creature spawn handling:
					EventDebugListener<CreatureSpawnEvent> spawnListener = new EventDebugListener<>(CreatureSpawnEvent.class, (priority, event) -> {
						LivingEntity spawnedEntity = event.getEntity();
						Log.info("  CreatureSpawnEvent (" + priority + "): " + "cancelled: " + event.isCancelled() + ", dead: " + spawnedEntity.isDead()
								+ ", valid: " + spawnedEntity.isValid() + ", chunk loaded: " + ChunkCoords.isChunkLoaded(spawnedEntity.getLocation()));
					});

					// try to spawn entity again:
					boolean result = this.spawn();

					// unregister listeners again:
					debugListener.unregister();
					spawnListener.unregister();
					debuggingSpawn = false;
					Log.info(".. Done. Successful: " + result);

					return result; // true if retry was successful
				}
			}
			return false;
		}
	}

	// gets called after the entity was spawned; can be used to apply any additionally configured mob-specific setup
	protected void onSpawn(E entity) {
		// nothing to do by default
	}

	// some mobs will always get their AI disabled in order to properly work:
	protected boolean isNoAIMobType() {
		switch (livingObjectType.getEntityType()) {
		case BAT:
		case ENDER_DRAGON:
		case ENDERMAN:
		case WITHER:
		case SILVERFISH:
		case BLAZE:
			return true;
		default:
			return false;
		}
	}

	protected void overwriteAI() {
		// setting the entity non-collidable:
		entity.setCollidable(false);
		NMSManager.getProvider().overwriteLivingEntityAI(entity);

		if (!Settings.useLegacyMobBehavior) {
			// disable AI (also disables gravity) and replace it with our own handling:
			this.setNoAI(entity);

			if (NMSManager.getProvider().supportsCustomMobAI()) {
				livingShops.getLivingEntityAI().addEntity(entity);
			}
		}

		if (Settings.silenceLivingShopEntities) {
			entity.setSilent(true);
		}
		if (Settings.disableGravity) {
			this.setNoGravity(entity);
			// when gravity gets disabled, we might be able to also disable collisions/pushing of mobs via noclip:
			NMSManager.getProvider().setNoclip(entity);
		}

		// set the NoAI tag always for certain entity types:
		if (this.isNoAIMobType()) {
			this.setNoAI(entity);
		}
	}

	protected final void setNoAI(E entity) {
		entity.setAI(false);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes that
		// it is currently falling:
		// TODO this can be removed once spigot ignores NoAI entities
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected final void setNoGravity(E entity) {
		entity.setGravity(false);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes
		// that it is currently falling:
		NMSManager.getProvider().setOnGround(entity, true);
	}

	protected void cleanupAI() {
		// disable AI:
		livingShops.getLivingEntityAI().removeEntity(entity);
	}

	@Override
	public void despawn() {
		if (entity == null) return;

		// clean up entity:
		this.cleanUpEntity(entity);

		// remove entity:
		entity.remove();
		entity = null;
	}

	@Override
	public Location getLocation() {
		if (this.isActive()) {
			return entity.getLocation();
		} else {
			return null;
		}
	}

	@Override
	public boolean check() {
		if (!this.isActive()) {
			Log.debug(() -> "Shopkeeper (" + shopkeeper.getPositionString() + ") missing, triggering respawn now");
			if (entity != null && ChunkCoords.isSameChunk(shopkeeper.getLocation(), entity.getLocation())) {
				// the chunk was silently unloaded before:
				Log.debug(
						() -> "  Chunk got silently unloaded or mob got removed! (dead: " + entity.isDead() + ", valid: "
								+ entity.isValid() + ", chunk loaded: " + ChunkCoords.isChunkLoaded(entity.getLocation()) + ")"
				);
			}
			boolean spawned = this.spawn(); // this will load the chunk if necessary
			if (spawned) {
				respawnAttempts = 0;
				return true;
			} else {
				// TODO maybe add a setting to remove shopkeeper if it can't be spawned a certain amount of times?
				Log.debug("  Respawn failed");
				return (++respawnAttempts > 5);
			}
		} else {
			Location entityLoc = entity.getLocation();
			Location spawnLocation = this.getSpawnLocation();
			assert spawnLocation != null; // since entity is active
			spawnLocation.setYaw(entityLoc.getYaw());
			spawnLocation.setPitch(entityLoc.getPitch());
			if (!entityLoc.getWorld().equals(spawnLocation.getWorld()) || entityLoc.distanceSquared(spawnLocation) > 0.4D) {
				// teleport back:
				entity.teleport(spawnLocation);
				this.overwriteAI();
				Log.debug(() -> "Shopkeeper (" + shopkeeper.getPositionString() + ") out of place, teleported back");
			}

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}
			return false;
		}
	}

	public void teleportBack() {
		E entity = this.getEntity(); // null if not active
		if (entity == null) return;

		Location spawnLocation = this.getSpawnLocation();
		assert spawnLocation != null; // since entity is active
		Location entityLoc = entity.getLocation();
		spawnLocation.setYaw(entityLoc.getYaw());
		spawnLocation.setPitch(entityLoc.getPitch());
		entity.teleport(spawnLocation);
	}

	@Override
	public void onShopkeeperOwnerChanged() {
		// The owner is not represented in this type of shop object.
	}

	// NAMING

	@Override
	public void setName(String name) {
		if (!this.isActive()) return;
		this.applyName(entity, name);
	}

	protected void applyName(E entity, String name) {
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.prepareName(name);
			// set entity name plate:
			entity.setCustomName(name);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		if (!this.isActive()) return null;
		return entity.getCustomName();
	}

	// EDITOR ACTIONS

	// no default actions
}
