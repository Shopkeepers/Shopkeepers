package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.PigZombie;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.CreeperPowerEvent.PowerCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Extends the base entity shop listener with event handlers specific to living entities.
 */
class LivingEntityShopListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;

	LivingEntityShopListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
	}

	void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onCreeperCharged(CreeperPowerEvent event) {
		if (event.getCause() != PowerCause.LIGHTNING) return;
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityTransformEvent(EntityTransformEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onSheepDyed(SheepDyeWoolEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPotionSplash(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities()) {
			assert entity != null;
			if (shopkeeperRegistry.isShopkeeper(entity)) {
				event.setIntensity(entity, 0.0D);
			}
		}
	}

	// Prevent shopkeeper entities from being affected by potion effects:
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() != Action.ADDED) return;

		AbstractShopkeeper shopkeeper = shopkeeperRegistry.getShopkeeperByEntity(event.getEntity());
		if (shopkeeper == null) return;

		// Ignore the default potion effects:
		ShopObject shopObject = shopkeeper.getShopObject();
		if (shopObject instanceof SKLivingShopObject<?>) {
			SKLivingShopObject<?> livingShopObject = (SKLivingShopObject<?>) shopObject;
			if (livingShopObject.getDefaultPotionEffects().contains(event.getNewEffect())) {
				return;
			}
		}

		event.setCancelled(true);
	}

	// Prevent shopkeeper entities (e.g. allays) from picking up items.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityPickupItemEvent(EntityPickupItemEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Prevent shopkeeper mobs from receiving items from dispensers.
	@EventHandler()
	void onBlockDispenseArmorEvent(BlockDispenseArmorEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getTargetEntity())) {
			event.setCancelled(true);
		}
	}

	// Allow sleeping if the only nearby monsters are shopkeepers:
	// Note: Cancellation state also reflects default behavior.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerEnterBed(PlayerBedEnterEvent event) {
		// Bed entering prevented due to nearby monsters?
		if (event.getBedEnterResult() != BedEnterResult.NOT_SAFE) return;

		// Find nearby monsters that prevent bed entering (see MC EntityHuman):
		Block bedBlock = event.getBed();
		Collection<Entity> monsters = Unsafe.castNonNull(bedBlock.getWorld().getNearbyEntities(
				bedBlock.getLocation(),
				8.0D, 5.0D, 8.0D,
				(entity) -> {
					// TODO Bukkit API to check if monster prevents sleeping?
					// E.g. PigZombies only prevent sleeping if they are angered.
					if (!(entity instanceof Monster)) return false;
					if (entity instanceof PigZombie) {
						return ((PigZombie) entity).isAngry();
					}
					return true;
				}
		));

		for (Entity entity : monsters) {
			if (!shopkeeperRegistry.isShopkeeper(entity)) {
				// Found non-shopkeeper entity. Do nothing (keep bed entering prevented):
				return;
			}
		}
		// Sleeping is only prevented due to nearby shopkeepers. -> Bypass and allow sleeping:
		Log.debug(() -> "Allowing sleeping of player '" + event.getPlayer().getName()
				+ "': The only nearby monsters are shopkeepers.");
		event.setUseBed(Result.ALLOW);
	}
}
