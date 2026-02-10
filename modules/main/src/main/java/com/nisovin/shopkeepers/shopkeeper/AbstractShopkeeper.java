package com.nisovin.shopkeepers.shopkeeper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperLoadException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObjectType;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.ticking.ShopkeeperTicker;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.ViewProvider;
import com.nisovin.shopkeepers.ui.trading.TradingViewProvider;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.ColorUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.DataKeyAccessor;
import com.nisovin.shopkeepers.util.data.property.EmptyDataPredicates;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.IntegerValidators;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.serialization.DataAccessor;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.ColoredStringSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.UUIDSerializers;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * Abstract base class for all shopkeeper implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to follow the initialization instructions outlined in the constructor description.
 * <li>Make sure to call {@link #markDirty()} on every change of data that might need to be
 * persisted.
 * </ul>
 */
public abstract class AbstractShopkeeper implements Shopkeeper {

	private static final String VIRTUAL_SHOPKEEPER_MARKER = "[virtual]";

	// The maximum supported name length:
	// The actual maximum name length that can be used might be lower depending on config settings
	// and on shop object specific limits.
	public static final int MAX_NAME_LENGTH = 256;

	/**
	 * We log a warning when a shopkeeper has more than this amount of snapshots.
	 */
	private static final int SNAPSHOTS_WARNING_LIMIT = 10;

	// Shopkeeper tick visualization:
	// Particles of different colors indicate the different ticking groups.
	// Note: The client seems to randomly change the color slightly each time a dust particle is
	// spawned.
	// Note: The particle size also determines the effect duration.
	private static final DustOptions[] TICK_VISUALIZATION_DUSTS = new DustOptions[ShopkeeperTicker.TICKING_GROUPS];
	static {
		// Even distribution of colors in the HSB color space: Ensures a distinct color for each
		// ticking group.
		float hueStep = (1.0F / ShopkeeperTicker.TICKING_GROUPS);
		for (int i = 0; i < ShopkeeperTicker.TICKING_GROUPS; ++i) {
			float hue = i * hueStep; // Starts with red
			int rgb = ColorUtils.HSBtoRGB(hue, 1.0F, 1.0F);
			Color color = Color.fromRGB(rgb);
			TICK_VISUALIZATION_DUSTS[i] = new DustOptions(color, 1.0F);
		}
	}

	/**
	 * Gets a short prefix that can be used for log messages related to the shopkeeper with the
	 * given id.
	 * 
	 * @param shopkeeperId
	 *            the shopkeeper id
	 * @return the log prefix, not <code>null</code>
	 */
	public static String getLogPrefix(int shopkeeperId) {
		return "Shopkeeper " + shopkeeperId + ": ";
	}

	private boolean initialized = false;

	private int id; // Valid and constant after initialization
	private UUID uniqueId = Unsafe.uncheckedNull(); // Not null and constant after initialization
	private AbstractShopObject shopObject = Unsafe.uncheckedNull(); // Not null after initialization
	// TODO Move location information into ShopObject?
	// Immutable instances, null for virtual shops, always has a world name:
	private @Nullable BlockLocation location;
	private float yaw;

	private @Nullable ChunkCoords chunkCoords; // Null for virtual shops
	// The ChunkCoords by which the shopkeeper is currently stored:
	private @Nullable ChunkCoords lastChunkCoords = null;
	private boolean open = true;
	private String name = ""; // Not null, can be empty

	private final List<SKShopkeeperSnapshot> snapshots = new ArrayList<>();
	private final List<? extends SKShopkeeperSnapshot> snapshotsView = Collections.unmodifiableList(snapshots);

	private final ShopkeeperComponentHolder components = new ShopkeeperComponentHolder(Unsafe.initialized(this));

	// Map of dynamically evaluated message arguments:
	private final Map<String, Supplier<@NonNull ?>> messageArgumentsMap = new HashMap<>();
	private final MessageArguments messageArguments = MessageArguments.ofMap(messageArgumentsMap);

	// Whether there have been changes to the shopkeeper's data that the storage is not yet aware
	// of. A value of 'false' only indicates that the storage is aware of the latest data of the
	// shopkeeper, not that it has actually persisted the data to disk yet.
	private boolean dirty = false;
	// Is currently registered:
	private boolean valid = false;
	private boolean active = false;
	private boolean ticking = false;

	// UI type identifier -> ViewProvider
	private final Map<String, ViewProvider> viewProviders = new HashMap<>();

	// Internally used for load balancing purposes:
	private final int tickingGroup = ShopkeeperTicker.nextTickingGroup();

	// CONSTRUCTION AND SETUP

	/**
	 * Creates a shopkeeper.
	 * <p>
	 * Important: Depending on whether the shopkeeper gets freshly created or loaded, either
	 * {@link #initOnCreation(int, ShopCreationData)} or {@link #initOnLoad(ShopkeeperData)} needs
	 * to be called to complete the initialization.
	 */
	protected AbstractShopkeeper() {
	}

	/**
	 * Initializes this shopkeeper based on the given {@link ShopCreationData}.
	 * <p>
	 * This operation will mark the shopkeeper as {@link #markDirty() dirty}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if the initialization fails (e.g. due to invalid or missing data)
	 * @see AbstractShopType#createShopkeeper(int, ShopCreationData)
	 * @see #loadFromCreationData(int, ShopCreationData)
	 */
	final void initOnCreation(
			int id,
			ShopCreationData shopCreationData
	) throws ShopkeeperCreateException {
		this.loadFromCreationData(id, shopCreationData);
		this.commonSetup();
	}

	/**
	 * Initializes this shopkeeper by loading its previously saved state from the given
	 * {@link ShopkeeperData}.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the shopkeeper cannot be loaded
	 * @see AbstractShopType#loadShopkeeper(ShopkeeperData)
	 * @see #loadFromSaveData(ShopkeeperData)
	 */
	final void initOnLoad(ShopkeeperData shopkeeperData) throws InvalidDataException {
		this.loadFromSaveData(shopkeeperData);
		this.commonSetup();
	}

	private void initialize() {
		Validate.State.isTrue(!initialized, "The shopkeeper has already been initialized!");
		initialized = true;
	}

	/**
	 * Checks if this shopkeeper has already been initialized.
	 * 
	 * @return <code>true</code> if this shopkeeper has already been initialized
	 */
	final boolean isInitialized() {
		return initialized;
	}

	private void commonSetup() {
		this.setup();
		this.postSetup();
	}

	/**
	 * Initializes this shopkeeper based on the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly initialized
	 * @see AbstractShopType#createShopkeeper(int, ShopCreationData)
	 */
	protected void loadFromCreationData(int id, ShopCreationData shopCreationData)
			throws ShopkeeperCreateException {
		this.getType().validateCreationData(shopCreationData);
		this.initialize();

		this.id = id;
		this.uniqueId = UUID.randomUUID();

		if (shopCreationData.getShopType() != this.getType()) {
			throw new ShopkeeperCreateException(
					"The shopCreationData is for a different shop type (expected: "
							+ this.getType().getIdentifier() + ", got: "
							+ shopCreationData.getShopType().getIdentifier() + ")!"
			);
		}

		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();
		Validate.isTrue(shopObjectType instanceof AbstractShopObjectType,
				"ShopObjectType of shopCreationData is not of type AbstractShopObjectType, but: "
						+ shopObjectType.getClass().getName());

		if (shopObjectType instanceof VirtualShopObjectType) {
			// Virtual shops ignore any potentially available spawn location:
			this.location = null;
			this.yaw = 0.0F;
		} else {
			Location spawnLocation = Unsafe.assertNonNull(shopCreationData.getSpawnLocation());
			assert spawnLocation.getWorld() != null;
			this.location = BlockLocation.of(spawnLocation);
			assert this.location.hasWorldName();
			this.yaw = spawnLocation.getYaw();
		}
		this.updateChunkCoords();

		this.shopObject = this.createShopObject(
				(AbstractShopObjectType<?>) shopObjectType,
				shopCreationData
		);

		// Automatically mark new shopkeepers as dirty:
		this.markDirty();
	}

	/**
	 * This is called at the end of construction, after the shopkeeper data has been loaded, and can
	 * be used to perform any remaining setup.
	 * <p>
	 * This might set up defaults for some things, if not yet specified by the sub-classes. So if
	 * you are overriding this method, consider doing your own setup before calling the overridden
	 * method. And also take into account that further sub-classes might perform their setup prior
	 * to calling your setup method as well. So don't replace any components that have already been
	 * set up by further sub-classes.
	 * <p>
	 * The shopkeeper has not yet been registered at this point! If the registration fails, or if
	 * the shopkeeper is created for some other purpose, the
	 * {@link #onRemoval(ShopkeeperRemoveEvent.Cause)} and {@link #onDeletion()} methods may never
	 * get called for this shopkeeper. For any setup that relies on cleanup during
	 * {@link #onRemoval(ShopkeeperRemoveEvent.Cause)} or {@link #onDeletion()},
	 * {@link #onAdded(ShopkeeperAddedEvent.Cause)} may be better suited.
	 */
	protected void setup() {
		// Default trading view provider:
		this.registerViewProviderIfMissing(DefaultUITypes.TRADING(), () -> {
			return new TradingViewProvider(this);
		});
	}

	/**
	 * This is called after {@link #setup()} and can be used to perform any setup that needs to
	 * happen last.
	 */
	protected void postSetup() {
		// Inform shop object:
		this.getShopObject().setup();
	}

	// STORAGE

	/**
	 * Initializes this shopkeeper by loading its previously saved state from the given
	 * {@link ShopkeeperData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * This also loads the shopkeeper's {@link #loadDynamicState(ShopkeeperData) dynamic state}.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the shopkeeper data cannot be loaded
	 * @see AbstractShopType#loadShopkeeper(ShopkeeperData)
	 */
	protected void loadFromSaveData(ShopkeeperData shopkeeperData) throws InvalidDataException {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		this.initialize();

		this.id = shopkeeperData.get(ID);
		this.uniqueId = shopkeeperData.get(UNIQUE_ID);

		AbstractShopType<?> shopType = this.getAndValidateShopType(shopkeeperData);
		assert shopType != null;

		// Shop object data:
		ShopObjectData shopObjectData = shopkeeperData.get(SHOP_OBJECT_DATA);
		assert shopObjectData != null;

		// Determine the shop object type:
		AbstractShopObjectType<?> objectType = shopObjectData.get(AbstractShopObject.SHOP_OBJECT_TYPE);
		assert objectType != null;

		// Not null, even if the world name is null:
		// Gets set to null afterwards if this shopkeeper is virtual.
		BlockLocation location = shopkeeperData.get(LOCATION);
		assert location != null;
		this.location = location;
		this.yaw = shopkeeperData.get(YAW);

		if (objectType instanceof VirtualShopObjectType) {
			if (!location.isEmpty() || yaw != 0.0F) {
				// TODO Allow virtual shopkeeper to store a location? This could later enable us to
				// allow users to dynamically change the shop object type, including from virtual to
				// non-virtual.
				throw new InvalidDataException(
						"Shopkeeper is virtual, but stores a non-empty location: "
								+ TextUtils.getLocationString(location, yaw)
				);
			}
			this.location = null;
		} else {
			if (!location.hasWorldName()) {
				throw new InvalidDataException("Missing world name!");
			}
		}
		this.updateChunkCoords();

		// Create the shop object:
		this.shopObject = this.createShopObject(objectType, null);

		this.loadSnapshots(shopkeeperData);

		// Load the dynamic shopkeeper and shop object state:
		this.loadDynamicState(shopkeeperData);
	}

	private AbstractShopType<?> getAndValidateShopType(ShopkeeperData shopkeeperData)
			throws InvalidDataException {
		assert shopkeeperData != null;
		AbstractShopType<?> shopType = shopkeeperData.get(SHOP_TYPE);
		assert shopType != null;
		if (shopType != this.getType()) {
			throw new InvalidDataException(
					"The shopkeeper data is for a different shop type (expected: "
							+ this.getType().getIdentifier() + ", got: "
							+ shopType.getIdentifier() + ")!"
			);
		}
		return shopType;
	}

	// shopCreationData can be null if the shopkeeper is getting loaded.
	private AbstractShopObject createShopObject(
			AbstractShopObjectType<?> objectType,
			@Nullable ShopCreationData shopCreationData
	) {
		assert objectType != null;
		AbstractShopObject shopObject = objectType.createObject(this, shopCreationData);
		Validate.State.notNull(shopObject, this.getLogPrefix() + "Shop object type '"
				+ objectType.getIdentifier() + "' created null shop object!");
		return shopObject;
	}

	/**
	 * Loads the shopkeeper's dynamic state from the given {@link ShopkeeperData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * The given shopkeeper data is expected to contain the shopkeeper's shop type identifier. If
	 * the given data was originally meant for a different shop type, loading fails. Any other
	 * non-dynamic shopkeeper data that the given {@link ShopkeeperData} may contain is silently
	 * ignored. If the given shopkeeper data contains data for a shop object of a different type,
	 * the given object data is silently ignored as well.
	 * <p>
	 * This operation does not modify the given {@link ShopkeeperData}. Any stored data elements
	 * (such as for example item stacks, etc.) and collections of data elements are assumed to not
	 * be modified, neither by the shopkeeper, nor in contexts outside the shopkeeper. If the
	 * shopkeeper can guarantee not to modify these data elements, it is allowed to directly store
	 * them without copying them first.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the data cannot be loaded
	 * @see #saveDynamicState(ShopkeeperData, boolean)
	 */
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		ShopType<?> shopType = this.getAndValidateShopType(shopkeeperData);
		assert shopType != null;

		this._setOpen(shopkeeperData.get(OPEN));
		this._setName(shopkeeperData.get(NAME));

		// Optional shop object data:
		ShopObjectData shopObjectData = shopkeeperData.getOrNullIfMissing(SHOP_OBJECT_DATA);
		if (shopObjectData != null) {
			AbstractShopObjectType<?> objectType = shopObjectData.get(AbstractShopObject.SHOP_OBJECT_TYPE);
			assert objectType != null;
			if (objectType == shopObject.getType()) {
				shopObject.load(shopObjectData);
			} else {
				// Skipping the incompatible shop object data.
				Log.debug(() -> this.getLogPrefix()
						+ "Ignoring shop object data of different type (expected: "
						+ shopObject.getType().getIdentifier() + ", got: "
						+ objectType.getIdentifier() + ")!");
			}
		}
	}

	/**
	 * Saves the shopkeeper's state to the given {@link ShopkeeperData}.
	 * <p>
	 * This also includes the shopkeeper's {@link #saveDynamicState(ShopkeeperData, boolean) dynamic
	 * state}.
	 * <p>
	 * Some types of shopkeepers or shop objects may rely on externally stored data and only save a
	 * reference to that external data as part of their shopkeeper data. However, in some
	 * situations, such as when creating a {@link #createSnapshot(String) shopkeeper snapshot}, it
	 * may be necessary to also save that external data as part of the shopkeeper data in order to
	 * later be able to restore it. The {@code saveAll} parameter indicates whether the shopkeeper
	 * should try to also save any external data.
	 * <p>
	 * It is assumed that the data stored in the given {@link ShopkeeperData} does not change
	 * afterwards and can be serialized asynchronously. The shopkeeper must therefore ensure that
	 * this data is not modified, for example by only inserting immutable data, or always making
	 * copies of the inserted data.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @param saveAll
	 *            <code>true</code> to also save any data that would usually be stored externally
	 */
	public void save(ShopkeeperData shopkeeperData, boolean saveAll) {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		shopkeeperData.set(ID, id);
		shopkeeperData.set(UNIQUE_ID, uniqueId);

		if (!this.isVirtual()) {
			shopkeeperData.set(LOCATION, location);
			shopkeeperData.set(YAW, yaw);
		}

		// Dynamic shopkeeper and shop object data:
		this.saveDynamicState(shopkeeperData, saveAll);

		// Snapshots:
		this.saveSnapshots(shopkeeperData);
	}

	/**
	 * Saves the shopkeeper's dynamic state to the given {@link ShopkeeperData}.
	 * <p>
	 * The dynamic state comprises at least every portion of state that the shop owner, an admin, an
	 * API user, or the shopkeeper itself might dynamically change at runtime. State that is
	 * currently part of the non-dynamic portion, such as the shopkeeper's type, location, or object
	 * type, might be moved to the dynamic portion in the future.
	 * <p>
	 * Some types of shopkeepers or shop objects may rely on externally stored data and only save a
	 * reference to that external data as part of their shopkeeper data. However, in some
	 * situations, such as when creating a {@link #createSnapshot(String) shopkeeper snapshot}, it
	 * may be necessary to also save that external data as part of the shopkeeper data in order to
	 * later be able to restore it. The {@code saveAll} parameter indicates whether the shopkeeper
	 * should try to also save any external data.
	 * <p>
	 * The saved dynamic state can be loaded again via {@link #loadDynamicState(ShopkeeperData)}.
	 * <p>
	 * It is assumed that the data stored in the given {@link ShopkeeperData} does not change
	 * afterwards and can be serialized asynchronously. The shopkeeper must therefore ensure that
	 * this data is not modified, for example by only inserting immutable data, or always making
	 * copies of the inserted data.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @param saveAll
	 *            <code>true</code> to also save any data that would usually be stored externally
	 */
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		shopkeeperData.set(SHOP_TYPE, this.getType());
		shopkeeperData.set(OPEN, open);
		shopkeeperData.set(NAME, name);

		// Shop object:
		ShopObjectData shopObjectData = ShopObjectData.ofNonNull(DataContainer.create());
		shopObject.save(shopObjectData, saveAll);
		shopkeeperData.set(SHOP_OBJECT_DATA, shopObjectData);
	}

	@Override
	public final void save() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}

	@Override
	public final void saveDelayed() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().saveDelayed();
	}

	/**
	 * Marks this shopkeeper as 'dirty'.
	 * <p>
	 * This indicates that there have been changes to the shopkeeper's data that the storage is not
	 * yet aware of. The shopkeeper and shop object implementations have to invoke this on every
	 * change of data that needs to be persisted.
	 * <p>
	 * If this shopkeeper is currently {@link #isValid() loaded}, or about to be loaded, its data is
	 * saved with the next successful save of the {@link ShopkeeperStorage}. If the shopkeeper has
	 * already been deleted or unloaded, invoking this method will have no effect on the data that
	 * is stored by the storage.
	 */
	public final void markDirty() {
		dirty = true;
		// Inform the storage that the shopkeeper is dirty:
		if (this.isValid()) {
			// If the shopkeeper is marked as dirty during creation or loading (while it is not yet
			// valid), the storage is informed once the shopkeeper becomes valid.
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().markDirty(this);
		}
	}

	/**
	 * Checks whether this shopkeeper had changes to its data that the storage is not yet aware of.
	 * <p>
	 * A return value of {@code false} indicates that the {@link ShopkeeperStorage} is aware of the
	 * shopkeeper's latest data, but not necessarily that this data has already been successfully
	 * persisted to disk.
	 * 
	 * @return <code>true</code> if there are data changes that the storage is not yet aware of
	 */
	public final boolean isDirty() {
		return dirty;
	}

	// Called by shopkeeper storage when it has retrieved the shopkeeper's latest data for the next
	// save. The data might not yet have been persisted at that point.
	// This may not be called if the shopkeeper was deleted.
	public final void onSave() {
		dirty = false;
	}

	// ITEM UPDATES

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note for sub-types: This method cannot be overridden. Override
	 * {@link #updateItems(String, ShopkeeperData)} instead.
	 */
	@Override
	public final int updateItems() {
		this.abortUISessionsDelayed();

		// We also need to update the item data inside snapshots. To only implement the item update
		// logic once, we save the current shopkeeper state, perform the item updates on the saved
		// state, and then re-apply the updated state.
		ShopkeeperData dynamicShopkeeperData = ShopkeeperData.ofNonNull(DataContainer.create());
		this.saveDynamicState(dynamicShopkeeperData, false); // saveAll: Not needed here.

		int updatedItems = this.updateItems(this.getLogPrefix(), dynamicShopkeeperData);
		if (updatedItems > 0) {
			try {
				this.loadDynamicState(dynamicShopkeeperData);
			} catch (InvalidDataException e) {
				Log.warning(this.getLogPrefix() + "Failed re-apply the updated shopkeeper data!", e);
				return 0;
			}
		}

		int snapshotId = 0;
		var snapshotIterator = snapshots.listIterator();
		while (snapshotIterator.hasNext()) {
			var snapshot = Unsafe.assertNonNull(snapshotIterator.next());
			snapshotId++;
			String snapshotLogPrefix = this.getLogPrefix(snapshotId, snapshot);

			// Copy to avoid modifying the original snapshot data:
			var updatedSnaphotShopkeeperData = ShopkeeperData.ofNonNull(DataContainer.ofNonNull(
					snapshot.getShopkeeperData().getValuesCopy()
			));

			int snapshotUpdatedItems = this.updateItems(snapshotLogPrefix, updatedSnaphotShopkeeperData);
			if (snapshotUpdatedItems > 0) {
				// Replace the snapshot with a snapshot with the new item data:
				var updatedSnapshot = new SKShopkeeperSnapshot(
						snapshot.getName(),
						snapshot.getTimestamp(),
						updatedSnaphotShopkeeperData
				);
				snapshotIterator.set(updatedSnapshot);
				updatedItems += snapshotUpdatedItems;
			}
		}

		if (updatedItems > 0) {
			this.markDirty();
		}

		return updatedItems;
	}

	/**
	 * Calls an {@link UpdateItemEvent} and updates each item stored by this shopkeeper in the given
	 * {@link ShopkeeperData}, such as trade offer items, hire cost items, items stored by the shop
	 * object, etc. This modifies the given {@link ShopkeeperData}.
	 * <p>
	 * Data that fails to load is logged but otherwise ignored.
	 * 
	 * @param logPrefix
	 *            a prefix for log messages, not <code>null</code>
	 * @param shopkeeperData
	 *            the {@link ShopkeeperData} to update, not <code>null</code>
	 * @return the number of updated items
	 */
	protected int updateItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		int updatedItems = 0;
		updatedItems += this.updateShopObjectItems(logPrefix, shopkeeperData);
		return updatedItems;
	}

	private int updateShopObjectItems(String logPrefix, @ReadWrite ShopkeeperData shopkeeperData) {
		ShopObjectData shopObjectData;
		try {
			shopObjectData = shopkeeperData.getOrNullIfMissing(SHOP_OBJECT_DATA);
		} catch (InvalidDataException e) {
			Log.warning(logPrefix + "Failed to load '" + SHOP_OBJECT_DATA.getName() + "'!", e);
			return 0;
		}
		if (shopObjectData == null) return 0;

		AbstractShopObjectType<?> objectType;
		try {
			objectType = shopObjectData.get(AbstractShopObject.SHOP_OBJECT_TYPE);
			assert objectType != null;
		} catch (InvalidDataException e) {
			Log.warning(logPrefix + "Failed to load '"
					+ AbstractShopObject.SHOP_OBJECT_TYPE.getName() + "'!", e);
			return 0;
		}

		// Skip incompatible shop object data:
		if (objectType != shopObject.getType()) {
			Log.debug(() -> this.getLogPrefix()
					+ "Ignoring shop object data of different type (expected: "
					+ shopObject.getType().getIdentifier() + ", got: "
					+ objectType.getIdentifier() + ")!");
			return 0;
		}

		// A modifiable copy of the shop object data:
		var updatedShopObjectData = ShopObjectData.ofNonNull(DataContainer.ofNonNull(
				shopObjectData.getValuesCopy()
		));

		int updatedItems = shopObject.updateItems(logPrefix, updatedShopObjectData);
		if (updatedItems > 0) {
			shopkeeperData.set(SHOP_OBJECT_DATA, updatedShopObjectData);
		}

		return updatedItems;
	}

	// COMPONENTS

	/**
	 * Gets the {@link ShopkeeperComponentHolder} the holds the components of this shopkeeper.
	 * 
	 * @return the component holder, not <code>null</code>
	 */
	public final ShopkeeperComponentHolder getComponents() {
		return components;
	}

	// LIFE CYCLE

	@Override
	public final boolean isValid() {
		return valid;
	}

	public final void informAdded(ShopkeeperAddedEvent.Cause cause) {
		assert !valid;
		valid = true;

		// If the shopkeeper has been marked as dirty earlier (e.g. due to data migrations during
		// loading, or when being newly created), we inform the storage here:
		if (this.isDirty()) {
			this.markDirty();
		}

		// Custom processing done by sub-classes:
		this.onAdded(cause);
	}

	/**
	 * This is called when the shopkeeper is added to the {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has not yet been spawned or activated at this point.
	 * 
	 * @param cause
	 *            the cause of the addition, not <code>null</code>
	 */
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		shopObject.onShopkeeperAdded(cause);
	}

	public final void informRemoval(ShopkeeperRemoveEvent.Cause cause) {
		assert valid;
		valid = false;

		this.onRemoval(cause);
		if (cause == ShopkeeperRemoveEvent.Cause.DELETE) {
			this.onDeletion();
		}
	}

	/**
	 * This is called when the shopkeeper is about to be removed from the
	 * {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has already been deactivated and marked as {@link #isValid() invalid} at this
	 * point, i.e. its ticking has been stopped and, if the shop object's spawning is
	 * {@link AbstractShopObjectType#mustBeSpawned() managed} by the Shopkeepers plugin, it has
	 * already been despawned. If the shop object handles its spawning itself, it may still be
	 * spawned and is then responsible to {@link AbstractShopObject#remove() unregister} itself
	 * during this call.
	 * 
	 * @param cause
	 *            the cause of the removal, not <code>null</code>
	 */
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		shopObject.remove();
	}

	@Override
	public final void delete() {
		this.delete(null);
	}

	// TODO Make this final and provide the involved player to the onDeletion method somehow.
	@Override
	public void delete(@Nullable Player player) {
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().deleteShopkeeper(this);
	}

	/**
	 * This is called when the shopkeeper is about to be permanently deleted.
	 * <p>
	 * This is called after {@link #onRemoval(ShopkeeperRemoveEvent.Cause)}.
	 */
	protected void onDeletion() {
		shopObject.delete();
	}

	/**
	 * Sets the shopkeeper's activation state.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @param active
	 *            the new activation state
	 */
	public final void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Gets the shopkeeper's current activation state.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @return <code>true</code> if the shopkeeper is currently active
	 */
	public final boolean isActive() {
		return active;
	}

	// SHOP TYPE

	private static final String DATA_KEY_SHOP_TYPE = "type";

	/**
	 * Shop type id.
	 */
	public static final Property<String> SHOP_TYPE_ID = new BasicProperty<String>()
			.name("shop-type-id")
			.dataKeyAccessor(DATA_KEY_SHOP_TYPE, StringSerializers.STRICT)
			.validator(StringValidators.NON_EMPTY)
			.build();

	/**
	 * Shop type, derived from the serialized {@link #SHOP_TYPE_ID shop type id}.
	 */
	public static final Property<AbstractShopType<?>> SHOP_TYPE = new BasicProperty<AbstractShopType<?>>()
			.dataKeyAccessor(DATA_KEY_SHOP_TYPE, new DataSerializer<AbstractShopType<?>>() {
				@Override
				public @Nullable Object serialize(AbstractShopType<?> value) {
					Validate.notNull(value, "value is null");
					return value.getIdentifier();
				}

				@Override
				public AbstractShopType<?> deserialize(Object data) throws InvalidDataException {
					String shopTypeId = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
					SKShopTypesRegistry shopTypeRegistry = SKShopkeepersPlugin.getInstance().getShopTypeRegistry();
					AbstractShopType<?> shopType = shopTypeRegistry.get(shopTypeId);
					if (shopType == null) {
						throw new InvalidDataException("Unknown shop type: " + shopTypeId);
					}
					return shopType;
				}
			})
			.build();

	@Override
	public abstract AbstractShopType<?> getType();

	// ATTRIBUTES

	public static final Property<Integer> ID = new BasicProperty<Integer>()
			.dataKeyAccessor("id", NumberSerializers.INTEGER)
			.validator(IntegerValidators.POSITIVE)
			.build();
	public static final Property<UUID> UNIQUE_ID = new BasicProperty<UUID>()
			.dataKeyAccessor("uniqueId", UUIDSerializers.LENIENT)
			.build();
	public static final Property<@Nullable String> WORLD_NAME = new BasicProperty<@Nullable String>()
			.dataAccessor(new DataKeyAccessor<>("world", StringSerializers.SCALAR)
					.emptyDataPredicate(EmptyDataPredicates.EMPTY_STRING)
			)
			.nullable() // For virtual shopkeepers
			.build();
	public static final Property<Integer> LOCATION_X = new BasicProperty<Integer>()
			.dataKeyAccessor("x", NumberSerializers.INTEGER)
			.useDefaultIfMissing() // For virtual shopkeepers
			.defaultValue(0)
			.build();
	public static final Property<Integer> LOCATION_Y = new BasicProperty<Integer>()
			.dataKeyAccessor("y", NumberSerializers.INTEGER)
			.useDefaultIfMissing() // For virtual shopkeepers
			.defaultValue(0)
			.build();
	public static final Property<Integer> LOCATION_Z = new BasicProperty<Integer>()
			.dataKeyAccessor("z", NumberSerializers.INTEGER)
			.useDefaultIfMissing() // For virtual shopkeepers
			.defaultValue(0)
			.build();
	public static final Property<Float> YAW = new BasicProperty<Float>()
			.dataKeyAccessor("yaw", NumberSerializers.FLOAT)
			// For virtual shopkeepers, and if missing (e.g. in pre 2.13.4 versions):
			.useDefaultIfMissing()
			.defaultValue(0.0F) // South
			.build();
	// This always loads a non-null location, even for virtual shopkeepers.
	// This ensures that we can inspect and validate the loaded coordinates even if the world name
	// is null.
	public static final Property<BlockLocation> LOCATION = new BasicProperty<BlockLocation>()
			.name("location")
			.dataAccessor(new DataAccessor<BlockLocation>() {
				@Override
				public void save(DataContainer dataContainer, @Nullable BlockLocation value) {
					if (value != null) {
						dataContainer.set(WORLD_NAME, value.getWorldName());
						dataContainer.set(LOCATION_X, value.getX());
						dataContainer.set(LOCATION_Y, value.getY());
						dataContainer.set(LOCATION_Z, value.getZ());
					} else {
						dataContainer.set(WORLD_NAME, null);
						dataContainer.set(LOCATION_X, null);
						dataContainer.set(LOCATION_Y, null);
						dataContainer.set(LOCATION_Z, null);
					}
				}

				@Override
				public BlockLocation load(DataContainer dataContainer) throws InvalidDataException {
					String worldName = dataContainer.get(WORLD_NAME); // Can be null
					int x = dataContainer.get(LOCATION_X);
					int y = dataContainer.get(LOCATION_Y);
					int z = dataContainer.get(LOCATION_Z);
					return new BlockLocation(worldName, x, y, z);
				}
			})
			.build();

	@Override
	public final int getId() {
		return id;
	}

	@Override
	public final UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public final String getIdString() {
		return id + " (" + uniqueId.toString() + ")";
	}

	@Override
	public final String getLogPrefix() {
		return getLogPrefix(id);
	}

	@Override
	public final String getUniqueIdLogPrefix() {
		return "Shopkeeper " + this.getIdString() + ": ";
	}

	@Override
	public final String getLocatedLogPrefix() {
		if (this.isVirtual()) {
			return "Shopkeeper " + id + " " + VIRTUAL_SHOPKEEPER_MARKER + ": ";
		} else {
			return "Shopkeeper " + id + " at " + this.getPositionString() + ": ";
		}
	}

	@Override
	public final String getDisplayName() {
		var name = this.getName(); // Can be empty
		if (!name.isEmpty()) {
			return name;
		}

		return String.valueOf(this.getId());
	}

	@Override
	public final boolean isVirtual() {
		assert (location != null && location.hasWorldName())
				^ (location == null && shopObject instanceof VirtualShopObject);
		return (location == null);
	}

	@Override
	public final @Nullable String getWorldName() {
		return (location != null) ? location.getWorldName() : null;
	}

	@Override
	public final int getX() {
		return (location != null) ? location.getX() : 0;
	}

	@Override
	public final int getY() {
		return (location != null) ? location.getY() : 0;
	}

	@Override
	public final int getZ() {
		return (location != null) ? location.getZ() : 0;
	}

	@Override
	public final String getPositionString() {
		if (this.isVirtual()) return VIRTUAL_SHOPKEEPER_MARKER;
		return TextUtils.getLocationString(Unsafe.assertNonNull(location));
	}

	@Override
	public final @Nullable Location getLocation() {
		if (this.isVirtual()) return null;
		BlockLocation location = Unsafe.assertNonNull(this.location);
		assert location != null && location.hasWorldName();
		World world = location.getWorld();
		if (world == null) return null;
		return new Location(world, location.getX(), location.getY(), location.getZ(), yaw, 0.0F);
	}

	/**
	 * Gets the shopkeeper's location.
	 * <p>
	 * Unlike {@link #getLocation()}, this returns a location even if the shopkeeper's world is not
	 * loaded currently.
	 * 
	 * @return the shopkeeper's block location, or <code>null</code> if the shopkeeper is virtual
	 */
	public final @Nullable BlockLocation getBlockLocation() {
		return location;
	}

	/**
	 * Sets the stored location and yaw of this shopkeeper.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper, not <code>null</code>
	 * @see #setLocation(Location, BlockFace)
	 */
	public final void setLocation(Location location) {
		this.setLocation(location, null);
	}

	/**
	 * Sets the stored location, yaw, and attached block face of this shopkeeper.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper, not <code>null</code>
	 * @param attachedBlockFace
	 *            The block face against which the shopkeeper is attached, or <code>null</code> to
	 *            not update the block face. This might not be used or stored by the shopkeeper
	 *            itself, but may be used by the shop object.
	 * @see #setLocation(BlockLocation)
	 * @see #setYaw(float)
	 */
	public final void setLocation(Location location, @Nullable BlockFace attachedBlockFace) {
		// This validates the given location, and throws an exception if the location's world is no
		// longer loaded:
		this.setLocation(BlockLocation.of(location));
		assert location != null;
		this.setYaw(location.getYaw());
		if (attachedBlockFace != null) {
			// This validates the given block face:
			shopObject.setAttachedBlockFace(attachedBlockFace);
		}
	}

	/**
	 * Sets the stored location of this shopkeeper.
	 * <p>
	 * This will not actually move the shop object on its own, until the next time it is spawned or
	 * teleported to its new location.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper, not <code>null</code>
	 */
	public final void setLocation(BlockLocation location) {
		Validate.State.isTrue(!this.isVirtual(), "Cannot set location of virtual shopkeeper!");
		Validate.notNull(location, "location is null");
		Validate.isTrue(location.hasWorldName(), "location has no world name");

		// TODO Changing the world is not safe (at least not for all types of shops)! Consider for
		// example player shops which currently use the shopkeeper's world name to locate their
		// container.
		this.location = location.immutable(); // Immutable copy if necessary

		this.updateChunkCoords();
		this.markDirty();

		// Inform shopkeeper registry:
		if (this.isValid()) {
			SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperMoved(this);
		}

		// Inform subclasses:
		this.onShopkeeperMoved();
	}

	/**
	 * This is called when the location of the shopkeeper has changed.
	 */
	protected void onShopkeeperMoved() {
	}

	/**
	 * Teleports the shopkeeper to a new location.
	 * <p>
	 * This updates the shopkeeper's {@link #setLocation(Location, BlockFace) location} and then
	 * {@link AbstractShopObject#move() moves} the shop object if necessary.
	 * 
	 * @param location
	 *            the new spawn location, not <code>null</code>
	 * @param attachedBlockFace
	 *            The block face against which the shopkeeper is attached, or <code>null</code> to
	 *            not update the block face. This might not be used or stored by the shopkeeper
	 *            itself, but may be used by the shop object.
	 */
	public final void teleport(Location location, @Nullable BlockFace attachedBlockFace) {
		Validate.notNull(location, "location is null");

		boolean spawned = shopObject.isSpawned();

		// This throws an exception if the shopkeeper cannot be moved (virtual shopkeepers) or if
		// the location provides no valid world.
		// This also validates the given attached block face.
		// TODO For some shop objects, setting the attached block face already respawns the shop
		// object.
		this.setLocation(location, attachedBlockFace);

		// Teleport the shop object to its new location:
		// If the shop object does not handle its spawning itself, we only need to teleport the shop
		// object if it was previously already spawned and is currently still spawned (checked by
		// the move method itself), because otherwise the shop object has already been spawned or
		// despawned by the location update.
		if (spawned || !shopObject.getType().mustBeSpawned()) {
			shopObject.move();
		}
	}

	@Override
	public final float getYaw() {
		return yaw;
	}

	/**
	 * Sets the yaw of this shopkeeper.
	 * <p>
	 * This will not automatically rotate the shop object until the next time it is spawned or
	 * teleported.
	 * 
	 * @param yaw
	 *            the new yaw
	 */
	public final void setYaw(float yaw) {
		Validate.State.isTrue(!this.isVirtual(), "Cannot set yaw of virtual shopkeeper!");
		this.yaw = yaw;
		this.markDirty();
	}

	@Override
	public final @Nullable ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	private void updateChunkCoords() {
		if (this.isVirtual()) {
			this.chunkCoords = null;
		} else {
			this.chunkCoords = Unsafe.assertNonNull(location).getChunkCoords();
		}
	}

	/**
	 * Gets the {@link ChunkCoords} by which the shopkeeper is currently stored.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @return the chunk coordinates, can be <code>null</code>
	 */
	public final @Nullable ChunkCoords getLastChunkCoords() {
		return lastChunkCoords;
	}

	/**
	 * Sets the {@link ChunkCoords} by which the shopkeeper is currently stored.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @param chunkCoords
	 *            the chunk coordinates, can be <code>null</code>
	 */
	public final void setLastChunkCoords(@Nullable ChunkCoords chunkCoords) {
		this.lastChunkCoords = chunkCoords;
	}

	/**
	 * Gets the {@link MessageArguments} for this shopkeeper.
	 * <p>
	 * The provided message arguments may be {@link Supplier Suppliers} that lazily and dynamically
	 * calculate the actual message arguments only when they are requested.
	 * 
	 * @param contextPrefix
	 *            this prefix is added in front of all message keys, not <code>null</code>, but may
	 *            be empty
	 * @return the message arguments
	 */
	public final MessageArguments getMessageArguments(String contextPrefix) {
		// Lazily populated map of message argument suppliers:
		if (messageArgumentsMap.isEmpty()) {
			this.populateMessageArguments(messageArgumentsMap);
			assert !messageArgumentsMap.isEmpty();
		}
		return messageArguments.prefixed(contextPrefix);
	}

	/**
	 * Populates the given {@link Map} with the possible message arguments for this shopkeeper.
	 * <p>
	 * In order to not calculate all message arguments in advance when they might not actually be
	 * required, the message arguments are meant to be {@link Supplier Suppliers} that lazily
	 * calculate the actual message arguments only when they are requested.
	 * <p>
	 * In order to be able to reuse the once populated Map, these {@link Supplier Suppliers} are
	 * also meant to be stateless: Any message arguments that depend on dynamic state of this
	 * shopkeeper are meant to dynamically retrieve the current values whenever their
	 * {@link Supplier Suppliers} are invoked.
	 * 
	 * @param messageArguments
	 *            the Map of lazily and dynamically evaluated message arguments
	 */
	protected void populateMessageArguments(Map<String, Supplier<@NonNull ?>> messageArguments) {
		messageArguments.put("id", this::getId);
		messageArguments.put("uuid", this::getUniqueId);
		messageArguments.put("name", () -> Text.parse(this.getName()));
		messageArguments.put("world", () -> StringUtils.getOrEmpty(this.getWorldName()));
		messageArguments.put("x", this::getX);
		messageArguments.put("y", this::getY);
		messageArguments.put("z", this::getZ);
		// TODO The decimal format is not localized. Move it into the language file?
		messageArguments.put("yaw", () -> TextUtils.format(this.getYaw()));
		// TODO Rename to 'position'?
		messageArguments.put("location", this::getPositionString);
		messageArguments.put("type", () -> this.getType().getIdentifier());
		messageArguments.put("object_type", () -> this.getShopObject().getType().getIdentifier());
	}

	// OPEN STATE

	public static final Property<Boolean> OPEN = new BasicProperty<Boolean>()
			.dataKeyAccessor("open", BooleanSerializers.STRICT)
			.useDefaultIfMissing()
			.defaultValue(true)
			.build();

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void setOpen(boolean open) {
		this._setOpen(open);
		this.markDirty();
	}

	private void _setOpen(boolean open) {
		this.open = open;

		if (!open) {
			this.onClosed();
		}
	}

	/**
	 * Called when the shop is {@link #setOpen(boolean) closed}.
	 */
	protected void onClosed() {
		UISessionManager.getInstance()
				.abortUISessionsForContextDelayed(this.getLocation(), this, DefaultUITypes.TRADING());
		UISessionManager.getInstance()
				.abortUISessionsForContextDelayed(this.getLocation(), this, DefaultUITypes.HIRING());
	}

	// NAMING

	public static final Property<String> NAME = new BasicProperty<String>()
			.dataKeyAccessor("name", ColoredStringSerializers.SCALAR)
			.useDefaultIfMissing()
			.defaultValue("")
			.build();

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final void setName(@Nullable String newName) {
		this._setName(newName);
		this.markDirty();
	}

	private void _setName(@Nullable String newName) {
		// Prepare and apply the new name:
		String preparedName = this.prepareName(newName);
		this.name = preparedName;

		// Update the name of the shop object:
		shopObject.setName(preparedName);
	}

	private String prepareName(@Nullable String name) {
		String preparedName = (name != null) ? name : "";
		preparedName = TextUtils.convertHexColorsToBukkit(preparedName);
		preparedName = TextUtils.colorize(preparedName);
		preparedName = this.trimName(preparedName);
		return preparedName;
	}

	private String trimName(String name) {
		assert name != null;
		if (name.length() <= MAX_NAME_LENGTH) {
			return name;
		}
		String trimmedName = name.substring(0, MAX_NAME_LENGTH);
		Log.warning(this.getLogPrefix() + "Name is more than " + MAX_NAME_LENGTH
				+ " characters long ('" + name + "'). Name is trimmed to '" + trimmedName + "'.");
		return trimmedName;
	}

	public boolean isValidName(@Nullable String name) {
		return name != null && name.length() <= MAX_NAME_LENGTH
				&& Settings.DerivedSettings.shopNamePattern.matcher(name).matches();
	}

	// SHOP OBJECT

	/**
	 * Shop object data.
	 */
	public static final Property<ShopObjectData> SHOP_OBJECT_DATA = new BasicProperty<ShopObjectData>()
			.dataKeyAccessor("object", new DataSerializer<ShopObjectData>() {
				@Override
				public @Nullable Object serialize(ShopObjectData value) {
					return DataContainerSerializers.DEFAULT.serialize(value);
				}

				@Override
				public ShopObjectData deserialize(Object data) throws InvalidDataException {
					DataContainer dataContainer = DataContainerSerializers.DEFAULT.deserialize(data);
					return ShopObjectData.ofNonNull(dataContainer);
				}
			})
			.build();

	@Override
	public AbstractShopObject getShopObject() {
		return shopObject;
	}

	// SNAPSHOTS

	public static final Property<List<? extends SKShopkeeperSnapshot>> SNAPSHOTS = new BasicProperty<List<? extends SKShopkeeperSnapshot>>()
			.dataKeyAccessor("snapshots", SKShopkeeperSnapshot.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	static {
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"snapshots",
				MigrationPhase.DEFAULT
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				List<? extends SKShopkeeperSnapshot> snapshots = shopkeeperData.get(SNAPSHOTS);
				if (snapshots.isEmpty()) return false;

				int shopkeeperId = shopkeeperData.get(ID);
				String shopkeeperPrefix = getLogPrefix(shopkeeperId);

				boolean migrated = false;
				int snapshotId = 1;
				for (SKShopkeeperSnapshot snapshot : snapshots) {
					String snapshotLogPrefix = getLogPrefix(shopkeeperPrefix, snapshotId, snapshot);
					migrated |= snapshot.getShopkeeperData().migrate(snapshotLogPrefix);
					snapshotId++;
				}
				return migrated;
			}
		});
	}

	public static String getLogPrefix(
			String shopkeeperPrefix,
			int snapshotId,
			ShopkeeperSnapshot snapshot
	) {
		return shopkeeperPrefix + "Snapshot " + snapshotId + " ('" + snapshot.getName() + "'): ";
	}

	public final String getLogPrefix(int snapshotId, ShopkeeperSnapshot snapshot) {
		return getLogPrefix(this.getLogPrefix(), snapshotId, snapshot);
	}

	private void loadSnapshots(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		List<? extends SKShopkeeperSnapshot> loadedSnapshots = shopkeeperData.get(SNAPSHOTS);
		snapshots.clear();
		try {
			// Applies additional shopkeeper specific validations:
			loadedSnapshots.forEach(this::_addSnapshot);
		} catch (IllegalArgumentException e) {
			int snapshotId = this.getSnapshots().size() + 1;
			ShopkeeperSnapshot snapshot = loadedSnapshots.get(snapshotId - 1);
			String snapshotLogPrefix = "Snapshot " + snapshotId + " ('" + snapshot.getName() + "'): ";
			throw new InvalidDataException(snapshotLogPrefix + e.getMessage(), e);
		}
		this.checkSnapshotsCountLimit();
	}

	private void checkSnapshotsCountLimit() {
		int snapshotsCount = this.getSnapshots().size();
		if (snapshotsCount > SNAPSHOTS_WARNING_LIMIT) {
			Log.warning(this.getLogPrefix() + "This shopkeeper has has more than "
					+ SNAPSHOTS_WARNING_LIMIT + " snapshots (" + snapshotsCount + ")! "
					+ "Consider deleting no longer needed snapshots to save memory and storage space.");
		}
	}

	private void saveSnapshots(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(SNAPSHOTS, snapshotsView);
	}

	@Override
	public final List<? extends SKShopkeeperSnapshot> getSnapshots() {
		return snapshotsView;
	}

	@Override
	public final SKShopkeeperSnapshot getSnapshot(int index) {
		return snapshotsView.get(index);
	}

	@Override
	public final int getSnapshotIndex(String name) {
		String normalizedName = StringUtils.normalize(name);
		if (StringUtils.isEmpty(normalizedName)) return -1;
		for (int index = 0; index < snapshotsView.size(); index++) {
			ShopkeeperSnapshot snapshot = snapshotsView.get(index);
			String normalizedSnapshotName = StringUtils.normalize(snapshot.getName());
			if (normalizedSnapshotName.equals(normalizedName)) {
				return index;
			}
		}
		return -1;
	}

	@Override
	public final @Nullable SKShopkeeperSnapshot getSnapshot(String name) {
		int index = this.getSnapshotIndex(name);
		return (index != -1) ? this.getSnapshot(index) : null;
	}

	@Override
	public final SKShopkeeperSnapshot createSnapshot(String name) {
		// The name is validated during the creation of the snapshot.
		Instant timestamp = Instant.now();
		ShopkeeperData dynamicShopkeeperData = ShopkeeperData.ofNonNull(DataContainer.create());
		this.saveDynamicState(dynamicShopkeeperData, true); // Save all data
		return new SKShopkeeperSnapshot(name, timestamp, dynamicShopkeeperData);
	}

	@Override
	public final void addSnapshot(ShopkeeperSnapshot snapshot) {
		this._addSnapshot(snapshot);
		this.checkSnapshotsCountLimit();
		this.markDirty();
	}

	private void _addSnapshot(ShopkeeperSnapshot snapshot) {
		Validate.notNull(snapshot, "snapshot is null");
		Validate.isTrue(snapshot instanceof SKShopkeeperSnapshot, () -> "snapshot is not of type "
				+ SKShopkeeperSnapshot.class.getName() + ", but " + snapshot.getClass().getName());
		SKShopkeeperSnapshot skSnapshot = (SKShopkeeperSnapshot) snapshot;
		try {
			this.getAndValidateShopType(skSnapshot.getShopkeeperData());
		} catch (InvalidDataException e) {
			Validate.error("Invalid snapshot shop type: " + e.getMessage());
		}

		// The name is assumed to be valid, since it has already been validated during the creation
		// of the snapshot.
		String snapshotName = snapshot.getName();
		Validate.isTrue(this.getSnapshot(snapshotName) == null,
				() -> "There already exists a snapshot with this name: " + snapshotName);

		snapshots.add(skSnapshot);
	}

	@Override
	public final SKShopkeeperSnapshot removeSnapshot(int index) {
		SKShopkeeperSnapshot snapshot = snapshots.remove(index);
		this.markDirty();
		return snapshot;
	}

	@Override
	public final void removeAllSnapshots() {
		snapshots.clear();
		this.markDirty();
	}

	@Override
	public final void applySnapshot(ShopkeeperSnapshot snapshot) throws ShopkeeperLoadException {
		Validate.notNull(snapshot, "snapshot is null");
		Validate.isTrue(snapshot instanceof SKShopkeeperSnapshot, () -> "snapshot is not of type "
				+ SKShopkeeperSnapshot.class.getName() + ", but " + snapshot.getClass().getName());
		// Note: The given snapshot is not necessarily stored by or based on this shopkeeper. Its
		// application may fail if it is not compatible with this shopkeeper.
		// TODO Inform players.
		SKShopkeepersPlugin.getInstance().getUIRegistry().abortUISessions(this);
		try {
			this.loadDynamicState(((SKShopkeeperSnapshot) snapshot).getShopkeeperData());
		} catch (InvalidDataException e) {
			throw new ShopkeeperLoadException(e.getMessage(), e);
		}
		// Note: We don't respawn the shop object here. Loading the data is expected to already
		// update any currently spawned object, similar to when a player manually edits the shop
		// object. This also avoids the brief visual glitching that would be caused by the
		// shopkeeper object being respawned.
		this.markDirty();
	}

	// TRADING

	@Override
	public abstract boolean hasTradingRecipes(@Nullable Player player);

	@Override
	public abstract List<? extends TradingRecipe> getTradingRecipes(@Nullable Player player);

	// USER INTERFACES

	@Override
	public final Collection<? extends UISession> getUISessions() {
		return ShopkeepersPlugin.getInstance().getUIRegistry().getUISessions(this);
	}

	@Override
	public final Collection<? extends UISession> getUISessions(UIType uiType) {
		return ShopkeepersPlugin.getInstance().getUIRegistry().getUISessions(this, uiType);
	}

	@Override
	public final void abortUISessionsDelayed() {
		ShopkeepersPlugin.getInstance().getUIRegistry().abortUISessionsDelayed(this);
	}

	/**
	 * Registers a {@link ViewProvider} that implements a specific type of user interface for this
	 * shopkeeper.
	 * <p>
	 * This replaces any {@link ViewProvider} that has been previously registered for the same
	 * {@link UIType}.
	 * 
	 * @param viewProvider
	 *            the view provider
	 */
	public final void registerViewProvider(ViewProvider viewProvider) {
		Validate.notNull(viewProvider, "viewProvider is null");
		viewProviders.put(viewProvider.getUIType().getIdentifier(), viewProvider);
	}

	/**
	 * Registers a {@link ViewProvider} that implements a specific type of user interface for this
	 * shopkeeper if no other {@link ViewProvider} is registered yet.
	 * 
	 * @param uiType
	 *            the {@link UIType}
	 * @param viewProviderSupplier
	 *            the {@link ViewProvider} supplier
	 */
	public final void registerViewProviderIfMissing(
			UIType uiType,
			Supplier<ViewProvider> viewProviderSupplier
	) {
		Validate.notNull(viewProviderSupplier, "viewProviderSupplier is null");
		if (this.getViewProvider(uiType) == null) {
			this.registerViewProvider(viewProviderSupplier.get());
		}
	}

	/**
	 * Gets the {@link ViewProvider} for the given {@link UIType}.
	 * 
	 * @param uiType
	 *            the {@link UIType}, not <code>null</code>
	 * @return the {@link ViewProvider}, or <code>null</code> if the UI is not supported
	 */
	public final @Nullable ViewProvider getViewProvider(UIType uiType) {
		Validate.notNull(uiType, "uiType is null");
		return viewProviders.get(uiType.getIdentifier());
	}

	@Override
	public final boolean openWindow(UIType uiType, Player player) {
		return this.openWindow(uiType, player, UIState.EMPTY);
	}

	public boolean openWindow(UIType uiType, Player player, UIState uiState) {
		Validate.notNull(uiType, "uiType is null");
		Validate.notNull(player, "player is null");

		String uiIdentifier = uiType.getIdentifier();

		if (!this.isValid()) {
			Log.debug(() -> this.getLogPrefix() + "Cannot open UI '" + uiIdentifier
					+ "' for player " + player.getName() + ": Shopkeeper not valid.");
			return false;
		}

		var viewProvider = this.getViewProvider(uiType);
		if (viewProvider == null) {
			Log.debug(() -> this.getLogPrefix() + "Cannot open UI '" + uiIdentifier
					+ "' for player " + player.getName()
					+ ": This shopkeeper does not support this type of UI.");
			return false;
		}

		return UISessionManager.getInstance().requestUI(viewProvider, player, uiState);
	}

	// Shortcuts for the default UI types:

	@Override
	public final boolean openEditorWindow(Player player) {
		return this.openWindow(DefaultUITypes.EDITOR(), player);
	}

	@Override
	public final boolean openTradingWindow(Player player) {
		return this.openWindow(DefaultUITypes.TRADING(), player);
	}

	/***
	 * Checks if the given {@link CommandSender} has access to the editor.
	 * <p>
	 * For players, this delegates to {@link ViewProvider#canAccess(Player, boolean)} of the
	 * configured editor {@link ViewProvider}. If no editor {@link ViewProvider} is found, this
	 * returns <code>false</code>. For non-player {@link CommandSender}s, this checks the bypass
	 * permission.
	 * 
	 * @param sender
	 *            the {@link CommandSender}, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to omit any feedback that might otherwise be sent to the sender
	 * @return <code>true</code> if the sender can edit this shopkeeper
	 */
	public final boolean canEdit(CommandSender sender, boolean silent) {
		return this.canAccess(sender, DefaultUITypes.EDITOR(), silent);
	}

	/***
	 * Checks if the given {@link CommandSender} has access to the specified {@link UIType}.
	 * <p>
	 * For players, this delegates to {@link ViewProvider#canAccess(Player, boolean)} of the
	 * configured {@link ViewProvider}. If no {@link ViewProvider} is found, this returns
	 * <code>false</code>. For non-player {@link CommandSender}s, this checks the bypass permission.
	 * 
	 * @param sender
	 *            the {@link CommandSender}, not <code>null</code>
	 * @param uiType
	 *            the {@link UIType}, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to omit any feedback that might otherwise be sent to the sender
	 * @return <code>true</code> if the sender can access the UI this shopkeeper
	 */
	public final boolean canAccess(CommandSender sender, UIType uiType, boolean silent) {
		var viewProvider = this.getViewProvider(uiType);
		if (viewProvider == null) return false;

		if (sender instanceof Player player) {
			return viewProvider.canAccess(player, silent);
		} else {
			// Check if the command sender has the bypass permission (e.g. the case for the console
			// and block command senders, but might not be the case for other unexpected types of
			// command senders):
			return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.BYPASS_PERMISSION);
		}
	}

	// EDITOR

	/**
	 * Gets a user-friendly short multi-line overview of the primary properties of this shopkeeper
	 * (id, unique id, name, shop type, object type, location, owner, etc.).
	 * <p>
	 * Sub-types may add additional properties.
	 * 
	 * @return the shop information
	 */
	public List<String> getInformation() {
		var messageArguments = this.getMessageArguments("shop_");
		return StringUtils.replaceArguments(Messages.shopInformation, messageArguments);
	}

	// INTERACTION HANDLING

	/**
	 * Called when a player interacts with this shopkeeper.
	 * 
	 * @param player
	 *            the interacting player
	 */
	public void onPlayerInteraction(Player player) {
		Validate.notNull(player, "player is null");
		if (player.isSneaking()) {
			// Open editor window:
			this.openEditorWindow(player);
		} else {
			// Open trading window:
			this.openTradingWindow(player);
		}
	}

	// TICKING

	/**
	 * Gets the shopkeeper's ticking group.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 * 
	 * @return the shopkeeper's ticking group
	 */
	public final int getTickingGroup() {
		return tickingGroup;
	}

	/**
	 * This is called when the shopkeeper starts ticking.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 */
	public final void informStartTicking() {
		ticking = true;
		this.onStartTicking();
	}

	/**
	 * This is called when the shopkeeper starts ticking.
	 */
	protected void onStartTicking() {
		shopObject.onStartTicking();
	}

	/**
	 * This is called when the shopkeeper stops ticking.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 */
	public final void informStopTicking() {
		// If the shopkeeper is currently processing a tick, this flag can also be used to abort the
		// processing:
		ticking = false;
		this.onStopTicking();
	}

	/**
	 * This is called when the shopkeeper stops ticking.
	 */
	protected void onStopTicking() {
		shopObject.onStopTicking();
	}

	/**
	 * Checks if the shopkeeper is currently ticking, i.e. its ticking has been
	 * {@link #onStartTicking() started} and not yet {@link #onStopTicking() stopped} again.
	 * <p>
	 * The return value does NOT indicate whether the shopkeeper is currently processing a tick.
	 * 
	 * @return <code>true</code> if the shopkeeper is currently ticking
	 */
	public final boolean isTicking() {
		return ticking;
	}

	/**
	 * Ticks this shopkeeper.
	 * <p>
	 * This method is meant to only be used internally by the Shopkeepers plugin itself!
	 */
	public final void tick() {
		assert this.isTicking();

		// An exception during onTickStart will abort the tick.
		this.onTickStart();
		// Abort if the ticking has already been stopped again:
		if (!this.isTicking()) return;

		try {
			this.onTick();
		} finally {
			// onTickEnd is always called, even if onTick was aborted by an exception.
			this.onTickEnd();
		}
	}

	/**
	 * This is called at the beginning of a shopkeeper tick.
	 */
	protected void onTickStart() {
		shopObject.onTickStart();
	}

	// TODO Maybe also tick shopkeepers if the container chunk is loaded? This might make sense once
	// a shopkeeper can be linked to multiple containers, and for virtual player shopkeepers.
	// TODO Maybe also (optionally) tick virtual shopkeepers.
	// TODO Indicate tick activity, similar to shop objects.
	/**
	 * This is called periodically (roughly once per second) for shopkeepers in active chunks.
	 * <p>
	 * This is not called for {@link Shopkeeper#isVirtual() virtual} shopkeepers currently.
	 * <p>
	 * This can for example be used for checks that need to happen periodically, such as checking if
	 * the container of a player shop still exists.
	 * <p>
	 * If the checks to perform are potentially costly performance-wise, or not required to happen
	 * every second, the shopkeeper may decide to run them only every X invocations.
	 * <p>
	 * The ticking of shopkeepers in active chunks may be spread across multiple ticks and might
	 * therefore not happen for all shopkeepers within the same tick.
	 * <p>
	 * If the shopkeeper is marked as {@link #isDirty() dirty}, a
	 * {@link ShopkeeperStorage#saveDelayed() delayed save} will subsequently be triggered.
	 * <p>
	 * When overriding this method, consider calling the parent class version of this method.
	 */
	protected void onTick() {
		// Abort if the ticking has already been stopped again (e.g. if the shopkeeper has been
		// deleted or its ticking stopped due to or during the onTick implementations of the
		// sub-classes):
		if (!this.isTicking()) return;

		// Tick the shop object:
		Location loc = this.getLocation();
		if (loc != null) {
			SchedulerUtils.runTaskOrOmit(loc, () -> {
				shopObject.onTick();
			});
		} else {
			shopObject.onTick();
		}
	}

	/**
	 * This is called at the end of a shopkeeper tick.
	 */
	protected void onTickEnd() {
		shopObject.onTickEnd();

		// Visualize tick activity:
		if (Debug.isDebugging(DebugOptions.visualizeShopkeeperTicks)) {
			this.visualizeLastTick();
		}
	}

	/**
	 * Visualizes the activity of this shopkeeper and all of its components during the last tick.
	 */
	protected void visualizeLastTick() {
		// Visualize the shopkeeper's own tick activity:
		this.visualizeLastShopkeeperTick();

		// Visualize the shop object's tick activity:
		shopObject.visualizeLastTick();
	}

	/**
	 * Visualizes the shopkeeper's own activity during the last tick.
	 * <p>
	 * To also visualize the activity of other components of this shopkeeper, see
	 * {@link #visualizeLastTick()}.
	 */
	protected void visualizeLastShopkeeperTick() {
		Location particleLocation = shopObject.getTickVisualizationParticleLocation();
		if (particleLocation == null) return;
		assert particleLocation.isWorldLoaded() && particleLocation.getWorld() != null;

		this.spawnTickVisualizationParticle(particleLocation);
	}

	private void spawnTickVisualizationParticle(Location location) {
		assert location != null && location.isWorldLoaded() && location.getWorld() != null;
		World world = LocationUtils.getWorld(location);
		world.spawnParticle(Particle.DUST, location, 1, TICK_VISUALIZATION_DUSTS[tickingGroup]);
	}

	// TOSTRING

	@Override
	public String toString() {
		return "Shopkeeper " + this.getIdString();
	}

	// HASHCODE AND EQUALS

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		return (this == obj); // Identity based comparison
	}
}
