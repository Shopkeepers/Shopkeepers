package com.nisovin.shopkeepers.commands.arguments;

import java.util.function.Predicate;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectNameArgument;
import com.nisovin.shopkeepers.text.Text;

/**
 * Provides suggestions for the {@link User#getName() names} of known {@link User Users}.
 * <p>
 * By default this accepts any name regardless of whether it corresponds to an existing user.
 */
public class UserNameArgument extends ObjectNameArgument {

	// Note: Not providing default argument filters that only accepts names of existing users, because this can be
	// achieved more efficiently by using UserByNameArgument instead.

	public UserNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserNameArgument(String name, ArgumentFilter<String> filter, int minimalCompletionInput) {
		super(name, false, filter, minimalCompletionInput);
	}

	// Note: We use the same error messages as the PlayerNameArgument.

	@Override
	public Text getMissingArgumentErrorMsg() {
		Text text = Settings.msgCommandPlayerArgumentMissing;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		return text;
	}

	// using the filter's 'invalid argument' message if the name is not accepted

	/**
	 * Gets the default name completion suggestions.
	 * 
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @param userFilter
	 *            only suggestions for Users accepted by this predicate get included
	 * @param includeDisplayNames
	 *            <code>true</code> to include display name suggestions
	 * @return the user name completion suggestions
	 */
	public static Iterable<String> getDefaultCompletionSuggestions(String namePrefix, Predicate<User> userFilter, boolean includeDisplayNames) {
		// Assert: The returned Users are expected to have a non-null name.
		return ShopkeepersAPI.getUserManager().getUsersByNamePrefix(namePrefix, includeDisplayNames)
				.filter(userFilter)
				.map(user -> user.getName())::iterator;
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String namePrefix) {
		return getDefaultCompletionSuggestions(namePrefix, (user) -> true, true);
	}
}
