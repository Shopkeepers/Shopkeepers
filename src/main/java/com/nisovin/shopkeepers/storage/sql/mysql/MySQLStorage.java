package com.nisovin.shopkeepers.storage.sql.mysql;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.storage.StorageType;
import com.nisovin.shopkeepers.storage.sql.SQLStorage;

public class MySQLStorage extends SQLStorage {

	// StorageType gets supplied via argument to allow for extension
	public MySQLStorage(SKShopkeepersPlugin plugin, StorageType storageType) {
		super(plugin, storageType);
	}

	@Override
	protected MySQLConnector createConnector() {
		return new MySQLConnector(LOG_PREFIX, Settings.databaseHost, Settings.databasePort,
				Settings.databaseName, Settings.databaseUser, Settings.databasePassword);
	}
}
