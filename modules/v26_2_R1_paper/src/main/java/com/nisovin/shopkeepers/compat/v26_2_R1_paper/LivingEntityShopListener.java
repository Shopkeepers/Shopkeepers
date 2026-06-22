package com.nisovin.shopkeepers.compat.v26_2_R1_paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.util.java.Validate;

public class LivingEntityShopListener implements Listener {

	private final ShopkeeperRegistry shopkeeperRegistry;

	public LivingEntityShopListener(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin");
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
	}

	// Paper 26.2 has deprecated the PigZapEvent.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityZapEvent(EntityZapEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}
