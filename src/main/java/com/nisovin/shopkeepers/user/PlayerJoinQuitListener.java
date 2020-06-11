package com.nisovin.shopkeepers.user;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class PlayerJoinQuitListener implements Listener {

	private final SKUserManager userManager;

	PlayerJoinQuitListener(SKUserManager userManager) {
		this.userManager = userManager;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerJoin(PlayerJoinEvent event) {
		userManager.onPlayerJoin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerQuit(PlayerQuitEvent event) {
		userManager.onPlayerQuit(event.getPlayer());
	}
}
