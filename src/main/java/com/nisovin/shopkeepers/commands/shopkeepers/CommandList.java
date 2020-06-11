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
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.AnyStringFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.DefaultValueFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerByNameArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.commands.lib.arguments.TransformedArgument;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.PlayerUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.OwnedPlayerShopsResult;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandList extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_NAME = "player:name";
	private static final String ARGUMENT_PLAYER_UUID = "player:uuid";
	private static final String ARGUMENT_ADMIN = "admin";
	private static final String ARGUMENT_PAGE = "page";

	private static final int ENTRIES_PER_PAGE = 8;

	private final ShopkeeperRegistry shopkeeperRegistry;

	CommandList(ShopkeeperRegistry shopkeeperRegistry) {
		super("list");
		this.shopkeeperRegistry = shopkeeperRegistry;

		// permission gets checked by testPermission and during execution

		// set description:
		this.setDescription(Settings.msgCommandDescriptionList);

		// arguments:
		this.addArgument(new FirstOfArgument("target", Arrays.asList(
				new LiteralArgument(ARGUMENT_ADMIN),
				new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
						// TODO provide completions for known shop owners?
						new PlayerUUIDArgument(ARGUMENT_PLAYER_UUID), // accepts any uuid
						// only accepts names of online players initially, but falls back to any given name or the
						// sender's name (using a fallback to give the following page argument a chance to parse the
						// input first)
						// TODO add alias 'own'?
						new SenderPlayerNameFallback(new AnyStringFallback(
								new TransformedArgument<>(
										new PlayerByNameArgument(ARGUMENT_PLAYER_NAME),
										(player) -> player.getName()
								)
						))
				), false) // don't join formats
		), true, true)); // join and reverse formats
		this.addArgument(new DefaultValueFallback<>(new PositiveIntegerArgument(ARGUMENT_PAGE), 1));
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;
		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_OTHERS_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		int page = context.get(ARGUMENT_PAGE);
		boolean listAdminShops = context.has(ARGUMENT_ADMIN); // can be null
		UUID targetPlayerUUID = context.get(ARGUMENT_PLAYER_UUID); // can be null
		String targetPlayerName = context.get(ARGUMENT_PLAYER_NAME); // can be null
		assert listAdminShops ^ (targetPlayerUUID != null ^ targetPlayerName != null); // xor

		List<? extends Shopkeeper> shops;
		if (listAdminShops) {
			// permission check:
			this.checkPermission(sender, ShopkeepersPlugin.LIST_ADMIN_PERMISSION);

			// searching admin shops:
			List<Shopkeeper> adminShops = new ArrayList<>();
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
				if (!(shopkeeper instanceof PlayerShopkeeper)) {
					adminShops.add(shopkeeper);
				}
			}
			shops = adminShops;
		} else {
			// check if the target matches the sender player:
			boolean targetOwnShops = false;
			Player senderPlayer = (sender instanceof Player) ? (Player) sender : null;
			if (senderPlayer != null && (senderPlayer.getUniqueId().equals(targetPlayerUUID) || senderPlayer.getName().equalsIgnoreCase(targetPlayerName))) {
				targetOwnShops = true;
				// get missing / exact player information:
				targetPlayerUUID = senderPlayer.getUniqueId();
				targetPlayerName = senderPlayer.getName();
			} else if (targetPlayerName != null) {
				// check if the target matches an online player:
				// if the name matches an online player, list that player's shops (regardless of if the name is
				// ambiguous / if there are shops of other players with matching name):
				Player onlinePlayer = Bukkit.getPlayerExact(targetPlayerName); // note: case insensitive
				if (onlinePlayer != null) {
					// get missing / exact player information:
					targetPlayerUUID = onlinePlayer.getUniqueId();
					targetPlayerName = onlinePlayer.getName();
				}
			}

			// permission check:
			if (targetOwnShops) {
				// list own player shopkeepers:
				this.checkPermission(sender, ShopkeepersPlugin.LIST_OWN_PERMISSION);
			} else {
				// list other player shopkeepers:
				this.checkPermission(sender, ShopkeepersPlugin.LIST_OTHERS_PERMISSION);
			}

			// search for shops owned by the target player:
			OwnedPlayerShopsResult ownedPlayerShopsResult = ShopkeeperUtils.getOwnedPlayerShops(targetPlayerUUID, targetPlayerName);
			assert ownedPlayerShopsResult != null;

			// if the input name is ambiguous, we print an error and require the player to be specified by uuid:
			Set<User> matchingShopOwners = ownedPlayerShopsResult.getMatchingShopOwners();
			assert matchingShopOwners != null;
			if (PlayerUtils.handleAmbiguousPlayerName(sender, targetPlayerName, matchingShopOwners)) {
				return;
			}

			// get missing / exact player information:
			targetPlayerUUID = ownedPlayerShopsResult.getPlayerUUID();
			targetPlayerName = ownedPlayerShopsResult.getPlayerName(); // can still be null

			// get found shops:
			shops = ownedPlayerShopsResult.getShops();
		}
		assert shops != null;

		int shopsCount = shops.size();
		int maxPage = Math.max(1, (int) Math.ceil((double) shopsCount / ENTRIES_PER_PAGE));
		page = Math.max(1, Math.min(page, maxPage));

		if (listAdminShops) {
			// listing admin shops:
			TextUtils.sendMessage(sender, Settings.msgListAdminShopsHeader,
					"shopsCount", shopsCount,
					"page", page,
					"maxPage", maxPage
			);
		} else {
			// listing player shops:
			TextUtils.sendMessage(sender, Settings.msgListPlayerShopsHeader,
					"player", TextUtils.getPlayerText(targetPlayerName, targetPlayerUUID),
					"shopsCount", shopsCount,
					"page", page,
					"maxPage", maxPage
			);
		}

		int startIndex = (page - 1) * ENTRIES_PER_PAGE;
		int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, shopsCount);
		for (int index = startIndex; index < endIndex; index++) {
			Shopkeeper shopkeeper = shops.get(index);
			String shopName = shopkeeper.getName(); // can be empty
			// TODO add shop info as hover text
			// TODO add owner name/uuid as message arguments?
			// TODO move into shopkeeper
			TextUtils.sendMessage(sender, Settings.msgListShopsEntry,
					"shopIndex", (index + 1),
					"shopUUID", shopkeeper.getUniqueId().toString(),
					// deprecated, use {shopId} instead; TODO remove at some point
					"shopSessionId", shopkeeper.getId(),
					"shopId", shopkeeper.getId(),
					// TODO find a better solution for this special cae, since this is specific to the used format
					// maybe by supporting conditional prefixes/suffixes for placeholders inside the format Strings?
					"shopName", (shopName.isEmpty() ? "" : (shopName + " ")),
					"location", shopkeeper.getPositionString(),
					"shopType", shopkeeper.getType().getIdentifier(),
					"objectType", shopkeeper.getShopObject().getType().getIdentifier()
			);
		}
	}
}
