package com.nisovin.shopkeepers.commands.arguments;

import java.util.UUID;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerUUIDArgument;

/**
 * Provides suggestions for the {@link User#getUniqueId() unique ids} of known {@link User Users}.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an existing user.
 */
public class UserUUIDArgument extends PlayerUUIDArgument {

	// Note: Not providing a default argument filter that only accepts uuids of existing users, because this can be
	// achieved more efficiently by using UserByUUIDArgument instead.

	public UserUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	// uses the same error messages as the parent PlayerUUIDArgument

	/**
	 * Gets the default uuid completion suggestions.
	 * 
	 * @param uuidPrefix
	 *            the uuid prefix, may be empty, not <code>null</code>
	 * @param filter
	 *            only suggestions for users accepted by this predicate get included
	 * @return the user uuid completion suggestions
	 */
	public static Iterable<UUID> getDefaultUserUUIDCompletionSuggestions(String uuidPrefix, Predicate<User> filter) {
		return ShopkeepersAPI.getUserManager().getUsersByUUIDPrefix(uuidPrefix)
				.filter(filter)
				.map(user -> user.getUniqueId())::iterator;
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
		return getDefaultUserUUIDCompletionSuggestions(idPrefix, (user) -> true);
	}
}
