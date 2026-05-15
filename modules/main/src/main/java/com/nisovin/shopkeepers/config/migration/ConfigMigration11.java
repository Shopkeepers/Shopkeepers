package com.nisovin.shopkeepers.config.migration;

import java.util.Collections;

import com.nisovin.shopkeepers.config.lib.value.types.StringListValue;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Migrates the config from version 10 to version 11.
 */
public class ConfigMigration11 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		ConfigMigrationHelper.migrateSetting(
				configData,
				"prevent-trading-while-owner-is-online",
				"prevent-trading-while-member-is-online"
		);
		ConfigMigrationHelper.migrateSetting(
				configData,
				"notify-shop-owners-about-trades",
				"notify-shop-members-about-trades"
		);
		ConfigMigrationHelper.migrateSetting(
				configData,
				"shop-owner-trade-notification-sound",
				"shop-member-trade-notification-sound"
		);

		var debugOptions = StringListValue.INSTANCE.load(
				configData,
				"debug-options",
				Collections.emptyList()
		);
		assert debugOptions != null;

		var ownerNameUpdatesIndex = debugOptions.indexOf("owner-name-updates");
		if (ownerNameUpdatesIndex >= 0) {
			Log.info("  Migrating debug option 'owner-name-updates' to 'player-name-updates'.");
			debugOptions.set(ownerNameUpdatesIndex, "player-name-updates");
		}
	}
}
