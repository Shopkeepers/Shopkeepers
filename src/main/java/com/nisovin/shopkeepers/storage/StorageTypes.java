package com.nisovin.shopkeepers.storage;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.storage.sql.mysql.MySQLStorage;
import com.nisovin.shopkeepers.storage.sql.sqlite.SQLiteStorage;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;

/**
 * Registry for storage types.
 */
public class StorageTypes extends AbstractTypeRegistry<StorageType> {

	// DEFAULTS
	public static final StorageType SQLITE = new StorageType("sqlite") {
		@Override
		public String getDisplayName() {
			return "SQLite";
		}

		@Override
		public SKStorage createStorage(ShopkeepersPlugin plugin) {
			return new SQLiteStorage((SKShopkeepersPlugin) plugin, this);
		}
	};
	public static final StorageType MYSQL = new StorageType("mysql") {
		@Override
		public String getDisplayName() {
			return "MySQL";
		}

		@Override
		public SKStorage createStorage(ShopkeepersPlugin plugin) {
			return new MySQLStorage((SKShopkeepersPlugin) plugin, this);
		}
	};

	public static final List<StorageType> getAllDefaults() {
		return Arrays.asList(SQLITE, MYSQL);
	}

	public StorageTypes() {
	}

	public void registerDefaults() {
		this.registerAll(getAllDefaults());
	}

	@Override
	protected String getTypeName() {
		return "storage type";
	}
}
