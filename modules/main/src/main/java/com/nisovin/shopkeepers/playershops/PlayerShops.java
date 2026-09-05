package com.nisovin.shopkeepers.playershops;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.playershops.expiration.PlayerShopsExpiration;
import com.nisovin.shopkeepers.playershops.inactivity.PlayerInactivity;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Functionality related to player shops.
 */
public class PlayerShops {

	private final PlayerShopsLimit playerShopsLimit;
	private final PlayerInactivity playerInactivity;
	private final PlayerShopsExpiration playerShopsExpiration;
	private final ShopMemberNameUpdates shopMemberNameUpdates;

	public PlayerShops(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.playerShopsLimit = new PlayerShopsLimit();
		this.playerInactivity = new PlayerInactivity(plugin);
		this.playerShopsExpiration = new PlayerShopsExpiration(plugin);
		this.shopMemberNameUpdates = new ShopMemberNameUpdates(plugin);
	}

	public void onEnable() {
		playerShopsLimit.onEnable();
		playerInactivity.onEnable();
		playerShopsExpiration.onEnable();
		shopMemberNameUpdates.onEnable();
	}

	public void onDisable() {
		playerShopsLimit.onDisable();
		playerInactivity.onDisable();
		playerShopsExpiration.onDisable();
		shopMemberNameUpdates.onDisable();
	}

	public PlayerShopsLimit getPlayerShopsLimit() {
		return playerShopsLimit;
	}

	public PlayerInactivity getPlayerInactivity() {
		return playerInactivity;
	}

	public PlayerShopsExpiration getPlayerShopsExpiration() {
		return playerShopsExpiration;
	}

	public ShopMemberNameUpdates getShopMemberNameUpdates() {
		return shopMemberNameUpdates;
	}
}
