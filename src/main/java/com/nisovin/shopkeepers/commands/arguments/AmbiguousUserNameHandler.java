package com.nisovin.shopkeepers.commands.arguments;

import java.util.UUID;

import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.lib.arguments.AbstractAmbiguousPlayerNameHandler;

// Uses the same messages as {@link AmbiguousPlayerNameHandler}.
public class AmbiguousUserNameHandler extends AbstractAmbiguousPlayerNameHandler<User> {

	public AmbiguousUserNameHandler(String input, Iterable<? extends User> matches) {
		this(input, matches, DEFAULT_MAX_ENTRIES);
	}

	public AmbiguousUserNameHandler(String input, Iterable<? extends User> matches, int maxEntries) {
		super(input, matches, maxEntries);
	}

	@Override
	protected String getName(User match) {
		assert match != null;
		return match.getName();
	}

	@Override
	protected UUID getUniqueId(User match) {
		assert match != null;
		return match.getUniqueId();
	}
}
