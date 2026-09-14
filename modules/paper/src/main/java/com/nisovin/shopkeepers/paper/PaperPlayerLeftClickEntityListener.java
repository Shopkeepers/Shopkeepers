package com.nisovin.shopkeepers.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopobjects.entity.base.BaseEntityShops;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.java.Validate;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/**
 * Detects left clicks on entities via Paper's {@link PrePlayerAttackEntityEvent}.
 * <p>
 * Note: We cannot use {@link EntityDamageByEntityEvent} for this, because the event is not called
 * in certain cases, including for shopkeeper mobs, because we intentionally mark them as
 * {@link LivingEntity#setInvulnerable(boolean) invulnerable} to disable certain other default mob
 * behaviors. On Spigot servers, i.e. without this event, a different workaround is used.
 */
class PaperPlayerLeftClickEntityListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final BaseEntityShops entityShops;

	PaperPlayerLeftClickEntityListener(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin");
		this.plugin = plugin;
		this.entityShops = plugin.getEntityShops();
	}

	void onEnable() {
		if (!Settings.enableLeftClickInteraction) {
			return;
		}

		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Similar to the normal interaction events, we handle this event before any other plugins:
		EventUtils.enforceExecuteFirst(
				PrePlayerAttackEntityEvent.class,
				EventPriority.LOWEST,
				this
		);
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	// Similar to the normal interaction events, we handle this event before any other plugins.
	// Note: We intentionally do not ignore cancelled events here: The event is called cancelled by
	// default if the attack would normally not take place.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerAttackEntity(PrePlayerAttackEntityEvent event) {
		var player = event.getPlayer();
		var clickedEntity = event.getAttacked();
		assert clickedEntity != null;

		if (entityShops.handlePlayerLeftClickEntity(player, clickedEntity)) {
			// Cancel the event to cancel certain default effects, such as hit sounds:
			event.setCancelled(true);
		}
	}
}
