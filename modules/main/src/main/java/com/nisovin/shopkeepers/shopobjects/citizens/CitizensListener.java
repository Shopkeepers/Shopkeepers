package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCAddTraitEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCRemoveTraitEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.event.NPCTeleportEvent;
import net.citizensnpcs.api.event.NPCTraitCommandAttachEvent;
import net.citizensnpcs.api.event.PlayerCreateNPCEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

/**
 * The {@link NPCAddTraitEvent} and {@link NPCRemoveTraitEvent} don't provide the player who adds or
 * removes the trait. We require the player to be able to send feedback messages about the creation
 * and deletion of the corresponding shopkeeper.
 * <p>
 * We therefore handle trait additions and removals deferred and additionally react to the
 * {@link NPCTraitCommandAttachEvent} (called <b>after</b> a player added a trait via command) and
 * the {@link PlayerCreateNPCEvent} (called when a player creates a NPC via command, which might
 * result in traits, specified in the NPC creation command, getting added <b>afterwards</b>) to try
 * to (heuristically) figure out the player responsible for adding or removing the trait.
 * <p>
 * Unfortunately there are no Citizens events for when a player removes a trait or NPC. If we cannot
 * determine the player who is adding or removing the trait, we still create or delete the
 * corresponding shopkeeper, but no player receives any feedback messages about it.
 */
class CitizensListener implements Listener {

	private static abstract class PendingTraitState {

		private final ShopkeepersPlugin plugin;

		// The uuids of the last NPC and the last player encountered in relevant events:
		private @Nullable UUID lastNPCId = null;
		private @Nullable UUID lastPlayerId = null;
		// The shopkeeper trait awaiting to be handled:
		private @Nullable CitizensShopkeeperTrait pendingTrait = null;
		// The task which handles the pending trait if we don't end up handling it within the
		// current tick:
		private @Nullable WrappedTask pendingTraitTask = null;

		PendingTraitState(ShopkeepersPlugin plugin) {
			assert plugin != null;
			this.plugin = plugin;
		}

		// Needs to be called before updating the last player or the pending trait.
		private void updateLastNPC(NPC npc) {
			assert npc != null;
			UUID npcId = npc.getUniqueId();
			// Check if the last captured state was for another NPC:
			if (lastNPCId != null && !lastNPCId.equals(npcId)) {
				this.reset(); // handles any currently pending trait
			}

			// Set new last encountered NPC:
			lastNPCId = npcId;
		}

		void updatePendingTrait(NPC npc, CitizensShopkeeperTrait trait) {
			assert npc != null && trait != null;
			this.updateLastNPC(npc);

			// Handle the previously pending trait, if there is one:
			this.handlePendingTrait();
			assert pendingTrait == null;

			// Set new pending trait:
			pendingTrait = trait;

			// Check if we can handle the trait:
			this.tryHandlePendingTraitWithPlayer();
		}

		private void handlePendingTrait() {
			if (pendingTrait != null) {
				// Otherwise the pending trait would have been handled already:
				assert lastPlayerId == null;
				CitizensShopkeeperTrait trait = pendingTrait;
				pendingTrait = null;
				this.reset();
				this.handleTrait(trait, null); // Handle without player
			}
		}

		void updateLastPlayer(NPC npc, Player player) {
			assert npc != null && player != null;
			this.updateLastNPC(npc);

			if (lastPlayerId != null) {
				assert pendingTrait == null;
				this.reset(); // Resets any currently active pendingTraitTask
			}

			// Set the last encountered player:
			lastPlayerId = player.getUniqueId();

			// Check if there is any pending trait that we can handle now:
			this.tryHandlePendingTraitWithPlayer();
		}

		private void tryHandlePendingTraitWithPlayer() {
			// Check if we have all the information available to associate the trait with a specific
			// player:
			UUID lastPlayerId = this.lastPlayerId;
			if (lastPlayerId != null && pendingTrait != null) {
				CitizensShopkeeperTrait trait = pendingTrait;
				// Can be null if the player is no longer online:
				Player player = Bukkit.getPlayer(lastPlayerId);
				pendingTrait = null;
				this.reset();
				this.handleTrait(trait, player); // Handle with player (can be null though)
			} else {
				// The task should get reset before we reach this state. We check for this anyway
				// just in case.
				assert pendingTraitTask == null;
				if (pendingTraitTask == null || Unsafe.assertNonNull(pendingTraitTask).isCancelled()) {
					pendingTraitTask = SchedulerUtils.runAsyncTaskLaterOrOmit(() -> {
						pendingTraitTask = null; // Reset
						reset(); // Handles any currently pending trait
					}, 1L);
				} // Else: There is already an active task that will handle the pending trait later.
			}
		}

		void reset() {
			if (pendingTrait != null) {
				this.handlePendingTrait();
				assert pendingTrait == null;
			}
			this.lastNPCId = null;
			this.lastPlayerId = null;
			if (pendingTraitTask != null) {
				pendingTraitTask.cancel();
				pendingTraitTask = null;
			}
		}

		// The player can be null.
		protected abstract void handleTrait(CitizensShopkeeperTrait trait, @Nullable Player player);
	}

	private final CitizensShops citizensShops;
	private final PendingTraitState pendingTraitAddition;

	CitizensListener(ShopkeepersPlugin plugin, CitizensShops citizensShops) {
		assert plugin != null && citizensShops != null;
		this.citizensShops = citizensShops;
		pendingTraitAddition = new PendingTraitState(plugin) {
			@Override
			protected void handleTrait(CitizensShopkeeperTrait trait, @Nullable Player player) {
				trait.onTraitAdded(player);
			}
		};
	}

	void onEnable() {
	}

	void onDisable() {
		pendingTraitAddition.reset(); // Handles any currently pending trait.
	}

	// Handling of trait additions:

	// Traits can also be added during NPC creation. This is called before these traits get added.
	// Heuristic: We assume that any directly following NPCAddTraitEvents for the same NPC and
	// within the current tick are caused by the player of this event.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerCreateNPC(PlayerCreateNPCEvent event) {
		NPC npc = Unsafe.assertNonNull(event.getNPC());
		Player creator = Unsafe.assertNonNull(event.getCreator());
		pendingTraitAddition.updateLastPlayer(npc, creator);
	}

	// This is called for every trait addition (not only by players).
	// This is also called whenever a trait gets added after the NPC got reloaded.
	// This is called after the trait got added and linked to the NPC (after Trait#onAttach has been
	// called).
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTraitAdded(NPCAddTraitEvent event) {
		NPC npc = event.getNPC();
		assert npc != null;
		Trait eventTrait = event.getTrait();
		Class<? extends Trait> traitClass = eventTrait.getClass();
		@Nullable Trait trait = Unsafe.nullable(npc.getTraitNullable(traitClass));
		if (trait == null) {
			// The trait got not removed again, probably by some other event handler:
			return;
		}
		if (trait != eventTrait) {
			// The current trait instance does not match the event's trait. This may happen if the
			// trait got removed and re-added again during event handling. There should have been
			// another event for the current trait then.
			return;
		}

		if (trait instanceof CitizensShopkeeperTrait) {
			pendingTraitAddition.updatePendingTrait(npc, (CitizensShopkeeperTrait) trait);
		}
	}

	// Called after the trait got already added.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTraitAddedByPlayer(NPCTraitCommandAttachEvent event) {
		if (!(event.getCommandSender() instanceof Player)) return;
		Player player = Unsafe.castNonNull(event.getCommandSender());
		NPC npc = event.getNPC();
		Class<? extends Trait> traitClass = Unsafe.castNonNull(event.getTraitClass());
		@Nullable Trait trait = Unsafe.nullable(npc.getTraitNullable(traitClass));
		if (trait == null) {
			// The trait got not removed again, probably by some other event handler:
			return;
		}
		if (trait instanceof CitizensShopkeeperTrait) {
			// The NPCTraitCommandAttachEvent is called right after the corresponding
			// NPCAddTraitEvent.
			assert trait == pendingTraitAddition.pendingTrait;
			pendingTraitAddition.updateLastPlayer(npc, player);
			// Note: Updating of the pendingTrait is only done by the NPCAddTraitEvent event
			// handler.
		}
	}

	// Handling of trait and NPC removals:

	// TODO There are currently no Citizens events which could provide us the player involved in the
	// removal of the trait or NPC. We therefore remove the corresponding shopkeeper without a
	// player.

	// Called after trait removal (but before Trait#onRemove() has been called).
	// Unlike NPCAddTraitEvent, which is also called when traits get added after reloading a NPC,
	// this is only called when the trait gets permanently removed.
	// This is not called when the traits get removed due to the deletion of the NPC.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTraitRemoved(NPCRemoveTraitEvent event) {
		pendingTraitAddition.reset(); // Handles any currently pending trait
		if (event.getTrait() instanceof CitizensShopkeeperTrait) {
			CitizensShopkeeperTrait shopkeeperTrait = (CitizensShopkeeperTrait) event.getTrait();
			shopkeeperTrait.onTraitDeleted(null); // Handle without player
		}
	}

	// Called right before the NPC gets deleted.
	// Called before the NPC is despawned
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onNPCRemoved(NPCRemoveEvent event) {
		pendingTraitAddition.reset(); // Handles any currently pending trait
		NPC npc = event.getNPC();
		CitizensShopkeeperTrait shopkeeperTrait = Unsafe.cast(npc.getTraitNullable(CitizensShopkeeperTrait.class));
		if (shopkeeperTrait != null) {
			shopkeeperTrait.onTraitDeleted(null); // Handle without player
		} else {
			// Delete the corresponding shopkeeper(s):
			// If there are multiple associated shopkeepers, we delete all of them.
			List<? extends Shopkeeper> shopkeepers = citizensShops.getShopkeepers(npc);
			if (!shopkeepers.isEmpty()) {
				new ArrayList<>(shopkeepers).forEach(shopkeeper -> {
					assert shopkeeper.getShopObject() instanceof SKCitizensShopObject;
					// Handle the deletion without a player:
					((SKCitizensShopObject) shopkeeper.getShopObject()).onNPCDeleted(null);
				});
			}
		}
	}

	// NPC (RE-)LOADING

	// Called when Citizens has reloaded its NPCs from storage.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onCitizensReloaded(CitizensReloadEvent event) {
		citizensShops.onCitizensReloaded();
	}

	// NPC SPAWNING & DESPAWNING

	// Called when the NPC entity was successfully spawned.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onNPCSpawned(NPCSpawnEvent event) {
		NPC npc = event.getNPC();
		assert npc != null;
		Shopkeeper shopkeeper = citizensShops.getShopkeeper(npc);
		if (shopkeeper == null) return;

		Log.debug(DebugOptions.regularTickActivities,
				shopkeeper.getLogPrefix() + "Citizens NPC has been spawned.");

		SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
		Entity entity = npc.getEntity();
		assert entity != null;
		shopObject.setEntity(entity);
	}

	// Called when the NPC entity is about to be removed. The entity might already be dead.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onNPCDepawn(NPCDespawnEvent event) {
		NPC npc = event.getNPC();
		assert npc != null;
		Shopkeeper shopkeeper = citizensShops.getShopkeeper(npc);
		if (shopkeeper == null) return;

		Log.debug(DebugOptions.regularTickActivities,
				shopkeeper.getLogPrefix() + "Citizens NPC is despawned.");

		SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
		shopObject.setEntity(null);
	}

	// NPC TELEPORTING

	@EventHandler
	void onNPCTeleport(NPCTeleportEvent event) {
		NPC npc = event.getNPC();
		assert npc != null;
		Location toLocation = event.getTo();
		assert toLocation != null;
		this.updateShopkeeperLocations(npc, toLocation);
	}

	private void updateShopkeeperLocations(NPC npc, Location toLocation) {
		citizensShops.getShopkeepers(npc).forEach(shopkeeper -> {
			assert shopkeeper.getShopObject() instanceof SKCitizensShopObject;
			SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
			shopObject.onNpcTeleport(toLocation);
		});
	}
}
