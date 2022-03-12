package com.nisovin.shopkeepers.commands.arguments;

import java.util.UUID;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

/**
 * Determines a shopkeeper by the given UUID input.
 */
public class ShopkeeperByUUIDArgument extends ObjectByIdArgument<UUID, Shopkeeper> {

	public ShopkeeperByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByUUIDArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, filter, ShopkeeperUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperByUUIDArgument(String name, ArgumentFilter<Shopkeeper> filter, int minimumCompletionInput) {
		super(name, filter, new IdArgumentArgs(minimumCompletionInput));
	}

	@Override
	protected ObjectIdArgument<UUID> createIdArgument(String name, IdArgumentArgs args) {
		return new ShopkeeperUUIDArgument(name, ArgumentFilter.acceptAny(), args.minimumCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
				return ShopkeeperByUUIDArgument.this.getCompletionSuggestions(input, context, minimumCompletionInput, idPrefix);
			}
		};
	}

	@Override
	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.commandShopkeeperArgumentInvalid;
	}

	@Override
	protected Shopkeeper getObject(CommandInput input, CommandContextView context, UUID uuid) throws ArgumentParseException {
		return ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(	CommandInput input, CommandContextView context,
														int minimumCompletionInput, String idPrefix) {
		return ShopkeeperUUIDArgument.getDefaultCompletionSuggestions(input, context, minimumCompletionInput, idPrefix, filter);
	}
}
