package com.nisovin.shopkeepers.user;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class PlayerJoinQuitListener implements Listener {

	private final SKUserManager userManager;

	PlayerJoinQuitListener(SKUserManager userManager) {
		this.userManager = userManager;
	}

	// Note: When an already connected player connects again, we receive a login event before we receive the
	// PlayerQuitEvent for the old player getting disconnected.
	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != Result.ALLOWED) return; // ignored
		// TODO load user and wait
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
