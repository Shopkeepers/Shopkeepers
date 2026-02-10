package com.nisovin.shopkeepers.shopobjects.citizens;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class CitizensShopkeeperTrait extends Trait {

	public static final String TRAIT_NAME = "shopkeeper";

	public CitizensShopkeeperTrait() {
		super(TRAIT_NAME);
	}

	@Override
	public void load(@Nullable DataKey key) {
		assert key != null;
	}

	@Override
	public void save(@Nullable DataKey key) {
		assert key != null;
	}

	// Also returns null if the Shopkeepers plugin is not enabled currently.
	public @Nullable AbstractShopkeeper getShopkeeper() {
		NPC npc = this.getNPC();
		if (npc == null) return null; // Not yet attached
		if (!SKShopkeepersPlugin.isPluginEnabled()) {
			// Shopkeepers plugin is not enabled.
			return null;
		}

		SKShopkeepersPlugin shopkeepersPlugin = SKShopkeepersPlugin.getInstance();
		CitizensShops citizensShops = shopkeepersPlugin.getCitizensShops();
		return citizensShops.getShopkeeper(npc);
	}

	@Override
	public void onRemove() {
		// In the past, this was also called when Citizens reloads or disables. We therefore detect
		// trait removal by listening to specific Citizens events instead.
	}

	void onShopkeeperDeletion(Shopkeeper shopkeeper) {
		NPC npc = this.getNPC();
		assert npc != null;
		Log.debug(() -> shopkeeper.getUniqueIdLogPrefix()
				+ "Removing the 'shopkeeper' trait from Citizens NPC "
				+ CitizensShops.getNPCIdString(npc) + " due to shopkeeper deletion.");
		npc.removeTrait(CitizensShopkeeperTrait.class);
		SKShopkeepersPlugin.getInstance().getCitizensShops().onNPCEdited(npc);
	}

	/**
	 * Called whenever this trait got deleted from the NPC.
	 * <p>
	 * Unlike {@link #onTraitAdded(Player)}, which is also called on reloads of the NPC, this is
	 * only called if the trait is permanently deleted.
	 * 
	 * @param player
	 *            the player who deleted the trait, or <code>null</code> if not available
	 */
	public void onTraitDeleted(@Nullable Player player) {
		// Note: This returns null if the trait got deleted due to the shopkeeper being deleted (the
		// NPC-shopkeeper association is cleared before the trait is deleted). But to make this more
		// clear, we also check if the shopkeeper is still valid before we delete it.
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null && shopkeeper.isValid()) {
			NPC npc = this.getNPC();
			assert npc != null;
			Log.debug(() -> shopkeeper.getUniqueIdLogPrefix()
					+ "Deletion due to the removal of the 'shopkeeper' trait from Citizens NPC "
					+ CitizensShops.getNPCIdString(npc));
			assert shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN();
			SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
			shopObject.setKeepNPCOnDeletion();
			// This should keep the citizens NPC and only remove the shopkeeper data:
			shopkeeper.delete(player);
			// Save:
			shopkeeper.save();
		} else {
			// TODO If the trait is removed while the Shopkeepers plugin is disabled, the associated
			// shopkeeper is not deleted and will remain attached to the NPC until it is deleted via
			// the shopkeeper editor or via command.
		}
	}

	/**
	 * This is called whenever this trait was added to a NPC.
	 * <p>
	 * This is also called whenever the NPC is reloaded. The corresponding shopkeeper might
	 * therefore already exist. This is called shortly after the trait is attached to the NPC.
	 * 
	 * @param player
	 *            the player who added the trait, can be <code>null</code> if not available
	 */
	void onTraitAdded(@Nullable Player player) {
		// Create a new shopkeeper for this NPC, if there isn't one already, using the given player
		// as creator:
		this.createShopkeeperIfMissing(player);
	}

	@Override
	public void onAttach() {
		// TODO Is this still required? We already handle all trait additions via events now.
		// Note: This is also called whenever Citizens gets reloaded.
		// Log.debug("Shopkeeper trait attached to NPC " + npc.getId());

		if (!SKShopkeepersPlugin.isPluginEnabled()) {
			// Shopkeepers is not running:
			return;
		}

		// Giving citizens some time to properly initialize the trait and NPC:
		// Also: Shopkeeper creation by a player is handled after trait attachment.
		SchedulerUtils.runAsyncTaskLaterOrOmit(() -> {
			// Create a new shopkeeper if there isn't one already for this NPC (without creator):
			this.createShopkeeperIfMissing(null);
		}, 5L);
	}

	// Creator can be null.
	private void createShopkeeperIfMissing(@Nullable Player creator) {
		Entity entity = npc.getEntity();
		assert entity != null;
		SchedulerUtils.runTaskOrOmit(entity, () -> {
			NPC npc = this.getNPC();
			if (npc == null || !npc.hasTrait(CitizensShopkeeperTrait.class)) {
				// The trait is no longer attached to the NPC. Has it already been removed again? We
				// skip creating a shopkeeper for this no longer attached trait:
				return;
			}

			if (this.getShopkeeper() != null) {
				// There is already a shopkeeper for this NPC. The trait was probably re-attached after
				// a reload of Citizens.
				return;
			}

			if (!SKShopkeepersPlugin.isPluginEnabled()) {
				// The Shopkeepers plugin is not enabled currently:
				return;
			}
			ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();

			Log.debug(() -> "Creating shopkeeper for Citizens NPC " + CitizensShops.getNPCIdString(npc)
					+ (creator != null ? " and player '" + creator.getName() + "'" : ""));

			Location location;
			Entity npcEntity = npc.getEntity();
			if (npcEntity != null) {
				location = npcEntity.getLocation();
			} else {
				location = npc.getStoredLocation(); // Can be null
			}

			String shopkeeperCreationError = null; // Null indicates success
			if (location != null) {
				ShopCreationData creationData = AdminShopCreationData.create(
						creator,
						DefaultShopTypes.ADMIN_REGULAR(),
						DefaultShopObjectTypes.CITIZEN(),
						location,
						null
				);
				creationData.setValue(SKCitizensShopObject.CREATION_DATA_NPC_UUID_KEY, npc.getUniqueId());

				Shopkeeper shopkeeper = null;
				if (creator != null) {
					// Handle shopkeeper creation by player:
					shopkeeper = plugin.handleShopkeeperCreation(creationData);
				} else {
					// Create shopkeeper directly (without available creator):
					ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
					try {
						shopkeeper = shopkeeperRegistry.createShopkeeper(creationData);
						assert shopkeeper != null;

						// Save:
						shopkeeper.save();
					} catch (ShopkeeperCreateException e) {
						// Some issue identified during shopkeeper creation (possibly hinting to a bug):
						Log.warning("Failed to create the shopkeeper for Citizens NPC "
								+ CitizensShops.getNPCIdString(npc) + "!", e);
					}
				}

				if (shopkeeper == null) {
					// Shopkeeper creation failed:
					// TODO Translation?
					shopkeeperCreationError = "Shopkeeper creation via the Citizens trait failed."
							+ " Removing the trait again.";
				}
			} else {
				// NPC did not provide any location. We cannot create a shopkeeper without location.
				// TODO Translation?
				shopkeeperCreationError = "Shopkeeper creation via the Citizens trait failed due to"
						+ " missing NPC location. Removing the trait again.";
			}

			if (shopkeeperCreationError != null) {
				// Shopkeeper creation failed:
				Log.warning(shopkeeperCreationError);
				if (creator != null) {
					TextUtils.sendMessage(creator, ChatColor.RED + shopkeeperCreationError);
				}

				// Note: We don't trigger a save of the NPC data when the trait is manually added, so we
				// also don't trigger a save when we remove the trait again here.
				npc.removeTrait(CitizensShopkeeperTrait.class);
			}
		});
	}
}
