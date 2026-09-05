package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Handles the expiration of player shops and expiration notifications.
 */
public class PlayerShopsExpiration {

	/**
	 * Updates the {@link DerivedSettings#playerShopExpirationNotificationThresholds} setting.
	 * <p>
	 * This is called on configuration changes.
	 * 
	 * @param invalidThresholdCallback
	 *            this callback is invoked for invalid notification thresholds, not
	 *            <code>null</code>
	 */
	public static void updateExpirationNotificationThresholds(
			Consumer<? super String> invalidThresholdCallback
	) {
		Validate.notNull(invalidThresholdCallback, "invalidThresholdCallback is null");
		String thresholdsSetting = Settings.playerShopExpirationNotificationThresholds;
		List<Duration> thresholds = DerivedSettings.playerShopExpirationNotificationThresholds;

		// Clear the previous thresholds:
		thresholds.clear();

		// Add the parsed thresholds, ignoring duplicates:
		// The thresholds are configured in minutes.
		for (String thresholdEntry : StringUtils.removeWhitespace(thresholdsSetting).split(",")) {
			if (thresholdEntry.isEmpty()) continue;

			Integer minutes = ConversionUtils.parseInt(thresholdEntry);
			if (minutes == null || minutes <= 0) {
				invalidThresholdCallback.accept(thresholdEntry);
				continue;
			}

			Duration threshold = Duration.ofMinutes(minutes);
			if (!thresholds.contains(threshold)) {
				thresholds.add(threshold);
			}
		}

		// Sort the thresholds in ascending order, i.e. from the most to the least urgent:
		Collections.sort(thresholds);
	}

	/**
	 * Called on settings changes.
	 * <p>
	 * For example marks all player shopkeeper expirations as outdated, because the configured
	 * expiration durations might have changed.
	 */
	public static void onSettingsChanged() {
		var shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();
		for (AbstractPlayerShopkeeper playerShopkeeper : shopkeeperRegistry.getAllPlayerShopkeepers()) {
			playerShopkeeper.markExpirationOutdated();
		}
	}

	private final ShopExpirationNotifier notifier;
	private final ShopExpirationCheckTask expirationCheckTask;

	public PlayerShopsExpiration(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.notifier = new ShopExpirationNotifier(plugin);
		this.expirationCheckTask = new ShopExpirationCheckTask(plugin, notifier);
	}

	public void onEnable() {
		if (!this.isEnabled()) return; // Feature is disabled

		// Check for expired shops periodically:
		expirationCheckTask.start();
	}

	public void onDisable() {
		expirationCheckTask.stop();
	}

	private boolean isEnabled() {
		return Settings.playerShopExpirationDays > 0 || Settings.hiredPlayerShopExpirationDays > 0;
	}

	/**
	 * Gets the {@link ShopExpirationNotifier}.
	 * 
	 * @return the {@link ShopExpirationNotifier}, not <code>null</code>
	 */
	public ShopExpirationNotifier getNotifier() {
		return notifier;
	}
}
