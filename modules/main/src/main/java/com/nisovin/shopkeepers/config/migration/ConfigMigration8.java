package com.nisovin.shopkeepers.config.migration;

import static com.nisovin.shopkeepers.config.migration.ConfigMigrationHelper.*;

import com.nisovin.shopkeepers.tradelog.TradeLogStorageType;
import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Migrates the config from version 7 to version 8.
 */
public class ConfigMigration8 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Replace the 'log-trades-to-csv' setting in favor of the 'new trade-log-storage' setting:
		// We preserve the enabled logging state.
		boolean logTradesToCsv = configData.getBoolean("log-trades-to-csv");
		removeSetting(configData, "log-trades-to-csv");
		addSetting(
				configData,
				"trade-log-storage",
				logTradesToCsv ? TradeLogStorageType.CSV.name()
						: TradeLogStorageType.DISABLED.name()
		);
	}
}
