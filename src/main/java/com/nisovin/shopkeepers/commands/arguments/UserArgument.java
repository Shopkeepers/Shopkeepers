package com.nisovin.shopkeepers.commands.arguments;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;

/**
 * Accepts a known {@link User} specified by either name (might not have to be exact, depending on the used matching
 * function) or UUID.
 * <p>
 * This does not load or create a new User if no currently loaded User is found.
 */
public class UserArgument extends CommandArgument<User> {

	protected final ArgumentFilter<User> filter; // not null
	private final UserByUUIDArgument userByUUIDArgument;
	private final UserByNameArgument userByNameArgument;
	private final TypedFirstOfArgument<User> firstOfArgument;

	public UserArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserArgument(String name, ArgumentFilter<User> filter) {
		this(name, filter, UserNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT, UserUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserArgument(String name, ArgumentFilter<User> filter, int minimalNameCompletionInput, int minimalUUIDCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.userByUUIDArgument = new UserByUUIDArgument(name + ":uuid", filter, minimalUUIDCompletionInput);
		this.userByNameArgument = new UserByNameArgument(name + ":name", filter, minimalNameCompletionInput) {
			@Override
			public User getObject(String nameInput) throws ArgumentParseException {
				return UserArgument.this.getUserByName(nameInput);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(name + ":firstOf", Arrays.asList(userByUUIDArgument, userByNameArgument), false, false);
		firstOfArgument.setParent(this);
	}

	@Override
	public User parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	/**
	 * Gets the {@link User} which matches the given name input.
	 * <p>
	 * This can be overridden if a different matching behavior is required. You may also want to override
	 * {@link #getNameCompletionSuggestions(String)} then.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched user, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the name is ambiguous
	 */
	public User getUserByName(String nameInput) throws ArgumentParseException {
		return userByNameArgument.getDefaultUserByName(nameInput);
	}

	/**
	 * Gets the name completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's user filter into account.
	 * 
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<String> getNameCompletionSuggestions(String namePrefix) {
		return UserNameArgument.getDefaultCompletionSuggestions(namePrefix, filter, true);
	}
}
