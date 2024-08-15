package com.nisovin.shopkeepers.config.migration;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class ConfigMigrations {

	private static final String CONFIG_VERSION_KEY = "config-version";
	private static final int FIRST_VERSION = 0; // Also applies if the config version is missing

	// Each index corresponds to a source config version and its migration to the next version
	private static final List<? extends ConfigMigration> migrations = Arrays.asList(
			new ConfigMigration1(),
			new ConfigMigration2(),
			new ConfigMigration3(),
			new ConfigMigration4(),
			new ConfigMigration5(),
			new ConfigMigration6(),
			new ConfigMigration7(),
			new ConfigMigration8()
	);

	public static int getLatestVersion() {
		return migrations.size();
	}

	// Returns true if any migrations were applied (if the config data might have changed)
	public static boolean applyMigrations(DataContainer configData) throws ConfigLoadException {
		// No migrations are required if the config is empty (missing entries will get generated
		// from defaults):
		if (configData.isEmpty()) return false;

		// Parse config version:
		int configVersion;
		if (!configData.contains(CONFIG_VERSION_KEY)) {
			Log.info("Missing config version. Assuming version '" + FIRST_VERSION + "'.");
			configVersion = FIRST_VERSION;
		} else {
			Object rawConfigVersion = configData.get(CONFIG_VERSION_KEY);
			Integer configVersionI = ConversionUtils.toInteger(rawConfigVersion);
			if (configVersionI == null) { // Parsing failed
				throw new ConfigLoadException("Could not parse config version: "
						+ rawConfigVersion);
			} else {
				configVersion = configVersionI.intValue();
			}
		}

		// Check bounds:
		if (configVersion < FIRST_VERSION) {
			throw new ConfigLoadException("Invalid config version: " + configVersion);
		}
		if (configVersion > getLatestVersion()) {
			throw new ConfigLoadException("Invalid config version: " + configVersion
					+ " (the latest version is " + getLatestVersion() + ")");
		}

		// Apply required migrations:
		boolean migrated = false;
		for (int version = configVersion; version < migrations.size(); ++version) {
			int nextVersion = (version + 1);
			ConfigMigration migration = migrations.get(version);
			assert migration != null;

			Log.info("Migrating config from version " + version + " to version " + nextVersion
					+ " ...");
			try {
				migration.apply(configData);
			} catch (Exception e) {
				throw new ConfigLoadException("Config migration failed with an error!", e);
			}

			// Update config version:
			configData.set(CONFIG_VERSION_KEY, nextVersion);
			migrated = true;
			Log.info("Config migrated to version " + nextVersion + ".");
		}
		return migrated;
	}

	private ConfigMigrations() {
	}
}
