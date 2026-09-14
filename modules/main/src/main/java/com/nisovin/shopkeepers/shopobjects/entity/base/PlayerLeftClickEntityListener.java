package com.nisovin.shopkeepers.shopobjects.entity.base;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.paper.PaperCompat;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Detects left clicks on entities if {@link Settings#enableLeftClickInteraction} is enabled.
 * <p>
 * Bukkit provides no event for when a player left clicks an entity, so depending on the server
 * version, we determine this ourselves: On Paper, we use the "PrePlayerAttackEntityEvent" (see the
 * Paper compatibility module). On Spigot, we listen for the {@link PlayerAnimationEvent} and
 * perform a ray trace on every arm swing to determine if the player is in reach of an entity.
 * <p>
 * Note: We cannot use the {@link EntityDamageByEntityEvent} for this: We mark shopkeeper mobs as
 * {@link LivingEntity#setInvulnerable(boolean) invulnerable} in order to disable certain default
 * mob behaviors, such as for example prevent other mobs from considering shopkeeper mobs as
 * potential enemies. However, this also skips the server from calling any damage event for
 * shopkeeper mobs. Also, there may be other circumstances in which the server does not call the
 * damage events.
 * <p>
 * This comes with limitations: Arm swing animations are also sent for various other actions, such
 * as when players mine blocks. And our ray trace can only approximate the entity that the server
 * itself considers to be clicked / attacked. Also, since arm swings are quite frequent, the ray
 * tracing involves a small performance overhead.
 */
class PlayerLeftClickEntityListener implements Listener {

	// Minecraft's default entity interaction range:
	private static final double ENTITY_INTERACTION_RANGE = 3.0D;
	// Players in creative mode can interact with entities from further away:
	private static final double CREATIVE_ENTITY_INTERACTION_RANGE = 5.0D;

	private final Plugin plugin;
	private final BaseEntityShops entityShops;

	PlayerLeftClickEntityListener(Plugin plugin, BaseEntityShops entityShops) {
		Validate.notNull(plugin, "plugin");
		Validate.notNull(entityShops, "entityShops");
		this.plugin = plugin;
		this.entityShops = entityShops;
	}

	void onEnable() {
		// Paper: Uses a different mechanism to detect left clicks.
		if (Settings.enableLeftClickInteraction && !PaperCompat.hasProvider()) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	// We do not ignore cancelled events here: Whether the animation is canceled does not affect
	// whether the player interacts with an entity in reach.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	void onPlayerAnimation(PlayerAnimationEvent event) {
		if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
			return;
		}

		Player player = event.getPlayer();
		GameMode gameMode = player.getGameMode();

		// Spectators cannot interact with entities:
		if (gameMode == GameMode.SPECTATOR) {
			return;
		}

		double interactionRange = (gameMode == GameMode.CREATIVE)
				? CREATIVE_ENTITY_INTERACTION_RANGE
				: ENTITY_INTERACTION_RANGE;
		Location eyeLocation = player.getEyeLocation();
		Vector direction = eyeLocation.getDirection();

		// We do not limit the ray trace to shopkeeper entities, because a non-shopkeeper entity in
		// front of a shopkeeper entity may block the interaction. The subsequent interaction
		// handler logs and ignores interactions with non-shopkeeper entities.
		@Nullable RayTraceResult hitResult = player.getWorld().rayTrace(
				eyeLocation,
				direction,
				interactionRange,
				FluidCollisionMode.NEVER,
				false, // ignorePassableBlocks (grass, signs, etc.)
				0.0D, // raySize (entity bounding box expansion)
				(entity) -> {
					// Minecraft also ignores spectators when it determines the clicked entity:
					// TODO SPIGOT-5228: Filtering dead entities.
					return !entity.isDead()
							&& !player.equals(entity)
							&& !EntityUtils.isSpectator(entity);
				}
		);
		if (hitResult == null) {
			return;
		}

		@Nullable Entity hitEntity = hitResult.getHitEntity();
		if (hitEntity == null) {
			return; // E.g. a block hit
		}

		entityShops.handlePlayerLeftClickEntity(player, hitEntity);
	}
}
