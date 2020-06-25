package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.stream.Stream;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.InvalidArgumentException;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.StreamUtils;

/**
 * Determines a known {@link User} by the given name input.
 * <p>
 * This does not load or create a new User if no currently loaded User is found.
 */
public class UserByNameArgument extends ObjectByIdArgument<String, User> {

	public UserByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserByNameArgument(String name, ArgumentFilter<User> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserByNameArgument(String name, ArgumentFilter<User> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, int minimalCompletionInput) {
		return new UserNameArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(String idPrefix) {
				return UserByNameArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	// Note: We use the same error messages as the PlayerByNameArgument.

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Settings.msgCommandPlayerArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	/**
	 * Gets the {@link AmbiguousUserNameHandler}.
	 * <p>
	 * When overriding this method, consider applying the {@link #getDefaultErrorMsgArgs() common message arguments} to
	 * the error message returned by the {@link AmbiguousUserNameHandler} (if any).
	 * 
	 * @param argumentInput
	 *            the argument input
	 * @param matchedUsers
	 *            the matched users
	 * @return the ambiguous user name handler, or <code>null</code> if the input was not ambiguous
	 */
	public AmbiguousUserNameHandler getAmbiguousUserNameHandler(String argumentInput, Iterable<? extends User> matchedUsers) {
		if (argumentInput == null) argumentInput = "";
		AmbiguousUserNameHandler ambiguousUserNameHandler = new AmbiguousUserNameHandler(argumentInput, matchedUsers);
		if (ambiguousUserNameHandler.isInputAmbiguous()) {
			// Apply common message arguments:
			Text errorMsg = ambiguousUserNameHandler.getErrorMsg();
			assert errorMsg != null;
			errorMsg.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
			errorMsg.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		}
		return ambiguousUserNameHandler;
	}

	/**
	 * The default implementation of getting a {@link User} by name.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched user, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the name is ambiguous
	 */
	public final User getDefaultUserByName(String nameInput) throws ArgumentParseException {
		// The name input can be both the player name or display name:
		Stream<? extends User> users = ShopkeepersAPI.getUserManager().getUsers(nameInput, true);
		AmbiguousUserNameHandler ambiguousUserNameHandler = this.getAmbiguousUserNameHandler(nameInput, StreamUtils.toIterable(users));
		assert ambiguousUserNameHandler != null;
		if (ambiguousUserNameHandler.isInputAmbiguous()) {
			Text errorMsg = ambiguousUserNameHandler.getErrorMsg();
			assert errorMsg != null;
			throw new InvalidArgumentException(this, errorMsg);
		} else {
			return ambiguousUserNameHandler.getFirstMatch();
		}
	}

	@Override
	public User getObject(String nameInput) throws ArgumentParseException {
		return this.getDefaultUserByName(nameInput);
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String namePrefix) {
		// Note: Whether or not to include display name suggestions usually depends on whether or not the the used
		// matching function considers display names
		return UserNameArgument.getDefaultCompletionSuggestions(namePrefix, filter, true);
	}
}
