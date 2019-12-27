package com.nisovin.shopkeepers.player.profile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class PlayerJoinQuitListener implements Listener {

	private final SKPlayerProfiles playerProfiles;

	PlayerJoinQuitListener(SKPlayerProfiles playerProfiles) {
		this.playerProfiles = playerProfiles;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		playerProfiles.onPlayerJoin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerQuit(PlayerQuitEvent event) {
		playerProfiles.onPlayerQuit(event.getPlayer());
	}
}
