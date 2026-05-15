package com.nisovin.shopkeepers.util.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * Utility functions related to Strings.
 */
public final class StringUtils {

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public static boolean isEmpty(@Nullable String string) {
		return (string == null) || string.isEmpty();
	}

	/**
	 * Makes sure that the given String is not empty.
	 * 
	 * @param string
	 *            the String
	 * @return the String itself, or <code>null</code> if it is empty
	 */
	public static @Nullable String getNotEmpty(@Nullable String string) {
		return isEmpty(string) ? null : string;
	}

	/**
	 * Makes sure that the given String is not <code>null</code>.
	 * 
	 * @param string
	 *            the String
	 * @return the String itself, or an empty String if it is <code>null</code>
	 */
	public static String getOrEmpty(@Nullable String string) {
		return (string == null) ? "" : string;
	}

	/**
	 * Converts the given {@link Object} to a String via its {@link #toString()} method.
	 * <p>
	 * If the object is <code>null</code>, or if its {@link #toString()} method returns
	 * <code>null</code>, this returns an empty String.
	 * 
	 * @param object
	 *            the object
	 * @return the String, not <code>null</code>
	 */
	public static String toStringOrEmpty(@Nullable Object object) {
		if (object == null) return "";
		return getOrEmpty(object.toString());
	}

	/**
	 * Converts the given {@link Object} to a String via its {@link #toString()} method.
	 * <p>
	 * Returns <code>null</code> if the object is <code>null</code>, or if its {@link #toString()}
	 * method returns <code>null</code>.
	 * 
	 * @param object
	 *            the object
	 * @return the String, possibly <code>null</code>
	 */
	public static @Nullable String toStringOrNull(@Nullable Object object) {
		if (object == null) return null;
		return object.toString(); // Incorrect implementations might return null here
	}

	/**
	 * Checks if the given {@link String} contains the specified character.
	 * <p>
	 * This is a shortcut for invoking {@link String#indexOf(int)} and checking if the return value
	 * does not equal {@code -1}.
	 * 
	 * @param string
	 *            the String
	 * @param character
	 *            the character
	 * @return <code>true</code> if the String contains the given character
	 */
	public static boolean contains(@Nullable String string, int character) {
		return (string != null) && string.indexOf(character) != -1;
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace and converts all remaining whitespace and
	 * underscores to dashes ('{@code -}').
	 * 
	 * @param identifier
	 *            the identifier, not <code>null</code>
	 * @return the normalized identifier
	 */
	public static String normalizeKeepCase(String identifier) {
		Validate.notNull(identifier, "identifier is null");
		@NonNull String normalized = identifier.trim();
		normalized = normalized.replace('_', '-');
		normalized = replaceWhitespace(normalized, "-");
		return normalized;
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace and underscores
	 * to dashes ('{@code -}') and converts all characters to lower case.
	 * 
	 * @param identifier
	 *            the identifier, not <code>null</code>
	 * @return the normalized identifier
	 */
	public static String normalize(String identifier) {
		String normalized = normalizeKeepCase(identifier);
		return normalized.toLowerCase(Locale.ROOT);
	}

	public static List<String> normalize(List<? extends String> identifiers) {
		List<String> normalized = new ArrayList<>(identifiers.size());
		for (String identifier : identifiers) {
			normalized.add(normalize(identifier));
		}
		return normalized;
	}

	/**
	 * Checks if the given String contains whitespace characters.
	 * 
	 * @param string
	 *            the String
	 * @return <code>true</code> if the given String is not empty and contains at least one
	 *         whitespace character
	 */
	public static boolean containsWhitespace(@Nullable String string) {
		if (isEmpty(string)) {
			return false;
		}
		assert string != null;

		int length = string.length();
		for (int i = 0; i < length; i++) {
			if (Character.isWhitespace(string.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all whitespace characters inside the given source String.
	 * 
	 * @param source
	 *            the source String, not <code>null</code>
	 * @return the String without any whitespace characters
	 */
	public static String removeWhitespace(String source) {
		return replaceWhitespace(source, "");
	}

	/**
	 * Replaces all whitespace characters inside the given source String.
	 * 
	 * @param source
	 *            the source String, not <code>null</code>
	 * @param replacement
	 *            the replacement String, not <code>null</code>
	 * @return the String with any whitespace characters replaced
	 */
	public static String replaceWhitespace(String source, String replacement) {
		Validate.notNull(source, "source is null");
		if (source.isEmpty()) {
			return source;
		}
		Validate.notNull(replacement, "replacement is null");
		String replaced = WHITESPACE_PATTERN.matcher(source).replaceAll(replacement);
		assert replaced != null;
		return replaced;
	}

	public static String capitalizeAll(String source) {
		Validate.notNull(source, "source is null");
		if (source.isEmpty()) {
			return source;
		}
		int sourceLength = source.length();
		StringBuilder builder = new StringBuilder(sourceLength);
		boolean capitalizeNext = true; // Capitalize first letter
		for (int i = 0; i < sourceLength; i++) {
			char currentChar = source.charAt(i);
			if (Character.isWhitespace(currentChar)) {
				capitalizeNext = true;
				builder.append(currentChar);
			} else if (capitalizeNext) {
				capitalizeNext = false;
				builder.append(Character.toTitleCase(currentChar));
			} else {
				builder.append(currentChar);
			}
		}
		return builder.toString();
	}

	// Matches any Unicode newlines (including the Windows newline sequence):
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
	// Additionally matches literal Unix newlines:
	private static final Pattern NEWLINE_OR_LITERAL_PATTERN = Pattern.compile("\\R|\\\\n");
	private static final Pattern ALL_TRAILING_NEWLINES_PATTERN = Pattern.compile("\\R+$");

	// Includes empty and trailing empty lines:
	public static @NonNull String[] splitLines(String source) {
		return splitLines(source, false);
	}

	// Includes empty and trailing empty lines:
	public static @NonNull String[] splitLines(String source, boolean splitLiteralNewlines) {
		if (splitLiteralNewlines) {
			return NEWLINE_OR_LITERAL_PATTERN.split(source, -1);
		} else {
			return NEWLINE_PATTERN.split(source, -1);
		}
	}

	public static List<String> flattenLines(
			List<? extends String> source,
			boolean splitLiteralNewlines
	) {
		List<String> flatten = new ArrayList<>(source.size());
		for (String item : source) {
			String[] lines = splitLines(item, splitLiteralNewlines);
			Collections.addAll(flatten, lines);
		}
		return flatten;
	}

	public static String stripTrailingNewlines(String string) {
		Validate.notNull(string, "string is null");
		String stripped = ALL_TRAILING_NEWLINES_PATTERN.matcher(string).replaceFirst("");
		assert stripped != null;
		return stripped;
	}

	public static boolean containsNewline(@Nullable String string) {
		if (string == null) return false;
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
			case '\n': // Line feed (\\u000A)
			case '\r': // Carriage return (\\u000D)
			case '\f': // Form feed (\\u000C)
			case '\u000B': // Vertical tab
			case '\u0085': // Next line
			case '\u2028': // Unicode line separator
			case '\u2029': // Unicode paragraph separator
				return true;
			default:
				continue;
			}
		}
		return false;
	}

	public static String escapeNewlinesAndBackslash(String string) {
		Validate.notNull(string, "string is null");
		int length = string.length();
		StringBuilder sb = new StringBuilder(length * 2);
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
			case '\\': // Backslash
				sb.append("\\\\");
				break;
			case '\n': // Line feed
				sb.append("\\n");
				break;
			case '\r': // Carriage return
				sb.append("\\r");
				break;
			case '\f': // Form feed
				sb.append("\\f");
				break;
			case '\u000B': // Vertical tab
				sb.append("\\u000B");
				break;
			case '\u0085': // Next line
				sb.append("\\u0085");
				break;
			case '\u2028': // Unicode line separator
				sb.append("\\u2028");
				break;
			case '\u2029': // Unicode paragraph separator
				sb.append("\\u2029");
				break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}

	// Wraps the individual texts in the list.
	// Tries to wrap at whitespace, but falls back to wrapping at the length limit.
	public static void wrap(@ReadWrite List<String> source, int maxLength) {
		Validate.isTrue(maxLength >= 1, "maxLength must be positive!");

		var iterator = source.listIterator();
		while (iterator.hasNext()) {
			String text = Unsafe.assertNonNull(iterator.next());

			if (text.length() <= maxLength) {
				continue;
			}
			assert text.length() > 0;

			iterator.remove();

			int startIndexInclusive = 0;
			while (startIndexInclusive < text.length()) {
				int endIndexExclusive = Math.min(startIndexInclusive + maxLength, text.length());

				// Try to find a whitespace to wrap at:
				int wrapAt = -1;
				for (int i = endIndexExclusive - 1; i > startIndexInclusive; i--) {
					if (Character.isWhitespace(text.charAt(i))) {
						wrapAt = i;
						break;
					}
				}

				if (wrapAt != -1) {
					iterator.add(text.substring(startIndexInclusive, wrapAt));
					startIndexInclusive = wrapAt + 1; // Skip whitespace
				} else {
					// No word boundary found -> hard wrap:
					iterator.add(text.substring(startIndexInclusive, endIndexExclusive));
					startIndexInclusive = endIndexExclusive;
				}
			}
		}
	}

	// ARGUMENTS REPLACEMENT

	// Throws NPE if source, target, or replacement are null.
	// Returns the source String (same instance) if there is no match.
	// Performance is okay for replacing a single argument, but for multiple arguments prefer using
	// replaceArguments rather than invoking this multiple times.
	public static String replaceFirst(String source, String target, CharSequence replacement) {
		int index = source.indexOf(target);
		if (index == -1) return source; // No match

		int sourceLength = source.length();
		int targetLength = target.length();
		int increase = replacement.length() - targetLength;

		StringBuilder result = new StringBuilder(sourceLength + increase);
		result.append(source, 0, index); // Prefix
		result.append(replacement); // Replacement
		result.append(source, index + targetLength, sourceLength); // Suffix
		return result.toString();
	}

	// Singleton for reuse:
	private static final ArgumentsReplacer ARGUMENTS_REPLACER = new ArgumentsReplacer();
	private static final Map<String, Object> TEMP_ARGUMENTS_MAP = new HashMap<>();
	private static final MessageArguments TEMP_ARGUMENTS = MessageArguments.ofMap(TEMP_ARGUMENTS_MAP);

	// Arguments format: [key1, value1, key2, value2, ...]
	// The keys are expected to be of type String.
	// The replaced keys use the format {key} (braces are not specified in the argument keys).
	public static <T> void addArgumentsToMap(
			Map<String, Object> argumentsMap,
			@NonNull Object... argumentPairs
	) {
		Validate.notNull(argumentsMap, "argumentsMap is null");
		Validate.notNull(argumentPairs, "argumentPairs is null");
		Validate.isTrue(argumentPairs.length % 2 == 0,
				"Length of argumentPairs is not a multiple of 2");
		int argumentsKeyLimit = argumentPairs.length - 1;
		for (int i = 0; i < argumentsKeyLimit; i += 2) {
			String key = (String) argumentPairs[i];
			Object value = argumentPairs[i + 1];
			argumentsMap.put(key, value);
		}
	}

	public static String replaceArguments(String source, @NonNull Object... argumentPairs) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(source, TEMP_ARGUMENTS);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // Reset
		}
	}

	// The replaced keys use the format {key} (braces are not specified in the argument keys).
	// Uses the String representation of the given arguments.
	// If an argument is a Supplier, it gets invoked to obtain the actual argument.
	public static String replaceArguments(
			String source,
			Map<? extends String, @NonNull ?> arguments
	) {
		return replaceArguments(source, MessageArguments.ofMap(arguments));
	}

	public static String replaceArguments(String source, MessageArguments arguments) {
		return ARGUMENTS_REPLACER.replaceArguments(source, arguments); // Checks arguments
	}

	// Creates and returns a new List:
	public static List<String> replaceArguments(
			Collection<? extends String> messages,
			@NonNull Object... argumentPairs
	) {
		assert TEMP_ARGUMENTS_MAP.isEmpty();
		try {
			addArgumentsToMap(TEMP_ARGUMENTS_MAP, argumentPairs);
			return replaceArguments(messages, TEMP_ARGUMENTS_MAP);
		} finally {
			TEMP_ARGUMENTS_MAP.clear(); // Reset
		}
	}

	// Creates and returns a new List:
	public static List<String> replaceArguments(
			Collection<? extends String> sources,
			Map<? extends String, @NonNull ?> arguments
	) {
		return replaceArguments(sources, MessageArguments.ofMap(arguments));
	}

	// Creates and returns a new List:
	public static List<String> replaceArguments(
			Collection<? extends String> sources,
			MessageArguments arguments
	) {
		Validate.notNull(sources, "sources is null");
		List<String> replaced = new ArrayList<>(sources.size());
		for (String source : sources) {
			replaced.add(replaceArguments(source, arguments)); // Checks arguments
		}
		return replaced;
	}

	// Faster than regex/matcher and StringBuilder#replace in a loop.
	// Argument's map: Faster than iterating and comparing.
	// Arguments may occur more than once.
	// Arguments inside arguments are not replaced.
	// TODO Add support for argument options (e.g.: formatting options, etc.).
	// TODO Allow escaping, e.g. via "\{key\}"?
	// TODO Improve handling of inner braces: "{some{key}", "{some{inner}key}"
	public static class ArgumentsReplacer {

		// Default key format: {key}
		public static final char DEFAULT_KEY_PREFIX_CHAR = '{';
		public static final char DEFAULT_KEY_SUFFIX_CHAR = '}';

		private @Nullable String source;
		private int sourceLength;
		private char keyPrefixChar;
		private char keySuffixChar;
		private @Nullable MessageArguments arguments;

		// Current search state:
		// Index of where to start the search for the next key in the source String:
		private int searchPos = 0;
		private int keyPrefixIndex = -1; // Index of last found key prefix char
		private int keySuffixIndex = -1; // Index of last found key suffix char
		private int keyStartIndex = -1; // Inclusive: points to first key char (after prefix char)
		private int keyEndIndex = -1; // Exclusive (= keySuffixIndex)
		protected @Nullable String key = null; // The current key
		protected @Nullable Object argument = null; // The current argument

		// Current result state:
		protected @Nullable StringBuilder resultBuilder = null;
		// Start index of remaining source text that still needs to be included in the result:
		private int resultSourcePos = 0;
		private @Nullable String result = null;

		public ArgumentsReplacer() {
		}

		public String replaceArguments(String source, MessageArguments arguments) {
			return this.replaceArguments(
					source,
					DEFAULT_KEY_PREFIX_CHAR, DEFAULT_KEY_SUFFIX_CHAR,
					arguments
			);
		}

		public String replaceArguments(
				String source,
				char keyPrefixChar,
				char keySuffixChar,
				MessageArguments arguments
		) {
			// Setup:
			this.setup(source, keyPrefixChar, keySuffixChar, arguments); // Validates input

			// Replace arguments:
			this.replaceArguments();

			// Capture result:
			String result = Unsafe.assertNonNull(this.result);

			// Clean up (avoids accidental memory leaks in case this ArgumentReplacer is kept around
			// for later reuse):
			this.cleanUp();

			// Return result:
			return result;
		}

		protected void setup(
				String source,
				char keyPrefixChar,
				char keySuffixChar,
				MessageArguments arguments
		) {
			Validate.notNull(source, "source is null");
			Validate.notNull(arguments, "arguments is null");

			this.source = source;
			sourceLength = source.length();
			this.keyPrefixChar = keyPrefixChar;
			this.keySuffixChar = keySuffixChar;
			this.arguments = arguments;

			// Current search state:
			searchPos = 0;
			keyPrefixIndex = -1;
			keySuffixIndex = -1;
			keyStartIndex = -1;
			keyEndIndex = -1;
			key = null;
			argument = null;

			// Current result state:
			result = null;
			if (resultBuilder != null) {
				// We reuse the previous StringBuilder (Note: keeps the current capacity):
				resultBuilder.setLength(0);
			}
			// Start index of remaining source text that still needs to be included in the result:
			resultSourcePos = 0;

			// Initial:
			if (sourceLength <= 2) {
				// Source is not large enough to contain any key: End the search right away.
				searchPos = sourceLength;
			}
		}

		// Clears any Object references that are no longer required after operation.
		protected void cleanUp() {
			source = null;
			arguments = null;
			key = null;
			argument = null;
			result = null;
			if (resultBuilder != null) {
				// We reuse the previous StringBuilder (Note: Keeps the current capacity):
				resultBuilder.setLength(0);
			}
		}

		private void replaceArguments() {
			assert result == null; // We don't have a result yet
			while (this.findNextKey()) {
				// Find replacement argument for the current key:
				String key = Unsafe.assertNonNull(this.key);
				argument = this.resolveArgument(key);

				// Append replacement:
				if (argument != null) {
					this.appendPrefix();
					this.appendArgument();
				}
				// Else: No argument found for the current key. -> Continue the search for the next
				// key.
			}

			// Append remaining suffix (if any):
			if (resultSourcePos <= 0) {
				// There has been no argument replacement, otherwise we would have included a prefix
				// to the result already. -> The 'suffix' matches the complete source String.
				// We skip copying the suffix to the result builder and instead use the source
				// directly as result.
				result = source;
				resultSourcePos = sourceLength; // Update resultSourcePos
				return;
			}

			if (resultSourcePos < sourceLength) {
				this.appendSuffix();
			} // Else: Remaining suffix is empty.

			// Prepare result:
			this.prepareResult();
		}

		// Returns true if a next key has been found.
		private boolean findNextKey() {
			if (searchPos >= sourceLength) {
				// We already searched through the whole source String.
				return false;
			}

			String source = Unsafe.assertNonNull(this.source);

			// Search key prefix character:
			keyPrefixIndex = source.indexOf(keyPrefixChar, searchPos);
			if (keyPrefixIndex < 0) {
				return false; // No prefix char found. -> No more keys.
			}
			keyStartIndex = keyPrefixIndex + 1;

			// Search key suffix character:
			keySuffixIndex = source.indexOf(keySuffixChar, keyStartIndex);
			if (keySuffixIndex < 0) {
				return false; // No suffix char found. -> No more keys.
			}
			keyEndIndex = keySuffixIndex;
			key = source.substring(keyStartIndex, keyEndIndex);

			// The search for the next key starts after the current key:
			searchPos = keySuffixIndex + 1;
			return true;
		}

		// The argument to replace the current key:
		protected @Nullable Object resolveArgument(String key) {
			Object argument = Unsafe.assertNonNull(arguments).get(key);
			if (argument instanceof Supplier supplier) {
				argument = supplier.get(); // Can be null
			}

			// String replacement only supports plain strings:
			if (argument instanceof Text text) {
				argument = text.toPlainText();
			}

			return argument; // Can be null
		}

		protected void appendPrefix() {
			// Initialize result StringBuilder:
			if (resultBuilder == null) {
				// Heuristic: Expecting at most 25% increase in size.
				resultBuilder = new StringBuilder(sourceLength + sourceLength / 4);
			}
			assert resultBuilder != null;

			// Append prefix (not yet included chars in front of key):
			resultBuilder.append(source, resultSourcePos, keyPrefixIndex);
			resultSourcePos = keySuffixIndex + 1; // Update resultSourcePos
		}

		protected void appendArgument() {
			// Append argument:
			String argumentString = Unsafe.assertNonNull(argument).toString();
			// Not null: We have already appended the prefix.
			Unsafe.assertNonNull(resultBuilder).append(argumentString);
		}

		protected void appendSuffix() {
			assert resultSourcePos > 0 && resultSourcePos < sourceLength && resultBuilder != null;
			// Append suffix:
			Unsafe.assertNonNull(resultBuilder).append(source, resultSourcePos, sourceLength);
			resultSourcePos = sourceLength; // Update resultSourcePos
		}

		protected void prepareResult() {
			result = Unsafe.assertNonNull(resultBuilder).toString();
		}
	}

	private StringUtils() {
	}
}
