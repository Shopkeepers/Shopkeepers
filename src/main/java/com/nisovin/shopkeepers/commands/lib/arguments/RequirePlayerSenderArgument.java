package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.RequiresPlayerArgumentException;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link CommandArgument} which require the sender to be a player and then delegates the argument parsing to another
 * {@link CommandArgument}.
 * <p>
 * If the sender is not a player, a {@link RequiresPlayerArgumentException} is thrown.
 */
public class RequirePlayerSenderArgument<T> extends FallbackArgument<T> {

	protected final CommandArgument<T> argument;

	public RequirePlayerSenderArgument(CommandArgument<T> argument) {
		super(argument.getName());
		Validate.notNull(argument, "Argument is null!");
		argument.setParent(this);
		this.argument = argument;
	}

	@Override
	public boolean isOptional() {
		return argument.isOptional();
	}

	@Override
	public String getReducedFormat() {
		return argument.getReducedFormat();
	}

	private void validateSenderIsPlayer(CommandInput input) throws ArgumentParseException {
		CommandSender sender = input.getSender();
		if (!(sender instanceof Player)) {
			throw this.requiresPlayerError();
		}
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		this.validateSenderIsPlayer(input);
		return argument.parseValue(input, context, argsReader);
	}

	@Override
	public T parse(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		this.validateSenderIsPlayer(input);
		return argument.parse(input, context, argsReader);
	}

	@Override
	public T parseFallback(CommandInput input, CommandContext context, ArgumentsReader argsReader, FallbackArgumentException fallbackException, boolean parsingFailed) throws ArgumentParseException {
		assert argument instanceof FallbackArgument; // else we would not have reached this state
		return ((FallbackArgument<T>) argument).parseFallback(input, context, argsReader, fallbackException, parsingFailed);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		CommandSender sender = input.getSender();
		if (!(sender instanceof Player)) {
			// If the sender is not a player, we skip the suggestions for the child argument:
			return Collections.emptyList();
		}
		return argument.complete(input, context, argsReader);
	}
}
