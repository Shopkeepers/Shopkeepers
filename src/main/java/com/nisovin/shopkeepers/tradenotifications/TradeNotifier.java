package com.nisovin.shopkeepers.tradenotifications;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.util.Validate;

public class TradeNotifier {

	private final Plugin plugin;

	public TradeNotifier(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}
}
