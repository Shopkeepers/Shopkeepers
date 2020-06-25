package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.text.Text;

/**
 * Provides suggestions for the UUIDs of online players.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an online player.
 */
public class PlayerUUIDArgument extends ObjectUUIDArgument {

	// Note: Not providing a default argument filter that only accepts uuids of online players, because this can be
	// achieved more efficiently by using PlayerByUUIDArgument instead.

	public PlayerUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		Text text = Settings.msgCommandPlayerArgumentMissing;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		return text;
	}

	// using the uuid argument's 'invalid argument' message if the uuid is invalid
	// using the filter's 'invalid argument' message if the uuid is not accepted

	/**
	 * Gets the default uuid completion suggestions.
	 * 
	 * @param uuidPrefix
	 *            the uuid prefix, may be empty, not <code>null</code>
	 * @param playerFilter
	 *            only suggestions for players accepted by this predicate get included
	 * @return the player uuid completion suggestions
	 */
	public static Iterable<UUID> getDefaultCompletionSuggestions(String uuidPrefix, Predicate<Player> playerFilter) {
		String normalizedUUIDPrefix = uuidPrefix.toLowerCase(Locale.ROOT);
		return Bukkit.getOnlinePlayers().stream()
				.filter(playerFilter)
				.map(player -> player.getUniqueId())
				.filter(uuid -> {
					// assumption: UUID#toString is already lowercase (normalized)
					return uuid.toString().startsWith(normalizedUUIDPrefix);
				})::iterator;
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(String uuidPrefix) {
		return getDefaultCompletionSuggestions(uuidPrefix, (player) -> true);
	}
}
