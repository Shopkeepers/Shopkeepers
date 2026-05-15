package com.nisovin.shopkeepers.metrics;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Plugin metrics powered by bStats.
 */
public class PluginMetrics {

	private final SKShopkeepersPlugin plugin;

	public PluginMetrics(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		if (Settings.enableMetrics) {
			this.setupMetrics();
		}
	}

	public void onDisable() {
	}

	private void setupMetrics() {
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		Metrics metrics = new Metrics(plugin);
		metrics.addCustomChart(new CitizensChart());
		metrics.addCustomChart(new WorldGuardChart());
		metrics.addCustomChart(new TownyChart());
		metrics.addCustomChart(new VaultEconomyChart());
		metrics.addCustomChart(new GringottsChart());
		metrics.addCustomChart(new ShopkeepersCountChart(shopkeeperRegistry));
		metrics.addCustomChart(new PlayerShopsChart(shopkeeperRegistry));
		metrics.addCustomChart(new FeaturesChart(shopkeeperRegistry));
		metrics.addCustomChart(new WorldsChart(shopkeeperRegistry));
		// TODO Add chart with number of virtual shops?
		// TODO Add chart with the server variant used (CraftBukkit, Spigot, Paper, other..).
	}
}
