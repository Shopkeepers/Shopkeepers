package com.nisovin.shopkeepers.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.util.MapUtils;

/**
 * Reports usage of various features.
 * <p>
 * TODO use a bar chart once it becomes available again
 */
public class FeaturesChart extends Metrics.DrilldownPie {

	public FeaturesChart() {
		super("used_features", () -> {
			Map<String, Map<String, Integer>> allFeatures = new LinkedHashMap<>();
			// plugin compatibility features:
			addFeatureEntry(allFeatures, "check-shop-interaction-result", Settings.checkShopInteractionResult);
			addFeatureEntry(allFeatures, "bypass-spawn-blocking", Settings.bypassSpawnBlocking);
			addFeatureEntry(allFeatures, "enable-spawn-verifier", Settings.enableSpawnVerifier);
			addFeatureEntry(allFeatures, "enable-world-guard-restrictions", Settings.enableWorldGuardRestrictions && WorldGuardHandler.isPluginEnabled());
			addFeatureEntry(allFeatures, "require-world-guard-allow-shop-flag", Settings.requireWorldGuardAllowShopFlag && WorldGuardHandler.isPluginEnabled());
			addFeatureEntry(allFeatures, "enable-towny-restrictions", Settings.enableTownyRestrictions && TownyHandler.isPluginEnabled());
			addFeatureEntry(allFeatures, "enable-citizen-shops", Settings.enableCitizenShops && CitizensHandler.isPluginEnabled());

			// mob behavior features:
			addFeatureEntry(allFeatures, "use-legacy-mob-behavior", Settings.useLegacyMobBehavior);
			addFeatureEntry(allFeatures, "disable-gravity", Settings.disableGravity);
			addFeatureEntry(allFeatures, "increased gravity chunk range", Settings.gravityChunkRange > 4);
			addFeatureEntry(allFeatures, "decreased gravity chunk range", Settings.gravityChunkRange < 4);

			// others:
			addFeatureEntry(allFeatures, "save-instantly", Settings.saveInstantly);
			addFeatureEntry(allFeatures, "colored names allowed", Settings.nameRegex.contains("&"));
			addFeatureEntry(allFeatures, "protect-chests", Settings.protectChests);
			addFeatureEntry(allFeatures, "prevent-item-movement", Settings.preventItemMovement);
			addFeatureEntry(allFeatures, "delete-shopkeeper-on-break-chest", Settings.deleteShopkeeperOnBreakChest);
			addFeatureEntry(allFeatures, "player-shopkeeper-inactive-days", Settings.playerShopkeeperInactiveDays > 0);
			addFeatureEntry(allFeatures, "tax-rate", Settings.taxRate > 0);
			addFeatureEntry(allFeatures, "use-strict-item-comparison", Settings.useStrictItemComparison);
			addFeatureEntry(allFeatures, "disable-other-villagers", Settings.disableOtherVillagers);
			addFeatureEntry(allFeatures, "block-villager-spawns", Settings.blockVillagerSpawns);
			addFeatureEntry(allFeatures, "hire-other-villagers", Settings.hireOtherVillagers);
			addFeatureEntry(allFeatures, "increment-villager-statistics", Settings.incrementVillagerStatistics);
			addFeatureEntry(allFeatures, "trading-history", Settings.enableTradingHistory);
			addFeatureEntry(allFeatures, "trading-history-omit-item-data", Settings.tradingHistoryOmitItemData);
			return allFeatures;
		});
	}

	private static void addFeatureEntry(Map<String, Map<String, Integer>> allFeatures, String featureName, boolean featureActive) {
		assert allFeatures != null && featureName != null;
		allFeatures.put(featureName, (featureActive ? MapUtils.createMap("Yes", 1) : MapUtils.createMap("No", 1)));
	}
}
