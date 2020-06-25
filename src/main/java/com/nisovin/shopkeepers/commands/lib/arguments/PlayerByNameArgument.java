package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.stream.Stream;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.InvalidArgumentException;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.PlayerUtils;

/**
 * Determines an online player by the given name input.
 */
public class PlayerByNameArgument extends ObjectByIdArgument<String, Player> {

	public PlayerByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, int minimalCompletionInput) {
		return new PlayerNameArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(String idPrefix) {
				return PlayerByNameArgument.this.getCompletionSuggestions(idPrefix);
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

	/**
	 * Gets the {@link AmbiguousPlayerNameHandler}.
	 * <p>
	 * When overriding this method, consider applying the {@link #getDefaultErrorMsgArgs() common message arguments} to
	 * the error message returned by the {@link AmbiguousPlayerNameHandler} (if any).
	 * 
	 * @param argumentInput
	 *            the argument input
	 * @param matchedPlayers
	 *            the matched players
	 * @return the ambiguous player name handler, or <code>null</code> if the input was not ambiguous
	 */
	public AmbiguousPlayerNameHandler<Player> getAmbiguousPlayerNameHandler(String argumentInput, Iterable<? extends Player> matchedPlayers) {
		if (argumentInput == null) argumentInput = "";
		AmbiguousPlayerNameHandler<Player> ambiguousPlayerNameHandler = new AmbiguousPlayerNameHandler<>(argumentInput, matchedPlayers);
		if (ambiguousPlayerNameHandler.isInputAmbiguous()) {
			// Apply common message arguments:
			Text errorMsg = ambiguousPlayerNameHandler.getErrorMsg();
			assert errorMsg != null;
			errorMsg.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
			errorMsg.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		}
		return ambiguousPlayerNameHandler;
	}

	/**
	 * The default implementation of getting a {@link Player} by name.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched player, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the name is ambiguous
	 */
	public final Player getDefaultPlayerByName(String nameInput) throws ArgumentParseException {
		// The name input can be both the player name or display name:
		Stream<Player> players = PlayerUtils.PlayerNameMatcher.EXACT.match(nameInput);
		AmbiguousPlayerNameHandler<Player> ambiguousPlayerNameHandler = this.getAmbiguousPlayerNameHandler(nameInput, players::iterator);
		assert ambiguousPlayerNameHandler != null;
		if (ambiguousPlayerNameHandler.isInputAmbiguous()) {
			Text errorMsg = ambiguousPlayerNameHandler.getErrorMsg();
			assert errorMsg != null;
			throw new InvalidArgumentException(this, errorMsg);
		} else {
			return ambiguousPlayerNameHandler.getFirstMatch();
		}
	}

	@Override
	public Player getObject(String nameInput) throws ArgumentParseException {
		return this.getDefaultPlayerByName(nameInput);
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String namePrefix) {
		// Note: Whether or not to include display name suggestions usually depends on whether or not the the used
		// matching function considers display names
		return PlayerNameArgument.getDefaultCompletionSuggestions(namePrefix, filter, true);
	}
}
