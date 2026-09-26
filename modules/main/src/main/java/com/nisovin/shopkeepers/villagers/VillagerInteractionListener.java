package com.nisovin.shopkeepers.villagers;

import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopcreation.ShopCreationItem;
import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.ui.villager.editor.VillagerEditorViewProvider;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Handles prevention of trading, hiring and editing of regular villagers (including wandering
 * traders).
 */
public class VillagerInteractionListener implements Listener {

	private final ShopkeepersPlugin plugin;

	public VillagerInteractionListener(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	// HIGH, since we don't want to handle hiring if another plugin has cancelled the event.
	// But not HIGHEST, so that other plugins can still react to us canceling the event.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof AbstractVillager)) return;
		AbstractVillager villager = (AbstractVillager) event.getRightClicked();
		boolean isVillager = (villager instanceof Villager);
		boolean isWanderingTrader = (!isVillager && villager instanceof WanderingTrader);
		if (!isVillager && !isWanderingTrader) return; // Unknown villager sub-type

		if (plugin.getShopkeeperRegistry().isShopkeeper(villager)) {
			// Shopkeeper interaction is handled elsewhere
			return;
		}
		Log.debug("Interaction with non-shopkeeper villager ..");

		if (CitizensUtils.isNPC(villager)) {
			// Ignore any interaction with Citizens NPCs
			Log.debug("  ignoring (probably Citizens) NPC");
			return;
		}

		if ((isVillager && this.isOtherVillagersDisabled(villager.getWorld()))
				|| (isWanderingTrader && this.isWanderingTradersDisabled(villager.getWorld()))) {
			// Prevent trading with non-shopkeeper villagers:
			event.setCancelled(true);
			Log.debug("  trading prevented");
		}

		Player player = event.getPlayer();

		// We only react to main hand events.
		// However, unlike vanilla Minecraft, Forge clients might send additional off-hand
		// interactions, even if the previous main-hand interaction was cancelled. This breaks the
		// villager editor, because it immediately closes the villager editor again and opens the
		// regular villager trading interface instead (see Shopkeepers#728). In an attempt to
		// resolve this incompatibility, we therefore cancel these off-hand interactions if the
		// player already has some inventory open currently.
		if (event.getHand() != EquipmentSlot.HAND) {
			if (InventoryUtils.hasInventoryOpen(player)) {
				event.setCancelled(true);
				Log.debug("  off-hand interaction prevented due to open inventory");
			}
			return;
		}

		boolean overrideTrading = false;
		if (this.handleEditRegularVillager(player, villager)) {
			// Villager editor for regular villagers.
			overrideTrading = true;
		} else if (this.handleHireOtherVillager(player, villager)) {
			// Hiring of regular villagers.
			overrideTrading = true;
		}

		if (overrideTrading) {
			// The villager interaction resulted in some action that overrides the default trading
			// behavior:
			event.setCancelled(true);
		}
	}

	private boolean isOtherVillagersDisabled(World world) {
		return Settings.disableOtherVillagers
				&& (Settings.disableOtherVillagersWorlds.isEmpty()
						|| Settings.disableOtherVillagersWorlds.contains(world.getName()));
	}

	private boolean isWanderingTradersDisabled(World world) {
		return Settings.disableWanderingTraders
				&& (Settings.disableWanderingTradersWorlds.isEmpty()
						|| Settings.disableWanderingTradersWorlds.contains(world.getName()));
	}

	private boolean handleEditRegularVillager(Player player, AbstractVillager villager) {
		if (!player.isSneaking()) return false;
		if ((Settings.editRegularVillagers && villager instanceof Villager)
				|| (Settings.editRegularWanderingTraders && villager instanceof WanderingTrader)) {
			Log.debug("  possible villager editor request ..");
			// Open the villager editor:
			// Silent request (fails if the player is missing the permission):
			var villagerEditorViewProvider = new VillagerEditorViewProvider(villager);
			boolean uiOpened = UISessionManager.getInstance().requestUI(
					villagerEditorViewProvider,
					player,
					true
			);
			if (uiOpened) {
				Log.debug("    ..success (normal trading prevented).");
				return true;
			} else {
				Log.debug("    ..no access (probably missing permission).");
			}
		}
		return false;
	}

	private boolean isHireOtherVillagersEnabled(World world) {
		return Settings.hireOtherVillagers
				&& (Settings.hireOtherVillagersWorlds.isEmpty()
						|| Settings.hireOtherVillagersWorlds.contains(world.getName()));
	}

	private boolean isHireWanderingTradersEnabled(World world) {
		return Settings.hireWanderingTraders
				&& (Settings.hireWanderingTradersWorlds.isEmpty()
						|| Settings.hireWanderingTradersWorlds.contains(world.getName()));
	}

	private boolean isHireVillagerEnabled(AbstractVillager villager) {
		if (villager instanceof Villager) {
			return this.isHireOtherVillagersEnabled(villager.getWorld());
		} else if (villager instanceof WanderingTrader) {
			return this.isHireWanderingTradersEnabled(villager.getWorld());
		} else {
			return false;
		}
	}

	// Returns false, if the player wasn't able to hire this villager.
	private boolean handleHireOtherVillager(Player player, AbstractVillager villager) {
		if (!this.isHireVillagerEnabled(villager)) {
			return false;
		}

		Log.debug("  possible hire ..");

		// Check if the player is allowed to remove (attack) the entity (in case the entity is
		// protected by another plugin).
		Log.debug("    checking villager access.");
		if (!this.checkEntityAccess(player, villager)) {
			Log.debug("    ..no permission to remove villager.");
			return false;
		}

		// Hire him if holding his hiring item.
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (!Settings.hireItem.matches(itemInMainHand)) {
			// TODO Show hire item via hover event?
			TextUtils.sendMessage(player, Messages.villagerForHire,
					"costs", Settings.hireOtherVillagersCosts,
					"hire-item", Settings.hireItem.getType().name()
			); // TODO Also print required hire item name and lore?
			Log.debug("    ..not holding hire item.");
			return false;
		}

		// Check if the player has enough of those hiring items:
		final int costs = Settings.hireOtherVillagersCosts;
		if (costs > 0) {
			@Nullable ItemStack @NonNull [] storageContents = Unsafe.cast(
					playerInventory.getStorageContents()
			);
			if (!InventoryUtils.containsAtLeast(storageContents, Settings.hireItem, costs)) {
				TextUtils.sendMessage(player, Messages.cannotHire);
				Log.debug("    ..not holding enough hire items.");
				return false;
			}

			Log.debug("  Villager hiring: The player has the needed amount of hiring items.");
			int inHandAmount = itemInMainHand.getAmount();
			int remaining = inHandAmount - costs;
			Log.debug(() -> "  Villager hiring: in hand=" + inHandAmount + " costs=" + costs
					+ " remaining=" + remaining);
			if (remaining > 0) {
				itemInMainHand.setAmount(remaining);
			} else { // remaining <= 0
				playerInventory.setItemInMainHand(null); // Remove item in hand
				if (remaining < 0) {
					// Also clear the held item slot in the captured storage contents:
					storageContents[playerInventory.getHeldItemSlot()] = null;

					// Remove remaining costs from inventory:
					InventoryUtils.removeItems(storageContents, Settings.hireItem, -remaining);
					// Apply the change to the player's inventory:
					InventoryUtils.setStorageContents(playerInventory, storageContents);
				}
			}
		}

		// Give the player the shop creation item:
		ItemStack shopCreationItem = ShopCreationItem.create();
		Map<Integer, ItemStack> remaining = playerInventory.addItem(shopCreationItem);
		if (!remaining.isEmpty()) {
			villager.getWorld().dropItem(villager.getLocation(), shopCreationItem);
		}

		// Remove the entity:
		// Note: The leashed trader llamas for the wandering trader will break and the llamas will
		// remain.
		villager.remove();

		// Update client's inventory:
		player.updateInventory();

		TextUtils.sendMessage(player, Messages.hired);
		Log.debug("    ..success (normal trading prevented).");
		return true;
	}

	// Returns true if the player can access (remove / attack) this entity.
	private boolean checkEntityAccess(Player player, Entity entity) {
		TestEntityDamageByEntityEvent fakeDamageEvent = new TestEntityDamageByEntityEvent(
				player,
				entity
		);
		plugin.getServer().getPluginManager().callEvent(fakeDamageEvent);
		return !fakeDamageEvent.isCancelled();
	}
}
