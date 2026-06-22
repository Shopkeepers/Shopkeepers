package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PigZapEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;

/**
 * Conditionally registered PigZapEvent listener. Not used in Paper 26.2+.
 */
class PigZapListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;

	PigZapListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
	}

	void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	// Derives from EntityTransformEvent but has its own HandlerList.
	// Paper 26.2: Deprecated in favor of Paper-specific EntityZapEvent.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPigZap(PigZapEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}
