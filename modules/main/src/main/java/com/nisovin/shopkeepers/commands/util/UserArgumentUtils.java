package com.nisovin.shopkeepers.commands.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.arguments.AmbiguousUserNameHandler;
import com.nisovin.shopkeepers.commands.lib.util.ObjectMatcher;
import com.nisovin.shopkeepers.user.SKUser;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StreamUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public final class UserArgumentUtils {

	/**
	 * Gets the users currently known to the plugin, e.g. because they are currently online, or are
	 * currently assigned as shop owners or shop members.
	 * <p>
	 * This does not take offline players into account, since we want to avoid loading all
	 * (potentially many) player files.
	 * <p>
	 * This may for example be used for command completion suggestions.
	 * 
	 * @return the users known to the plugin that can be determined without loading player files
	 */
	public static Stream<User> getKnownUsers() {
		// Note: We are not using the cache inside SKUser here:
		// - The cache is modified on each call to "SKUser::of" (LRUCache resorts on access), so we
		// run into an ConcurrentModificationException when we first stream the online users.
		// - The cache is of limited size. It might not even cover all of the current shop owners.
		// - The cache may also contain the dummy values such as the empty user.

		var playerShops = SKShopkeepersPlugin.getInstance()
				.getShopkeeperRegistry()
				.getAllPlayerShopkeepers();

		return Stream.of(
				EntityUtils.getOnlinePlayersStream().map(SKUser::of),
				playerShops.stream().map(x -> x.getOwnerUser()),
				playerShops.stream()
						// Optimization: Skip member stream creation for shops without members (most
						// shops):
						.filter(x -> !x.getMembers().isEmpty())
						.flatMap(x -> x.getMembers().stream().map(m -> m.getUser()))
		)
				.flatMap(Function.identity())
				// Return each user at most once:
				.filter(StreamUtils.distinctByKey(User::getUniqueId));
	}

	public static @Nullable User findUser(UUID uniqueId) {
		// Check the known users first:
		var userOpt = getKnownUsers().filter(x -> x.getUniqueId().equals(uniqueId)).findFirst();
		if (userOpt.isPresent()) {
			return userOpt.get();
		}

		// Check the offline players:
		var offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
		// Null if the player never played on the server before:
		var name = offlinePlayer.getName();
		if (name == null) return null;

		return SKUser.of(uniqueId, name);
	}

	/*
	 * Player name assumptions:
	 * - Names are unique (including case) among online players.
	 * - Allowed characters for names: [a-zA-Z0-9_]
	 * - Names don't contain whitespace.
	 * - Names don't include color codes.
	 * - Display names may include whitespace, color codes, arbitrary characters and might not be
	 *   unique.
	 * 
	 * However, user names are also retrieved from the shop owners stored in the save file. Since
	 * the save data might have been manually edited, and since users can change their names without
	 * the names of the previous owners having been updated yet, user names are not guaranteed to be
	 * unique and not guaranteed to only contain valid player name characters.
	 */
	public interface UserNameMatcher extends ObjectMatcher<User> {

		@Override
		public default Stream<User> match(String input) {
			return this.match(input, false);
		}

		// Note: Avoid looking up offline players during command completion, since this may perform
		// a lot of slow lookups, causing lag. This also applies to the parse/getObject operation of
		// the UserByNameArgument, since argument parsing is also invoked during command completion.
		public Stream<User> match(String input, boolean lookupOfflinePlayer);

		/**
		 * Whether this {@link UserNameMatcher} matches display names.
		 * 
		 * @return <code>true</code> if matching display names
		 */
		public boolean matchesDisplayNames();

		// COMMON NAME MATCHERS

		public static final UserNameMatcher NAME_EXACT = new UserNameMatcher() {
			@Override
			public Stream<User> match(String input, boolean lookupOfflinePlayer) {
				if (StringUtils.isEmpty(input)) return Stream.empty();

				// Case-insensitive comparison:
				String normalizedInput = StringUtils.normalize(input);

				// Check the known users first:
				var matchingUsers = getKnownUsers()
						.filter(x -> StringUtils.normalize(x.getName()).equals(normalizedInput));
				var iterator = matchingUsers.iterator();
				if (iterator.hasNext()) {
					return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
				}

				if (lookupOfflinePlayer) {
					// Check the offline players:
					// Potentially blocking :( Cached by the server for subsequent lookups
					// Does not account for ambiguities: Returns at most one candidate offline
					// player:
					var offlinePlayer = Bukkit.getOfflinePlayer(input);
					if (offlinePlayer.hasPlayedBefore()) {
						var name = offlinePlayer.getName();
						if (name == null || name.isEmpty()) {
							name = input;
						}
						var user = SKUser.of(offlinePlayer.getUniqueId(), name);
						return Stream.of(user);
					}
				}

				return Stream.empty();
			}

			@Override
			public boolean matchesDisplayNames() {
				return false;
			}
		};

		// Includes matching display names.
		public static final UserNameMatcher EXACT = new AbstractUserNameMatcher() {
			@Override
			protected boolean checkExactMatchesFirst() {
				// We check for exact matches later anyway, so we can avoid this.
				return false;
			}

			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.equals(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.getPlayer(String) but also considers display names and ignores
		// dashes/underscores/whitespace.
		public static final UserNameMatcher STARTS_WITH = new AbstractUserNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.startsWith(normalizedInputName);
			}
		};

		// Note: Similar to Bukkit.matchPlayer(String) but also considers display names and ignores
		// dashes/underscores/whitespace.
		public static final UserNameMatcher CONTAINS = new AbstractUserNameMatcher() {
			@Override
			protected boolean matches(String normalizedInputName, String normalizedName) {
				return normalizedName.contains(normalizedInputName);
			}
		};
	}

	private static abstract class AbstractUserNameMatcher implements UserNameMatcher {

		@Override
		public Stream<User> match(String input, boolean lookupOfflinePlayer) {
			if (StringUtils.isEmpty(input)) return Stream.empty();

			// Check for exact matches first:
			if (this.checkExactMatchesFirst()) {
				var exactMatches = NAME_EXACT.match(input);
				var iterator = exactMatches.iterator();
				if (iterator.hasNext()) {
					return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
				}
			}

			String normalizedInput = StringUtils.normalize(input);
			// TODO Return a lazily evaluated stream somehow?
			List<User> matchingUsers = new ArrayList<>();
			boolean[] onlyPerfectMatches = new boolean[] { false };
			Iterable<User> users = getKnownUsers()::iterator;
			for (User user : users) {
				assert user != null;
				// Check name:
				String name = user.getName();
				String normalizedName = StringUtils.normalize(name);

				boolean matched = this.match(
						normalizedInput,
						user,
						normalizedName,
						matchingUsers,
						onlyPerfectMatches
				);
				if (matched) {
					// Add user at most once. -> Skip display name check.
					continue;
				}

				// Check display name:
				String displayName = user.getDisplayName();
				String normalizedDisplayName = StringUtils.normalize(TextUtils.stripColor(displayName));
				this.match(
						normalizedInput,
						user,
						normalizedDisplayName,
						matchingUsers,
						onlyPerfectMatches
				);
			}

			if (!onlyPerfectMatches[0] && lookupOfflinePlayer) {
				// We found no perfect match. Check for an offline player match:
				var offlinePlayer = Bukkit.getOfflinePlayer(input);
				if (offlinePlayer.hasPlayedBefore()) {
					var name = offlinePlayer.getName();
					if (name == null || name.isEmpty()) {
						name = input;
					}
					var user = SKUser.of(offlinePlayer.getUniqueId(), name);
					return Stream.of(user);
				}
			}

			return matchingUsers.stream();
		}

		@Override
		public boolean matchesDisplayNames() {
			return true;
		}

		protected boolean checkExactMatchesFirst() {
			return true;
		}

		protected boolean match(
				String normalizedInput,
				User user,
				String normalizedName,
				List<User> matchingUsers,
				boolean[] onlyPerfectMatches
		) {
			if (this.matches(normalizedInput, normalizedName)) {
				if (normalizedName.length() == normalizedInput.length()) {
					// Perfect match of normalized names:
					if (!onlyPerfectMatches[0]) {
						// The previous matches were not perfect matches, disregard them:
						matchingUsers.clear();
					}
					onlyPerfectMatches[0] = true; // Only accepting other perfect matches now
					matchingUsers.add(user);
					return true;
				} else {
					if (!onlyPerfectMatches[0]) {
						matchingUsers.add(user);
						return true;
					} // Else: Only accepting perfect matches.
				}
			}
			return false; // No match
		}

		protected abstract boolean matches(String normalizedInputName, String normalizedName);
	}

	private static final int DEFAULT_AMBIGUOUS_USER_NAME_MAX_ENTRIES = 5;

	// Note: Iterable is only iterated once.
	// Returns true if there are multiple matches.
	public static boolean handleAmbiguousUserName(
			CommandSender sender,
			String name,
			Iterable<? extends User> matches
	) {
		return handleAmbiguousUserName(
				sender,
				name,
				matches,
				DEFAULT_AMBIGUOUS_USER_NAME_MAX_ENTRIES
		);
	}

	// Note: Iterable is only iterated once.
	// Returns true if there are multiple matches.
	public static boolean handleAmbiguousUserName(
			CommandSender sender,
			String name,
			Iterable<? extends User> matches,
			int maxEntries
	) {
		var ambiguousUserNameHandler = new AmbiguousUserNameHandler(name, matches, maxEntries);
		if (ambiguousUserNameHandler.isInputAmbiguous()) {
			var errorMsg = ambiguousUserNameHandler.getErrorMsg();
			assert errorMsg != null;
			TextUtils.sendMessage(sender, errorMsg);
			return true;
		}

		return false;
	}

	private UserArgumentUtils() {
	}
}
