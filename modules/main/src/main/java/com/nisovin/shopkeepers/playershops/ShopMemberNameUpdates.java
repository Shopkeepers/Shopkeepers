package com.nisovin.shopkeepers.playershops;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Trilean;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class ShopMemberNameUpdates implements Listener {

	private final SKShopkeepersPlugin plugin;

	public ShopMemberNameUpdates(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Update the owner and member information for all players that are already online:
		for (Player player : Bukkit.getOnlinePlayers()) {
			assert player != null;
			if (CitizensUtils.isNPC(player)) continue;

			UUID playerId = player.getUniqueId();
			String playerName = Unsafe.assertNonNull(player.getName());
			this.updateShopkeepersForPlayer(playerId, playerName);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!player.isOnline()) {
			// Ignore if the player is already no longer online (maybe the player was kicked):
			return;
		}

		// Update the owner and member information for this player:
		String playerName = Unsafe.assertNonNull(player.getName());
		this.updateShopkeepersForPlayer(player.getUniqueId(), playerName);
	}

	// Updates owner and member names for the specified player:
	private void updateShopkeepersForPlayer(UUID playerId, String playerName) {
		Log.debug(DebugOptions.playerNameUpdates,
				() -> "Updating shopkeeper owner and member names for: "
						+ TextUtils.getPlayerString(playerName, playerId));

		boolean dirty = false;
		for (AbstractPlayerShopkeeper playerShop : plugin.getShopkeeperRegistry().getAllPlayerShopkeepers()) {
			boolean playerNameUnchanged = false;

			// Update the owner name:
			var ownerNameUpdated = this.updateShopOwnerName(playerShop, playerId, playerName);
			if (ownerNameUpdated == Trilean.TRUE) {
				dirty = true;
			} else if (ownerNameUpdated == Trilean.FALSE) {
				playerNameUnchanged = true;
			} // Else: Not the shop owner. Continue.

			// Update the shop member name:
			var memberNameUpdated = this.updateShopMemberName(playerShop, playerId, playerName);
			if (memberNameUpdated == Trilean.TRUE) {
				dirty = true;
			} else if (memberNameUpdated == Trilean.FALSE) {
				playerNameUnchanged = true;
			} // Else: Not a shop member. Continue.

			// Dirty if we already found a shop with mismatching name:
			if (!dirty && playerNameUnchanged) {
				// The stored player name matches the player's current name.
				// Since we assume that the stored player names are consistent across all
				// shopkeepers, we can abort checking the remaining shops.
				Log.debug(DebugOptions.playerNameUpdates, () -> playerShop.getLogPrefix()
						+ "Player name '" + playerName + "' is up-to-date. "
						+ "Assuming the player name is consistent across all shopkeepers, "
						+ "we skip checking any other shopkeepers.");
				return;
			}
		}

		// Save:
		if (dirty) {
			plugin.getShopkeeperStorage().save();
		}
	}

	// Returns true if the owner name was updated.
	// Returns false if the shop is owned by the specified player, but the name is up-to-date.
	// Returns undefined if the shop is not owned by the specified player.
	private Trilean updateShopOwnerName(PlayerShopkeeper playerShop, UUID playerId, String playerName) {
		if (!playerShop.isOwner(playerId)) {
			return Trilean.UNDEFINED;
		}

		String ownerName = playerShop.getOwnerName();
		if (ownerName.equals(playerName)) {
			return Trilean.FALSE;
		}

		// Update the stored name:
		Log.debug(DebugOptions.playerNameUpdates, () -> playerShop.getLogPrefix()
				+ "Updating owner name '" + ownerName + "' to '" + playerName + "'.");
		playerShop.setOwner(playerId, playerName);

		return Trilean.TRUE;
	}

	// Returns true if the member name was updated.
	// Returns false if the member was found but the stored name equals the specified name.
	// Returns undefined if the shop member was not found.
	private Trilean updateShopMemberName(AbstractPlayerShopkeeper playerShop, UUID playerId, String playerName) {
		var shopMember = playerShop.getMember(playerId);
		if (shopMember == null) {
			return Trilean.UNDEFINED;
		}

		var memberName = shopMember.getUser().getName();
		if (memberName.equals(playerName)) {
			return Trilean.FALSE;
		}

		// Update the stored name (preserve the current permission):
		Log.debug(DebugOptions.playerNameUpdates, () -> playerShop.getLogPrefix()
				+ "Updating member name '" + memberName + "' to '" + playerName + "'.");
		playerShop.updateMember(playerId, playerName, null);

		return Trilean.TRUE;
	}
}
