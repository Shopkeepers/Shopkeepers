package com.nisovin.shopkeepers.debug.events;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Tool to debug event handlers.
 */
public class EventDebugger {

	private final SKShopkeepersPlugin plugin;

	public EventDebugger(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		if (Settings.debug) {
			// Register debug listener if enabled:
			// Run delayed to also catch events / event listeners of other plugins.
			SchedulerUtils.runAsyncTaskLaterOrOmit(() -> {
				boolean logAllEvent = Debug.isDebugging(DebugOptions.logAllEvents);
				boolean printListeners = Debug.isDebugging(DebugOptions.printListeners);
				if (logAllEvent || printListeners) {
					DebugListener.register(logAllEvent, printListeners);
				}
			}, 10L);
		}
	}

	public void onDisable() {
	}
}
