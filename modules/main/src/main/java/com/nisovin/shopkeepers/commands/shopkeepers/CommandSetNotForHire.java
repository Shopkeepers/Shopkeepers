package com.nisovin.shopkeepers.commands.shopkeepers;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandSetNotForHire extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";

	CommandSetNotForHire() {
		super("setNotForHire");

		// Set permission (same permission as the setForHire command):
		this.setPermission(ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionSetnotforhire);

		// Arguments:
		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.PLAYER
								.and(ShopkeeperFilter.withAccessLevel(DefaultPlayerShopAccessLevels.FULL()))),
				TargetShopkeeperFilter.PLAYER
		));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		AbstractPlayerShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);

		// Check access:
		if (!shopkeeper.checkAccess(sender, DefaultPlayerShopAccessLevels.FULL(), false)) {
			return;
		}

		// Clear the for-hire state (this also clears the hire cost item):
		shopkeeper.setForHire((UnmodifiableItemStack) null);

		// Success:
		TextUtils.sendMessage(sender, Messages.setNotForHire);

		// Save:
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}
}
