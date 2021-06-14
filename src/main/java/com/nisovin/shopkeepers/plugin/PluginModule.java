package com.nisovin.shopkeepers.plugin;

import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.util.Validate;

public abstract class PluginModule<T extends JavaPlugin> {

	protected final T plugin;

	public PluginModule(T plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	protected void onConfigChanged() {
	}
}
