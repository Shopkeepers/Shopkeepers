package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.arguments.UserNameArgument;
import com.nisovin.shopkeepers.commands.arguments.UserUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.commands.util.UserArgumentUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandTransfer extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_NEW_OWNER = "new-owner";
	private static final String ARGUMENT_NEW_OWNER_UUID = "new-owner:uuid";
	private static final String ARGUMENT_NEW_OWNER_NAME = "new-owner:name";

	CommandTransfer() {
		super("transfer");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.TRANSFER_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionTransfer);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.PLAYER
								.and(ShopkeeperFilter.withAccessLevel(DefaultPlayerShopAccessLevels.FULL()))),
				TargetShopkeeperFilter.PLAYER
		));
		// Accept any uuid or user name. We then also supports offline player lookup.
		this.addArgument(new FirstOfArgument(ARGUMENT_NEW_OWNER, Arrays.asList(
				new UserUUIDArgument(ARGUMENT_NEW_OWNER_UUID),
				new UserNameArgument(ARGUMENT_NEW_OWNER_NAME)
		), false)); // Don't join formats
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		AbstractPlayerShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		UUID newOwnerUUID = context.getOrNull(ARGUMENT_NEW_OWNER_UUID); // Can be null
		String newOwnerName = context.getOrNull(ARGUMENT_NEW_OWNER_NAME); // Can be null
		assert newOwnerUUID != null ^ newOwnerName != null;

		// TODO Move this logic into the argument itself, but avoid looking up the offline player by
		// name more than once per command invocation.
		@Nullable User newOwner = UserArgumentUtils.resolveUser(sender, newOwnerUUID, newOwnerName);
		if (newOwner == null) {
			// Abort. Sender feedback was already handled.
			return;
		}

		// Check access:
		if (!shopkeeper.checkAccess(sender, DefaultPlayerShopAccessLevels.FULL(), false)) {
			return;
		}

		// Reset the for-hire state, as if the new owner just hired the shop:
		// TODO Also automatically reset the previous trade offers here? However, admins would then
		// no longer be able to only change the shop owner without also clearing the shop offers.
		shopkeeper.setHired();

		// Set new owner:
		shopkeeper.setOwner(newOwner);

		// Success:
		TextUtils.sendMessage(sender, Messages.ownerSet,
				"owner", TextUtils.getPlayerText(newOwner)
		);

		// Inform the new owner, if they are online:
		@Nullable Player newOwnerPlayer = newOwner.getPlayer();
		if (newOwnerPlayer != null) {
			String shopName = shopkeeper.getName(); // Can be empty
			TextUtils.sendMessage(newOwnerPlayer, Messages.shopReceived,
					"shopName", (shopName.isEmpty() ? "" : (shopName + " ")),
					"location", shopkeeper.getPositionString()
			);

			// Additionally inform them about the shop's expiration, if applicable:
			SKShopkeepersPlugin.getInstance()
					.getPlayerShops()
					.getPlayerShopsExpiration()
					.getNotifier()
					.informAboutExpiration(newOwnerPlayer, shopkeeper);
		}

		// Save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
