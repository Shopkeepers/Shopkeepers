package com.nisovin.shopkeepers.shopobjects.fancynpcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.dependencies.fancynpcs.FancyNpcsDependency;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Manages FancyNpcs shopkeepers.
 * <p>
 * FancyNpcs shopkeepers can be created by creating a shopkeeper of type 'fancynpc', either via
 * command, the shop creation item, or by another plugin through the Shopkeepers API. This will
 * create both the shopkeeper and the corresponding FancyNpc.
 * </p>
 */
public class FancyNpcsShops {

	private final SKShopkeepersPlugin plugin;
	private final SKFancyNpcShopObjectType fancyNpcShopObjectType = new SKFancyNpcShopObjectType(
			Unsafe.initialized(this)
	);
	private final FancyNpcsPluginListener pluginListener = new FancyNpcsPluginListener(Unsafe.initialized(this));

	private final FancyNpcsListener fancyNpcsListener;
	private boolean fancyNpcsShopsEnabled = false;

	// Maps NPC id (String) to List of shopkeepers using that NPC
	private final Map<String, List<AbstractShopkeeper>> shopkeepersByNpcId = new HashMap<>();

	public FancyNpcsShops(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.fancyNpcsListener = new FancyNpcsListener(plugin, Unsafe.initialized(this));
	}

	// Called on plugin enable.
	public void onEnable() {
		this.enable();
		Bukkit.getPluginManager().registerEvents(pluginListener, plugin);
	}

	// Called on plugin disable.
	public void onDisable() {
		this.disable();
		HandlerList.unregisterAll(pluginListener);
		shopkeepersByNpcId.clear();
	}

	public SKFancyNpcShopObjectType getFancyNpcShopObjectType() {
		return fancyNpcShopObjectType;
	}

	/**
	 * Returns whether FancyNpc shopkeepers are currently enabled.
	 *
	 * @return {@code true} if currently enabled
	 */
	public boolean isEnabled() {
		return fancyNpcsShopsEnabled;
	}

	void enable() {
		if (this.isEnabled()) {
			this.disable();
		}

		if (!Settings.enableCitizenShops) return; // Reuse the same config flag
		if (!FancyNpcsDependency.isPluginEnabled()) {
			Log.debug("FancyNpc shops enabled, but FancyNpcs plugin not found or disabled.");
			return;
		}

		Log.info("FancyNpcs found: Enabling FancyNpc shopkeepers.");

		// Register FancyNpcs listener:
		Bukkit.getPluginManager().registerEvents(fancyNpcsListener, plugin);
		fancyNpcsListener.onEnable();

		// Delayed setup after shopkeepers and NPCs are loaded:
		Bukkit.getScheduler().runTaskLater(plugin, new DelayedSetupTask(), 3L);

		// Enabled:
		fancyNpcsShopsEnabled = true;
	}

	private class DelayedSetupTask implements Runnable {
		@Override
		public void run() {
			if (!isEnabled()) return; // No longer enabled

			// Check for invalid FancyNpc shopkeepers:
			validateFancyNpcShopkeepers(Settings.deleteInvalidCitizenShopkeepers, false);

			// Inform the FancyNpc shop objects:
			shopkeepersByNpcId.values().stream().flatMap(List::stream).forEach(shopkeeper -> {
				((SKFancyNpcShopObject) shopkeeper.getShopObject()).onFancyNpcsShopsEnabled();
			});
		}
	}

	void disable() {
		if (!this.isEnabled()) {
			return;
		}

		// Inform the FancyNpc shop objects:
		shopkeepersByNpcId.values().stream().flatMap(List::stream).forEach(shopkeeper -> {
			((SKFancyNpcShopObject) shopkeeper.getShopObject()).onFancyNpcsShopsDisabled();
		});

		// Unregister the FancyNpcs listener:
		fancyNpcsListener.onDisable();
		HandlerList.unregisterAll(fancyNpcsListener);

		// Disabled:
		fancyNpcsShopsEnabled = false;
	}

	void registerFancyNpcShopkeeper(SKFancyNpcShopObject fancyNpcShop, String npcId) {
		assert fancyNpcShop != null && npcId != null;
		AbstractShopkeeper shopkeeper = fancyNpcShop.getShopkeeper();
		List<AbstractShopkeeper> shopkeepers = shopkeepersByNpcId.computeIfAbsent(
				npcId,
				key -> new ArrayList<>(1)
		);
		assert shopkeepers != null;
		shopkeepers.add(shopkeeper);
	}

	void unregisterFancyNpcShopkeeper(SKFancyNpcShopObject fancyNpcShop, String npcId) {
		assert fancyNpcShop != null && npcId != null;
		AbstractShopkeeper shopkeeper = fancyNpcShop.getShopkeeper();
		shopkeepersByNpcId.computeIfPresent(npcId, (key, shopkeepers) -> {
			shopkeepers.remove(shopkeeper);
			if (shopkeepers.isEmpty()) {
				return Unsafe.uncheckedNull();
			} else {
				return shopkeepers;
			}
		});
	}

	public boolean isShopkeeper(Npc npc) {
		return !this.getShopkeepers(npc).isEmpty();
	}

	// If there are multiple shopkeepers associated with the given NPC, only returns the first one.
	public @Nullable AbstractShopkeeper getShopkeeper(Npc npc) {
		List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npc);
		return shopkeepers.isEmpty() ? null : shopkeepers.get(0);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC.
	public List<? extends AbstractShopkeeper> getShopkeepers(Npc npc) {
		Validate.notNull(npc, "npc is null");
		String npcId = npc.getData().getId();
		assert npcId != null;
		return this.getShopkeepers(npcId);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC id.
	List<? extends AbstractShopkeeper> getShopkeepers(String npcId) {
		Validate.notNull(npcId, "npcId is null");
		List<AbstractShopkeeper> shopkeepers = shopkeepersByNpcId.get(npcId);
		return (shopkeepers != null) ? shopkeepers : Collections.emptyList();
	}

	public static String getNpcIdString(Npc npc) {
		return npc.getData().getName() + " (" + npc.getData().getId() + ")";
	}

	/**
	 * Creates a FancyNpc for the given location and entity type.
	 *
	 * @param location   spawn location (can be null)
	 * @param entityType the entity type for the NPC
	 * @param name       name for the NPC
	 * @param creatorId  UUID of the creator (player)
	 * @return the created Npc, or null on failure
	 */
	public @Nullable Npc createNpc(
			@Nullable Location location,
			EntityType entityType,
			String name,
			@Nullable UUID creatorId
	) {
		if (!this.isEnabled()) return null;

		UUID creator = creatorId != null ? creatorId
				: UUID.fromString("00000000-0000-0000-0000-000000000000");

		NpcData data = new NpcData(name, creator, location);
		data.setType(entityType);
		data.setDisplayName(name);
		data.setCollidable(false);
		data.setTurnToPlayer(true);
		data.setShowInTab(false);
		data.setSpawnEntity(true);

		try {
			// Ensure no NPC with this name already exists before registration
			Npc existing = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
			if (existing != null) {
				existing.removeForAll();
				FancyNpcsPlugin.get().getNpcManager().removeNpc(existing);
			}

			// Follow proper FancyNpcs creation sequence for persistent NPCs:
			Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
			npc.setSaveToFile(true);
			npc.create();
			npc.spawnForAll();
			FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
			FancyNpcsPlugin.get().getNpcManager().saveNpcs(false);

			return npc;
		} catch (Exception e) {
			Log.warning("Failed to create FancyNpc!", e);
			return null;
		}
	}

	/**
	 * Removes a FancyNpc from the NPC manager.
	 *
	 * @param npc the NPC to remove
	 */
	public void removeNpc(Npc npc) {
		if (!this.isEnabled()) return;
		try {
			npc.removeForAll();
			FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
			FancyNpcsPlugin.get().getNpcManager().saveNpcs(false);
		} catch (Exception e) {
			Log.warning("Failed to remove FancyNpc!", e);
		}
	}

	/**
	 * Checks for and optionally warns about or deletes invalid FancyNpc shopkeepers.
	 *
	 * @param deleteInvalidShopkeepers {@code true} to also delete any found invalid shopkeepers
	 * @param silent                   {@code true} to not log warnings
	 * @return the number of found invalid shopkeepers
	 */
	public int validateFancyNpcShopkeepers(boolean deleteInvalidShopkeepers, boolean silent) {
		if (!this.isEnabled()) {
			return 0;
		}

		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> invalidShopkeepers = new ArrayList<>();
		shopkeeperRegistry.getAllShopkeepers().forEach(shopkeeper -> {
			if (!(shopkeeper.getShopObject() instanceof SKFancyNpcShopObject)) {
				return;
			}

			SKFancyNpcShopObject fnShop = (SKFancyNpcShopObject) shopkeeper.getShopObject();
			String npcId = fnShop.getNpcId();
			if (npcId == null) {
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning(shopkeeper.getLogPrefix() + "There is no FancyNpc associated.");
				}
				return;
			}

			Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(npcId);
			if (npc == null) {
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning(shopkeeper.getLogPrefix()
							+ "There is no FancyNpc with id " + npcId);
				}
				return;
			}

			List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npcId);
			if (shopkeepers.size() > 1) {
				Shopkeeper mainShopkeeper = shopkeepers.get(0);
				if (mainShopkeeper != shopkeeper) {
					invalidShopkeepers.add(shopkeeper);
					if (!silent) {
						Log.warning(shopkeeper.getLogPrefix() + "Shopkeeper " + mainShopkeeper.getId()
								+ " is already using the same FancyNpc with id " + npcId);
					}
					return;
				}
			}
		});

		if (!invalidShopkeepers.isEmpty()) {
			if (deleteInvalidShopkeepers) {
				for (Shopkeeper shopkeeper : invalidShopkeepers) {
					shopkeeper.delete();
				}
				plugin.getShopkeeperStorage().save();
				if (!silent) {
					Log.warning("Deleted " + invalidShopkeepers.size()
							+ " invalid FancyNpc shopkeepers!");
				}
			} else {
				if (!silent) {
					Log.warning("Found " + invalidShopkeepers.size() + " invalid FancyNpc "
							+ "shopkeepers! Either enable the setting "
							+ "'delete-invalid-citizen-shopkeepers' inside the config, or use the "
							+ "command '/shopkeepers cleanupCitizenShopkeepers' to automatically "
							+ "delete these shopkeepers and get rid of these warnings.");
				}
			}
		}
		return invalidShopkeepers.size();
	}
}
