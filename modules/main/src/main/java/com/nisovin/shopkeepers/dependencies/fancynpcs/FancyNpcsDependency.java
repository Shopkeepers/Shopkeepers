package com.nisovin.shopkeepers.dependencies.fancynpcs;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class FancyNpcsDependency {

	public static final String PLUGIN_NAME = "FancyNpcs";

	public static @Nullable Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
			return false;
		}
		try {
			Class.forName("de.oliver.fancynpcs.api.NpcData");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private FancyNpcsDependency() {
	}
}
