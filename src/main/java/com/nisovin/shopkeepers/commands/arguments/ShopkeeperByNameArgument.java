package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.stream.Stream;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;

/**
 * Determines a shopkeeper by the given name input.
 */
public class ShopkeeperByNameArgument extends ObjectByIdArgument<String, Shopkeeper> {

	private final boolean joinRemainingArgs;

	public ShopkeeperByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByNameArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, false, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperByNameArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
		this.joinRemainingArgs = joinRemainingArgs;
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, int minimalCompletionInput) {
		return new ShopkeeperNameArgument(name, joinRemainingArgs, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(String idPrefix) {
				return ShopkeeperByNameArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Settings.msgCommandShopkeeperArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	/**
	 * The default implementation of getting a {@link Shopkeeper} by name.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched shopkeeper, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the name is ambiguous
	 */
	public final Shopkeeper getDefaultShopkeeperByName(String nameInput) throws ArgumentParseException {
		Stream<? extends Shopkeeper> shopkeepers = ShopkeeperUtils.ShopkeeperNameMatchers.DEFAULT.match(nameInput);
		Shopkeeper shopkeeper = shopkeepers.findFirst().orElse(null);
		return shopkeeper;
		// TODO deal with ambiguities
	}

	@Override
	public Shopkeeper getObject(String nameInput) throws ArgumentParseException {
		return this.getDefaultShopkeeperByName(nameInput);
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String namePrefix) {
		return ShopkeeperNameArgument.getDefaultCompletionSuggestions(namePrefix, filter);
	}
}
