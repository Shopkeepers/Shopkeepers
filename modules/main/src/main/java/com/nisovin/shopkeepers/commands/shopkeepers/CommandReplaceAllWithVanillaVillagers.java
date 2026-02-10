package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.MerchantRecipe;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.compat.Compat;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.living.types.VillagerShop;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.logging.Log;

class CommandReplaceAllWithVanillaVillagers extends Command {

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;
	private final Confirmations confirmations;

	CommandReplaceAllWithVanillaVillagers(
			SKShopkeepersPlugin plugin,
			SKShopkeeperRegistry shopkeeperRegistry,
			Confirmations confirmations
	) {
		super("replaceAllWithVanillaVillagers");

		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.confirmations = confirmations;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionReplaceAllWithVanillaVillagers);

		// Hidden utility command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		// Additionally required permissions:
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PLAYER_PERMISSION)
				&& PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		int shopsCount = shopkeeperRegistry.getAllShopkeepers().size();
		if (shopsCount == 0) {
			TextUtils.sendMessage(sender, Messages.noShopsFound);
			return;
		}

		// This is dangerous: Let the sender first confirm this action.
		confirmations.awaitConfirmation(sender, () -> replaceAllShopsWithVillagers(sender));

		// Inform the player on how to confirm the action:
		TextUtils.sendMessage(sender, Messages.confirmReplaceAllShopsWithVanillaVillagers,
				"shopsCount", shopsCount
		);
		// TODO Add clickable command suggestion?
		TextUtils.sendMessage(sender, Messages.confirmationRequired);
	}

	private void replaceAllShopsWithVillagers(CommandSender sender) {
		List<? extends AbstractShopkeeper> shopkeepers = new ArrayList<>(shopkeeperRegistry.getAllShopkeepers());
		if (shopkeepers.isEmpty()) {
			TextUtils.sendMessage(sender, Messages.noShopsFound);
			return;
		}

		int invalidShops = 0;
		int deletedAdminShopsCount = 0;
		int deletedPlayerShopsCount = 0;
		int skippedShopsCount = 0;
		for (AbstractShopkeeper shopkeeper : shopkeepers) {
			// Skip the shopkeeper if it no longer exists:
			if (!shopkeeper.isValid()) {
				invalidShops++;
				continue;
			}

			// Note: No need to call a PlayerDeleteShopkeeperEvent here, or pass the sender player
			// to shopkeeper.delete(): This action is run by an admin with the intention to
			// delete/replace all shopkeepers. There is no need to perform additional deletion logic
			// (e.g. returning shop creation items, etc.).

			// Try to spawn a corresponding vanilla villager:
			// A villager is spawned regardless of the object type (sign, NPC, other mob type,
			// etc.).
			if (!this.spawnVanillaVillager(sender, shopkeeper)) {
				skippedShopsCount++;
				continue;
			}

			// Delete the shopkeeper:
			shopkeeper.delete();
			if (shopkeeper instanceof PlayerShopkeeper) {
				deletedPlayerShopsCount++;
			} else {
				deletedAdminShopsCount++;
			}
		}

		// Trigger save:
		plugin.getShopkeeperStorage().save();

		// Print the result messages:
		if (invalidShops > 0) {
			TextUtils.sendMessage(sender, Messages.shopsAlreadyRemoved,
					"shopsCount", invalidShops
			);
		}
		TextUtils.sendMessage(sender, Messages.allShopsReplacedWithVanillaVillagers,
				"adminShopsCount", deletedAdminShopsCount,
				"playerShopsCount", deletedPlayerShopsCount,
				"skippedShopsCount", skippedShopsCount
		);
	}

	private boolean spawnVanillaVillager(CommandSender sender, AbstractShopkeeper shopkeeper) {
		assert shopkeeper.isValid();
		if (shopkeeper.isVirtual()) {
			String message = shopkeeper.getLogPrefix() + "Skipping virtual shopkeeper.";
			if (!(sender instanceof ConsoleCommandSender)) {
				Log.debug(message);
			}
			TextUtils.sendMessage(sender, message);
			return false;
		}

		Location spawnLocation = shopkeeper.getLocation();
		if (spawnLocation == null) {
			String message = shopkeeper.getLogPrefix() + "Skipping shopkeeper without location."
					+ " Is world '" + shopkeeper.getWorldName() + "' loaded?";
			if (!(sender instanceof ConsoleCommandSender)) {
				Log.debug(message);
			}
			TextUtils.sendMessage(sender, message);
			return false;
		}

		// Adjust the spawn location: Center of block.
		// TODO Adjust similar to villager shopkeepers.
		spawnLocation.add(0.5D, 0.0D, 0.5D);

		AbstractShopObject shopObject = shopkeeper.getShopObject();
		boolean isShopkeeperSpawned = shopObject.isSpawned();

		Class<? extends Entity> entityClass = Unsafe.assertNonNull(EntityType.VILLAGER.getEntityClass());
		World world = Unsafe.assertNonNull(spawnLocation.getWorld());
		try {
			// Despawn the shopkeeper first to not interfere with the spawning of the replacement
			// villager:
			shopObject.despawn();

			world.spawn(spawnLocation, entityClass, entity -> {
				assert entity != null;
				// Note: This callback is run after the entity has been prepared (this includes the
				// creation of random equipment and the random spawning of passengers) and right
				// before the entity gets added to the world (which triggers the corresponding
				// CreatureSpawnEvent).

				// Prepare entity, before it gets spawned:
				prepareEntity((Villager) entity, shopkeeper);

				// Try to bypass entity-spawn blocking plugins:
				plugin.getForcingEntitySpawner().forceEntitySpawn(spawnLocation, EntityType.VILLAGER);
			});
		} catch (Exception e) {
			String message = shopkeeper.getLogPrefix()
					+ "Failed to spawn corresponding vanilla villager.";
			if (!(sender instanceof ConsoleCommandSender)) {
				Log.debug(e, () -> message);
			}
			TextUtils.sendMessage(sender, message);

			// Try to respawn the shopkeeper:
			if (isShopkeeperSpawned) {
				SchedulerUtils.runTaskOrOmit(shopkeeper.getLocation(), () -> {
					shopObject.spawn();
				});
			}

			return false;
		}

		return true;
	}

	// Similar preparations as when spawning a villager shopkeeper:
	private void prepareEntity(Villager entity, Shopkeeper shopkeeper) {
		// Apply name:
		this.applyName(entity, shopkeeper.getName());

		// Clear equipment:
		EntityEquipment equipment = entity.getEquipment();
		if (equipment != null) {
			equipment.clear();
		}

		entity.setRemoveWhenFarAway(false);
		entity.setCanPickupItems(false);
		entity.setInvulnerable(true);
		entity.setAdult();
		entity.setBreed(false);
		entity.setAgeLock(true);
		entity.setAI(false);
		if (Settings.silenceShopEntities) {
			entity.setSilent(true);
		}
		if (Settings.disableGravity) {
			entity.setGravity(false);
		}
		Compat.getProvider().setOnGround(entity, true);

		// Lock the villager profession and trades:
		entity.setVillagerExperience(1);

		// Preserve villager shopkeeper properties:
		if (shopkeeper.getShopObject() instanceof VillagerShop) {
			VillagerShop villagerShop = (VillagerShop) shopkeeper.getShopObject();
			entity.setProfession(villagerShop.getProfession());
			entity.setVillagerType(villagerShop.getVillagerType());
			entity.setVillagerLevel(villagerShop.getVillagerLevel());
			// Note: We ignore the baby state, since baby villagers cannot be traded with.
			// We also ignore custom entity equipment currently, since the villagers might drop this
			// equipment, for example on death, which might not be intended.
		}

		this.applyTradingRecipes(entity, shopkeeper);
	}

	private void applyName(Villager entity, @Nullable String name) {
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			String preparedName = Messages.nameplatePrefix + name;
			// Set entity name plate:
			entity.setCustomName(preparedName);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// Remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	private void applyTradingRecipes(Villager entity, Shopkeeper shopkeeper) {
		List<? extends TradingRecipe> tradingRecipes = shopkeeper.getTradingRecipes(null);
		List<MerchantRecipe> merchantRecipes = MerchantUtils.createMerchantRecipes(tradingRecipes);
		entity.setRecipes(merchantRecipes);
	}
}
