package com.nisovin.shopkeepers.shopobjects.fancynpcs;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * A shop object that is represented by a FancyNpcs NPC.
 * <p>
 * This is the FancyNpcs equivalent of {@code SKCitizensShopObject}.
 * </p>
 */
public class SKFancyNpcShopObject extends AbstractEntityShopObject {

	public static final String CREATION_DATA_NPC_ID_KEY = "FancyNpcId";

	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(
			1,
			CHECK_PERIOD_SECONDS + 1
	);

	// The NPC id (the NpcData#getId() String) - stored to disk.
	public static final Property<@Nullable String> NPC_ID = new BasicProperty<@Nullable String>()
			.dataKeyAccessor("fancyNpcId", StringSerializers.LENIENT)
			.nullable()
			.defaultValue(null)
			.build();

	protected final FancyNpcsShops fancyNpcsShops;

	private final PropertyValue<@Nullable String> npcIdProperty = new PropertyValue<>(NPC_ID)
			.onValueChanged((property, oldValue, newValue, updateFlags) -> {
				Unsafe.initialized(this).onNpcIdChanged(oldValue, newValue);
			})
			.build(properties);

	// If false, we won't remove the NPC on deletion:
	private boolean destroyNpc = true;

	// Only used initially, when the shopkeeper is created by a player:
	private @Nullable String creatorName = null;

	// Rate limiter for periodic checks:
	private final RateLimiter checkLimiter = new RateLimiter(
			CHECK_PERIOD_SECONDS,
			nextCheckingOffset.getAndIncrement()
	);

	// The currently tracked NPC entity (null if not spawned):
	private @Nullable Entity entity = null;

	protected SKFancyNpcShopObject(
			FancyNpcsShops fancyNpcsShops,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(shopkeeper, creationData);
		this.fancyNpcsShops = fancyNpcsShops;
		if (creationData != null) {
			String npcId = creationData.getValue(CREATION_DATA_NPC_ID_KEY);
			npcIdProperty.setValue(npcId, Collections.emptySet());
			Player creator = creationData.getCreator();
			this.creatorName = (creator != null) ? creator.getName() : null;
		}
	}

	@Override
	public SKFancyNpcShopObjectType getType() {
		return SKDefaultShopObjectTypes.FANCYNPC();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		npcIdProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		npcIdProperty.save(shopObjectData);
	}

	// NPC ID

	public @Nullable String getNpcId() {
		return npcIdProperty.getValue();
	}

	private void setNpcId(@Nullable String npcId) {
		npcIdProperty.setValue(npcId);
	}

	private void onNpcIdChanged(@Nullable String oldValue, @Nullable String newValue) {
		if (shopkeeper.isValid()) {
			if (oldValue != null) {
				fancyNpcsShops.unregisterFancyNpcShopkeeper(SKFancyNpcShopObject.this, oldValue);
			}
			if (newValue != null) {
				fancyNpcsShops.registerFancyNpcShopkeeper(SKFancyNpcShopObject.this, newValue);
			}
		}
	}

	// NPC

	public @Nullable Npc getNpc() {
		String npcId = this.getNpcId();
		if (npcId == null) return null;
		if (!fancyNpcsShops.isEnabled()) return null;
		return FancyNpcsPlugin.get().getNpcManager().getNpcById(npcId);
	}

	private @Nullable EntityType getEntityType() {
		Entity entity = this.getEntity();
		if (entity != null) return entity.getType();
		Npc npc = this.getNpc();
		if (npc == null) return null;
		return npc.getData().getType();
	}

	private @Nullable Npc createNpcIfNotYetCreated() {
		if (this.getNpcId() != null) {
			return null; // Already created (or was created in the past)
		}
		assert this.getNpc() == null;
		if (!fancyNpcsShops.isEnabled()) return null;

		Log.debug(() -> shopkeeper.getLogPrefix() + "Creating FancyNpc NPC.");

		EntityType entityType = Settings.defaultCitizenNpcType;
		Location spawnLocation = this.getSpawnLocation();

		// Determine creator UUID:
		UUID creatorUUID = null;
		if (shopkeeper instanceof PlayerShopkeeper) {
			creatorUUID = ((PlayerShopkeeper) shopkeeper).getOwnerUUID();
		}

		// Ensure NPC name is unique using a prefix and shopkeeper's unique ID
		String uniqueId = shopkeeper.getUniqueId().toString().replace("-", "").substring(0, 12);
		String name = "sk_" + uniqueId;

		Npc npc = fancyNpcsShops.createNpc(spawnLocation, entityType, name, creatorUUID);
		if (npc == null) {
			Log.debug(() -> shopkeeper.getLogPrefix() + "Failed to create FancyNpc NPC!");
			return null;
		}

		this.setNpcId(npc.getData().getId());
		return npc;
	}

	private void synchronizeNpc() {
		Npc npc = this.getNpc();
		boolean justCreated = false;
		if (npc == null) {
			npc = this.createNpcIfNotYetCreated();
			if (npc == null) {
				return;
			}
			justCreated = true;
		}
		assert npc != null;

		// Sync location if needed:
		if (!this.isSpawned()) {
			this.updateShopkeeperLocation(npc);
		}

		// If the NPC was just created, createNpc has already called spawnForAll.
		// If it's an existing NPC, check and update visibility for all online players.
		if (!justCreated) {
			npc.checkAndUpdateVisibilityForAll();
		}
	}

	// LIFE CYCLE

	protected void setKeepNpcOnDeletion() {
		destroyNpc = false;
	}

	@Override
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onShopkeeperAdded(cause);

		this.synchronizeNpc();

		String npcId = this.getNpcId();
		if (npcId != null) {
			fancyNpcsShops.registerFancyNpcShopkeeper(this, npcId);
		}
	}

	@Override
	public void remove() {
		super.remove();

		this.setEntity(null);

		String npcId = this.getNpcId();
		if (npcId != null) {
			fancyNpcsShops.unregisterFancyNpcShopkeeper(this, npcId);
		}
	}

	@Override
	public void delete() {
		super.delete();
		assert this.entity == null;

		if (this.getNpcId() == null) return;
		if (destroyNpc) {
			Npc npc = this.getNpc();
			if (npc != null) {
				Log.debug(() -> shopkeeper.getUniqueIdLogPrefix() + "Deleting FancyNpc NPC "
						+ FancyNpcsShops.getNpcIdString(npc) + " due to shopkeeper deletion.");
				fancyNpcsShops.removeNpc(npc);
			}
		}
		this.setNpcId(null);
	}

	/**
	 * Called when the corresponding FancyNpc NPC is about to be deleted externally.
	 *
	 * @param player the player who deleted the NPC, can be null if not available
	 */
	void onNpcDeleted(@Nullable Player player) {
		if (!shopkeeper.isValid()) return;

		Npc npc = Unsafe.assertNonNull(this.getNpc());
		Log.debug(() -> shopkeeper.getUniqueIdLogPrefix()
				+ "Deletion due to the deletion of FancyNpc NPC " + FancyNpcsShops.getNpcIdString(npc)
				+ (player != null ? " by player " + player.getName() : ""));
		// NPC is already being deleted, so we don't attempt to remove it again:
		this.setKeepNpcOnDeletion();
		shopkeeper.delete(player);
	}

	void onFancyNpcsShopsEnabled() {
		this.synchronizeNpc();
	}

	void onFancyNpcsShopsDisabled() {
		this.setEntity(null);
	}

	// ACTIVATION

	@Override
	public @Nullable Entity getEntity() {
		return entity;
	}

	@Override
	public boolean isActive() {
		Npc npc = this.getNpc();
		if (npc == null) return false;
		return npc.getData().isSpawnEntity();
	}

	private @Nullable Location getSpawnLocation() {
		Location spawnLocation = shopkeeper.getLocation();
		if (spawnLocation == null) return null;
		// FancyNpcs are packet-based and do not handle gravity like regular entities.
		// Therefore, we align the NPC to the exact Y level of the block, without adding a 0.5 offset on the Y-axis.
		spawnLocation.add(0.5D, 0.0D, 0.5D);
		return spawnLocation;
	}

	@Override
	public boolean spawn() {
		Npc npc = this.getNpc();
		if (npc == null) {
			this.onSpawnFailed();
			return false;
		}

		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) {
			this.onSpawnFailed();
			return false;
		}

		// FancyNpcs manages its own spawning - just refresh all players:
		npc.getData().setLocation(spawnLocation);
		npc.getData().setSpawnEntity(true);
		
		npc.spawnForAll();

		this.onSpawnSucceeded();
		return true;
	}

	@Override
	public void despawn() {
		Npc npc = this.getNpc();
		if (npc == null) return;

		// Hide from all online players:
		npc.getData().setSpawnEntity(false);
		
		npc.removeForAll();
	}

	@Override
	public boolean move() {
		Npc npc = this.getNpc();
		if (npc == null) return false;

		Location spawnLocation = this.getSpawnLocation();
		if (spawnLocation == null) return false;

		npc.getData().setLocation(spawnLocation);
		
		npc.updateForAll();
		return true;
	}

	// Null if the NPC entity despawned or should no longer be tracked.
	void setEntity(@Nullable Entity entity) {
		if (entity != null) {
			this.onSpawnSucceeded();
			this.updateShopkeeperLocation();
		}

		this.entity = entity;
		this.onIdChanged();
	}

	// TICKING

	@Override
	public void onStopTicking() {
		super.onStopTicking();
		this.updateShopkeeperLocation();
	}

	@Override
	public void onTick() {
		super.onTick();
		if (!checkLimiter.request()) {
			return;
		}

		Npc npc = this.getNpc();
		if (npc == null) {
			return;
		}

		this.indicateTickActivity();
		this.updateShopkeeperLocation(npc);
	}

	// SHOPKEEPER LOCATION

	void onNpcTeleport(Location toLocation) {
		assert toLocation != null;
		shopkeeper.setLocation(toLocation);
	}

	private void updateShopkeeperLocation() {
		Npc npc = this.getNpc();
		if (npc == null) return;
		this.updateShopkeeperLocation(npc);
	}

	private void updateShopkeeperLocation(Npc npc) {
		assert npc != null;
		Location currentLocation = npc.getData().getLocation();
		if (currentLocation == null) return;

		Location shopkeeperLocation = shopkeeper.getLocation();
		if (shopkeeperLocation != null
				&& shopkeeperLocation.getWorld() == currentLocation.getWorld()
				&& shopkeeperLocation.distanceSquared(currentLocation) < 0.001D) {
			return; // No significant change
		}

		shopkeeper.setLocation(currentLocation);
	}

	// SHOP OBJECT NAME

	@Override
	public void setName(@Nullable String name) {
		Npc npc = this.getNpc();
		if (npc == null) return;
		npc.getData().setDisplayName(name != null ? name : "");
		
		npc.updateForAll();
	}

	@Override
	public @Nullable String getName() {
		Npc npc = this.getNpc();
		if (npc == null) return null;
		return npc.getData().getDisplayName();
	}
}
