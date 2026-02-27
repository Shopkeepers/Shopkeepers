package com.nisovin.shopkeepers.commands.shopkeepers;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;

class CommandReload extends Command {

	private final SKShopkeepersPlugin plugin;

	CommandReload(SKShopkeepersPlugin plugin) {
		super("reload");
		this.plugin = plugin;

		// Set permission:
		this.setPermission(ShopkeepersPlugin.RELOAD_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionReload);
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();

		// Reload plugin:
		plugin.reload();
		sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
	}
}
