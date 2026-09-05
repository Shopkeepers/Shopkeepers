package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.arguments.UserNameArgument;
import com.nisovin.shopkeepers.commands.arguments.UserUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.SenderPlayerNameFallback;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.UserArgumentUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.ObjectUtils;

class CommandExpiration extends Command {

	private static final String ARGUMENT_PLAYER = "player";
	private static final String ARGUMENT_PLAYER_UUID = ARGUMENT_PLAYER + ":uuid";
	private static final String ARGUMENT_PLAYER_NAME = ARGUMENT_PLAYER + ":name";

	CommandExpiration() {
		super("expiration", Arrays.asList("expiry"));

		// Permission gets checked by testPermission and during execution.

		// Set description:
		this.setDescription(Messages.commandDescriptionExpiration);

		// Arguments:
		// Accept any uuid or user name. We then also support offline user lookup.
		// The name falls back to the sender's name, if the sender is a player.
		this.addArgument(new FirstOfArgument(ARGUMENT_PLAYER, Arrays.asList(
				new UserUUIDArgument(ARGUMENT_PLAYER_UUID),
				new SenderPlayerNameFallback(new UserNameArgument(ARGUMENT_PLAYER_NAME))
		), false)); // Don't join formats
	}

	@Override
	public boolean testPermission(CommandSender sender) {
		if (!super.testPermission(sender)) return false;

		return PermissionUtils.hasPermission(sender, ShopkeepersPlugin.EXPIRATION_OWN_PERMISSION)
				|| PermissionUtils.hasPermission(sender, ShopkeepersPlugin.EXPIRATION_OTHERS_PERMISSION);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		@Nullable Player senderPlayer = ObjectUtils.castOrNull(sender, Player.class);
		@Nullable UUID playerUUID = context.getOrNull(ARGUMENT_PLAYER_UUID);
		@Nullable String playerName = context.getOrNull(ARGUMENT_PLAYER_NAME);
		assert playerUUID != null ^ playerName != null;

		// TODO Move this logic into the argument itself, but avoid looking up the offline player by
		// name more than once per command invocation.
		@Nullable User user = UserArgumentUtils.resolveUser(sender, playerUUID, playerName);
		if (user == null) {
			// Abort. Sender feedback was already handled.
			return;
		}

		// Check permission:
		// This compares the resolved user, so that players are also recognized as targeting their
		// own shops when they specify their display name.
		boolean targetOwnShops = senderPlayer != null
				&& senderPlayer.getUniqueId().equals(user.getUniqueId());
		if (targetOwnShops) {
			this.checkPermission(sender, ShopkeepersPlugin.EXPIRATION_OWN_PERMISSION);
		} else {
			this.checkPermission(sender, ShopkeepersPlugin.EXPIRATION_OTHERS_PERMISSION);
		}

		SKShopkeepersPlugin.getInstance()
				.getPlayerShops()
				.getPlayerShopsExpiration()
				.getNotifier()
				.sendExpiringShopsList(sender, user);
	}
}
