package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperPropertyValuesHolder;
import com.nisovin.shopkeepers.shopkeeper.registry.ShopObjectRegistry;
import com.nisovin.shopkeepers.shopkeeper.spawning.ShopkeeperSpawnState;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorLayout;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValuesHolder;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Abstract base class for all shop object implementations.
 * <p>
 * Make sure to call {@link AbstractShopkeeper#markDirty()} on every change of data that might need
 * to be persisted.
 * <p>
 * The shop object needs to {@link #onIdChanged() register} itself whenever it is spawned,
 * despawned, or its object id might have changed. If this type of shop object manages its
 * {@link AbstractShopObjectType#mustBeSpawned() spawning} itself, it might need to register itself
 * earlier than shop objects whose spawning is managed by the Shopkeepers plugin: For instance, if
 * the shop object is already spawned during or shortly after chunk loads, it needs to be registered
 * in this exact moment, rather than when the shopkeeper's chunk is activated. And if the shop
 * object is able to spawn before the shopkeeper has been created or loaded, it may need to register
 * itself during {@link #onShopkeeperAdded(ShopkeeperAddedEvent.Cause)}.
 * <p>
 * If this type of shop object is able to move, teleport, or be spawned into a different chunk, the
 * {@link ShopkeeperRegistry} needs to be made aware of these location changes by
 * {@link AbstractShopkeeper#setLocation(Location) updating the location} of the corresponding
 * shopkeeper. If the shopkeeper is not ticking currently (i.e. if its previous chunk is not active
 * currently), these location updates need to happen quickly, so that the chunk of the shopkeeper's
 * new location, if currently loaded, can be activated and start the shopkeeper's ticking as quickly
 * as possible.
 */
public abstract class AbstractShopObject implements ShopObject {

	private static final String DATA_KEY_SHOP_OBJECT_TYPE = "type";

	/**
	 * Shop object type id.
	 */
	public static final Property<String> SHOP_OBJECT_TYPE_ID = new BasicProperty<String>()
			.name("type-id")
			.dataKeyAccessor(DATA_KEY_SHOP_OBJECT_TYPE, StringSerializers.STRICT)
			.validator(StringValidators.NON_EMPTY)
			.build();

	/**
	 * Shop object type, derived from the serialized {@link #SHOP_OBJECT_TYPE_ID shop object type
	 * id}.
	 */
	public static final Property<AbstractShopObjectType<?>> SHOP_OBJECT_TYPE = new BasicProperty<AbstractShopObjectType<?>>()
			.dataKeyAccessor(DATA_KEY_SHOP_OBJECT_TYPE, new DataSerializer<AbstractShopObjectType<?>>() {
				@Override
				public @Nullable Object serialize(AbstractShopObjectType<?> value) {
					Validate.notNull(value, "value is null");
					return value.getIdentifier();
				}

				@Override
				public AbstractShopObjectType<?> deserialize(
						Object data
				) throws InvalidDataException {
					String shopObjectTypeId = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
					SKShopObjectTypesRegistry shopObjectTypeRegistry = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry();
					AbstractShopObjectType<?> shopObjectType = shopObjectTypeRegistry.get(shopObjectTypeId);
					if (shopObjectType == null) {
						throw new InvalidDataException("Unknown shop object type: "
								+ shopObjectTypeId);
					}
					return shopObjectType;
				}
			})
			.build();

	/**
	 * Whether the last spawn attempt failed.
	 */
	public static final Property<Boolean> SPAWN_FAILED = new BasicProperty<Boolean>()
			.dataKeyAccessor("spawn-failed", BooleanSerializers.STRICT)
			.defaultValue(false)
			.omitIfDefault()
			.build();

	protected final AbstractShopkeeper shopkeeper; // Not null
	protected final PropertyValuesHolder properties; // Not null

	private final PropertyValue<Boolean> lastSpawnFailedProperty;

	private @Nullable Object lastId = null;
	private boolean tickActivity = false;

	// Fresh creation
	protected AbstractShopObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
		this.properties = new ShopkeeperPropertyValuesHolder(shopkeeper);
		this.lastSpawnFailedProperty = new PropertyValue<>(SPAWN_FAILED).build(properties);
	}

	@Override
	public abstract AbstractShopObjectType<?> getType();

	/**
	 * Gets the shopkeeper associated with this shop object.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Loads the shop object's data from the given {@link ShopObjectData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * The given shop object data is expected to contain the shop object's type identifier. Loading
	 * fails if the given data was originally meant for a different shop object type.
	 * <p>
	 * Any stored data elements (such as for example item stacks, etc.) and collections of data
	 * elements are assumed to not be modified, neither by the shop object, nor in contexts outside
	 * the shop object. If the shop object can guarantee not to modify these data elements, it is
	 * allowed to directly store them without copying them first.
	 * 
	 * @param shopObjectData
	 *            the shop object data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the data cannot be loaded
	 */
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		Validate.notNull(shopObjectData, "shopObjectData is null");
		ShopObjectType<?> shopObjectType = shopObjectData.get(SHOP_OBJECT_TYPE);
		assert shopObjectType != null;
		if (shopObjectType != this.getType()) {
			throw new InvalidDataException(
					"Shop object data is for a different shop object type (expected: "
							+ this.getType().getIdentifier() + ", got: "
							+ shopObjectType.getIdentifier() + ")!"
			);
		}

		lastSpawnFailedProperty.load(shopObjectData);
	}

	/**
	 * Saves the shop object's data to the given {@link ShopObjectData}.
	 * <p>
	 * It is assumed that the data stored in the given {@link ShopObjectData} does not change
	 * afterwards and can be serialized asynchronously. The shop object must therefore ensure that
	 * this data is not modified, for example by only inserting immutable data, or always making
	 * copies of the inserted data.
	 * <p>
	 * Some types of shop objects may rely on externally stored data and only save a reference to
	 * that external data as part of their shop object data. However, in some situations, such as
	 * when creating a {@link Shopkeeper#createSnapshot(String) shopkeeper snapshot}, it may be
	 * necessary to also save that external data as part of the shop object data in order to later
	 * be able to restore it. The {@code saveAll} parameter indicates whether the shop object should
	 * try to also save any external data.
	 * 
	 * @param shopObjectData
	 *            the shop object data, not <code>null</code>
	 * @param saveAll
	 *            <code>true</code> to also save any data that would usually be stored externally
	 */
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		Validate.notNull(shopObjectData, "shopObjectData is null");
		shopObjectData.set(SHOP_OBJECT_TYPE, this.getType());
		lastSpawnFailedProperty.save(shopObjectData);
	}

	/**
	 * This is called at the end of shopkeeper construction, when the shopkeeper has been fully
	 * loaded and setup, and can be used to perform any remaining initial shop object setup.
	 * <p>
	 * The shopkeeper has not yet been registered at this point! If the registration fails, or if
	 * the shopkeeper is created for some other purpose, the {@link #remove()} and {@link #delete()}
	 * methods may never get called for this shop object. For any setup that relies on cleanup
	 * during {@link #remove()} or {@link #delete()},
	 * {@link #onShopkeeperAdded(ShopkeeperAddedEvent.Cause)} may be better suited.
	 */
	public void setup() {
	}

	// ITEM UPDATES

	/**
	 * Calls an {@link UpdateItemEvent} and updates each item stored by this shop object in the
	 * given {@link ShopObjectData}, such as {@link LivingShopObject#getEquipment() equipment}
	 * items, etc. This modifies the given {@link ShopObjectData}.
	 * <p>
	 * This is usually called automatically as part of {@link Shopkeeper#updateItems()}.
	 * <p>
	 * Data that fails to load is logged but otherwise ignored.
	 * 
	 * @param logPrefix
	 *            a prefix for log messages, not <code>null</code>
	 * @param shopObjectData
	 *            the {@link ShopObjectData} to update, not <code>null</code>
	 * @return the number of updated items
	 */
	public int updateItems(String logPrefix, @ReadWrite ShopObjectData shopObjectData) {
		return 0;
	}
	// LIFE CYCLE

	/**
	 * This is called when the shopkeeper is added to the {@link ShopkeeperRegistry}.
	 * <p>
	 * Usually, the shopkeeper has not yet been spawned or activated at this point. However, if this
	 * this type of shop object handles it spawning {@link AbstractShopObjectType#mustBeSpawned()
	 * itself}, and the shop object is currently already {@link #isSpawned() spawned}, this may
	 * {@link #onIdChanged() register} the spawned shop object.
	 * 
	 * @param cause
	 *            the cause of the addition, not <code>null</code>
	 */
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
	}

	/**
	 * This is called when the {@link ShopObject} is removed, usually when the corresponding
	 * shopkeeper is removed from the {@link ShopkeeperRegistry}. The shopkeeper has already been
	 * marked as {@link Shopkeeper#isValid() invalid} at this point.
	 * <p>
	 * This can for example be used to disable any active components (e.g. listeners) for this shop
	 * object.
	 * <p>
	 * If this type of shop object handles its spawning
	 * {@link AbstractShopObjectType#mustBeSpawned() itself}, and the shop object is currently
	 * {@link #isSpawned() spawned}, the shop object needs to mark itself as despawned and
	 * {@link #onIdChanged() unregister} itself when this is called.
	 */
	public void remove() {
	}

	/**
	 * This is called when the {@link ShopObject} is permanently deleted.
	 * <p>
	 * This is called after {@link #remove()}.
	 * <p>
	 * This can for example be used to clean up any persistent data corresponding to this shop
	 * object.
	 */
	public void delete() {
	}

	// ATTACHED BLOCK FACE

	/**
	 * Sets the {@link BlockFace} against which the shop object is attached.
	 * <p>
	 * The block face is relative to the block the shopkeeper is attached to. E.g. a shulker
	 * attached to the south of a block has an "AttachFace" of "north".
	 * <p>
	 * Not all types of shop objects might use or store the attached block face.
	 * 
	 * @param attachedBlockFace
	 *            the block side block face, not <code>null</code>
	 */
	public void setAttachedBlockFace(BlockFace attachedBlockFace) {
		Validate.notNull(attachedBlockFace, "attachedBlockFace is null");
		Validate.isTrue(BlockFaceUtils.isBlockSide(attachedBlockFace),
				"attachedBlockFace is not a block side");
	}

	// SPAWNING

	/**
	 * Gets the object id by which the shopkeeper is currently registered inside the
	 * {@link ShopObjectRegistry}.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @return the object id, or <code>null</code>
	 */
	public final @Nullable Object getLastId() {
		return lastId;
	}

	/**
	 * Sets the object id by which the shopkeeper is currently registered inside the
	 * {@link ShopObjectRegistry}.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @param lastId
	 *            the object id, can be <code>null</code>
	 */
	public final void setLastId(@Nullable Object lastId) {
		this.lastId = lastId;
	}

	/**
	 * Gets an object that uniquely identifies this {@link ShopObject} while it is
	 * {@link #isSpawned() spawned}.
	 * <p>
	 * The id has to be unique among all currently spawned shop objects, including other types of
	 * shop objects. It has to be suitable to be used as key in {@link Object#hashCode() hash-based}
	 * data structures, and not change while the shop object is spawned. The id may change whenever
	 * the shop object is respawned.
	 * 
	 * @return the id of the shop object, or <code>null</code> if it is not spawned currently
	 */
	public abstract @Nullable Object getId();

	/**
	 * This has to be invoked whenever the {@link #getId() id} of this shop object might have
	 * changed, such as when this shop object has been {@link #spawn() spawned}, {@link #despawn()
	 * despawned}, or has changed its id for some other reason.
	 * <p>
	 * This updates the shop object id by which the shopkeeper is currently registered inside the
	 * {@link ShopkeeperRegistry}: If the shop object has been freshly spawned, this will register
	 * the current shop object id. If the shop object has been despawned, this will unregister any
	 * previously registered object id. If the shop object id has changed, this will both unregister
	 * any previous object id and then register the current object id.
	 */
	protected final void onIdChanged() {
		ShopObjectRegistry shopObjectRegistry = SKShopkeepersPlugin.getInstance()
				.getShopkeeperRegistry().getShopObjectRegistry();
		shopObjectRegistry.updateShopObjectRegistration(shopkeeper);
	}

	/**
	 * Checks if the spawning of this shop object is scheduled, for example by some external
	 * component.
	 * <p>
	 * The shop object should usually avoid spawning itself while its spawning is still scheduled.
	 * 
	 * @return <code>true</code> if a spawn of this shop object is scheduled
	 */
	protected final boolean isSpawningScheduled() {
		return shopkeeper.getComponents()
				.getOrAdd(ShopkeeperSpawnState.class)
				.isSpawningScheduled();
	}

	@Override
	public abstract boolean isActive();

	/**
	 * Spawns the shop object into the world at its spawn location.
	 * <p>
	 * This may have no effect if the shop object has already been spawned. To respawn this shop
	 * object if it is currently already spawned, one can use {@link #respawn()}.
	 * <p>
	 * This needs to call {@link #onSpawnSucceeded()} and {@link #onIdChanged()} if the shop object
	 * was successfully spawned, and {@link #onSpawnFailed()} if the spawning failed.
	 * 
	 * @return <code>false</code> if the spawning failed, or <code>true</code> if the shop object
	 *         either is already spawned or has successfully been spawned
	 */
	public abstract boolean spawn();

	/**
	 * Whether the last attempt of spawning this shop object failed.
	 * <p>
	 * This is for example used to inform admins about potential spawning issues or automatically
	 * cleanup shopkeepers that are no longer spawnable.
	 * 
	 * @return <code>true</code> if the shop object failed to spawn the last time {@link #spawn()}
	 *         was invoked
	 */
	public boolean isLastSpawnFailed() {
		return lastSpawnFailedProperty.getValue();
	}

	/**
	 * Invoke this during {@link #spawn()} if the spawning failed.
	 */
	protected final void onSpawnFailed() {
		lastSpawnFailedProperty.setValue(true);
	}

	/**
	 * Invoke this during {@link #spawn()} if the spawning succeeded.
	 */
	protected final void onSpawnSucceeded() {
		lastSpawnFailedProperty.setValue(false);
	}

	/**
	 * Removes this shop object from the world.
	 * <p>
	 * This has no effect if the shop object is not spawned currently.
	 * <p>
	 * This needs to call {@link #onIdChanged()} if the shop object was successfully despawned.
	 */
	public abstract void despawn();

	/**
	 * Respawns this shop object.
	 * <p>
	 * This is the same as calling both {@link #despawn()} and then {@link #spawn()}. However, this
	 * has no effect if the shop object is not {@link #isSpawned() spawned} currently.
	 * 
	 * @return <code>true</code> if the shop object was successfully respawned
	 */
	public final CompletableFuture<Boolean> respawn() {
		if (!this.isSpawned()) return CompletableFuture.completedFuture(false);
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		SchedulerUtils.runTaskOrOmit(this.getLocation(), () -> {
			this.despawn();
			future.complete(this.spawn());
		});
		return future;
	}

	@Override
	public abstract @Nullable Location getLocation();

	/**
	 * Teleports this shop object to its intended spawn location.
	 * <p>
	 * This can be used to move the shop object after the {@link Shopkeeper#getLocation() location}
	 * of its associated shopkeeper has changed. This behaves similar to {@link #respawn()}, but may
	 * be implemented more efficiently since it does not necessarily require the shop object to be
	 * respawned.
	 * <p>
	 * This method has no effect if the world of the shopkeeper's location is not loaded currently.
	 * <p>
	 * If this type of shop object handles its {@link AbstractShopObjectType#mustBeSpawned()
	 * spawning} itself, this may need to spawn the shop object (e.g. if its new location is in a
	 * loaded chunk). Otherwise, the shop object can ignore the call to this method if it is not
	 * {@link #isSpawned() spawned} currently.
	 * <p>
	 * Note: There is intentionally no method to teleport a shop object to a specific location,
	 * because the location of a shop object is meant to always match the location of its associated
	 * shopkeeper. If a shop object is able to change its location independently of the location of
	 * its shopkeeper, it is required to manually
	 * {@link AbstractShopkeeper#setLocation(Location, BlockFace) update} the location of the
	 * shopkeeper in order to keep it synchronized.
	 * 
	 * @return <code>true</code> if the shop object was successfully moved
	 */
	public abstract CompletableFuture<Boolean> move();

	// TICKING

	/**
	 * This is called when the shopkeeper starts ticking.
	 */
	public void onStartTicking() {
	}

	/**
	 * This is called when the shopkeeper stops ticking.
	 */
	public void onStopTicking() {
	}

	/**
	 * This is called at the beginning of a shopkeeper tick.
	 */
	public void onTickStart() {
		// Reset activity indicator:
		tickActivity = false;
	}

	/**
	 * This is called periodically (roughly once per second) for shopkeepers in active chunks.
	 * <p>
	 * This can for example be used to check if everything is still okay with the shop object, such
	 * as if it still exists and if it is still in its expected location and state. If any of these
	 * checks fail, the shop object may be respawned, teleported back into place, or otherwise
	 * brought back into its expected state.
	 * <p>
	 * However, note that shop objects may already be ticked while they are still
	 * {@link #isSpawningScheduled() scheduled} to be spawned. It is usually recommended to skip any
	 * shop object checks and spawning attempts until after the scheduled spawning attempt took
	 * place.
	 * <p>
	 * This is also called for shop objects that manage their spawning and despawning
	 * {@link AbstractShopObjectType#mustBeSpawned() manually}.
	 * <p>
	 * If the checks to perform are potentially costly performance-wise, or not required to happen
	 * every second, the shop object may decide to run them only every X invocations. For debugging
	 * purposes, the shop object can indicate tick activity by calling
	 * {@link #indicateTickActivity()} whenever it is doing actual work.
	 * <p>
	 * The ticking of shop objects in active chunks may be spread across multiple ticks and might
	 * therefore not happen for all shopkeepers within the same tick.
	 * <p>
	 * If the shopkeeper of this ticked shop object is marked as {@link AbstractShopkeeper#isDirty()
	 * dirty}, a {@link ShopkeeperStorage#saveDelayed() delayed save} will subsequently be
	 * triggered.
	 * <p>
	 * When overriding this method, consider calling the parent class version of this method.
	 */
	public void onTick() {
	}

	/**
	 * This is called at the end of a shopkeeper tick.
	 */
	public void onTickEnd() {
	}

	/**
	 * Subclasses can call this method to indicate activity during their last tick. When enabled,
	 * this can for example be used to visualize ticking activity, for example for debugging
	 * purposes.
	 */
	protected void indicateTickActivity() {
		tickActivity = true;
	}

	/**
	 * Gets the location at which particles for the shopkeeper's tick visualization shall be
	 * spawned.
	 * 
	 * @return the location, or possibly (but not necessarily) <code>null</code> if the shop object
	 *         is not spawned currently, or if it does not support the tick visualization
	 */
	public abstract @Nullable Location getTickVisualizationParticleLocation();

	/**
	 * Visualizes the shop object's activity during the last tick.
	 */
	public void visualizeLastTick() {
		// Default visualization:
		if (!tickActivity) return;

		Location particleLocation = this.getTickVisualizationParticleLocation();
		if (particleLocation == null) return;

		World world = LocationUtils.getWorld(particleLocation);
		world.spawnParticle(Particle.ANGRY_VILLAGER, particleLocation, 1);
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return AbstractShopkeeper.MAX_NAME_LENGTH;
	}

	@Override
	public @Nullable String prepareName(@Nullable String name) {
		if (name == null) return null;
		String prepared = name;
		// Trim to max name length:
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) {
			prepared = name.substring(0, lengthLimit);
		}
		return prepared;
	}

	@Override
	public abstract void setName(@Nullable String name);

	@Override
	public abstract @Nullable String getName();

	// PLAYER SHOP OWNER

	/**
	 * This is called by {@link PlayerShopkeeper}s when their owner has changed.
	 */
	public void onShopOwnerChanged() {
	}

	// EDITOR ACTIONS

	/**
	 * Creates the editor buttons for editing this shop object.
	 * <p>
	 * This is usually only invoked once, when the {@link ShopkeeperEditorLayout} is set up for the
	 * shopkeeper. So it is not possible to dynamically add or remove buttons with this method.
	 * <p>
	 * In order to allow for subtypes to more easily add or modify the returned editor buttons, this
	 * method is expected to return a new modifiable list with each invocation.
	 * 
	 * @return the editor buttons
	 */
	public List<Button> createEditorButtons() {
		return new ArrayList<>(); // None by default, modifiable by subtypes
	}
}
