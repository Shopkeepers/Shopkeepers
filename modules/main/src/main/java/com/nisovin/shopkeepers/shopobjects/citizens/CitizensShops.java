package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensDependency;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.trait.LookClose;

/**
 * Citizens shopkeepers can be created in two ways:
 * <ol>
 * <li>By creating a shopkeeper of type 'npc' (alias 'citizen'), either via command, the shop
 * creation item or by another plugin through the Shopkeepers API. This will create both the
 * shopkeeper and the corresponding Citizens NPC.
 * <li>By attaching the 'shopkeeper' trait to an already existing Citizens NPC. The trait can also
 * be attached to a NPC for which there already exists a corresponding shopkeeper. No new shopkeeper
 * will be created then.
 * </ol>
 * 
 * Citizen shopkeepers can be removed again in the following ways:
 * <ol>
 * <li>The shopkeeper gets deleted, either due to a player removing it via command or via the editor
 * option, or due the Shopkeepers plugin removing it due to reasons such owner inactivity or when
 * the shopkeeper's container is broken. If the corresponding Citizens NPC has the 'shopkeeper'
 * trait attached, only this trait gets removed and the NPC remains existing. Otherwise, the
 * Citizens NPC is removed. TODO Removing the Citizens NPC only works if the Citizens plugin is
 * currently running.
 * <li>The Citizens NPC gets deleted. This deletes the corresponding shopkeeper. If the Shopkeepers
 * plugin is not running currently, the shopkeeper gets deleted during the next startup of the
 * Shopkeepers plugin.
 * <li>The 'shopkeeper' trait gets removed from the Citizens NPC. This deletes the corresponding
 * shopkeeper but keeps the Citizens NPC alive. TODO Removing the shopkeeper only works if the
 * Citizens plugin is currently running. Otherwise the shopkeeper will remain existing and attached
 * to the NPC until it is removed directly (e.g. via the editor or via command).
 * <li>On plugin startup, if the Citizens plugin is running as well, we check for and remove invalid
 * shopkeepers such as shopkeepers for which there is no corresponding Citizens NPC (anymore) or
 * shopkeepers which have the same Citizens NPC assigned as another shopkeeper.
 * </ol>
 */
public class CitizensShops {

	private final SKShopkeepersPlugin plugin;
	private final SKCitizensShopObjectType citizensShopObjectType = new SKCitizensShopObjectType(
			Unsafe.initialized(this)
	);
	private final PluginListener pluginListener = new PluginListener(Unsafe.initialized(this));

	private final CitizensListener citizensListener;
	private boolean citizensShopsEnabled = false;
	private @Nullable TraitInfo shopkeeperTrait = null;

	// This mapping is updated whenever shopkeepers with Citizens shop objects are added or removed
	// from the shopkeeper registry. Shopkeepers for which the corresponding NPC could not be
	// created are not contained in this mapping.
	// If multiple shopkeepers are associated with the same NPC, this mapping keeps track of all of
	// these shopkeepers.
	private final Map<UUID, List<AbstractShopkeeper>> shopkeepersByNpcId = new ConcurrentHashMap<>();

	public CitizensShops(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.citizensListener = new CitizensListener(plugin, Unsafe.initialized(this));
	}

	// This is called on plugin enable.
	public void onEnable() {
		this.enable();
		Bukkit.getPluginManager().registerEvents(pluginListener, plugin);
	}

	// This is called on plugin disable.
	public void onDisable() {
		this.disable();
		HandlerList.unregisterAll(pluginListener);
		shopkeepersByNpcId.clear();
	}

	public SKCitizensShopObjectType getCitizensShopObjectType() {
		return citizensShopObjectType;
	}

	/**
	 * Returns whether Citizens shopkeepers are currently enabled.
	 * 
	 * @return <code>true</code> if currently enabled
	 */
	public boolean isEnabled() {
		this.verifyCitizensAPIAvailable();
		return citizensShopsEnabled;
	}

	/**
	 * Verify that the Citizens API is still available, and otherwise disable the Citizen shops.
	 * 
	 * @see CitizensShops#isCitizensAPIAvailable()
	 */
	private void verifyCitizensAPIAvailable() {
		if (!citizensShopsEnabled) return;

		if (!this.isCitizensAPIAvailable()) {
			Log.debug("No valid Citizens API implementation available."
					+ " Disabling the Citizen shops.");
			this.disable();
			return;
		}
	}

	/**
	 * Check if the Citizens API has a valid implementation set.
	 * <p>
	 * This assumes that the caller has already verified that the Citizens plugin is currently
	 * enabled.
	 * <p>
	 * In the past, we encountered situations in which the Citizens plugin reported as "enabled" but
	 * no CitizensAPI implementation was set. Or if an error occurs during the enabling of Citizens,
	 * an implementation might be set, but cannot actually be used (e.g.
	 * {@link CitizensAPI#getNPCRegistry()} might return <code>null</code> then). Or if plugins like
	 * PlugMan reload the Citizens plugin, the Citizens API might not be properly initialized
	 * either, maybe due to the plugin's classes being reloaded in a new class loader.
	 * 
	 * @return <code>true</code> if a valid Citizens API implementation is available
	 */
	private boolean isCitizensAPIAvailable() {
		assert CitizensDependency.isPluginEnabled();
		return CitizensAPI.hasImplementation() && CitizensAPI.getNPCRegistry() != null;
	}

	void enable() {
		if (this.isEnabled()) {
			// Disable first, to perform cleanup if needed:
			this.disable();
		}

		if (!Settings.enableCitizenShops) return; // Feature disabled
		if (!CitizensDependency.isPluginEnabled()) {
			Log.debug("Citizen shops enabled, but Citizens plugin not found or disabled.");
			return;
		}
		if (!this.isCitizensAPIAvailable()) {
			Log.debug("Citizen shops enabled, but Citizens API not available. Did the Citizens "
					+ "plugin enable correctly? Or did you try to reload the Citizens plugin?");
			return;
		}

		Log.info("Citizens found: Enabling NPC shopkeepers.");

		// Register shopkeeper trait:
		this.registerShopkeeperTrait();

		// Register citizens listener:
		Bukkit.getPluginManager().registerEvents(citizensListener, plugin);
		citizensListener.onEnable();

		// Delayed to run after shopkeepers and NPCs were loaded:
		SchedulerUtils.runAsyncTaskLaterOrOmit(new DelayedSetupTask(), 3L);

		// Enabled:
		citizensShopsEnabled = true;
	}

	private class DelayedSetupTask implements Runnable {
		@Override
		public void run() {
			if (!isEnabled()) return; // No longer enabled

			// Check for invalid Citizens shopkeepers:
			validateCitizenShopkeepers(Settings.deleteInvalidCitizenShopkeepers, false);

			// Inform the Citizens NPC shop objects:
			shopkeepersByNpcId.values().stream().flatMap(List::stream).forEach(shopkeeper -> {
				((SKCitizensShopObject) shopkeeper.getShopObject()).onCitizensShopsEnabled();
			});
		}
	}

	void disable() {
		if (!this.isEnabled()) {
			// Already disabled
			return;
		}

		// Inform the Citizens NPC shop objects:
		shopkeepersByNpcId.values().stream().flatMap(List::stream).forEach(shopkeeper -> {
			((SKCitizensShopObject) shopkeeper.getShopObject()).onCitizensShopsDisabled();
		});

		Plugin citizensPlugin = CitizensDependency.getPlugin();
		if (citizensPlugin != null) {
			// Unregister the shopkeeper trait:
			this.unregisterShopkeeperTrait();
		}

		// Unregister the Citizens listener:
		citizensListener.onDisable();
		HandlerList.unregisterAll(citizensListener);

		// Note: We intentionally don't clear the shopkeepersByNpcId mapping here, so that we do not
		// freshly have to fill it again on reloads of the Citizens plugin. It is cleaned up when
		// the corresponding shopkeepers are unloaded, and on disable of the Shopkeepers plugin.

		// Disabled:
		citizensShopsEnabled = false;
	}

	private void registerShopkeeperTrait() {
		assert shopkeeperTrait == null;
		TraitInfo shopkeeperTrait = TraitInfo.create(CitizensShopkeeperTrait.class)
				.withName(CitizensShopkeeperTrait.TRAIT_NAME);
		this.shopkeeperTrait = shopkeeperTrait;
		try {
			CitizensAPI.getTraitFactory().registerTrait(shopkeeperTrait);
		} catch (Throwable e) {
			Log.debug("Shopkeeper trait registration failed!", e);
		}
	}

	private void unregisterShopkeeperTrait() {
		TraitInfo shopkeeperTrait = this.shopkeeperTrait;
		if (shopkeeperTrait != null) {
			try {
				CitizensAPI.getTraitFactory().deregisterTrait(shopkeeperTrait);
			} catch (Throwable e) {
				Log.debug("Shopkeeper trait deregistration failed!", e);
			} finally {
				this.shopkeeperTrait = null;
			}
		}
	}

	// Called when the Citizens plugin has reloaded its NPCs.
	void onCitizensReloaded() {
		if (shopkeepersByNpcId.isEmpty()) return;

		Log.debug("Citizens plugin has been reloaded.");

		// Inform all Citizens NPC shop objects:
		shopkeepersByNpcId.values().stream().flatMap(List::stream).forEach(shopkeeper -> {
			((SKCitizensShopObject) shopkeeper.getShopObject()).onCitizensReloaded();
		});
	}

	void registerCitizensShopkeeper(SKCitizensShopObject citizensShop, UUID npcId) {
		assert citizensShop != null && npcId != null;
		AbstractShopkeeper shopkeeper = citizensShop.getShopkeeper();
		// If there is no entry for this NPC id yet, we create a new List with an initial capacity
		// of 1, because we usually expect there to only be one shopkeeper associated with the NPC:
		List<AbstractShopkeeper> shopkeepers = shopkeepersByNpcId.computeIfAbsent(
				npcId,
				key -> new ArrayList<>(1)
		);
		assert shopkeepers != null;
		shopkeepers.add(shopkeeper);
	}

	void unregisterCitizensShopkeeper(SKCitizensShopObject citizensShop, UUID npcId) {
		assert citizensShop != null && npcId != null;
		AbstractShopkeeper shopkeeper = citizensShop.getShopkeeper();
		shopkeepersByNpcId.computeIfPresent(npcId, (key, shopkeepers) -> {
			shopkeepers.remove(shopkeeper);
			if (shopkeepers.isEmpty()) {
				// TODO uncheckedNull required due to CheckerFramework limitation
				return Unsafe.uncheckedNull(); // Removes the mapping
			} else {
				return shopkeepers;
			}
		});
	}

	public boolean isShopkeeper(NPC npc) {
		return !this.getShopkeepers(npc).isEmpty();
	}

	// If there are multiple shopkeepers associated with the given NPC, this only returns one of
	// them.
	public @Nullable AbstractShopkeeper getShopkeeper(NPC npc) {
		List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npc);
		return shopkeepers.isEmpty() ? null : shopkeepers.get(0);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC.
	public List<? extends AbstractShopkeeper> getShopkeepers(NPC npc) {
		Validate.notNull(npc, "npc is null");
		UUID npcId = npc.getUniqueId();
		assert npcId != null;
		return this.getShopkeepers(npcId);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC id.
	private List<? extends AbstractShopkeeper> getShopkeepers(UUID npcId) {
		Validate.notNull(npcId, "npcId is null");
		List<AbstractShopkeeper> shopkeepers = shopkeepersByNpcId.get(npcId);
		return (shopkeepers != null) ? shopkeepers : Collections.emptyList();
	}

	public static String getNPCIdString(NPC npc) {
		return npc.getId() + " (" + npc.getUniqueId() + ")";
	}

	// Returns null if the entity is no Citizens NPC, or if the Citizens plugin or Citizens shops
	// are disabled.
	public @Nullable UUID getNPCUniqueId(Entity entity) {
		if (this.isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return (npc != null) ? npc.getUniqueId() : null;
		} else {
			return null;
		}
	}

	// Returns the created NPC, or null.
	public @Nullable NPC createNPC(
			@Nullable Location location,
			EntityType entityType,
			String name
	) {
		if (!this.isEnabled()) return null;

		NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		if (npc == null) return null;

		// Ensure that Citizens remembers the mob type:
		MobType mobType = Unsafe.assertNonNull(npc.getOrAddTrait(MobType.class));
		mobType.setType(entityType);

		// Look towards nearby players:
		LookClose lookClose = Unsafe.assertNonNull(npc.getOrAddTrait(LookClose.class));
		lookClose.lookClose(true);

		if (location != null) {
			// This will log a debug message from Citizens if it cannot spawn the NPC currently, but
			// will then later attempt to spawn it when the chunk gets loaded:
			npc.spawn(location);
		} else {
			// The Citizens shop object will periodically check if the NPC has already been spawned
			// (i.e. if it has no location assigned yet) and will otherwise attempt to spawn it.
		}
		return npc;
	}

	void onNPCEdited(NPC npc) {
		// TODO We may sometimes unnecessarily call this multiple times within the same tick. Merge
		// all save requests within a short time period (i.e. a few ticks)? But we would also need
		// to ensure that the save is triggered on plugin shutdown, including when Citizens shuts
		// down.
		if (Settings.saveCitizenNpcsInstantly) {
			this.saveNPCs();
		}
		// Else: Saving is controlled only by Citizens itself: Periodically, manually, and on
		// shutdown.
	}

	public void saveNPCs() {
		if (!this.isEnabled()) return;

		long startNanos = System.nanoTime();
		// TODO Saving is quite a heavy operation, but there is no API yet to trigger an
		// asynchronous save. We therefore execute this command to trigger an asynchronous save.
		// However, as a side effect, this will log messages to the server console.
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "citizens save -a");
		// CitizensAPI.getNPCRegistry().saveToStore();
		double durationMillis = TimeUtils.convert(
				System.nanoTime() - startNanos,
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		Log.debug(() -> "Saved Citizens NPCs (" + TextUtils.format(durationMillis) + " ms).");
	}

	/**
	 * Checks for and optionally warns about or deletes invalid Citizen shopkeepers.
	 * <p>
	 * This does nothing if the Citizens integration is disabled or if the Citizens plugin is not
	 * running currently.
	 * 
	 * @param deleteInvalidShopkeepers
	 *            <code>true</code> to also delete any found invalid Citizen shopkeepers
	 * @param silent
	 *            <code>true</code> to not log warnings about any found invalid Citizen shopkeepers
	 * @return the number of found invalid Citizen shopkeepers
	 */
	public int validateCitizenShopkeepers(boolean deleteInvalidShopkeepers, boolean silent) {
		if (!this.isEnabled()) {
			// Cannot determine which shopkeepers have a backing NPC if Citizens isn't running:
			return 0;
		}

		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> invalidShopkeepers = new ArrayList<>();
		shopkeeperRegistry.getAllShopkeepers().forEach(shopkeeper -> {
			if (!(shopkeeper.getShopObject() instanceof SKCitizensShopObject)) {
				return;
			}

			SKCitizensShopObject citizensShop = (SKCitizensShopObject) shopkeeper.getShopObject();
			UUID npcUniqueId = citizensShop.getNPCUniqueId();
			if (npcUniqueId == null) {
				// The id of the associated Citizens NPC is missing. If the NPC wasn't created yet,
				// maybe the shopkeeper was somehow created without the Citizens plugin being
				// enabled.
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning(shopkeeper.getLogPrefix() + "There is no Citizens NPC associated.");
				}
				return;
			}

			if (CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId) == null) {
				// There is no NPC with the stored NPC id:
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning(shopkeeper.getLogPrefix()
							+ "There is no Citizens NPC with unique id " + npcUniqueId);
				}
				return;
			}

			List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npcUniqueId);
			if (shopkeepers.size() > 1) {
				// There are multiple Citizen shopkeepers using the same NPC.
				// We consider the first registered shopkeeper to be the legitimate one and don't
				// add it to the invalid shopkeepers:
				Shopkeeper mainShopkeeper = shopkeepers.get(0);
				if (mainShopkeeper != shopkeeper) {
					citizensShop.setKeepNPCOnDeletion();
					invalidShopkeepers.add(shopkeeper);
					if (!silent) {
						Log.warning(shopkeeper.getLogPrefix() + "Shopkeeper " + mainShopkeeper.getId()
								+ " is already using the same Citizens NPC with unique id "
								+ npcUniqueId);
					}
					return;
				}
			}
		});

		if (!invalidShopkeepers.isEmpty()) {
			if (deleteInvalidShopkeepers) {
				// Delete those shopkeepers:
				for (Shopkeeper shopkeeper : invalidShopkeepers) {
					shopkeeper.delete();
				}

				// Save:
				plugin.getShopkeeperStorage().save();

				if (!silent) {
					Log.warning("Deleted " + invalidShopkeepers.size()
							+ " invalid Citizen shopkeepers!");
				}
			} else {
				// Only log a warning:
				if (!silent) {
					Log.warning("Found " + invalidShopkeepers.size() + " invalid Citizen "
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
