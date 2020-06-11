package com.nisovin.shopkeepers.commands.arguments;

import java.util.function.Predicate;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;

/**
 * Provides suggestions for the {@link User#getName() names} of known {@link User Users}.
 * <p>
 * By default this accepts any name regardless of whether it corresponds to an existing user.
 */
public class UserNameArgument extends PlayerNameArgument {

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or player shops,
	// because this can be achieved more efficiently by using ShopkeeperByNameArgument instead.

	public UserNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserNameArgument(String name, ArgumentFilter<String> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	// uses the same error messages as the parent PlayerNameArgument

	/**
	 * Gets the default name completion suggestions.
	 * 
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @param filter
	 *            only suggestions for Users accepted by this predicate get included
	 * @return the user name completion suggestions
	 */
	public static Iterable<String> getDefaultUserNameCompletionSuggestions(String namePrefix, Predicate<User> filter) {
		// Assert: The returned Users are expected to have a non-null name.
		return ShopkeepersAPI.getUserManager().getUsersByNamePrefix(namePrefix)
				.filter(filter)
				.map(user -> user.getName())::iterator;
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String namePrefix) {
		return getDefaultUserNameCompletionSuggestions(namePrefix, (user) -> true);
	}
}
