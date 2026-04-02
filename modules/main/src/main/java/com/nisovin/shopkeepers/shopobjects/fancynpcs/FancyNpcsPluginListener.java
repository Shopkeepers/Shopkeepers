package com.nisovin.shopkeepers.shopobjects.fancynpcs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import com.nisovin.shopkeepers.dependencies.fancynpcs.FancyNpcsDependency;

class FancyNpcsPluginListener implements Listener {

	private final FancyNpcsShops fancyNpcsShops;

	FancyNpcsPluginListener(FancyNpcsShops fancyNpcsShops) {
		this.fancyNpcsShops = fancyNpcsShops;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPluginEnable(PluginEnableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(FancyNpcsDependency.PLUGIN_NAME)) {
			fancyNpcsShops.enable();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPluginDisable(PluginDisableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(FancyNpcsDependency.PLUGIN_NAME)) {
			fancyNpcsShops.disable();
		}
	}
}
