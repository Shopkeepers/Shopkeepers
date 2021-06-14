package com.nisovin.shopkeepers.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginModuleList {

	private final List<PluginModule<?>> modules = new ArrayList<>();

	public PluginModuleList() {
	}

	public void add(PluginModule<?> module) {
		modules.add(module);
	}

	public void remove(PluginModule<?> module) {
		modules.remove(module);
	}

	public void clear() {
		modules.clear();
	}

	public void onEnable() {
		modules.forEach(PluginModule::onEnable);
	}

	public void onDisable() {
		// Disable in reverse order:
		List<PluginModule<?>> reversedModules = new ArrayList<>(modules);
		Collections.reverse(reversedModules);
		reversedModules.forEach(PluginModule::onDisable);
	}

	public void onConfigChanged() {
		modules.forEach(PluginModule::onConfigChanged);
	}
}
