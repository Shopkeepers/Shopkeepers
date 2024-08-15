package com.nisovin.shopkeepers.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensDependency;
import com.nisovin.shopkeepers.dependencies.towny.TownyDependency;
import com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency;
import com.nisovin.shopkeepers.util.java.MapUtils;

/**
 * Reports usage of various features.
 * <p>
 * TODO use a bar chart once it becomes available again
 */
public class FeaturesChart extends Metrics.DrilldownPie {

	public FeaturesChart() {
		super("used_features", () -> {
			Map<String, Map<String, Integer>> allFeatures = new LinkedHashMap<>();
			// Plugin compatibility features:
			addFeatureEntry(
					allFeatures,
					"check-shop-interaction-result",
					Settings.checkShopInteractionResult
			);
			addFeatureEntry(
					allFeatures,
					"bypass-spawn-blocking",
					Settings.bypassSpawnBlocking
			);
			addFeatureEntry(
					allFeatures,
					"enable-world-guard-restrictions",
					Settings.enableWorldGuardRestrictions && WorldGuardDependency.isPluginEnabled()
			);
			addFeatureEntry(
					allFeatures,
					"require-world-guard-allow-shop-flag",
					Settings.requireWorldGuardAllowShopFlag && WorldGuardDependency.isPluginEnabled()
			);
			addFeatureEntry(
					allFeatures,
					"enable-towny-restrictions",
					Settings.enableTownyRestrictions && TownyDependency.isPluginEnabled()
			);
			addFeatureEntry(
					allFeatures,
					"enable-citizen-shops",
					Settings.enableCitizenShops && CitizensDependency.isPluginEnabled()
			);

			// Mob behavior features:
			addFeatureEntry(
					allFeatures,
					"disable-gravity",
					Settings.disableGravity
			);
			addFeatureEntry(
					allFeatures,
					"gravity-chunk-range",
					Settings.gravityChunkRange
			);
			addFeatureEntry(
					allFeatures,
					"mob-behavior-tick-period",
					Settings.mobBehaviorTickPeriod
			);

			// Others:
			addFeatureEntry(
					allFeatures,
					"save-instantly",
					Settings.saveInstantly
			);
			addFeatureEntry(
					allFeatures,
					"colored names allowed",
					Settings.nameRegex.contains("&")
			);
			addFeatureEntry(
					allFeatures,
					"protect-containers",
					Settings.protectContainers
			);
			addFeatureEntry(
					allFeatures,
					"prevent-item-movement",
					Settings.preventItemMovement
			);
			addFeatureEntry(
					allFeatures,
					"delete-shopkeeper-on-break-container",
					Settings.deleteShopkeeperOnBreakContainer
			);
			addFeatureEntry(
					allFeatures,
					"player-shopkeeper-inactive-days",
					Settings.playerShopkeeperInactiveDays > 0
			);
			addFeatureEntry(
					allFeatures,
					"tax-rate",
					Settings.taxRate > 0
			);
			addFeatureEntry(
					allFeatures,
					"use-strict-item-comparison",
					Settings.useStrictItemComparison
			);
			addFeatureEntry(
					allFeatures,
					"trade-log-storage",
					Settings.tradeLogStorage
			);
			addFeatureEntry(
					allFeatures,
					"notify-players-about-trades",
					Settings.notifyPlayersAboutTrades
			);
			addFeatureEntry(
					allFeatures,
					"notify-shop-owners-about-trades",
					Settings.notifyShopOwnersAboutTrades
			);
			addFeatureEntry(
					allFeatures,
					"disable-other-villagers",
					Settings.disableOtherVillagers
			);
			addFeatureEntry(
					allFeatures,
					"block-villager-spawns",
					Settings.blockVillagerSpawns
			);
			addFeatureEntry(
					allFeatures,
					"hire-other-villagers",
					Settings.hireOtherVillagers
			);
			addFeatureEntry(
					allFeatures,
					"increment-villager-statistics",
					Settings.incrementVillagerStatistics
			);
			return Unsafe.cast(allFeatures);
		});
	}

	// Converts the given boolean value to a more user-friendly 'Yes'/'No' value.
	private static void addFeatureEntry(
			Map<String, Map<String, Integer>> allFeatures,
			String featureName,
			boolean value
	) {
		assert allFeatures != null && featureName != null;
		addFeatureEntry(allFeatures, featureName, value ? "Yes" : "No");
	}

	// Uses the String representation of the given object as value in the chart.
	private static void addFeatureEntry(
			Map<String, Map<String, Integer>> allFeatures,
			String featureName,
			Object value
	) {
		assert allFeatures != null && featureName != null;
		allFeatures.put(featureName, MapUtils.createMap(String.valueOf(value), 1));
	}
}
