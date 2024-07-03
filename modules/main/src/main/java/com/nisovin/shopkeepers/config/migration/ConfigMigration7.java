package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.addSetting;
import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.removeSetting;

/**
 * Migrates the config from version 6 to version 7.
 */
public class ConfigMigration7 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Remove the log-trades-to-csv setting in favor of being able to configure
		// multiple trade log storage options through trade-log-storage.
		removeSetting(configData, "log-trades-to-csv");
		addSetting(configData, "trade-log-storage", "");
	}
}
