package com.nisovin.shopkeepers.shopobjects.entity.base;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;

/**
 * Implements common default behaviors for {@link BaseEntityShopObject}.
 */
public class BaseEntityShops {

	private final SKShopkeepersPlugin plugin;
	private final EntityAI entityAI;
	private final BaseEntityShopListener baseEntityShopListener;

	public BaseEntityShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.entityAI = new EntityAI(plugin);
		this.baseEntityShopListener = new BaseEntityShopListener(plugin, this);
	}

	public void onEnable() {
		entityAI.onEnable();
		baseEntityShopListener.onEnable();
	}

	public void onDisable() {
		baseEntityShopListener.onDisable();

		// Stop entity AI:
		entityAI.onDisable();
	}

	/**
	 * Gets the {@link EntityAI}.
	 * 
	 * @return access to the entity AI system
	 */
	public EntityAI getEntityAI() {
		return entityAI;
	}

	// Bypassing entity spawn blocking plugins (e.g. region protection plugins):
	void forceEntitySpawn(Location location, EntityType entityType) {
		if (Settings.bypassSpawnBlocking) {
			plugin.getForcingEntitySpawner().forceEntitySpawn(location, entityType);
		}
	}

	/**
	 * Called when a player left clicks an entity. This handles the entity interaction.
	 * <p>
	 * This is expected to only be called by the server version specific left-click detection
	 * implementations when {@link Settings#enableLeftClickInteraction} is enabled.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param clickedEntity
	 *            the left clicked entity, not <code>null</code>
	 * @return <code>true</code> if the player interacted with a shopkeeper entity
	 */
	public boolean handlePlayerLeftClickEntity(Player player, Entity clickedEntity) {
		return baseEntityShopListener.onPlayerLeftClickEntity(player, clickedEntity);
	}
}
