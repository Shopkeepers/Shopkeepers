package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.text.Text;

/**
 * Determines an online player by the given UUID input.
 */
public class PlayerByUUIDArgument extends ObjectByIdArgument<UUID, Player> {

	public PlayerByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerByUUIDArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerByUUIDArgument(String name, ArgumentFilter<Player> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<UUID> createIdArgument(String name, int minimalCompletionInput) {
		return new PlayerUUIDArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
				return PlayerByUUIDArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Settings.msgCommandPlayerArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	protected Player getObject(UUID uuid) throws ArgumentParseException {
		return Bukkit.getPlayer(uuid);
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(String uuidPrefix) {
		return PlayerUUIDArgument.getDefaultCompletionSuggestions(uuidPrefix, filter);
	}
}
