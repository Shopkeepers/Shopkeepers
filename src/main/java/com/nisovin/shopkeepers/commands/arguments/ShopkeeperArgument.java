package com.nisovin.shopkeepers.commands.arguments;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;

public class ShopkeeperArgument extends CommandArgument<Shopkeeper> {

	protected final ArgumentFilter<Shopkeeper> filter; // not null
	private final ShopkeeperByUUIDArgument shopByUUIDArgument;
	private final ShopkeeperByIdArgument shopByIdArgument;
	private final ShopkeeperByNameArgument shopByNameArgument;
	private final TypedFirstOfArgument<Shopkeeper> firstOfArgument;

	public ShopkeeperArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs) {
		this(name, joinRemainingArgs, ArgumentFilter.acceptAny());
	}

	public ShopkeeperArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, false, filter);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter) {
		this(name, joinRemainingArgs, filter, ShopkeeperNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT, ShopkeeperUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter, int minimalNameCompletionInput, int minimalUUIDCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.shopByUUIDArgument = new ShopkeeperByUUIDArgument(name + ":uuid", filter, minimalUUIDCompletionInput);
		this.shopByIdArgument = new ShopkeeperByIdArgument(name + ":id", filter);
		this.shopByNameArgument = new ShopkeeperByNameArgument(name + ":name", joinRemainingArgs, filter, minimalNameCompletionInput) {
			@Override
			public Shopkeeper getObject(String nameInput) throws ArgumentParseException {
				return ShopkeeperArgument.this.getShopkeeperByName(nameInput);
			}

			@Override
			protected Iterable<String> getCompletionSuggestions(String namePrefix) {
				return ShopkeeperArgument.this.getNameCompletionSuggestions(namePrefix);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(name + ":firstOf", Arrays.asList(shopByUUIDArgument, shopByIdArgument, shopByNameArgument), false, false);
		firstOfArgument.setParent(this);
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	/**
	 * Gets the {@link Shopkeeper} which matches the given name input.
	 * <p>
	 * This can be overridden if a different matching behavior is required. You may also want to override
	 * {@link #getNameCompletionSuggestions(String)} then.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched shopkeeper, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the name is ambiguous
	 */
	public Shopkeeper getShopkeeperByName(String nameInput) throws ArgumentParseException {
		return shopByNameArgument.getDefaultShopkeeperByName(nameInput);
	}

	/**
	 * Gets the name completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's shopkeeper filter into account.
	 * 
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<String> getNameCompletionSuggestions(String namePrefix) {
		return ShopkeeperNameArgument.getDefaultCompletionSuggestions(namePrefix, filter);
	}
}
