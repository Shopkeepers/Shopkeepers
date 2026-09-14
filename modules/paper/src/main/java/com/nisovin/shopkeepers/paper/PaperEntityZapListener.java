package com.nisovin.shopkeepers.paper;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Prevents the transformation of shopkeeper entities that are struck by lightning.
 * <p>
 * This replaces the plugin core's PigZapEvent handler on the Paper server versions that have
 * deprecated the PigZapEvent in favor of the Paper-specific EntityZapEvent.
 */
class PaperEntityZapListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	PaperEntityZapListener(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin");
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
	}

	void onEnable() {
		// In newer Paper versions, we handle the EntityZapEvent instead of the deprecated
		// PigZapEvent:
		if (!Compat.getProvider().isHandlePigZapEvent()) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityZapEvent(EntityZapEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}
