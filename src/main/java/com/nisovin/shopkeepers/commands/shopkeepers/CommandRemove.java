package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.PlayerUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.OwnedPlayerShopsResult;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandRemove extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_NAME = "player:name";
	private static final String ARGUMENT_PLAYER_UUID = "player:uuid";
	private static final String ARGUMENT_ALL = "all";
	private static final String ARGUMENT_ADMIN = "admin";

	private final ShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;
	private final Confirmations confirmations;

	CommandRemove(ShopkeepersPlugin plugin, ShopkeeperRegistry shopkeeperRegistry, Confirmations confirmations) {
		super("remove", Arrays.asList("delete"));
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.confirmations = confirmations;

		// Permission gets checked by testPermission and during execution

		// Set description:
		this.setDescription(Settings.msgCommandDescriptionRemove);

		// Arguments: TODO allow specifying a single shopkeeper?
		this.addArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				new LiteralArgument(ARGUMENT_ALL),
				new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
						// TODO provide completions for known shop owners?
						new PlayerUUIDArgument(ARGUMENT_PLAYER_UUID), // accepts any uuid
						// accepts any name, falls back to sender if no name is specified
						// TODO add alias 'own'?
						new SenderPlayerNameFallback(new PlayerNameArgument(ARGUMENT_PLAYER_NAME))
				), false) // don't join formats
		), true, true)); // join and reverse formats
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		Player senderPlayer = (sender instanceof Player) ? (Player) sender : null;
		boolean all = context.has(ARGUMENT_ALL);
		boolean admin = context.has(ARGUMENT_ADMIN);
		UUID targetPlayerUUID = context.get(ARGUMENT_PLAYER_UUID); // can be null
		String targetPlayerName = context.get(ARGUMENT_PLAYER_NAME); // can be null
		assert all ^ admin ^ (targetPlayerUUID != null ^ targetPlayerName != null); // xor

		boolean targetOwnShops = false;
		if (targetPlayerUUID != null || targetPlayerName != null) {
			// Check if the target matches the sender player:
			if (senderPlayer != null && (senderPlayer.getUniqueId().equals(targetPlayerUUID) || senderPlayer.getName().equalsIgnoreCase(targetPlayerName))) {
				targetOwnShops = true;
				// Get missing / exact player information:
				targetPlayerUUID = senderPlayer.getUniqueId();
				targetPlayerName = senderPlayer.getName();
			} else if (targetPlayerName != null) {
				// Check if the target matches an online player:
				// If the name matches an online player, remove that player's shops (regardless of if the name is
				// ambiguous / if there are shops of other players with matching name):
				Player onlinePlayer = Bukkit.getPlayerExact(targetPlayerName); // note: case insensitive
				if (onlinePlayer != null) {
					// Get missing / exact player information:
					targetPlayerUUID = onlinePlayer.getUniqueId();
					targetPlayerName = onlinePlayer.getName();
				}
			}
		}

		// permission checks:
		if (admin) {
			// remove all admin shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ADMIN_PERMISSION);
		} else if (all) {
			// remove all player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PERMISSION);
		} else if (targetOwnShops) {
			// remove own player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OWN_PERMISSION);
		} else {
			// remove other player's shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_OTHERS_PERMISSION);
		}

		// Get the affected shops:
		// Note: Doing this before prompting the command executor for confirmation allows us to detect ambiguous player
		// names and missing player information (the player name/uuid if only the uuid/name is specified).
		List<? extends Shopkeeper> affectedShops;
		if (admin) {
			// Search all admin shops:
			List<Shopkeeper> adminShops = new ArrayList<>();
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof AdminShopkeeper) {
					adminShops.add(shopkeeper);
				}
			}
			affectedShops = adminShops;
		} else if (all) {
			// Search all player shops:
			List<Shopkeeper> playerShops = new ArrayList<>();
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					playerShops.add(shopkeeper);
				}
			}
			affectedShops = playerShops;
		} else {
			assert targetPlayerUUID != null ^ targetPlayerName != null;
			// Search for shops owned by the target player:
			OwnedPlayerShopsResult ownedPlayerShopsResult = ShopkeeperUtils.getOwnedPlayerShops(targetPlayerUUID, targetPlayerName);
			assert ownedPlayerShopsResult != null;

			// If the input name is ambiguous, we print an error and require the player to be specified by uuid:
			Set<User> matchingShopOwners = ownedPlayerShopsResult.getMatchingShopOwners();
			assert matchingShopOwners != null;
			if (PlayerUtils.handleAmbiguousPlayerName(sender, targetPlayerName, matchingShopOwners)) {
				return;
			}

			// Get missing / exact player information:
			targetPlayerUUID = ownedPlayerShopsResult.getPlayerUUID();
			targetPlayerName = ownedPlayerShopsResult.getPlayerName();

			// Get the found shops:
			affectedShops = ownedPlayerShopsResult.getShops();
		}
		assert affectedShops != null;

		UUID finalTargetPlayerUUID = targetPlayerUUID;
		String finalTargetPlayerName = targetPlayerName;
		// This is dangerous: Let the sender first confirm this action.
		confirmations.awaitConfirmation(sender, () -> {
			// Note: New shops might have been created in the meantime, but the command only affects the already
			// determined affected shops.
			// Remove shops:
			int actualShopCount = 0;
			for (Shopkeeper shopkeeper : affectedShops) {
				// Skip the shopkeeper if it no longer exists:
				if (!shopkeeper.isValid()) continue;

				if (senderPlayer != null) {
					// Call event:
					PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(shopkeeper, senderPlayer);
					if (deleteEvent.isCancelled()) {
						continue;
					}
				}

				shopkeeper.delete(senderPlayer);
				actualShopCount += 1;
			}

			// Trigger save:
			plugin.getShopkeeperStorage().save();

			// Print the result message:
			if (admin) {
				// Removed all admin shops:
				TextUtils.sendMessage(sender, Settings.msgRemovedAdminShops,
						"shopsCount", actualShopCount
				);
			} else if (all) {
				// Removed all player shops:
				TextUtils.sendMessage(sender, Settings.msgRemovedPlayerShops,
						"shopsCount", actualShopCount
				);
			} else {
				// Removed all shops of the specified player:
				TextUtils.sendMessage(sender, Settings.msgRemovedShopsOfPlayer,
						"player", TextUtils.getPlayerText(finalTargetPlayerName, finalTargetPlayerUUID),
						"shopsCount", actualShopCount
				);
			}
		});

		// TODO print 'no shops found' if shop count is 0?

		// Inform the sender about required confirmation:
		int shopsCount = affectedShops.size();
		if (admin) {
			// Removing all admin shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAllAdminShops,
					"shopsCount", shopsCount
			);
		} else if (all) {
			// Removing all player shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAllPlayerShops,
					"shopsCount", shopsCount
			);
		} else if (targetOwnShops) {
			// Removing own shops:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAllOwnShops,
					"shopsCount", shopsCount
			);
		} else {
			// Removing shops of specific player:
			TextUtils.sendMessage(sender, Settings.msgConfirmRemoveAllShopsOfPlayer,
					"player", TextUtils.getPlayerText(targetPlayerName, targetPlayerUUID),
					"shopsCount", shopsCount
			);
		}
		// Inform player on how to confirm the action:
		// TODO add clickable command suggestion?
		TextUtils.sendMessage(sender, Settings.msgConfirmationRequired);
	}
}
