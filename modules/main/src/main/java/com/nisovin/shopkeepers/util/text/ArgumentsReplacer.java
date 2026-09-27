package com.nisovin.shopkeepers.util.text;

import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

// Faster than regex/matcher and StringBuilder#replace in a loop.
// Argument's map: Faster than iterating and comparing.
// Arguments may occur more than once.
// Arguments inside arguments are not replaced.
// TODO Add support for argument options (e.g.: formatting options, etc.).
// TODO Allow escaping, e.g. via "\{key\}"?
// TODO Improve handling of inner braces: "{some{key}", "{some{inner}key}"
public class ArgumentsReplacer {

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
		if (argument instanceof Supplier<?> supplier) {
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
