package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFallbackArgument;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link FallbackArgument} that returns the {@link User} for the sender if it is a player, without consuming any
 * arguments.
 * <p>
 * If the sender is not a player, a {@link RequiresPlayerArgumentException} is thrown.
 */
public class SenderUserFallback extends TypedFallbackArgument<User> {

	public static class SenderUserArgument extends CommandArgument<User> {

		public SenderUserArgument(String name) {
			super(name);
		}

		@Override
		public boolean isOptional() {
			return true; // does not require user input
		}

		@Override
		public User parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.requiresPlayerError();
			} else {
				Player player = (Player) sender;
				// We expect the player to still be online and therefore the user to be cached:
				User user = ShopkeepersAPI.getUserManager().getAssertedUser(player);
				assert user != null;
				return user;
			}
		}

		@Override
		public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
			return Collections.emptyList();
		}
	}

	public SenderUserFallback(CommandArgument<User> argument) {
		super(argument, new SenderUserArgument(Validate.notNull(argument).getName()));
	}
}
