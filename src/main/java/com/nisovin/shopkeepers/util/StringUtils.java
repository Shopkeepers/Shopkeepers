package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Utility functions related to Strings.
 */
public class StringUtils {

	private StringUtils() {
	}

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public static boolean isEmpty(String string) {
		return (string == null) || string.isEmpty();
	}

	/**
	 * Makes sure that the given string is not empty.
	 * 
	 * @param string
	 *            the string
	 * @return the string itself, or <code>null</code> if it is empty
	 */
	public static String getNotEmpty(String string) {
		return isEmpty(string) ? null : string;
	}

	/**
	 * Makes sure that the given string is not <code>null</code>.
	 * 
	 * @param string
	 *            the string
	 * @return the string itself, or an empty string if it is <code>null</code>
	 */
	public static String getNotNull(String string) {
		return (string == null) ? "" : string;
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace and converts all remaining whitespace and underscores to dashes
	 * ('{@code -}').
	 * <p>
	 * This returns <code>null</code> for a <code>null</code> input identifier.
	 * 
	 * @param identifier
	 *            the identifier
	 * @return the normalized identifier
	 */
	public static String normalizeKeepCase(String identifier) {
		if (identifier == null) return null;
		identifier = identifier.trim();
		identifier = identifier.replace('_', '-');
		identifier = replaceWhitespace(identifier, "-");
		return identifier;
	}

	/**
	 * Normalizes the given identifier.
	 * <p>
	 * This trims leading and trailing whitespace, converts all remaining whitespace and underscores to dashes
	 * ('{@code -}') and converts all characters to lower case.
	 * <p>
	 * This returns <code>null</code> for a <code>null</code> input identifier.
	 * 
	 * @param identifier
	 *            the identifier
	 * @return the normalized identifier
	 */
	public static String normalize(String identifier) {
		if (identifier == null) return null;
		identifier = normalizeKeepCase(identifier);
		return identifier.toLowerCase(Locale.ROOT);
	}

	public static List<String> normalize(List<String> identifiers) {
		if (identifiers == null) return null;
		List<String> normalized = new ArrayList<>(identifiers.size());
		for (String identifier : identifiers) {
			normalized.add(normalize(identifier));
		}
		return normalized;
	}

	/**
	 * Checks if the given strings contains whitespace characters.
	 * 
	 * @param string
	 *            the string
	 * @return <code>true</code> if the given string is not empty and contains at least one whitespace character
	 */
	public static boolean containsWhitespace(String string) {
		if (isEmpty(string)) {
			return false;
		}

		int length = string.length();
		for (int i = 0; i < length; i++) {
			if (Character.isWhitespace(string.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all whitespace characters from the given string.
	 * 
	 * @param source
	 *            the source string
	 * @return the source string itself if it is empty, otherwise the string without any whitespace characters
	 */
	public static String removeWhitespace(String source) {
		return replaceWhitespace(source, "");
	}

	/**
	 * Replaces all whitespace characters from the given string.
	 * 
	 * @param source
	 *            the source string
	 * @param replacement
	 *            the replacement string
	 * @return the source string itself if it is empty, otherwise the string with any whitespace characters replaced
	 */
	public static String replaceWhitespace(String source, String replacement) {
		if (isEmpty(source)) {
			return source;
		}
		if (replacement == null) {
			replacement = "";
		}
		return WHITESPACE_PATTERN.matcher(source).replaceAll(replacement);
	}

	public static String capitalizeAll(String source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		int sourceLength = source.length();
		StringBuilder builder = new StringBuilder(sourceLength);
		boolean capitalizeNext = true; // capitalize first letter
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

	// matches windows and unix line endings, and literal newlines
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("(\\r?\\n)|\\\\n");

	// includes empty and trailing empty lines
	public static String[] splitLines(String source) {
		return NEWLINE_PATTERN.split(source, -1);
	}

	public static String prefix(String prefix, String delimiter, String message) {
		if (StringUtils.isEmpty(prefix)) return message;
		return prefix + (delimiter == null ? "" : delimiter) + message;
	}

	// ARGUMENTS REPLACEMENT

	// throws NPE if source, target, or replacement are null
	// returns the source String (same instance) if there is no match
	// performance is okay for replacing a single argument, but for multiple arguments prefer using replaceArguments
	// rather than invoking this multiple times
	public static String replaceFirst(String source, String target, CharSequence replacement) {
		int index = source.indexOf(target);
		if (index == -1) return source; // no match

		int sourceLength = source.length();
		int targetLength = target.length();
		int increase = replacement.length() - targetLength;

		StringBuilder result = new StringBuilder(sourceLength + increase);
		result.append(source, 0, index); // prefix
		result.append(replacement); // replacement
		result.append(source, index + targetLength, sourceLength); // suffix
		return result.toString();
	}

	private static final ArgumentsReplacer ARGUMENTS_REPLACER = new ArgumentsReplacer(); // singleton for reuse

	// uses the String representation of the given arguments
	// if an argument is a Supplier, it gets invoked to obtain the actual argument
	public static String replaceArguments(String source, Map<String, Object> arguments) {
		return ARGUMENTS_REPLACER.replaceArguments(source, arguments);
	}

	// faster than regex/matcher and StringBuilder#replace in a loop
	// argument's map: faster than iterating and comparing
	// arguments may occur more than once
	// arguments inside arguments are not replaced
	// TODO add support for argument options (eg.: formatting options, etc.)
	// TODO allow escaping, eg. via "\{key\}"?
	// TODO improve handling of inner braces: "{some{key}", "{some{inner}key}"
	public static class ArgumentsReplacer {

		// default key format: {key}
		public static final char DEFAULT_KEY_PREFIX_CHAR = '{';
		public static final char DEFAULT_KEY_SUFFIX_CHAR = '}';

		private String source;
		private int sourceLength;
		private char keyPrefixChar;
		private char keySuffixChar;
		private Map<String, Object> arguments;

		// current search state:
		private int searchPos = 0; // index of where to start the search for the next key in the source String
		private int keyPrefixIndex = -1; // index of last found key prefix char
		private int keySuffixIndex = -1; // index of last found key suffix char
		private int keyStartIndex = -1; // inclusive: points to first key char (after prefix char)
		private int keyEndIndex = -1; // exclusive (= keySuffixIndex)
		protected String key = null; // the current key
		protected Object argument = null; // the current argument

		// current result state:
		protected StringBuilder resultBuilder = null;
		// start index of remaining source text that still needs to be included in the result:
		private int resultSourcePos = 0;
		private String result = null;

		public ArgumentsReplacer() {
		}

		public String replaceArguments(String source, Map<String, Object> arguments) {
			return this.replaceArguments(source, DEFAULT_KEY_PREFIX_CHAR, DEFAULT_KEY_SUFFIX_CHAR, arguments);
		}

		public String replaceArguments(String source, char keyPrefixChar, char keySuffixChar, Map<String, Object> arguments) {
			// setup:
			this.setup(source, keyPrefixChar, keySuffixChar, arguments); // validates input

			// replace arguments:
			this.replaceArguments();

			// capture result:
			String result = this.result;

			// clean up (avoids accidental memory leaks in case this ArgumentReplacer is kept around for later reuse):
			this.cleanUp();

			// return result:
			return result;
		}

		protected void setup(String source, char keyPrefixChar, char keySuffixChar, Map<String, Object> arguments) {
			Validate.notNull(source, "Source is null!");
			Validate.notNull(arguments, "Arguments is null!");

			this.source = source;
			sourceLength = source.length();
			this.keyPrefixChar = keyPrefixChar;
			this.keySuffixChar = keySuffixChar;
			this.arguments = arguments;

			// current search state:
			searchPos = 0;
			keyPrefixIndex = -1;
			keySuffixIndex = -1;
			keyStartIndex = -1;
			keyEndIndex = -1;
			key = null;
			argument = null;

			// current result state:
			result = null;
			if (resultBuilder != null) {
				resultBuilder.setLength(0); // we reuse the previous StringBuilder (note: keeps the current capacity)
			}
			resultSourcePos = 0; // start index of remaining source text that still needs to be included in the result

			// initial:
			if (sourceLength <= 2) {
				// source is not large enough to contain any key: end the search right away
				searchPos = sourceLength;
			}
		}

		// clears any Object references that are no longer required after operation
		protected void cleanUp() {
			source = null;
			arguments = null;
			key = null;
			argument = null;
			result = null;
			if (resultBuilder != null) {
				resultBuilder.setLength(0); // we reuse the previous StringBuilder (note: keeps the current capacity)
			}
		}

		private void replaceArguments() {
			assert result == null; // we don't have a result yet
			while (this.findNextKey()) {
				// find replacement argument for the current key:
				argument = this.resolveArgument(key);

				// append replacement:
				if (argument != null) {
					this.appendPrefix();
					this.appendArgument();
				} // else: no argument found for the current key -> continue the search for the next key
			}

			// append remaining suffix (if any):
			if (resultSourcePos <= 0) {
				// there has been no argument replacement, otherwise we would have included a prefix to the result
				// already -> the 'suffix' matches the complete source String
				// we skip copying the suffix to the result builder and instead use the source directly as result
				result = source;
				resultSourcePos = sourceLength; // update resultSourcePos
				return;
			}

			if (resultSourcePos < sourceLength) {
				this.appendSuffix();
			} // else: remaining suffix is empty

			// prepare result:
			this.prepareResult();
		}

		// returns true if a next key has been found
		private boolean findNextKey() {
			if (searchPos >= sourceLength) return false; // we already searched through the whole source String

			// search key prefix character:
			keyPrefixIndex = source.indexOf(keyPrefixChar, searchPos);
			if (keyPrefixIndex < 0) {
				return false; // no prefix char found -> no more keys
			}
			keyStartIndex = keyPrefixIndex + 1;

			// search key suffix character:
			keySuffixIndex = source.indexOf(keySuffixChar, keyStartIndex);
			if (keySuffixIndex < 0) {
				return false; // no suffix char found -> no more keys
			}
			keyEndIndex = keySuffixIndex;
			key = source.substring(keyStartIndex, keyEndIndex);

			// the search for the next key starts after the current key:
			searchPos = keySuffixIndex + 1;
			return true;
		}

		// the argument to replace the current key
		protected Object resolveArgument(String key) {
			Object argument = arguments.get(key);
			if (argument instanceof Supplier) {
				return ((Supplier<?>) argument).get(); // can be null
			} else {
				return argument; // can be null
			}
		}

		protected void appendPrefix() {
			// initialize result StringBuilder:
			if (resultBuilder == null) {
				// heuristic: expecting at most 25% increase in size
				resultBuilder = new StringBuilder(sourceLength + sourceLength / 4);
			}

			// append prefix (not yet included chars in front of key):
			resultBuilder.append(source, resultSourcePos, keyPrefixIndex);
			resultSourcePos = keySuffixIndex + 1; // update resultSourcePos
		}

		protected void appendArgument() {
			assert resultBuilder != null; // we have already appended the prefix
			assert argument != null;
			// append argument:
			String argumentString = argument.toString();
			resultBuilder.append(argumentString);
		}

		protected void appendSuffix() {
			assert resultSourcePos > 0 && resultSourcePos < sourceLength && resultBuilder != null;
			resultBuilder.append(source, resultSourcePos, sourceLength); // append suffix
			resultSourcePos = sourceLength; // update resultSourcePos
		}

		protected void prepareResult() {
			assert resultBuilder != null;
			result = resultBuilder.toString();
		}
	}
}
