package com.nisovin.shopkeepers.config.migration;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 11 to version 12.
 */
public class ConfigMigration12 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		ConfigMigrationHelper.migrateSetting(
				configData,
				"enable-container-option-on-player-shop",
				"enable-player-shop-open-container"
		);
	}
}
