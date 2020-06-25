package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerUtils {

	private PlayerUtils() {
	}

	/**
	 * Normalizes the given player name.
	 * <p>
	 * This is a subset of the normalizations performed by {@link #normalizeDisplayName(String)}. This assumes that the
	 * player name is valid, i.e. it only consist of characters in the range of [a-zA-Z0-9_] (no whitespace, no color
	 * codes). Normalizing the name then simply consists of converting the characters to lower case.
	 * <p>
	 * This returns <code>null</code> for a <code>null</code> input name.
	 * 
	 * @param playerName
	 *            the player name
	 * @return the normalized player name
	 */
	public static String normalizePlayerName(String playerName) {
		if (playerName == null) return null;
		return playerName.toLowerCase(Locale.ROOT);
	}

	/**
	 * Normalizes the given player display name.
	 * <p>
	 * This performs the following normalizations:
	 * <ul>
	 * <li>Strip all color codes.
	 * <li>Remove any leading and trailing whitespace.
	 * <li>Convert all remaining whitespace and underscores to dashes('-').
	 * <li>Convert all characters to lower case.
	 * </ul>
	 * <p>
	 * This returns <code>null</code> for a <code>null</code> input display name.
	 * 
	 * @param displayName
	 *            the player display name
	 * @return the normalized player display name
	 */
	public static String normalizeDisplayName(String displayName) {
		if (displayName == null) return null;
		return StringUtils.normalize(TextUtils.stripColor(displayName));
	}

	/*
	 * Assumptions:
	 * - Names are unique (including case) among online players.
	 * - Allowed characters for names: [a-zA-Z0-9_]
	 * - Names don't contain whitespace.
	 * - Names don't include color codes.
	 * - Display names may include whitespace, color codes, arbitrary characters and may not be unique.
	 */
	public interface PlayerNameMatcher extends ObjectMatcher<Player> {

		@Override
		public Stream<Player> match(String input);

		/**
		 * Whether this {@link PlayerNameMatcher} matches display names.
		 * 
		 * @return <code>true</code> if matching display names
		 */
		public boolean matchesDisplayNames();

		// COMMON NAME MATCHERS

		public static final PlayerNameMatcher NAME_EXACT = new PlayerNameMatcher() {
			@Override
			public Stream<Player> match(String input) {
				if (StringUtils.isEmpty(input)) return Stream.empty();
				// note: this is case insensitive
				// assumption: player names are unique regardless of case
				Player exactMatch = Bukkit.getPlayerExact(input);
				return (exactMatch != null) ? Stream.of(exactMatch) : Stream.empty();
			}

			@Override
			public boolean matchesDisplayNames() {
				return false;
			}
		};

		// includes matching display names
		public static final PlayerNameMatcher EXACT = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean checkExactMatchFirst() {
				return false; // we check for exact matches later anyways so we can avoid this
			}

			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.equals(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.getPlayer(String) but also considers display names and ignores
		// dashes/underscores/whitespace.
		public static final PlayerNameMatcher STARTS_WITH = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.startsWith(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.matchPlayer(String) but also considers display names and ignores
		// dashes, underscores and whitespace.
		public static final PlayerNameMatcher CONTAINS = new AbstractPlayerNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.contains(normalizedInputName);
			}
		};
	}

	private static abstract class AbstractPlayerNameMatcher implements PlayerNameMatcher {

		@Override
		public Stream<Player> match(String input) {
			if (StringUtils.isEmpty(input)) return Stream.empty();

			// Check for an exact match first:
			if (this.checkExactMatchFirst()) {
				Player exactMatch = Bukkit.getPlayerExact(input); // case insensitive
				if (exactMatch != null) return Stream.of(exactMatch);
			}

			// We only do the full input normalization if it is required (if we also compare with display names):
			final boolean matchDisplayNames = this.matchesDisplayNames();
			String normalizedInput = matchDisplayNames ? normalizeDisplayName(input) : normalizePlayerName(input);
			List<Player> matchingPlayers = new ArrayList<>();
			boolean[] onlyPerfectMatches = new boolean[] { false };
			for (Player player : Bukkit.getOnlinePlayers()) {
				// check name:
				String playerName = player.getName();
				String normalizedPlayerName = normalizePlayerName(playerName);

				boolean matched = this.match(normalizedInput, player, normalizedPlayerName, matchingPlayers, onlyPerfectMatches);
				if (matched) {
					if (onlyPerfectMatches[0]) {
						// we found an exact player name match, return that player:
						// note: this can usually only occur with checkExactMatchFirst disabled
						return Stream.of(player);
					}
					continue; // add player at most once -> skip display name check
				}

				// check display name:
				if (matchDisplayNames) {
					String displayName = player.getDisplayName();
					String normalizedDisplayName = normalizeDisplayName(displayName);
					this.match(normalizedInput, player, normalizedDisplayName, matchingPlayers, onlyPerfectMatches);
				}
			}
			return matchingPlayers.stream();
		}

		@Override
		public boolean matchesDisplayNames() {
			return true;
		}

		protected boolean checkExactMatchFirst() {
			return true;
		}

		protected boolean match(String normalizedInput, Player player, String normalizedName,
								List<Player> matchingPlayers, boolean[] onlyPerfectMatches) {
			if (this.matches(normalizedInput, normalizedName)) {
				if (normalizedName.length() == normalizedInput.length()) {
					// perfect match of normalized names:
					if (!onlyPerfectMatches[0]) {
						// the previous matches were not perfect matches, disregard them:
						matchingPlayers.clear();
					}
					onlyPerfectMatches[0] = true; // only accepting other perfect matches now
					matchingPlayers.add(player);
					return true;
				} else {
					if (!onlyPerfectMatches[0]) {
						matchingPlayers.add(player);
						return true;
					} // else: only accepting perfect matches
				}
			}
			return false; // no match
		}

		protected abstract boolean matches(String normalizedInputName, String normalizedName);
	}
}
