package com.nisovin.shopkeepers.storage.sql.sqlite;

import java.io.File;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.storage.StorageType;
import com.nisovin.shopkeepers.storage.sql.SQLStorage;

public class SQLiteStorage extends SQLStorage {

	private static final File getDatabaseFile(Plugin plugin) {
		return new File(plugin.getDataFolder(), "shopkeepers-sqlite.db");
	}

	// StorageType gets supplied via argument to allow for extension
	public SQLiteStorage(SKShopkeepersPlugin plugin, StorageType storageType) {
		super(plugin, storageType);
	}

	@Override
	protected SQLiteConnector createConnector() {
		return new SQLiteConnector(LOG_PREFIX, getDatabaseFile(plugin));
	}
}
