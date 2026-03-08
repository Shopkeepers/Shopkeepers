package com.nisovin.shopkeepers.storage;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.storage.migration.RawDataMigrations;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.PluginUtils;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.SingletonTask;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.persistence.DataStore;
import com.nisovin.shopkeepers.util.data.persistence.InvalidDataFormatException;
import com.nisovin.shopkeepers.util.data.persistence.bukkit.BukkitConfigDataStore;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.FileUtils;
import com.nisovin.shopkeepers.util.java.Retry;
import com.nisovin.shopkeepers.util.java.ThrowableUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.java.VoidCallable;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Storage responsible for persisting and loading the data of shopkeepers.
 * <p>
 * Implementation notes:
 * <ul>
 * <li>There can at most be one thread doing file IO at the same time.
 * <li>Saving preparation always happens on the server's main thread. At most one save can be
 * prepared and processed at the same time.
 * <li>If there is a request for another <b>async</b> save while an async save is already in
 * progress, a flag is set to indicate that another save needs to take place once the current async
 * save completes.
 * <li>If there is a request for a <b>sync</b> save while an async save is already in progress, the
 * main thread waits for the async save to finish (or aborts it), before preparing the next save.
 * <li>It is not safe to externally edit the save file while the plugin is running, because the
 * plugin might still store unsaved shopkeeper data in memory or overwrite the save file with new
 * contents at any time.
 * </ul>
 */
public class SKShopkeeperStorage implements ShopkeeperStorage {

	private static final String DATA_FOLDER = "data";
	private static final String SAVE_FILE_NAME = "save.yml";

	private static final String DATA_VERSION_KEY = "data-version";

	private static final List<@Nullable String> HEADER = Collections.unmodifiableList(Arrays.asList(
			"This file is not intended to be manually modified! If you want to manually edit this"
					+ " file anyway, ensure that the server is not running currently and that you"
					+ " have prepared a backup of this file."
	));

	private static final int DELAYED_SAVE_TICKS = 600; // 30 seconds

	// Max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;
	private static final long SAVE_ERROR_MSG_THROTTLE_MILLIS = TimeUnit.MINUTES.toMillis(4);

	private final SKShopkeepersPlugin plugin;

	private final Path saveFile;

	/* Data */
	/*
	 * Holds the data that is used by the current/next (possibly async) save.
	 * This also contains any data of shopkeepers that could not be loaded correctly.
	 * This cannot be modified while an async save is in progress.
	 */
	private final BukkitConfigDataStore saveData = BukkitConfigDataStore.ofNewYamlConfig();

	private int maxUsedShopkeeperId = 0;
	private int nextShopkeeperId = 1;

	/* Unsaved changes */
	// Whether we got an explicit save request. This triggers a write to the save file, even if
	// there have been no changes to the shopkeeper data itself.
	private boolean pendingSaveRequest = false;
	// Shopkeepers that had changes to their data that we did not yet apply to the storage's memory.
	// These shopkeepers may no longer be loaded. This does not include shopkeepers that were
	// deleted. This Set is swapped with another, empty Set when the shopkeepers are saved, so that
	// we can track the shopkeepers that are marked as dirty in the meantime.
	private Set<AbstractShopkeeper> dirtyShopkeepers = new LinkedHashSet<>();
	// Shopkeepers (their ids) whose data we transferred to the storage, but which we were not yet
	// able to save to disk.
	// This Set is not modified while a save is in progress.
	private final Set<Integer> unsavedShopkeepers = new HashSet<>();
	// Shopkeepers (their ids) that got deleted since the last save. The next save will remove their
	// data from the save file. This Set is not modified while a save is in progress.
	private final Set<Integer> unsavedDeletedShopkeepers = new HashSet<>();
	// Shopkeepers that got deleted during the last async save. Their data is removed from memory
	// after the current save completes, and removed from the save file by the subsequent save.
	private final Set<AbstractShopkeeper> shopkeepersToDelete = new LinkedHashSet<>();

	/* Loading */
	private boolean currentlyLoading = false;

	/* Saving */
	private final SaveTask saveTask;
	// Flag to (temporarily) turn off saving. This can for example be set if there is an issue with
	// loading the shopkeeper data, so that the save file doesn't get overwritten by any subsequent
	// save requests.
	private boolean savingDisabled = false;
	private @Nullable WrappedTask delayedSaveTask = null;

	public SKShopkeeperStorage(SKShopkeepersPlugin plugin) {
		DataVersion.init();
		this.plugin = plugin;
		this.saveFile = Unsafe.initialized(this)._getSaveFile();
		this.saveTask = new SaveTask(plugin);
	}

	private Path getPluginDataFolder() {
		return plugin.getDataFolder().toPath();
	}

	private Path _getDataFolder() {
		return this.getPluginDataFolder().resolve(DATA_FOLDER);
	}

	private Path _getSaveFile() {
		return this._getDataFolder().resolve(SAVE_FILE_NAME);
	}

	public void onEnable() {
		// Start periodic save task:
		if (!Settings.saveInstantly) {
			new PeriodicSaveTask().start();
		}
	}

	public void onDisable() {
		// Ensure that there is no unsaved data and that all saves are completed before we continue:
		this.saveIfDirtyAndAwaitCompletion();

		// Verify that the storage is actually no longer dirty:
		// We may run into this if the previous save failed, or if there is a bug. In either case,
		// it might indicate that data has been lost.
		if (this.isDirty()) {
			Log.warning("The shopkeeper storage is still dirty (pendingSaveRequest="
					+ pendingSaveRequest
					+ ", dirtyShopkeepers=" + dirtyShopkeepers.size()
					+ ", unsavedShopkeepers=" + unsavedShopkeepers.size()
					+ ", unsavedDeletedShopkeepers=" + unsavedDeletedShopkeepers.size()
					+ ", shopkeepersToDelete=" + shopkeepersToDelete.size()
					+ "). Did the previous save fail? Data might have been lost!");
		}
		// Also verify that the save task has actually completed its executions:
		if (saveTask.isRunning() || saveTask.isExecutionPending()) {
			Log.warning("There is still a save of shopkeeper data in progress ("
					+ saveTask.isRunning() + ") or pending execution ("
					+ saveTask.isExecutionPending() + ")!");
		}

		// Reset a few things:
		saveTask.onDisable();
		this.clearSaveData();
		savingDisabled = false;
		pendingSaveRequest = false;
		dirtyShopkeepers.clear();
		unsavedShopkeepers.clear();
		unsavedDeletedShopkeepers.clear();
		shopkeepersToDelete.clear();
		delayedSaveTask = null;
	}

	private class PeriodicSaveTask implements Runnable {

		private static final long PERIOD_TICKS = 6000L; // 5 minutes

		void start() {
			SchedulerUtils.runAsyncTaskTimerOrOmit(this, PERIOD_TICKS, PERIOD_TICKS);
		}

		@Override
		public void run() {
			saveIfDirty();
		}
	}

	private SKShopkeeperRegistry getShopkeeperRegistry() {
		return plugin.getShopkeeperRegistry();
	}

	// SHOPKEEPER IDs

	/**
	 * Gets an unused shopkeeper id that can be used for a new shopkeeper.
	 * <p>
	 * This does not increment the shopkeeper id counter on its own, because we do not want to
	 * increment it in case the shopkeeper creation fails. Use {@link #onShopkeeperIdUsed(int)} once
	 * the id is actually being used.
	 * 
	 * @return the next unused shopkeeper id
	 */
	public int getNextShopkeeperId() {
		int nextId = nextShopkeeperId; // Can end up negative after increments due to overflows
		if (nextId <= 0 || !this.isUnusedId(nextId)) {
			// Try to use an id larger than the max currently used id:
			int maxId = maxUsedShopkeeperId;
			assert maxId > 0;
			if (maxId < Integer.MAX_VALUE) {
				nextId = maxId + 1; // Causes no overflow
				assert this.isUnusedId(nextId); // There is no used id greater than the max id
			} else {
				// Find the first unused id:
				nextId = 1;
				while (!this.isUnusedId(nextId)) {
					if (nextId == Integer.MAX_VALUE) {
						// All ids are in use (unlikely..):
						throw new IllegalStateException("No unused shopkeeper ids available!");
					}
					nextId++;
				}
				assert nextId > 0;
			}
			// Remember the found next id:
			nextShopkeeperId = nextId;
		}
		return nextId;
	}

	/**
	 * Checks if the given id is already used by any shopkeeper.
	 * <p>
	 * This also takes the shopkeepers into account that are not currently loaded (for example if
	 * they could not be loaded for some reason, or if they were already unloaded again).
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @return <code>true</code> if the shopkeeper id is not yet being used
	 */
	private boolean isUnusedId(int id) {
		// Check if there is data for a shopkeeper with this id (this includes shopkeepers that
		// could not be loaded for some reason, or are currently not loaded):
		if (saveData.contains(String.valueOf(id))) return false;

		// Check the unsaved deleted shopkeepers: As long as their deletion has not yet been
		// persisted, we block their ids from being reused. This also applies if these deleted
		// shopkeepers are currently being saved.
		if (unsavedDeletedShopkeepers.contains(id)) {
			return false;
		}

		// Checking the shopkeepersToDelete is not necessarily required: If they have been saved
		// before, the saveData should still contain their entry, so the above check already finds
		// them. And if they have never been saved before, it might seem safe to reuse their ids.
		// However, in order to maintain a consistent view on the used ids throughout the plugin
		// (ids are expected to only represent a single shopkeeper throughout their lifetime), we
		// nevertheless block these ids from being reused until their deletion has been fully
		// processed.
		for (Shopkeeper shopkeeper : shopkeepersToDelete) {
			if (shopkeeper.getId() == id) {
				return false;
			}
		}

		// Check the dirty shopkeepers (they might no longer be loaded, but their data might not
		// have been added to the storage yet):
		for (Shopkeeper shopkeeper : dirtyShopkeepers) {
			if (shopkeeper.getId() == id) {
				return false;
			}
		}

		// Note: We are not checking the unsavedShopkeepers. Their data has already been added to
		// the saveData, so the above check should find them.
		// We are also not checking the dirty shopkeepers that may currently be getting saved. The
		// saveData is prepared synchronously, and we don't expect shopkeepers to be created while
		// this preparation is in progress. So the saveData should already contain them.
		// And we are also not checking the currently loaded shopkeepers in the ShopkeeperRegistry:
		// The saveData either already contains them, or they are part of the dirty shopkeepers (new
		// shopkeepers are marked as dirty right away).

		// We could not find a shopkeeper with this id, so it is unused:
		return true;
	}

	/**
	 * Informs this storage that the given shopkeeper id is now being used.
	 * <p>
	 * This has to be called by the {@link ShopkeeperRegistry} whenever it creates or loads a
	 * shopkeeper.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	public void onShopkeeperIdUsed(int id) {
		if (id > maxUsedShopkeeperId) {
			maxUsedShopkeeperId = id;
		}
		if (id >= nextShopkeeperId) {
			nextShopkeeperId = id + 1;
		}
	}

	/**
	 * Tries to revert the {@link #nextShopkeeperId} and {@link #maxUsedShopkeeperId} to a smaller
	 * unused id.
	 * <p>
	 * For example invoked after the deletion of some shopkeeper(s) was persisted.
	 * <p>
	 * There are cases in which shopkeepers are temporarily created and then immediately deleted
	 * again because they fail to spawn. In these cases, we want to avoid unnecessarily consuming
	 * shopkeeper ids.
	 * <p>
	 * Note: Since there can be some delay between creating the shopkeeper and then persisting its
	 * immediate deletion, it is possible for another shopkeeper to be created before we are able to
	 * rollback the used id. In those cases, we are not able to reuse the id, since we currently try
	 * to avoid reusing ids that are smaller than other used ids (at least until we run out of
	 * available ids).
	 */
	private void rollbackNextShopkeeperId() {
		int maxUsedId = maxUsedShopkeeperId;
		assert maxUsedId >= 0 && maxUsedId <= Integer.MAX_VALUE;

		while (maxUsedId > 0 && this.isUnusedId(maxUsedId)) {
			maxUsedId -= 1;
		}

		if (maxUsedId != maxUsedShopkeeperId) {
			maxUsedShopkeeperId = maxUsedId;
			nextShopkeeperId = maxUsedId + 1;
		}
	}

	// LOADING

	/**
	 * Clears the in-memory save data and resets any corresponding state of this storage.
	 */
	private void clearSaveData() {
		saveData.clear();
		maxUsedShopkeeperId = 0;
		nextShopkeeperId = 1;
	}

	// Returns true on success, and false if there was some severe issue during loading.
	// This is blocking and will wait for any currently ongoing or pending saves to complete!
	public boolean reload() {
		if (currentlyLoading) {
			throw new IllegalStateException("Already loading right now!");
		}

		// To avoid concurrent access of the save file, we wait for any ongoing and pending saves to
		// complete:
		// TODO Skip the reload if we just triggered another save? The reloaded data is expected to
		// match the data we just saved.
		this.saveIfDirtyAndAwaitCompletion();

		currentlyLoading = true;
		boolean result;
		try {
			result = this.doReload();
		} catch (Exception e) {
			Log.severe(
					"Something unexpected went wrong during the loading of the saved shopkeepers data!",
					e
			);
			result = false; // Error
		} finally {
			currentlyLoading = false;
		}
		return result;
	}

	// TODO Move parts of this into the ShopkeeperRegistry (resolves the currently existing cyclic
	// dependency between the storage and the registry).
	// Returns true on success, and false if there was some severe issue during loading.
	private boolean doReload() {
		// Unload all currently loaded shopkeepers:
		SKShopkeeperRegistry shopkeeperRegistry = this.getShopkeeperRegistry();
		shopkeeperRegistry.unloadAllShopkeepers();
		this.clearSaveData();

		Path saveFile = this.saveFile;
		if (!Files.exists(saveFile)) {
			var tempSaveFile = FileUtils.getTempSibling(saveFile);
			if (Files.exists(tempSaveFile)) {
				// Load from temporary save file instead:
				Log.warning("Found no save file, but an existing temporary save file ("
						+ PluginUtils.relativize(plugin, tempSaveFile) + ")!"
						+ " This might indicate an issue during a previous saving attempt!"
						+ " We try to load the Shopkeepers data from this temporary save file"
						+ " instead!");
				saveFile = tempSaveFile;
			} else {
				// No save file exists yet -> No shopkeeper data available.
				// We silently set up the data version and abort:
				saveData.set(DATA_VERSION_KEY, DataVersion.current().toString());
				return true;
			}
		}

		boolean rawDataMigrated = false;

		// Load the save data:
		try (Reader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
			var content = FileUtils.read(reader);

			// Apply string-based migrations:
			var migratedContent = RawDataMigrations.applyMigrations(content);
			rawDataMigrated = !content.equals(migratedContent);

			if (rawDataMigrated) {
				var now = LocalDateTime.now();
				var backupSaveFile = saveFile.resolveSibling(
						now.format(FileUtils.DATE_TIME_FORMATTER) + "_" + saveFile.getFileName()
								+ ".backup"
				);
				Log.info("Shopkeeper data migrated. Writing backup to "
						+ PluginUtils.relativize(plugin, backupSaveFile));

				try {
					// Error if a file already exists at the destination:
					Files.copy(saveFile, backupSaveFile);
				} catch (Exception e) {
					Log.severe("Failed to write backup file!", e);
					return false; // Disable without save
				}
			}

			// If a migration was applied, write the intermediate result to disk for debugging
			// purposes (e.g. if the subsequent loading fails):
			if (Debug.isDebugging() && rawDataMigrated) {
				var migratedSaveFile = saveFile.resolveSibling(saveFile.getFileName() + ".migrated");
				Log.info("Writing migrated save file to "
						+ PluginUtils.relativize(plugin, migratedSaveFile));
				try {
					FileUtils.writeSafely(
							migratedSaveFile,
							migratedContent,
							StandardCharsets.UTF_8,
							Log.getLogger(),
							getPluginDataFolder()
					);
				} catch (Exception e) {
					Log.warning("Failed to write migrated save file ("
							+ PluginUtils.relativize(plugin, migratedSaveFile)
							+ "). This file is only written for debugging purposes."
							+ " Continuing the data loading ...", e);
				}
			}

			// Since Bukkit 1.16.5, this automatically clears the save data before loading the new
			// entries:
			saveData.loadFromString(migratedContent);
		} catch (InvalidDataFormatException e) {
			Log.severe("Failed to load the save file! Note: Server downgrades or manually "
					+ "editing the save file are not supported!", e);
			return false; // Disable without save
		} catch (Exception e) {
			Log.severe("Failed to load the save file!", e);
			return false; // Disable without save
		}

		// Insert the data version as the first (top) entry:
		// Explicitly setting the 'missing' data version value here ensures that the data version
		// will be the first entry in the save file, even if it is missing in the save file
		// currently. If a data version is present in the loaded data, the 'missing' data version
		// value is replaced with the actual data version afterwards.
		Map<? extends String, @NonNull ?> saveDataEntries = saveData.getValuesCopy();
		saveData.clear();
		saveData.set(DATA_VERSION_KEY, DataVersion.MISSING.toString());
		saveData.setAll(saveDataEntries);

		// Parse data version:
		String dataVersionString = saveData.getString(DATA_VERSION_KEY);
		assert dataVersionString != null;
		DataVersion dataVersion;
		try {
			dataVersion = DataVersion.parse(dataVersionString);
		} catch (IllegalArgumentException e) {
			Log.severe("Failed to parse the data version of the save file!", e);
			return false; // Disable without save
		}

		// Check if we can detect a server downgrade:
		// TODO This check is often never reached, because Bukkit already fails to load the save
		// file during server downgrades if it contains saved item stacks. This check only catches
		// cases in which the save file contains no saved item stacks, or the item stacks are for
		// some reason stored in an older data version.
		if (DataVersion.current().isMinecraftDowngrade(dataVersion)) {
			Log.severe("Detected Minecraft server downgrade from data version '"
					+ dataVersion + "' to '" + DataVersion.current()
					+ "'! Server downgrades are not supported. "
					+ "Disabling the plugin in order to prevent data loss!");
			return false; // Disable without save
		}

		// Check if we can detect a Shopkeepers plugin downgrade:
		// Even if the Minecraft server did not downgrade, plugin downgrades can also be
		// problematic, because shopkeeper data other than item stacks can also be affected by
		// backwards incompatible changes that have previously been applied by a newer Shopkeepers
		// version. Note that even the addition of new shopkeeper attributes is a 'backwards
		// incompatible' change: Even if the current plugin version simply ignores any additional
		// data and would run fine, our current storage implementation does not keep track of
		// unknown data.
		// So with the next save this additional data would be lost.
		// TODO This check can only detect changes for which we incremented the shopkeeper data
		// version. Any external shopkeeper or shop object object implementations are not yet
		// covered by this check. A possible solution could be to allow external add-ons to specify
		// their data versions and then save those into the save file as well.
		if (DataVersion.current().isShopkeeperStorageDowngrade(dataVersion)
				|| DataVersion.current().isShopkeeperDataDowngrade(dataVersion)) {
			Log.severe("Detected Shopkeepers plugin downgrade from data version '"
					+ dataVersion + "' to '" + DataVersion.current()
					+ "'! Plugin downgrades are not supported. "
					+ "Disabling the plugin in order to prevent data loss!");
			return false; // Disable without save
		}

		Set<? extends String> keys = saveData.getKeys();
		// Contains at least the data-version entry:
		assert keys.contains(DATA_VERSION_KEY);
		int shopkeepersCount = (keys.size() - 1);
		if (shopkeepersCount == 0) {
			// No shopkeeper data exists yet. Silently update the data version and abort:
			saveData.set(DATA_VERSION_KEY, DataVersion.current().toString());
			return true;
		}

		Log.info("Loading the data of " + shopkeepersCount + " shopkeepers ...");

		// Check if the data version has changed, and whether we need to trigger a full save of all
		// shopkeeper data:
		boolean dataVersionChanged = !DataVersion.current().equals(dataVersion);
		boolean forceSaveAllShopkeepers = rawDataMigrated
				|| DataVersion.current().isMinecraftUpgrade(dataVersion)
				|| DataVersion.current().isShopkeeperStorageUpgrade(dataVersion);
		if (dataVersionChanged) {
			Log.info("The save file's data version has changed from '" + dataVersion
					+ "' to '" + DataVersion.current() + "'.");
			// Update the data version:
			saveData.set(DATA_VERSION_KEY, DataVersion.current().toString());

			// Mark the storage as dirty so that the new data version is saved to disk even if none
			// of the loaded shopkeepers is marked as dirty:
			this.requestSave();
		}

		if (forceSaveAllShopkeepers) {
			Log.info("The saved data of all shopkeepers is updated.");
			this.requestSave();
		}

		for (String key : keys) {
			if (key.equals(DATA_VERSION_KEY)) continue; // Skip the data version entry

			// If the shopkeeper cannot be loaded, it is skipped and the loading continues with the
			// remaining shopkeepers:
			// Note: When a player shopkeeper cannot be loaded, its associated containers might no
			// longer be protected. So this is potentially a severe issue that admins should
			// immediately look into. However, we do not abort the enabling of the plugin if
			// individual shopkeepers cannot be loaded, because this would disable the protection of
			// all player shop containers on the server (which is even worse).
			this.loadShopkeeper(key, forceSaveAllShopkeepers);
		}
		return true;
	}

	private @Nullable ShopkeeperData getShopkeeperData(int shopkeeperId) {
		DataContainer shopkeeperDataContainer = saveData.getContainer(String.valueOf(shopkeeperId));
		if (shopkeeperDataContainer == null) {
			return null;
		}

		// We create a shallow copy of the shopkeeper data and then re-insert the separately stored
		// shopkeeper id:
		// The copy is required because we don't want to insert the id into the data container that
		// is stored by
		// saveData, because that data container can end up being saved back to disk again (e.g.
		// when the shopkeeper
		// fails to load, or when it fails to save its state during shopkeeper saving).
		ShopkeeperData shopkeeperData = ShopkeeperData.ofNonNull(DataContainer.ofNonNull(
				shopkeeperDataContainer.getValuesCopy()
		));
		shopkeeperData.set(AbstractShopkeeper.ID, shopkeeperId);
		return shopkeeperData;
	}

	private void loadShopkeeper(String key, boolean forceSave) {
		Integer idInt = ConversionUtils.parseInt(key);
		if (idInt == null || idInt <= 0) {
			this.failedToLoadShopkeeper(key, "Invalid id: " + key);
			return;
		}

		int shopkeeperId = idInt.intValue();
		if (shopkeeperId > maxUsedShopkeeperId) {
			maxUsedShopkeeperId = shopkeeperId;
		}

		ShopkeeperData shopkeeperData = this.getShopkeeperData(shopkeeperId);
		if (shopkeeperData == null) {
			this.failedToLoadShopkeeper(key, "Invalid shopkeeper data!");
			return;
		}

		// Perform data migrations:
		boolean migrated;
		try {
			migrated = shopkeeperData.migrate(AbstractShopkeeper.getLogPrefix(shopkeeperId));
		} catch (InvalidDataException e) {
			this.failedToLoadShopkeeper(key, "Shopkeeper data migration failed!", e);
			return;
		}

		// Load the shopkeeper:
		SKShopkeeperRegistry shopkeeperRegistry = this.getShopkeeperRegistry();
		AbstractShopkeeper shopkeeper;
		try {
			shopkeeper = shopkeeperRegistry.loadShopkeeper(shopkeeperData);
			assert shopkeeper != null && shopkeeper.isValid();
		} catch (InvalidDataException e) {
			this.failedToLoadShopkeeper(key, "Shopkeeper data could not be loaded!", e);
			return;
		} catch (Exception e) {
			this.failedToLoadShopkeeper(key, "Unexpected error!", e);
			return;
		}

		// If the shopkeeper was migrated or a forced save is requested, mark the shopkeeper as
		// dirty:
		// During plugin enable, after the shopkeepers have been loaded, a save is triggered if the
		// storage has been marked as dirty.
		if (migrated || forceSave) {
			shopkeeper.markDirty();
		}
	}

	private void failedToLoadShopkeeper(String idKey, String reason) {
		this.failedToLoadShopkeeper(idKey, reason, null);
	}

	private void failedToLoadShopkeeper(
			String idKey,
			String reason,
			@Nullable Throwable throwable
	) {
		Log.warning("Failed to load shopkeeper '" + idKey + "': " + reason, throwable);
	}

	// SHOPKEEPER DATA CHANGES

	// Note: This does not take into account any unsaved data that a save in progress might
	// currently process.
	@Override
	public boolean isDirty() {
		assert !saveTask.isPostProcessing();

		// Explicit save request pending:
		if (pendingSaveRequest) return true;

		// Dirty shopkeepers:
		if (!dirtyShopkeepers.isEmpty()) return true;
		if (!saveTask.isRunning()) {
			// Only take the unsaved shopkeepers into account if there is currently no save in
			// progress:
			if (!unsavedShopkeepers.isEmpty()) return true;
		}

		// Unsaved deleted shopkeepers:
		if (!saveTask.isRunning()) {
			assert shopkeepersToDelete.isEmpty();
			if (!unsavedDeletedShopkeepers.isEmpty()) return true;
		} else {
			if (!shopkeepersToDelete.isEmpty()) return true;
			// We do not take the unsavedDeletedShopkeepers into account while they are being saved.
		}

		return false;
	}

	/**
	 * Deletes the data for the given shopkeeper.
	 * <p>
	 * The actual deletion is persisted with the next successful save.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 */
	public void deleteShopkeeper(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		// If the save task is currently running (and not in its synchronous post-processing
		// callback), we defer the deletion of the shopkeeper's data:
		if (saveTask.isRunning() && !saveTask.isPostProcessing()) {
			// Remember to remove the data after the current async save completes:
			shopkeepersToDelete.add(shopkeeper);
			// Note: We could check here if the saveData even contains data for the given
			// shopkeeper. However, the result of this check would be unreliable, because the
			// current save in progress might be about to save data for the shopkeeper, which we
			// would then miss.
		} else {
			int shopkeeperId = shopkeeper.getId();
			String key = String.valueOf(shopkeeperId);

			// Check if there is data for the deleted shopkeeper. If there is no data for it, we can
			// assume right away that the shopkeeper has been deleted from the storage, and that
			// there is also no data for it on disk, even without waiting for the next save.
			boolean shopkeeperDataExists = saveData.contains(key);

			if (shopkeeperDataExists) {
				// Remove the shopkeeper's data:
				saveData.remove(key);

				// The next save removes the data from the save file on disk:
				unsavedDeletedShopkeepers.add(shopkeeperId);
			}

			// Remove the shopkeeper from the dirty and unsaved shopkeepers (there is no need to
			// save it anymore):
			dirtyShopkeepers.remove(shopkeeper);
			unsavedShopkeepers.remove(shopkeeperId);

			if (!shopkeeperDataExists) {
				// We can immediately try to rollback the used id:
				this.rollbackNextShopkeeperId();
			}
		}
	}

	/**
	 * Gets the number of shopkeepers that were deleted, but whose deletions have not yet been
	 * persisted.
	 * <p>
	 * This also includes any deleted shopkeepers that are currently being saved.
	 * 
	 * @return the number of unsaved deleted shopkeepers
	 */
	public int getUnsavedDeletedShopkeepersCount() {
		return unsavedDeletedShopkeepers.size() + shopkeepersToDelete.size();
	}

	/**
	 * Informs this storage that the given shopkeeper had changes to its data that need to be
	 * persisted with the next save.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 */
	public void markDirty(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(shopkeeper.isValid(), "shopkeeper is invalid");
		assert !unsavedDeletedShopkeepers.contains(shopkeeper.getId());
		assert !shopkeepersToDelete.contains(shopkeeper);
		dirtyShopkeepers.add(shopkeeper);

		// Remove the shopkeeper from the unsavedShopkeepers: It's either dirty or unsaved.
		if (!saveTask.isRunning()) {
			unsavedShopkeepers.remove(shopkeeper.getId());
		}
		// Else: The save task's post-processing will clean up the unsavedShopkeepers (if
		// necessary).
	}

	/**
	 * Gets the number of shopkeepers that had changes to their data, but whose changes have not yet
	 * been persisted.
	 * <p>
	 * This also includes any shopkeepers that are currently being saved. This does not include the
	 * {@link #getUnsavedDeletedShopkeepersCount() unsaved deleted shopkeepers}.
	 * 
	 * @return the number of shopkeeper with unsaved changes to their data
	 */
	public int getUnsavedDirtyShopkeepersCount() {
		int count = dirtyShopkeepers.size() + unsavedShopkeepers.size();
		if (saveTask.isRunning()) {
			// These Sets of shopkeepers might overlap, so we need to avoid counting their common
			// elements multiple times:
			for (AbstractShopkeeper shopkeeper : dirtyShopkeepers) {
				if (unsavedShopkeepers.contains(shopkeeper.getId())) {
					count--;
				}
			}
			for (AbstractShopkeeper shopkeeper : saveTask.savingDirtyShopkeepers) {
				// The dirty shopkeepers that are currently being saved are consistent (disjoint)
				// with the unsavedShopkeepers:
				assert !unsavedShopkeepers.contains(shopkeeper.getId());
				// But they might overlap with the shopkeepers that have been marked as dirty in the
				// meantime:
				if (!dirtyShopkeepers.contains(shopkeeper)) {
					count++;
				}
			}
		} else {
			// Assert: dirtyShopkeepers and unsavedShopkeepers are disjoint.
			// Assert: saveTask.savingDirtyShopkeepers is empty.
		}
		return count;
	}

	// SAVING

	public void disableSaving() {
		this.savingDisabled = true;
	}

	public void enableSaving() {
		this.savingDisabled = false;
	}

	private void requestSave() {
		pendingSaveRequest = true;
	}

	@Override
	public void save() {
		if (Settings.saveInstantly) {
			this.saveNow();
		} else {
			this.requestSave();
		}
	}

	@Override
	public void saveDelayed() {
		this.requestSave();
		if (Settings.saveInstantly && delayedSaveTask == null) {
			new DelayedSaveTask().start();
		} // Else: The periodic save task will trigger a save at some point.
	}

	private class DelayedSaveTask implements Runnable {

		void start() {
			assert delayedSaveTask == null;
			delayedSaveTask = SchedulerUtils.runAsyncTaskLaterOrOmit(this, DELAYED_SAVE_TICKS);
		}

		@Override
		public void run() {
			delayedSaveTask = null;
			saveIfDirty();
		}
	}

	@Override
	public void saveNow() {
		this.doSave(true);
	}

	@Override
	public void saveImmediate() {
		this.doSave(false);
	}

	@Override
	public void saveIfDirtyAndAwaitCompletion() {
		if (this.isDirty()) {
			this.saveImmediate();
		} else {
			saveTask.awaitExecutions();
		}
	}

	private void doSave(boolean async) {
		if (savingDisabled) {
			Log.warning("Skipping save, because saving got disabled.");
			return;
		}

		if (async) {
			saveTask.run();
		} else {
			saveTask.runImmediately();
		}
	}

	private class SaveTask extends SingletonTask {

		// Previously dirty shopkeepers that we currently attempt to save. This Set is only modified
		// synchronously, so it is safe to be accessed at any time by the storage.
		Set<AbstractShopkeeper> savingDirtyShopkeepers = new LinkedHashSet<>();
		// The shopkeepers that we were not able to save for some reason:
		private final Set<AbstractShopkeeper> failedToSave = new LinkedHashSet<>();

		/* Last save */
		// These variables get replaced during the next save.
		// Note: Explicit synchronization is not needed for these variables, because they already
		// get synchronized before they are used, either by the Bukkit Scheduler (when starting the
		// async task and when going back to the main thread by starting a sync task), or/and via
		// synchronization with the save task's lock.
		private boolean savingSucceeded = false;
		private long lastSaveErrorMsgMillis = 0L;

		SaveTask(Plugin plugin) {
			super(plugin);
		}

		void onDisable() {
			lastSaveErrorMsgMillis = 0L;
		}

		private class InternalAsyncTask extends SingletonTask.InternalAsyncTask {
		}

		private class InternalSyncCallbackTask extends SingletonTask.InternalSyncCallbackTask {
		}

		@Override
		protected InternalAsyncTask createInternalAsyncTask() {
			return new InternalAsyncTask();
		}

		@Override
		protected InternalSyncCallbackTask createInternalSyncCallbackTask() {
			return new InternalSyncCallbackTask();
		}

		@Override
		protected void prepare() {
			// Stop any active delayed save task:
			if (delayedSaveTask != null) {
				delayedSaveTask.cancel();
				delayedSaveTask = null;
			}

			// Set up the file header:
			// This replaces any previously existing and loaded header and thereby ensures that it
			// is always up-to-date after we have saved the file.
			saveData.getConfig().options().setHeader(HEADER);

			// Reset the pendingSaveRequest flag here (and not just after a successful save), so
			// that we can track any save requests that occur in the meantime, which require another
			// save later:
			// Note: This flag is also reset to true if the current save attempt fails.
			pendingSaveRequest = false;

			// Swap the dirty shopkeepers sets:
			assert savingDirtyShopkeepers.isEmpty();
			Set<AbstractShopkeeper> newDirtyShopkeepers = savingDirtyShopkeepers;
			savingDirtyShopkeepers = dirtyShopkeepers;
			dirtyShopkeepers = newDirtyShopkeepers;

			// Save the data of dirty shopkeepers:
			assert failedToSave.isEmpty();
			savingDirtyShopkeepers.forEach(this::saveShopkeeper);
		}

		private void saveShopkeeper(AbstractShopkeeper shopkeeper) {
			// Note: The shopkeeper might no longer be valid (loaded).
			assert shopkeeper.isDirty();
			String key = String.valueOf(shopkeeper.getId());
			Object previousData = saveData.get(key);
			// This replaces the previous shopkeeper data:
			ShopkeeperData newData = ShopkeeperData.ofNonNull(saveData.createContainer(key));
			try {
				shopkeeper.save(newData, false); // May reference externally stored data
			} catch (Exception e) {
				// Error while saving shopkeeper data:
				// Restore previous shopkeeper data and then skip this shopkeeper.
				saveData.set(key, previousData);
				Log.warning(shopkeeper.getLogPrefix() + "Saving failed!", e);
				// We remember the shopkeeper and keep it marked as dirty, so that the next save of
				// all shopkeepers attempts to save it again.
				// However, we won't automatically initiate a new save for this shopkeeper as the
				// risk is high that saving will fail again anyway.
				failedToSave.add(shopkeeper);
				return;
			}

			// Remove the separately stored shopkeeper id from the shopkeeper data:
			newData.set(AbstractShopkeeper.ID.getUnvalidatedSaver(), null);

			// We transferred the shopkeeper's data into the storage. Reset the shopkeeper's dirty
			// flag:
			shopkeeper.onSave();
		}

		// Can be run async or sync.
		@Override
		protected void execute() {
			savingSucceeded = this.saveToFile(saveData);
		}

		// Returns true if the saving was successful.
		private boolean saveToFile(DataStore saveData) {
			try {
				// Serialize data to String:
				// TODO Do this on the main thread? Bukkit's serialization API is not strictly
				// thread-safe ...
				// However, this should usually not be an issue if the serialized objects inside the
				// save data are not accessed externally, and do not rely on external state during
				// serialization.
				String data;
				try {
					data = saveData.saveToString();
				} catch (Exception e) {
					throw new ShopkeeperStorageSaveException(
							"Could not serialize shopkeeper data!", e
					);
				}

				Retry.retry((VoidCallable) () -> {
					this.doSaveToFile(data);
				}, SAVING_MAX_ATTEMPTS, (attemptNumber, exception, retry) -> {
					// Saving failed:
					assert exception != null;
					// Don't spam with errors and stacktraces: Only print them once for the first
					// failed saving attempt (and again for the last failed attempt), and otherwise
					// log a compact description of the issue:
					String errorMsg = "Failed to save shopkeepers (attempt " + attemptNumber + ")";
					if (attemptNumber == 1) {
						Log.severe(errorMsg, exception);
					} else {
						String issue = ThrowableUtils.getDescription(exception);
						Log.severe(errorMsg + ": " + issue);
					}

					// Try again after a small delay:
					if (retry) {
						try {
							Thread.sleep(SAVING_ATTEMPTS_DELAY_MILLIS);
						} catch (InterruptedException e) {
							// Restore the interrupt status for anyone interested in it, but
							// otherwise ignore the interrupt here, because we prefer to keep
							// retrying to still save the data to disk after all:
							Thread.currentThread().interrupt();
						}
					}
				});

				return true; // Success
			} catch (Exception e) {
				// Saving failed even after several attempts:
				Log.severe("Saving of shopkeepers failed! Data might have been lost! :(", e);
				return false;
			}
		}

		/**
		 * Writes the given properly formatted shopkeeper data to disk.
		 * <p>
		 * Saving procedure:
		 * <ul>
		 * <li>If there already is a temporary save file:
		 * <ul>
		 * <li>If there is no save file: Rename temporary save file to save file (ideally atomic).
		 * <li>Else: Remove temporary save file.
		 * </ul>
		 * <li>Create temporary save file's parent directories (if required).
		 * <li>Create new temporary save file and write data to it.
		 * <li>Sync temporary save file and containing directory (ensures that the data is persisted
		 * to disk).
		 * <li>Remove old save file (if it exists).
		 * <li>Create save file's parent directories (if required).
		 * <li>Rename temporary save file to save file (ideally atomic).
		 * <li>Sync save file's parent directory (ensures that the rename operation is persisted to
		 * disk).
		 * </ul>
		 * 
		 * @param data
		 *            the formatted data
		 * @throws ShopkeeperStorageSaveException
		 *             if something goes wrong
		 */
		private void doSaveToFile(String data) throws ShopkeeperStorageSaveException {
			assert data != null;
			try {
				FileUtils.writeSafely(
						saveFile,
						data,
						StandardCharsets.UTF_8,
						Log.getLogger(),
						getPluginDataFolder()
				);
			} catch (Exception e) {
				throw new ShopkeeperStorageSaveException(e.getMessage(), e);
			}
		}

		@Override
		protected void syncCallback() {
			// Print debug info:
			printDebugInfo();

			if (savingSucceeded) {
				// Saving succeeded:

				boolean shopkeepersDeleted = unsavedDeletedShopkeepers.size() > 0;

				// Cleanup the unsavedShopkeepers and unsavedDeletedShopkeepers:
				unsavedShopkeepers.clear();
				unsavedDeletedShopkeepers.clear();

				// Check if we can reuse the last deleted shopkeeper id(s):
				if (shopkeepersDeleted) {
					rollbackNextShopkeeperId();
				}
			} else {
				// Saving failed:

				// Remove any shopkeepers from the unsavedShopkeepers that have been marked as dirty
				// again in the meantime. This is only required if there are shopkeepers that we
				// couldn't save previously, and if this save has been unsuccessful (because
				// otherwise we would completely clear the unsavedShopkeepers).
				if (!unsavedShopkeepers.isEmpty()) {
					dirtyShopkeepers.forEach(shopkeeper -> unsavedShopkeepers.remove(shopkeeper.getId()));
				}

				// We do not mark the shopkeepers, which we failed to save to disk, as dirty again
				// here. Their dirty flags only indicate whether the storage is aware of their
				// latest data changes. Since we already transferred their data to the storage
				// memory, we do not need to do that again during the next save (unless they are
				// marked as dirty again in the meantime).
				// However, for debugging purposes we nevertheless remember these shopkeepers:
				savingDirtyShopkeepers.forEach(shopkeeper -> {
					// If we failed to save the shopkeeper (i.e. failed to transfer its data to the
					// storage's memory), we will keep track of it as part of the dirty shopkeepers.
					// The 'unsaved' shopkeepers only contains the shopkeepers whose data we
					// transferred, but failed to persist.
					if (failedToSave.contains(shopkeeper)) return;

					// Ignore the shopkeeper if it has been marked as dirty again in the meantime:
					if (dirtyShopkeepers.contains(shopkeeper)) return;

					unsavedShopkeepers.add(shopkeeper.getId());
					// Note: If the shopkeeper has been deleted in the meantime, the subsequent
					// processing of the shopkeepersToDelete will remove the shopkeeper again from
					// the unsavedShopkeepers.
				});
			}

			// Regardless of whether the save has been successful:

			// Transfer the shopkeepers that we failed to save to the dirty shopkeepers:
			dirtyShopkeepers.addAll(failedToSave);
			failedToSave.clear();
			// Note: Any shopkeepers that have been deleted in the meantime are removed again from
			// the dirtyShopkeepers when the shopkeepersToDelete are processed in the following.

			// Cleanup the Set of processed dirty shopkeepers:
			savingDirtyShopkeepers.clear();

			// Remove the data of shopkeepers that have been deleted in the meantime:
			shopkeepersToDelete.forEach(SKShopkeeperStorage.this::deleteShopkeeper);
			shopkeepersToDelete.clear();

			// Any other remaining post-processing that should happen after the storage's state has
			// been updated:
			if (!savingSucceeded) {
				// Attempt the save again after a short delay (this requests another save):
				// However, during the final save attempt during plugin disable, this is skipped and
				// data might be lost.
				saveDelayed();

				// Inform admins about the saving issue:
				// Error message throttle of 4 minutes (slightly less than the saving interval).
				long nowMillis = System.currentTimeMillis();
				if (Math.abs(nowMillis - lastSaveErrorMsgMillis) > SAVE_ERROR_MSG_THROTTLE_MILLIS) {
					lastSaveErrorMsgMillis = nowMillis;
					String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED
							+ "Saving shopkeepers failed! "
							+ "Please check the server log and look into the issue!";
					for (Player player : Bukkit.getOnlinePlayers()) {
						assert player != null;
						if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.ADMIN_PERMISSION)) {
							player.sendMessage(errorMsg);
						}
					}
				}
			}
		}

		private void printDebugInfo() {
			Log.debug(() -> {
				StringBuilder sb = new StringBuilder();
				sb.append("Saved shopkeeper data (");

				// Dirty shopkeepers:
				sb.append(savingDirtyShopkeepers.size()).append(" dirty");

				// Previously unsaved shopkeepers:
				if (!unsavedShopkeepers.isEmpty()) {
					sb.append(", ").append(unsavedShopkeepers.size()).append(" previously unsaved");
				}

				// Deleted shopkeepers:
				sb.append(", ").append(unsavedDeletedShopkeepers.size()).append(" deleted");

				// Failed to save:
				if (!failedToSave.isEmpty()) {
					sb.append(", ").append(failedToSave.size()).append(" failed to save");
				}

				// Timing summary:
				sb.append("): ");
				sb.append(this.getExecutionTimingString());

				// Failure indicator:
				if (!savingSucceeded) {
					sb.append(" -- Saving failed!");
				}
				return sb.toString();
			});
		}
	}
}
