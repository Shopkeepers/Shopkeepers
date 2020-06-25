package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.UUID;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.text.Text;

/**
 * Determines a known {@link User} by the given UUID input.
 * <p>
 * This does not load or create a new User if no currently loaded User is found.
 */
public class UserByUUIDArgument extends ObjectByIdArgument<UUID, User> {

	public UserByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public UserByUUIDArgument(String name, ArgumentFilter<User> filter) {
		this(name, filter, UserUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public UserByUUIDArgument(String name, ArgumentFilter<User> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<UUID> createIdArgument(String name, int minimalCompletionInput) {
		return new UserUUIDArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
				return UserByUUIDArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	// Note: We use the same error messages as the PlayerByUUIDArgument.

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Settings.msgCommandPlayerArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	protected User getObject(UUID uuid) throws ArgumentParseException {
		return ShopkeepersAPI.getUserManager().getUser(uuid);
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(String uuidPrefix) {
		return UserUUIDArgument.getDefaultUserUUIDCompletionSuggestions(uuidPrefix, filter);
	}
}
