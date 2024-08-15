package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

/**
 * Migrates the config from version 7 to version 8.
 */
public class ConfigMigration8 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Remove the log-trades-to-csv setting in favor of being able to configure the trade log
		// storage via the new trade-log-storage:
		removeSetting(configData, "log-trades-to-csv");
		addSetting(configData, "trade-log-storage", "");
	}
}
