package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObject;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPC.Metadata;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;

/**
 * A shop object that is represented by a Citizens NPC.
 * <p>
 * Citizens NPCs might move around, be teleported even when they are not yet spawned, or change
 * their location while the Shopkeepers plugin is not running. This shop object will try to
 * dynamically adopt the NPC's current (possibly stored) location whenever this shop object is
 * loaded, the Citizens plugin is dynamically enabled or reloads its NPCs, the NPC is spawned or
 * teleported, or this shop object is ticked or ends ticking and detects that the NPC has moved.
 * <p>
 * However, there is one exception to this: If the NPC's world is currently not loaded, the NPC
 * provides no stored location, and we can therefore not differentiate between whether the NPC's
 * world is currently not loaded, or whether the NPC has not yet been spawned at all. In the latter
 * case, we may want to spawn the NPC once the shopkeeper's chunk is loaded and activated.
 * Consequently, if the shopkeeper's last known chunk is activated and the shop object starts
 * ticking, but the NPC still provides no stored (and loaded) location, we will spawn the NPC at the
 * shopkeeper's location, and might thereby teleport the NPC from its previous, but no longer loaded
 * location, to the shopkeeper's location.
 */
public class SKCitizensShopObject extends AbstractEntityShopObject implements CitizensShopObject {

	// TODO This shop object currently relies on the regular living entity shopkeeper interaction
	// handling. Separate this?

	// Note: When the Citizens plugin (re-)loads NPCs that are marked as 'should-spawn' (i.e. that
	// were not explicitly despawned), it will spawn the NPCs, even if their chunks are not loaded
	// currently. This will briefly load the chunks, and then unload them again a tick or so later,
	// which despawns the NPCs again.
	// This brief spawning and immediate despawning of the NPC can result in the shop object being
	// briefly registered and then immediately unregistered again.

	// Null if there is no associated NPC, e.g. because no NPC has been created yet for the shop
	// object (e.g. if Citizens was not enabled at the time the shop object has been created):
	public static final Property<@Nullable UUID> NPC_UNIQUE_ID = new BasicProperty<@Nullable UUID>()
			.dataKeyAccessor("npcId", UUIDSerializers.LENIENT)
			.nullable()
			.defaultValue(null)
			.build();
	public static final Property<@Nullable DataContainer> NPC_DATA = new BasicProperty<@Nullable DataContainer>()
			.dataKeyAccessor("npc-data", DataContainerSerializers.DEFAULT)
			.nullable() // Saving of NPC data is optional
			.defaultValue(null)
			.build();

	public static final String CREATION_DATA_NPC_UUID_KEY = "CitizensNpcUUID";
	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_PERIOD_SECONDS + 1
	);

	protected final CitizensShops citizensShops;

	private final PropertyValue<@Nullable UUID> npcUniqueIdProperty = new PropertyValue<>(NPC_UNIQUE_ID)
			.onValueChanged((property, oldValue, newValue, updateFlags) -> {
				Unsafe.initialized(this).onNPCUniqueIdChanged(oldValue, newValue);
			})
			.build(properties);
	private @Nullable DataContainer npcData = null;

	// Only used initially, when the shopkeeper is created by a player. If this name is not
	// available when we create the NPC, we fall back to a different name.
	private @Nullable String creatorName = null;
	// If false, this will not remove the NPC on deletion:
	private boolean destroyNPC = true;

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkLimiter = new RateLimiter(
			CHECK_PERIOD_SECONDS,
			nextCheckingOffset.getAndIncrement()
	);

	private @Nullable Entity entity = null;

	protected SKCitizensShopObject(
			CitizensShops citizensShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(shopkeeper, creationData);
		this.citizensShops = citizensShops;
		if (creationData != null) {
			// Can be null here, as currently only NPC shopkeepers created by the shopkeeper trait
			// provide the NPC's unique id via the creation data:
			UUID npcId = creationData.getValue(CREATION_DATA_NPC_UUID_KEY);
			npcUniqueIdProperty.setValue(npcId, Collections.emptySet()); // Not marking dirty
			Player creator = creationData.getCreator();
			this.creatorName = (creator != null) ? creator.getName() : null;
		}
	}

	@Override
	public SKCitizensShopObjectType getType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		npcUniqueIdProperty.load(shopObjectData);
		this.loadNpcData(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		npcUniqueIdProperty.save(shopObjectData);
		this.saveNpcData(shopObjectData, saveAll);
	}

	// NPC ID

	// Can be null if not yet set.
	public @Nullable UUID getNPCUniqueId() {
		return npcUniqueIdProperty.getValue();
	}

	private void setNPCUniqueId(@Nullable UUID npcId) {
		npcUniqueIdProperty.setValue(npcId); // Can be null
	}

	private void onNPCUniqueIdChanged(@Nullable UUID oldValue, @Nullable UUID newValue) {
		// Update registrations:
		if (shopkeeper.isValid()) {
			if (oldValue != null) {
				citizensShops.unregisterCitizensShopkeeper(SKCitizensShopObject.this, oldValue);
			}
			if (newValue != null) {
				citizensShops.registerCitizensShopkeeper(SKCitizensShopObject.this, newValue);
			}
			// TODO Run NPC sync here? If the uuid has changed, the shopkeeper might be linked to a
			// different NPC now!
			// However, on load (e.g. snapshot loaded) and we also have NPC data (but only then), we
			// already invoke NPC sync.
			// And in createNPCIfMissing, we also already invoke NPC sync.
			// And if we were to invoke NPC skin when the uuid has been set to null during delete,
			// we would end up recreating the NPC
		}
	}

	// NPC

	public @Nullable NPC getNPC() {
		UUID npcUniqueId = this.getNPCUniqueId();
		if (npcUniqueId == null) return null;
		if (!citizensShops.isEnabled()) return null;
		return CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId);
	}

	private @Nullable EntityType getEntityType() {
		Entity entity = this.getEntity(); // Null if not spawned
		if (entity != null) return entity.getType();

		NPC npc = this.getNPC();
		if (npc == null) return null;

		MobType mobType = Unsafe.assertNonNull(npc.getOrAddTrait(MobType.class));
		return mobType.getType();
	}

	// Returns null if the NPC could not be created, including if the NPC has already been created
	// before.
	private @Nullable NPC createNpcIfNotYetCreated() {
		if (this.getNPCUniqueId() != null) {
			// The NPC has already been created in the past. Even if it no longer exists, we won't
			// force a recreation of it here, because periodically recreating new NPCs could go
			// really wrong.
			return null;
		}
		assert this.getNPC() == null;
		if (!citizensShops.isEnabled()) return null;

		Log.debug(() -> shopkeeper.getLogPrefix() + "Creating Citizens NPC.");

		EntityType entityType = Settings.defaultCitizenNpcType;
		// Note: The spawn location can be null if the world is not loaded currently. We can still
		// create the NPC, but spawning will happen later once the world is loaded.
		Location spawnLocation = this.getSpawnLocation(); // Can be null
		// The NPC name is initially empty (works fine, even for player NPCs):
		@Nullable NPC npc = citizensShops.createNPC(spawnLocation, entityType, "");
		if (npc == null) {
			// NPC creation failed for some reason.
			Log.debug(() -> shopkeeper.getLogPrefix() + "Failed to create Citizens NPC!");
			return null;
		}

		this.setNPCUniqueId(npc.getUniqueId());

		// Empty initial name for non-player NPCs:
		String name = "";
		if (entityType == EntityType.PLAYER) {
			// The name influences the initial skin of the player NPC:
			if (shopkeeper instanceof PlayerShopkeeper) {
				// Use the owner name for player shops (regardless of who actually created the
				// shop):
				name = ((PlayerShopkeeper) shopkeeper).getOwnerName();
			} else {
				// Fallback to empty name (works fine, even for player NPCs):
				name = (creatorName != null) ? creatorName : "";
			}
		}
		assert name != null;

		// Apply the name:
		// This also applies the nameplate prefix and adjusts the nameplate visibility if required.
		this.setNpcName(npc, name);

		return npc;
	}

	// This synchronizes the state of this shop object with its corresponding Citizens NPC.
	// This is run whenever this shop object is loaded, or the NPC might have become available or
	// has been reloaded.
	private void synchronizeNpc() {
		boolean npcHasChanged = false;
		NPC npc = this.getNPC();
		if (npc == null) {
			npc = this.createNpcIfNotYetCreated();
			if (npc == null) {
				return; // NPC could not be created or is not available
			} else {
				// NPC has been newly created:
				npcHasChanged = true;
			}
		}
		assert npc != null;

		npcHasChanged |= this.applyNpcData(npc);
		npcHasChanged |= this.updateNpcOwner(npc);
		npcHasChanged |= this.updateNpcFluidPushable(npc);

		// Update the registered NPC entity:
		// This also updates the shopkeeper's location if the NPC is spawned and has changed its
		// location.
		this.setEntity(npc.getEntity());

		if (!this.isSpawned()) {
			// Check if we can update the shopkeeper's location anyway, even if the NPC is not
			// spawned currently:
			this.updateShopkeeperLocation(npc);
		}

		if (npcHasChanged) {
			citizensShops.onNPCEdited(npc);
		}
	}

	// Returns true if the NPC has changed.
	private boolean updateNpcOwner(NPC npc) {
		assert npc != null;
		if (!(shopkeeper instanceof PlayerShopkeeper)) return false;
		PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;

		boolean npcHasChanged = false;
		if (Settings.setCitizenNpcOwnerOfPlayerShops) {
			UUID ownerId = playerShop.getOwnerUUID();
			Owner ownerTrait = Unsafe.assertNonNull(npc.getOrAddTrait(Owner.class));
			if (!ownerId.equals(ownerTrait.getOwnerId())) {
				ownerTrait.setOwner(ownerId);
				npcHasChanged = true;
				Log.debug(() -> shopkeeper.getLogPrefix() + "Citizens NPC owner set.");
			}
		} else if (npc.hasTrait(Owner.class)) {
			npc.removeTrait(Owner.class);
			npcHasChanged = true;
			Log.debug(() -> shopkeeper.getLogPrefix() + "Citizens NPC owner removed.");
		}
		return npcHasChanged;
	}

	private boolean updateNpcFluidPushable(NPC npc) {
		switch (Settings.citizenNpcFluidPushable) {
		case TRUE:
			if (!npc.isPushableByFluids()) {
				npc.data().set(Metadata.FLUID_PUSHABLE, true);
				Log.debug(() -> shopkeeper.getLogPrefix() + "Made Citizens NPC fluid pushable.");
				return true;
			}
			return false;
		case FALSE:
			if (npc.isPushableByFluids()) {
				npc.data().set(Metadata.FLUID_PUSHABLE, false);
				Log.debug(() -> shopkeeper.getLogPrefix() + "Made Citizens NPC fluid unpushable.");
				return true;
			}
			return false;
		default:
			return false;
		}
	}

	// NPC DATA

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"citizens-npc-data-cleanup",
				MigrationPhase.ofShopObjectClass(SKCitizensShopObject.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				// When this setting has been disabled, we automatically delete any previously saved
				// but no longer required NPC data:
				if (Settings.snapshotsSaveCitizenNpcData) return false;

				ShopObjectData shopObjectData = shopkeeperData.get(AbstractShopkeeper.SHOP_OBJECT_DATA);
				DataContainer npcData = shopObjectData.get(NPC_DATA);
				if (npcData != null) {
					Log.warning(logPrefix + "Deleted previously saved Citizens NPC data!");
					shopObjectData.set(NPC_DATA, null);
					return true;
				}
				return false;
			}
		});
	}

	private void loadNpcData(ShopObjectData shopObjectData) throws InvalidDataException {
		// Note: If we store previously restored but not yet applied NPC data, and we then restore
		// another snapshot that does not store any NPC data, we would normally clear the currently
		// stored but not yet applied NPC data! In order to prevent this loss of data, we keep the
		// previous NPC data in this case.
		// TODO However, there might be situations in which we want the shop object to strictly
		// adapt the loaded state.
		// Ideally, this would require some kind of flag to differentiate here between a snapshot
		// being applied vs the shop object being loaded normally. However, this isn't much of an
		// issue currently: When we normally reload the shopkeepers from disk (eg. during a reload
		// of the plugin), we fully recreate the shop objects. So any state in memory is lost
		// anyway (if it hasn't been saved to disk before).
		DataContainer previousNpcData = this.npcData;
		this.npcData = shopObjectData.get(NPC_DATA);
		if (npcData == null && previousNpcData != null) {
			Log.warning(shopkeeper.getLogPrefix() + "Prevented previously restored but not yet "
					+ "applied Citizens NPC data from being cleared!");
			this.npcData = previousNpcData;
			shopkeeper.markDirty(); // Our shop object state differs from the loaded data
		}
		if (npcData != null) {
			// TODO Do we need to somehow apply NPC data migrations?
			// Previously saved but no longer needed NPC data is deleted during data migrations:
			assert Settings.snapshotsSaveCitizenNpcData;
			// Update the NPC, if it is currently available:
			// If the NPC is not available currently, we remember the NPC data and try to apply it
			// once the NPC becomes available again.
			if (shopkeeper.isValid()) {
				this.synchronizeNpc();
			}
			// Else: The shopkeeper is currently being set up. The NPC setup is triggered once the
			// shopkeeper is added to the shopkeeper registry.
		}
	}

	private void saveNpcData(ShopObjectData shopObjectData, boolean saveAll) {
		// If the saving of NPC data is disabled, we also skip the saving of any previously restored
		// but not yet applied NPC data.
		if (!Settings.snapshotsSaveCitizenNpcData) return;

		// There is no need to try to get the current NPC data if we still store previously restored
		// NPC data that we were not yet able to apply (because the NPC hasn't been loaded in the
		// meantime). We then preserve the not yet applied NPC data, even if 'saveAll' is false.
		DataContainer npcData = this.npcData;
		if (saveAll && npcData == null) {
			NPC npc = this.getNPC();
			if (npc == null) {
				UUID npcId = this.getNPCUniqueId();
				if (npcId != null) {
					Log.warning(shopkeeper.getLogPrefix()
							+ "Could not save the data of the corresponding Citizens NPC! "
							+ "Citizens NPC not found (uuid: " + npcId + ")! "
							+ "Is the Citizens plugin enabled?");
				}
				// Else: The NPC has not yet been created, so there is no data to save, and no need
				// to log a warning.
				return;
			} else {
				npcData = CitizensUtils.Internal.saveNpc(npc);
			}
		}
		shopObjectData.set(NPC_DATA, npcData);
	}

	// It may be necessary to also apply other changes to the NPC after the NPC state has been
	// restored. Usually, this should therefore not be called directly, but as part of #setupNpc().
	// Returns true if the NPC has changed.
	private boolean applyNpcData(NPC npc) {
		assert npc != null;
		DataContainer npcData = this.npcData;
		if (npcData == null) return false; // Nothing to apply

		Log.debug(() -> shopkeeper.getLogPrefix() + "Applying stored Citizens NPC state to NPC "
				+ npc.getId());
		CitizensUtils.Internal.loadNpc(npc, npcData);

		// Once applied, we can delete our copy of the NPC data:
		this.npcData = null;
		shopkeeper.markDirty();
		return true;
	}

	// LIFE CYCLE

	protected void setKeepNPCOnDeletion() {
		destroyNPC = false;
	}

	@Override
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onShopkeeperAdded(cause);

		// Synchronize with the NPC:
		// If not yet done, this will first try to create the NPC.
		this.synchronizeNpc();

		// Register:
		UUID npcId = this.getNPCUniqueId();
		if (npcId != null) {
			citizensShops.registerCitizensShopkeeper(this, npcId);
		}
	}

	@Override
	public void remove() {
		super.remove();

		// Unset any currently tracked NPC entity:
		this.setEntity(null);

		// Unregister:
		UUID npcId = this.getNPCUniqueId();
		if (npcId != null) {
			citizensShops.unregisterCitizensShopkeeper(this, npcId);
		}
	}

	@Override
	public void delete() {
		super.delete();
		assert this.entity == null; // Already cleared during #remove()

		// Check if there even is a corresponding NPC (maybe it has already been deleted, or it has
		// not actually been created yet):
		if (this.getNPCUniqueId() == null) return;
		if (destroyNPC) {
			NPC npc = this.getNPC();
			if (npc != null) {
				CitizensShopkeeperTrait shopkeeperTrait = Unsafe.cast(npc.getTraitNullable(CitizensShopkeeperTrait.class));
				if (shopkeeperTrait != null) {
					// Let the trait handle NPC related cleanup (i.e. we only remove the trait in
					// this case):
					shopkeeperTrait.onShopkeeperDeletion(shopkeeper);
				} else {
					Log.debug(() -> shopkeeper.getUniqueIdLogPrefix() + "Deleting Citizens NPC "
							+ CitizensShops.getNPCIdString(npc) + " due to shopkeeper deletion.");
					npc.destroy(); // The NPC was created by us, so we remove it again
					citizensShops.onNPCEdited(npc);
				}
			} else {
				// TODO: NPC not yet loaded or citizens not enabled: How to remove the Citizens NPC
				// later?
				// Usually not a problem, because players cannot delete Citizens shopkeepers if the
				// corresponding Citizens NPC isn't spawned in the world (exception: deletion via
				// commands).
			}
		}
		this.setNPCUniqueId(null);
	}

	/**
	 * Called when the corresponding Citizens NPC is about to be deleted.
	 * <p>
	 * This might be called before the NPC is despawned.
	 * 
	 * @param player
	 *            the player who deleted the NPC, can be <code>null</code> if not available
	 */
	void onNPCDeleted(@Nullable Player player) {
		// Ignore if the NPC is deleted due to the shopkeeper being deleted:
		if (!shopkeeper.isValid()) return;

		NPC npc = Unsafe.assertNonNull(this.getNPC());
		Log.debug(() -> shopkeeper.getUniqueIdLogPrefix()
				+ "Deletion due to the deletion of Citizens NPC " + CitizensShops.getNPCIdString(npc)
				+ (player != null ? " by player " + TextUtils.getPlayerString(player) : ""));
		// The NPC is already getting deleted, so we don't need to delete it:
		this.setKeepNPCOnDeletion();
		shopkeeper.delete(player);
	}

	/**
	 * This is called when Citizens shops have been enabled.
	 * <p>
	 * This might be called a few ticks after the actual enabling of Citizens shops, and only if the
	 * Citizens shops are still enabled at this point.
	 * <p>
	 * This might not be called if Citizens shops are already enabled at the time this shop object
	 * is created or loaded.
	 */
	void onCitizensShopsEnabled() {
		// Synchronize with the NPC:
		this.synchronizeNpc();
	}

	/**
	 * This is called when Citizens shops are being disabled (e.g. when the Citizens plugin is
	 * disabled).
	 */
	void onCitizensShopsDisabled() {
	}

	/**
	 * This is called when the Citizens plugin has reloaded its NPCs.
	 */
	void onCitizensReloaded() {
		// Synchronize with the NPC:
		this.synchronizeNpc();
	}

	// ACTIVATION

	@Override
	public @Nullable Entity getEntity() {
		if (entity != null) {
			assert this.getNPC() != null;
			assert Unsafe.assertNonNull(this.getNPC()).getEntity() == entity;
		}
		return entity;
	}

	@Override
	public boolean isActive() {
		Entity entity = this.getEntity();
		if (entity == null) return false;
		// Note: Citizens despawns the entity on chunk unloads. It is therefore sufficient to check
		// if the entity is still alive.
		return !entity.isDead();
	}

	private @Nullable Location getSpawnLocation() {
		Location spawnLocation = shopkeeper.getLocation();
		if (spawnLocation == null) return null; // World not loaded currently
		spawnLocation.add(0.5D, 0.5D, 0.5D); // Spawn at the center of the block
		return spawnLocation;
	}

	@Override
	public boolean spawn() {
		NPC npc = this.getNPC();
		if (npc == null) {
			this.onSpawnFailed();
			return false;
		}

		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) {
			this.onSpawnFailed();
			return false;
		}

		if (!npc.spawn(spawnLocation, SpawnReason.PLUGIN)) {
			this.onSpawnFailed();
			return false;
		}

		this.onSpawnSucceeded();
		return true;
	}

	@Override
	public void despawn() {
		NPC npc = this.getNPC();
		if (npc == null) return;

		npc.despawn(DespawnReason.PLUGIN);
	}

	// TODO Use De/SpawnReason PendingRespawn/Respawn for respawns?

	@Override
	public CompletableFuture<Boolean> move() {
		// TODO If the NPC or the shopkeeper's world is not loaded currently, the NPC remains at its
		// previous location, and it may update the shopkeeper's location back to its current
		// location once the NPC is loaded. I.e. moving the shopkeeper will have no effect then.
		// Maybe remember the target location and apply it to the NPC once it is loaded? But what if
		// the NPC's location has changed in the meantime as well (e.g. externally or while the
		// Shopkeepers plugin was not enabled)?
		NPC npc = this.getNPC();
		if (npc == null) return CompletableFuture.completedFuture(false);

		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) return CompletableFuture.completedFuture(false);

		if (npc.isSpawned()) {
			npc.teleport(spawnLocation, TeleportCause.PLUGIN);
			// For simplicity, we assume that the teleport succeeded:
			return CompletableFuture.completedFuture(true);
		} else {
			// TODO This also changes the NPC's spawn state (similar to Citizens own teleport
			// command though).
			return CompletableFuture.completedFuture(npc.spawn(spawnLocation, SpawnReason.PLUGIN));
		}
	}

	// Null if the NPC entity despawned or should no longer be tracked.
	void setEntity(@Nullable Entity entity) {
		if (entity != null) {
			NPC npc = Unsafe.assertNonNull(this.getNPC());
			assert npc.getEntity() == entity;

			this.onSpawnSucceeded();

			// Check if our shopkeeper location is still correct:
			// If not yet done, this will also activate the shopkeeper's chunk and start ticking the
			// shopkeeper. While the shopkeeper is ticked, we regularly update its location to match
			// that of the NPC.
			// We also update the shopkeeper's location once the shopkeeper's chunk is deactivated,
			// i.e. once the shopkeeper stops ticking: If the NPC has been moved to a different
			// chunk since the last shopkeeper tick, this ensures that we continue to activate the
			// NPC's current chunk and keep ticking the shopkeeper as long as the NPC is still
			// spawned somewhere.
			this.updateShopkeeperLocation();
		}

		this.entity = entity;
		this.onIdChanged();
	}

	// TICKING

	@Override
	public void onStopTicking() {
		super.onStopTicking();

		// Update the shopkeeper's location if the NPC moved since the last shopkeeper tick. This
		// ensures that we activate the NPC's current chunk and that we therefore continue to tick
		// this shopkeeper while the NPC is still spawned somewhere.
		this.updateShopkeeperLocation();
	}

	@Override
	public void onTick() {
		super.onTick();
		// TODO If the NPC is moved to a different world, and the previous chunk is unloaded, it may
		// take up to 10 seconds before the NPC can be interacted with again because it is no longer
		// part of the active shopkeepers.
		// TODO Actually, the NPC is no longer considered a shopkeeper during these 10 seconds! This
		// could result is all kinds of issues.
		if (!checkLimiter.request()) {
			return;
		}

		NPC npc = this.getNPC();
		if (npc == null) {
			// The NPC is not available currently.
			// Not going to force NPC creation, as this seems like it could go really wrong.
			return;
		}

		// Indicate ticking activity for visualization:
		this.indicateTickActivity();

		this.respawnNpcIfMissing(npc);

		// Update the shopkeeper location if the NPC has moved:
		this.updateShopkeeperLocation(npc);
	}

	private void respawnNpcIfMissing(NPC npc) {
		assert npc != null;
		// Check if the NPC has been spawned at least once before:
		Location currentLocation = npc.getStoredLocation();
		if (currentLocation == null) {
			// TODO The NPC's stored location may also be null if the NPC's world is not loaded
			// currently. However, we cannot differentiate these cases. So this might move the NPC
			// from its previous but no longer loaded location to the expected shopkeeper location
			// (which isn't that big of an issue).
			assert !npc.isSpawned();
			Location expectedLocation = this.getSpawnLocation();
			if (expectedLocation == null) {
				// The spawn location's world is not loaded currently. We only tick shop objects in
				// loaded chunks, but the world might have been unloaded during the ticking.
				return;
			}
			assert expectedLocation.getWorld() != null;

			Log.debug(() -> shopkeeper.getLocatedLogPrefix()
					+ "Citizens NPC has no stored location. Attempting spawn.");
			// Citizens will log a debug message when it cannot spawn the NPC currently, but will
			// then later attempt to spawn the NPC when the chunk gets loaded:
			npc.spawn(expectedLocation);
			// Note: We don't trigger a save of the Citizens NPC data if we only spawned the NPC /
			// changed its stored location. Even if this data is not eventually saved by Citizens,
			// we will be able to just spawn the NPC again in the future.
			return;
		}
		// Citizens never returns a location with null world:
		assert currentLocation != null && currentLocation.getWorld() != null;

		// Check if a previously spawned NPC entity is still there:
		// Note: If the entity is null, the NPC might also have been intentionally despawned.
		// Note: Entity#isDead is sufficient to detect the entity removal. Checking the slightly
		// more costly Entity#isValid is not required, since Citizens removes the entity on chunk
		// unloads anyway.
		Entity entity = npc.getEntity();
		if (entity != null && entity.isDead()) {
			// The entity has previously been spawned but is marked for removal.
			// The Citizens plugin only handles EntityDeathEvents (by playing a death animation and
			// respawning the NPC a few moments later). However, the entity can also be removed
			// without a corresponding death event being called (e.g. when a plugin manually removes
			// the entity). The Citizens plugin does not automatically respawn the NPC in this case.
			// Also, we don't expect shopkeeper entities to naturally receive damage currently,
			// since we cancel all damage events involving shopkeeper entities, including NPC
			// entities. For consistency with non-Citizens shopkeepers, we automatically respawn the
			// NPC in this case.
			Log.debug(() -> shopkeeper.getLocatedLogPrefix()
					+ "Citizens NPC is missing. Attempting respawn.");
			// Note: We respawn the entity at its last known location, rather than the (expected)
			// shopkeeper location.
			// Citizens will log a debug message if it cannot spawn the NPC currently:
			npc.spawn(currentLocation);
			return;
		}
	}

	// SHOPKEEPER LOCATION

	// TODO Maybe avoid this immediate location update (we cannot reliably catch all location
	// updates immediately anyway) and instead ensure that we keep all shop objects ticking for as
	// long as they are still spawned. I.e. tick shopkeepers if either their chunks is active, or
	// they have been spawned and not despawned yet.
	// This is called whenever the NPC is about to teleport.
	void onNpcTeleport(Location toLocation) {
		assert toLocation != null;
		// Update shopkeeper location in advance (avoids a delayed task):
		shopkeeper.setLocation(toLocation);
	}

	private void updateShopkeeperLocation() {
		NPC npc = this.getNPC();
		if (npc == null) return;
		this.updateShopkeeperLocation(npc);
	}

	// This may also be called outside of shop object ticking.
	private void updateShopkeeperLocation(NPC npc) {
		assert npc != null;
		// Get the NPC's current location:
		Location currentLocation = npc.getStoredLocation();
		if (currentLocation == null) {
			// The NPC is not spawned, and either has not yet been spawned, or its world is not
			// loaded currently.
			// If the NPC's world is not loaded currently, we will update the shopkeeper's location
			// once the NPC is spawned.
			// Alternatively, if we start ticking the chunk of the shopkeeper's expected location
			// and the NPC still does not provide a valid location, we will spawn the NPC at the
			// shopkeeper's expected location.
			return;
		}
		// Citizens never returns a location with null world:
		assert currentLocation.getWorld() != null;

		// The shopkeeper's spawn location can be null if the shopkeeper's world is not loaded
		// currently.
		// However, in this case, since the NPC has a location with valid world, we can assume that
		// the NPC has changed its location to another world.
		Location expectedLocation = this.getSpawnLocation();
		assert expectedLocation == null || expectedLocation.getWorld() != null;

		// Update the shopkeeper's location if the NPC has moved:
		if (expectedLocation == null
				|| LocationUtils.getDistanceSquared(expectedLocation, currentLocation) > 1.0D) {
			Log.debug(DebugOptions.regularTickActivities, () -> shopkeeper.getLocatedLogPrefix()
					+ "Citizens NPC moved. Updating shopkeeper location.");
			shopkeeper.setLocation(currentLocation);
		}
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		// Note: We are not using Citizens' SpigotUtil.getMaxNameLength here because this might be
		// called even when Citizens is not enabled.
		// When exceeding the limit, Citizens will dynamically truncate the name of the NPC and log
		// a warning.
		return 256;
	}

	@Override
	public void setName(@Nullable String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (this.setNpcName(npc, name)) {
			citizensShops.onNPCEdited(npc);
		}
	}

	// Returns true if the NPC has changed.
	private boolean setNpcName(NPC npc, @Nullable String name) {
		assert npc != null;
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			boolean isPlayerNPC = (this.getEntityType() == EntityType.PLAYER);
			String preparedName;
			if (!isPlayerNPC) {
				preparedName = Messages.nameplatePrefix + name;
			} else {
				// The name influences the player NPC skin, so we avoid adding the prefix:
				preparedName = name;
			}
			preparedName = Unsafe.assertNonNull(this.prepareName(preparedName));

			// Set name:
			npc.setName(preparedName);

			// Update the nameplate visibility:
			// Player NPC don't support the hover option (the nameplate is always shown regardless).
			// Also, when using the hover option on player NPCs, the nameplate is limited to a
			// length of 16. Whereas otherwise, depending on Citizens settings, player NPC either
			// use scoreboards or holograms to display the NPC name, which are not affected by this
			// length limitation.
			npc.data().setPersistent(
					NPC.Metadata.NAMEPLATE_VISIBLE,
					Settings.alwaysShowNameplates || isPlayerNPC ? "true" : "hover"
			);
		} else {
			// Remove the name:
			npc.setName("");

			// Update the nameplate visibility:
			npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, "false");
		}
		return true;
	}

	@Override
	public @Nullable String getName() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getName();
	}

	// PLAYER SHOP OWNER

	@Override
	public void onShopOwnerChanged() {
		super.onShopOwnerChanged();
		assert shopkeeper instanceof PlayerShopkeeper;

		NPC npc = this.getNPC();
		if (npc == null) return;

		boolean npcHasChanged = this.updateNpcOwner(npc);

		if (!Settings.allowRenamingOfPlayerNpcShops) {
			// Update the NPC's name:
			String ownerName = ((PlayerShopkeeper) shopkeeper).getOwnerName();
			npcHasChanged |= this.setNpcName(npc, ownerName);
		}

		if (npcHasChanged) {
			citizensShops.onNPCEdited(npc);
		}
	}

	// EDITOR ACTIONS

	// TODO: Support sub types? A menu of entity types here would be cool.
	// TODO: Support equipping items? Is there a generic Citizens API for this?
}
