package com.nisovin.shopkeepers.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class ShopkeeperEventHelper {

	private ShopkeeperEventHelper() {
	}

	/**
	 * Calls a {@link PlayerDeleteShopkeeperEvent} and debug logs when the event has been cancelled.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper that is deleted
	 * @param player
	 *            the player who is deleting the shopkeeper
	 * @return the {@link PlayerDeleteShopkeeperEvent}
	 */
	public static PlayerDeleteShopkeeperEvent callPlayerDeleteShopkeeperEvent(
			Shopkeeper shopkeeper,
			Player player
	) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notNull(player, "player is null");
		PlayerDeleteShopkeeperEvent event = new PlayerDeleteShopkeeperEvent(shopkeeper, player);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			Log.debug(() -> shopkeeper.getLogPrefix() + "PlayerDeleteShopkeeperEvent for player '"
					+ player.getName() + "' was cancelled.");
		}
		return event;
	}
}
