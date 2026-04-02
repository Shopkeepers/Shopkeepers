package com.nisovin.shopkeepers.shopobjects.fancynpcs;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.events.NpcRemoveEvent;
import de.oliver.fancynpcs.api.events.NpcSpawnEvent;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Listens to FancyNpcs events to keep shopkeeper data in sync.
 * <p>
 * This is the FancyNpcs equivalent of {@code CitizensListener}.
 * </p>
 */
class FancyNpcsListener implements Listener {

	private final ShopkeepersPlugin plugin;
	private final FancyNpcsShops fancyNpcsShops;

	FancyNpcsListener(ShopkeepersPlugin plugin, FancyNpcsShops fancyNpcsShops) {
		assert plugin != null && fancyNpcsShops != null;
		this.plugin = plugin;
		this.fancyNpcsShops = fancyNpcsShops;
	}

	void onEnable() {
	}

	void onDisable() {
	}

	// NPC INTERACTION

	@EventHandler(priority = EventPriority.LOWEST)
	public void onNpcInteract(NpcInteractEvent event) {
		Npc npc = event.getNpc();
		if (npc == null) return;

		Shopkeeper shopkeeper = fancyNpcsShops.getShopkeeper(npc);
		if (shopkeeper == null) return;

		// Cancel FancyNpcs's own action handling so we can handle the interaction ourselves:
		event.setCancelled(true);

		Player player = event.getPlayer();
		if (player == null) return;

		Log.debug(() -> shopkeeper.getLogPrefix() + "FancyNpc NPC has been interacted with by " + player.getName() + ".");

		// Open the shop or editor GUI:
		((AbstractShopkeeper) shopkeeper).onPlayerInteraction(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onNpcSpawn(NpcSpawnEvent event) {
		Npc npc = event.getNpc();
		if (npc == null) return;

		Shopkeeper shopkeeper = fancyNpcsShops.getShopkeeper(npc);
		if (shopkeeper == null) return;

		Log.debug(() -> shopkeeper.getLogPrefix() + "FancyNpc NPC has been spawned.");

		SKFancyNpcShopObject shopObject = (SKFancyNpcShopObject) shopkeeper.getShopObject();
		Player player = event.getPlayer();
		// FancyNpcs spawns NPCs per-player (packet-based), not with actual Bukkit entities.
		// We update the shopkeeper location based on NPC data location if location changed.
		Location npcLoc = npc.getData().getLocation();
		if (npcLoc != null) {
			shopObject.onNpcTeleport(npcLoc);
		}
	}

	// NPC DELETION

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNpcRemove(NpcRemoveEvent event) {
		Npc npc = event.getNpc();
		if (npc == null) return;

		List<? extends Shopkeeper> shopkeepers = fancyNpcsShops.getShopkeepers(npc);
		if (!shopkeepers.isEmpty()) {
			new ArrayList<>(shopkeepers).forEach(shopkeeper -> {
				assert shopkeeper.getShopObject() instanceof SKFancyNpcShopObject;
				// Handle without player (NpcDeleteEvent doesn't provide who deleted it directly):
				((SKFancyNpcShopObject) shopkeeper.getShopObject()).onNpcDeleted(null);
			});
		}
	}
}
