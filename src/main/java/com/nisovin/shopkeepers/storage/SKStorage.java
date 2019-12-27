package com.nisovin.shopkeepers.storage;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.history.storage.HistoryStorage;
import com.nisovin.shopkeepers.player.profile.storage.PlayerStorage;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public abstract class SKStorage implements Storage {

	public static interface PlayerStorageComponent extends PlayerStorage, StorageComponent {
	}

	public static interface HistoryStorageComponent extends HistoryStorage, StorageComponent {
	}

	public static final String LOG_PREFIX = "[History-Storage] ";

	public static SKStorage setup(SKShopkeepersPlugin plugin) throws StorageException {
		// determine storage type:
		String storageTypeId = Settings.storageType;
		StorageType storageType = plugin.getStorageTypes().match(storageTypeId);
		if (storageType == null) {
			Log.severe(LOG_PREFIX + "Unknown storage type '" + storageTypeId + "'. Defaulting to storage type '"
					+ StorageTypes.SQLITE.getDisplayName() + "'.");
			storageType = StorageTypes.SQLITE;
		}
		assert storageType != null;

		// create storage backend:
		Log.info(LOG_PREFIX + "Storage type: " + storageType.getDisplayName());
		SKStorage storage = storageType.createStorage(plugin);
		Validate.State.notNull(storage, "StorageType '" + storageType.getDisplayName() + "' provided null storage!");
		assert storage != null;

		// setup storage and components:
		storage.setup();

		return storage;
	}

	protected final SKShopkeepersPlugin plugin;
	private final StorageType storageType;

	// components:
	private PlayerStorageComponent playerStorage;
	private HistoryStorageComponent historyStorage;

	protected SKStorage(SKShopkeepersPlugin plugin, StorageType storageType) {
		Validate.notNull(plugin, "Plugin is null!");
		Validate.notNull(storageType, "StorageType is null!");
		this.plugin = plugin;
		this.storageType = storageType;
	}

	@Override
	public final StorageType getType() {
		return storageType;
	}

	public final boolean isShutdown() {
		return (playerStorage != null);
	}

	protected final void validateNotShutdown() {
		Validate.State.isTrue(this.isShutdown(), "Storage is shutdown!");
	}

	protected abstract PlayerStorageComponent createPlayerStorage();

	protected abstract HistoryStorageComponent createHistoryStorage();

	public void setup() throws StorageException {
		// setup common components:
		this.playerStorage = this.createPlayerStorage();
		playerStorage.setup();

		this.historyStorage = this.createHistoryStorage();
		historyStorage.setup();
	}

	public void shutdown() {
		this.validateNotShutdown();
		// reverse order:
		historyStorage.shutdown();
		historyStorage = null;

		playerStorage.shutdown();
		playerStorage = null;
	}

	// COMPONENTS

	// null if shutdown
	@Override
	public PlayerStorage getPlayerStorage() {
		return playerStorage;
	}

	// null if shutdown
	@Override
	public HistoryStorage getHistoryStorage() {
		return historyStorage;
	}

	// TASKS

	// TODO somehow force callers to not forget handling potential exceptions?
	// might not be necessary / too important

	/*public <T> CompletableFuture<T> addTask(Supplier<T> task) {
		Validate.notNull(task, "Task is null!");
		return CompletableFuture.supplyAsync(task, plugin.getAsyncExecutor());
	}*/

	public <T> CompletableFuture<T> addTask(Callable<T> task) {
		Validate.notNull(task, "Task is null!");
		return CompletableFuture.supplyAsync(() -> {
			try {
				return task.call();
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		}, plugin.getAsyncExecutor());
	}

	public CompletableFuture<Void> addTask(Runnable task) {
		Validate.notNull(task, "Task is null!");
		return CompletableFuture.runAsync(task, plugin.getAsyncExecutor());
	}
}
