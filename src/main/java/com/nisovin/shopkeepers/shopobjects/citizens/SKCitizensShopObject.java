package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.citizens.CitizensShopObject;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObject;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;

/**
 * Note: This relies on the regular living entity shopkeeper interaction handling.
 * TODO separate this?
 */
public class SKCitizensShopObject extends AbstractEntityShopObject implements CitizensShopObject {

	public static String CREATION_DATA_NPC_UUID_KEY = "CitizensNpcUUID";

	protected final CitizensShops citizensShops;
	private UUID npcUniqueId = null;
	private Integer npcLegacyId = null;
	// if false, this will not remove the npc on deletion:
	private boolean destroyNPC = true;

	protected SKCitizensShopObject(CitizensShops citizensShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.citizensShops = citizensShops;
		if (creationData != null) {
			// can be null here, as currently only NPC shopkeepers created by the shopkeeper trait provide the npc's
			// unique id via the creation data:
			this.npcUniqueId = creationData.getValue(CREATION_DATA_NPC_UUID_KEY);
		}
	}

	@Override
	public SKCitizensShopObjectType getType() {
		return SKDefaultShopObjectTypes.CITIZEN();
	}

	// can be null if not set yet
	public UUID getNPCUniqueId() {
		return npcUniqueId;
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		if (configSection.contains("npcId")) {
			// legacy conversion from integer ids
			// TODO remove again at some point
			if (configSection.isInt("npcId")) {
				npcLegacyId = configSection.getInt("npcId");
			} else {
				String npcUniqueIdString = configSection.getString("npcId");
				this.npcUniqueId = ConversionUtils.parseUUID(npcUniqueIdString);
				if (npcUniqueId == null) {
					Log.warning("Couldn't parse NPC unique id for shopkeeper " + shopkeeper.getId() + ": " + npcUniqueIdString);
				}
			}
		}
	}

	// TODO remove again at some point
	// gets called from CitizensShops
	void convertLegacyId() {
		if (npcLegacyId != null && citizensShops.isEnabled()) {
			assert npcUniqueId == null;
			NPC npc = CitizensAPI.getNPCRegistry().getById(npcLegacyId);
			if (npc != null) {
				Log.info("Citizens shopkeeper id conversion: Mapping shopkeeper " + shopkeeper.getId() + " to NPC " + CitizensShops.getNPCIdString(npc));
				npcUniqueId = npc.getUniqueId();
				npcLegacyId = null;
				shopkeeper.markDirty();

				// re-activate by new object id:
				SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperObjectIdChanged(shopkeeper);
			}
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		if (npcUniqueId != null) {
			configSection.set("npcId", npcUniqueId.toString());
		} else if (npcLegacyId != null) {
			// TODO remove again at some point
			configSection.set("npcId", npcLegacyId);
		}
	}

	@Override
	public void setup() {
		super.setup();
		if (npcLegacyId != null || npcUniqueId != null || !citizensShops.isEnabled()) return;

		// create npc:
		Log.debug(() -> "Creating citizens NPC for shopkeeper " + shopkeeper.getId());

		EntityType entityType;
		String name;
		if (shopkeeper instanceof PlayerShopkeeper) {
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper;
			// player shops will use a player npc:
			entityType = EntityType.PLAYER;
			User owner = playerShopkeeper.getOwner();
			name = owner.getName();
		} else {
			entityType = EntityType.VILLAGER;
			name = "Shopkeeper";
		}

		// create npc:
		Location spawnLocation = this.getSpawnLocation();
		npcUniqueId = citizensShops.createNPC(spawnLocation, entityType, name);
		shopkeeper.markDirty();
	}

	// LIFE CYCLE

	protected void setKeepNPCOnDeletion() {
		destroyNPC = false;
	}

	@Override
	public void delete() {
		if (destroyNPC) {
			NPC npc = this.getNPC();
			if (npc != null) {
				if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
					// let the trait handle npc related cleanup:
					npc.getTrait(CitizensShopkeeperTrait.class).onShopkeeperRemove();
				} else {
					Log.debug(() -> "Removing citizens NPC " + CitizensShops.getNPCIdString(npc) + " due to deletion of shopkeeper " + shopkeeper.getId());
					npc.destroy(); // the npc was created by us, so we remove it again
				}
			} else {
				// TODO: npc not yet loaded or citizens not enabled: how to remove the citizens npc later?
				// usually not a problem, because players cannot delete citizens shopkeepers if the corresponding
				// citizens npc isn't present in the world (exception: deletion via commands..)
			}
		}
		npcUniqueId = null;
		shopkeeper.markDirty();
	}

	// ACTIVATION

	public NPC getNPC() {
		if (npcUniqueId == null) return null;
		if (!citizensShops.isEnabled()) return null;
		return CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId);
	}

	@Override
	public Entity getEntity() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getEntity();
	}

	@Override
	public boolean isActive() {
		return (this.getNPC() != null);
	}

	@Override
	public String getId() {
		if (npcUniqueId == null) {
			if (npcLegacyId == null) return null;
			else {
				return this.getType().createObjectId(npcLegacyId);
			}
		}
		return this.getType().createObjectId(npcUniqueId);
	}

	private Location getSpawnLocation() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		return new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + 0.5D, shopkeeper.getZ() + 0.5D);
	}

	@Override
	public boolean needsSpawning() {
		return false; // spawning and despawning is handled by citizens
	}

	@Override
	public boolean spawn() {
		return false; // handled by citizens
	}

	@Override
	public void despawn() {
		// handled by citizens
	}

	@Override
	public Location getLocation() {
		Entity entity = this.getEntity();
		return entity != null ? entity.getLocation() : null;
	}

	@Override
	public boolean check() {
		NPC npc = this.getNPC();
		if (npc == null) {
			// Not going to force Citizens creation, this seems like it could go really wrong.
		} else {
			Location currentLocation = npc.getStoredLocation();
			Location expectedLocation = this.getSpawnLocation();
			if (currentLocation == null) {
				npc.teleport(expectedLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
				Log.debug(() -> "Shopkeeper NPC (" + shopkeeper.getPositionString() + ") had no location, teleported");
			} else if (!currentLocation.getWorld().equals(expectedLocation.getWorld()) || currentLocation.distanceSquared(expectedLocation) > 1.0D) {
				shopkeeper.setLocation(currentLocation);
				Log.debug(() -> "Shopkeeper NPC (" + shopkeeper.getPositionString() + ") out of place, re-indexing");
			}
		}
		return false;
	}

	@Override
	public void onShopkeeperOwnerChanged() {
		if (!Settings.allowRenamingOfPlayerNpcShops) {
			// Update the NPC's name:
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper;
			User owner = playerShopkeeper.getOwner();
			this.setName(owner.getName());
		}
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		// Citizens uses different limits depending on the npc type (64 for mobs, 46 for players)
		// Citizens might dynamically truncate the name of the corresponding npc, and will then print a warning
		NPC npc = this.getNPC();
		if (npc != null && npc.getTrait(MobType.class).getType() == EntityType.PLAYER) return 46;
		return 64;
	}

	@Override
	public void setName(String name) {
		NPC npc = this.getNPC();
		if (npc == null) return;

		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.prepareName(name);
			// set entity name plate:
			npc.setName(name);
			// this.entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			npc.setName("");
			// this.entity.setCustomNameVisible(false);
		}
	}

	@Override
	public String getName() {
		NPC npc = this.getNPC();
		if (npc == null) return null;
		return npc.getFullName();
	}

	// EDITOR ACTIONS

	// TODO: Support sub types? A menu of entity types here would be cool
	// TODO: Support equipping items? Is there a generic Citizens API for this?
}
