package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.util.PlayerArgumentUtils;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.OwnedPlayerShopsResult;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.ObjectUtils;

class CommandRemoveAll extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_NAME = "player:name";
	private static final String ARGUMENT_PLAYER_UUID = "player:uuid";
	private static final String ARGUMENT_ALL_PLAYER_SHOPS = "all-player";
	private static final String ARGUMENT_ALL_PLAYER_SHOPS_DISPLAY_NAME = "player";
	private static final String ARGUMENT_ALL_ADMIN_SHOPS = "all-admin";
	private static final String ARGUMENT_ALL_ADMIN_SHOPS_DISPLAY_NAME = "admin";

	private final ShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;
	private final Confirmations confirmations;

	CommandRemoveAll(
			ShopkeepersPlugin plugin,
			ShopkeeperRegistry shopkeeperRegistry,
			Confirmations confirmations
	) {
		super("removeAll", Arrays.asList("deleteAll"));
		this.plugin = plugin;
		this.shopkeeperRegistry = shopkeeperRegistry;
		this.confirmations = confirmations;

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionRemoveAll);

		// Arguments:
		this.addArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(
						ARGUMENT_ALL_ADMIN_SHOPS,
						Arrays.asList(ARGUMENT_ALL_ADMIN_SHOPS_DISPLAY_NAME)
				).setDisplayName(ARGUMENT_ALL_ADMIN_SHOPS_DISPLAY_NAME),
				new LiteralArgument(
						ARGUMENT_ALL_PLAYER_SHOPS,
						Arrays.asList(ARGUMENT_ALL_PLAYER_SHOPS_DISPLAY_NAME)
				).setDisplayName(ARGUMENT_ALL_PLAYER_SHOPS_DISPLAY_NAME),
				new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
						// TODO Provide completions for known shop owners?
						new PlayerUUIDArgument(ARGUMENT_PLAYER_UUID), // Accepts any uuid
						// Accepts any name, falls back to sender if no name is specified.
						// TODO Add alias 'own'?
						new SenderPlayerNameFallback(new PlayerNameArgument(ARGUMENT_PLAYER_NAME))
				), false) // Don't join formats
		), true, true)); // Join and reverse formats
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PLAYER_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.REMOVE_ALL_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		Player senderPlayer = ObjectUtils.castOrNull(sender, Player.class);
		boolean allPlayers = context.has(ARGUMENT_ALL_PLAYER_SHOPS);
		boolean allAdmin = context.has(ARGUMENT_ALL_ADMIN_SHOPS);
		UUID targetPlayerUUID = context.getOrNull(ARGUMENT_PLAYER_UUID); // Can be null
		String targetPlayerName = context.getOrNull(ARGUMENT_PLAYER_NAME); // Can be null
		assert allPlayers ^ allAdmin ^ (targetPlayerUUID != null ^ targetPlayerName != null);

		boolean targetOwnShops = false;
		if (targetPlayerUUID != null || targetPlayerName != null) {
			// Check if the target matches the sender player:
			String senderName = Unsafe.assertNonNull(sender.getName());
			if (senderPlayer != null
					&& (senderPlayer.getUniqueId().equals(targetPlayerUUID)
							|| senderName.equalsIgnoreCase(targetPlayerName))) {
				targetOwnShops = true;
				// Get missing / exact player information:
				targetPlayerUUID = senderPlayer.getUniqueId();
				targetPlayerName = senderPlayer.getName();
			} else if (targetPlayerName != null) {
				// Check if the target matches an online player:
				// This check is case-insensitive.
				// If the name matches an online player, remove that player's shops (regardless of
				// if the name is ambiguous / if there are shops of other players with matching
				// name).
				Player onlinePlayer = Bukkit.getPlayerExact(targetPlayerName);
				if (onlinePlayer != null) {
					// Get missing / exact player information:
					targetPlayerUUID = onlinePlayer.getUniqueId();
					targetPlayerName = onlinePlayer.getName();
				}
			}
		}

		// Permission checks:
		if (allAdmin) {
			// Remove all admin shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_ADMIN_PERMISSION);
		} else if (allPlayers) {
			// Remove all player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_PLAYER_PERMISSION);
		} else if (targetOwnShops) {
			// Remove own player shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_OWN_PERMISSION);
		} else {
			// Remove other player's shopkeepers:
			this.checkPermission(sender, ShopkeepersPlugin.REMOVE_ALL_OTHERS_PERMISSION);
		}

		// Get the affected shops:
		// Note: Doing this before prompting the command executor for confirmation allows us to
		// detect ambiguous player names and missing player information (the player name/uuid if
		// only the uuid/name is specified).
		List<? extends Shopkeeper> affectedShops;
		if (allAdmin) {
			// Search all admin shops:
			List<Shopkeeper> adminShops = new ArrayList<>();
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (shopkeeper instanceof AdminShopkeeper) {
					adminShops.add(shopkeeper);
				}
			}
			affectedShops = adminShops;
		} else if (allPlayers) {
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
			OwnedPlayerShopsResult ownedPlayerShopsResult = ShopkeeperArgumentUtils.getOwnedPlayerShops(
					targetPlayerUUID,
					targetPlayerName
			);
			assert ownedPlayerShopsResult != null;

			// If the input name is ambiguous, we print an error and require the player to be
			// specified by uuid:
			Map<? extends UUID, ? extends String> matchingShopOwners = ownedPlayerShopsResult.getMatchingShopOwners();
			assert matchingShopOwners != null;
			if (matchingShopOwners.size() > 1) {
				assert targetPlayerName != null;
				boolean ambiguous = PlayerArgumentUtils.handleAmbiguousPlayerName(
						sender,
						targetPlayerName,
						matchingShopOwners.entrySet()
				);
				if (ambiguous) {
					return;
				}
			}

			// Get missing / exact player information:
			targetPlayerUUID = ownedPlayerShopsResult.getPlayerUUID();
			targetPlayerName = ownedPlayerShopsResult.getPlayerName();

			// Get the found shops:
			affectedShops = ownedPlayerShopsResult.getShops();
		}
		assert affectedShops != null;

		int shopsCount = affectedShops.size();
		if (shopsCount == 0) {
			TextUtils.sendMessage(sender, Messages.noShopsFound);
			return;
		}

		UUID finalTargetPlayerUUID = targetPlayerUUID;
		String finalTargetPlayerName = targetPlayerName;
		// This is dangerous: Let the sender first confirm this action.
		confirmations.awaitConfirmation(sender, () -> {
			// Shops that can be hired are only removed for senders with the bypass permission: They
			// are usually meant to be restored to their for-hire state instead of being deleted.
			boolean canRemoveHireableShops = PermissionUtils.hasPermission(
					sender,
					ShopkeepersPlugin.BYPASS_PERMISSION
			);

			// Note: New shops might have been created in the meantime, but the command only affects
			// the already determined affected shops.
			// Remove shops:
			int invalidShops = 0;
			int cancelledDeletions = 0;
			int skippedHireableShops = 0;
			int actualShopCount = 0;
			for (Shopkeeper shopkeeper : affectedShops) {
				// Skip the shopkeeper if it no longer exists:
				if (!shopkeeper.isValid()) {
					invalidShops += 1;
					continue;
				}

				// Skip the shopkeeper if it can be hired and the sender cannot remove such shops:
				if (!canRemoveHireableShops
						&& shopkeeper instanceof PlayerShopkeeper playerShop
						&& playerShop.isHireable()) {
					skippedHireableShops += 1;
					continue;
				}

				if (senderPlayer != null) {
					// Call event:
					PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(
							shopkeeper,
							senderPlayer
					);
					if (deleteEvent.isCancelled()) {
						cancelledDeletions += 1;
						continue;
					}
				}

				shopkeeper.delete(senderPlayer);
				actualShopCount += 1;
			}

			// Trigger save:
			plugin.getShopkeeperStorage().save();

			// Print the result messages:
			if (invalidShops > 0) {
				TextUtils.sendMessage(sender, Messages.shopsAlreadyRemoved,
						"shopsCount", invalidShops
				);
			}
			if (cancelledDeletions > 0) {
				TextUtils.sendMessage(sender, Messages.shopRemovalsCancelled,
						"shopsCount", cancelledDeletions
				);
			}
			if (skippedHireableShops > 0) {
				TextUtils.sendMessage(sender, Messages.hiredShopsNotRemoved,
						"shopsCount", skippedHireableShops
				);
			}
			if (allAdmin) {
				// Removed all admin shops:
				TextUtils.sendMessage(sender, Messages.adminShopsRemoved,
						"shopsCount", actualShopCount
				);
			} else if (allPlayers) {
				// Removed all player shops:
				TextUtils.sendMessage(sender, Messages.playerShopsRemoved,
						"shopsCount", actualShopCount
				);
			} else {
				// Removed all shops of the specified player:
				TextUtils.sendMessage(sender, Messages.shopsOfPlayerRemoved,
						"player", TextUtils.getPlayerText(
								finalTargetPlayerName,
								finalTargetPlayerUUID
						),
						"shopsCount", actualShopCount
				);
			}
		});

		// Inform the sender about required confirmation:
		if (allAdmin) {
			// Removing all admin shops:
			TextUtils.sendMessage(sender, Messages.confirmRemoveAllAdminShops,
					"shopsCount", shopsCount
			);
		} else if (allPlayers) {
			// Removing all player shops:
			TextUtils.sendMessage(sender, Messages.confirmRemoveAllPlayerShops,
					"shopsCount", shopsCount
			);
		} else if (targetOwnShops) {
			// Removing own shops:
			TextUtils.sendMessage(sender, Messages.confirmRemoveAllOwnShops,
					"shopsCount", shopsCount
			);
		} else {
			// Removing shops of specific player:
			TextUtils.sendMessage(sender, Messages.confirmRemoveAllShopsOfPlayer,
					"player", TextUtils.getPlayerText(targetPlayerName, targetPlayerUUID),
					"shopsCount", shopsCount
			);
		}

		// Inform player on how to confirm the action:
		// TODO Add clickable command suggestion?
		TextUtils.sendMessage(sender, Messages.confirmationRequired);
	}
}
