package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.text.TextBuilder;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AmbiguousInputHandler<M> {

	protected static final int DEFAULT_MAX_ENTRIES = 5;

	protected final String input; // not null
	protected final Iterable<? extends M> matches; // not null; only iterated once
	protected final int maxEntries;

	private boolean alreadyProcessed = false;
	protected M firstMatch = null; // can be null, is also set even if the input is ambiguous
	private Text errorMsg = null; // null if the input is not ambiguous

	public AmbiguousInputHandler(String input, Iterable<? extends M> matches) {
		this(input, matches, DEFAULT_MAX_ENTRIES);
	}

	public AmbiguousInputHandler(String input, Iterable<? extends M> matches, int maxEntries) {
		Validate.notNull(input, "input is null");
		Validate.notNull(matches, "matches is null");
		this.input = input;
		this.matches = matches;
		this.maxEntries = maxEntries;
	}

	// Assigns the 'firstMatch' and builds the error message if there are multiple matches.
	private void processMatches() {
		if (alreadyProcessed) return;
		alreadyProcessed = true;

		Iterator<? extends M> matchesIterator = matches.iterator();
		if (!matchesIterator.hasNext()) {
			// Empty -> not ambiguous.
			// firstMatch and errorMsg remain null.
			return;
		}
		// Note: Null may be a valid match.
		this.firstMatch = matchesIterator.next();
		if (!matchesIterator.hasNext()) {
			// Only one element -> not ambiguous.
			// errorMsg remains null.
			return;
		}
		this.errorMsg = buildErrorMessage(firstMatch, matchesIterator);
		assert errorMsg != null;
	}

	// Can return null to skip.
	protected abstract Text getHeaderText();

	// Index starts at 1 for the first match.
	// If null is considered to be a valid match, this method needs to be able to deal with that.
	// Does not return null.
	protected abstract Text getEntryText(M match, int index);

	// Can return null to skip.
	protected abstract Text getMoreText();

	// This is only called if the input has been determined to actually be ambiguous. Does not return null.
	protected Text buildErrorMessage(M firstMatch, Iterator<? extends M> furtherMatches) {
		// Note: Null may be a valid match.
		assert furtherMatches != null && furtherMatches.hasNext();
		Map<String, Object> arguments = new HashMap<>();
		TextBuilder errorMsgBuilder = Text.text("");

		// Header:
		Text header = this.getHeaderText();
		if (header != null) {
			String headerPlaceholderKey = "header";
			errorMsgBuilder = errorMsgBuilder.placeholder(headerPlaceholderKey);
			arguments.put(headerPlaceholderKey, header);
		}

		int index = 1;
		M match = firstMatch;
		while (true) {
			// Limit the number of listed match entries:
			if (index > maxEntries) {
				// Text indicating that there are more matches:
				Text more = this.getMoreText();
				if (more != null) {
					String morePlaceholderKey = "more";
					errorMsgBuilder = errorMsgBuilder.newline().reset().placeholder(morePlaceholderKey);
					arguments.put(morePlaceholderKey, more);
				}
				break;
			}

			// Entry for the current match:
			Text entry = this.getEntryText(match, index);
			assert entry != null;
			String entryPlaceholderKey = "entry" + index;
			errorMsgBuilder = errorMsgBuilder.newline().reset().placeholder(entryPlaceholderKey);
			arguments.put(entryPlaceholderKey, entry);

			if (furtherMatches.hasNext()) {
				match = furtherMatches.next();
				index++;
			} else {
				break;
			}
		}
		assert errorMsgBuilder != null;
		Text errorMsg = errorMsgBuilder.buildRoot();
		errorMsg.setPlaceholderArguments(arguments);
		assert errorMsg != null;
		return errorMsg;
	}

	/**
	 * Gets the first match.
	 * 
	 * @return the first match, may be <code>null</code> if there are no matches or if <code>null</code> is a valid
	 *         value for the matches
	 */
	public final M getFirstMatch() {
		this.processMatches();
		return firstMatch;
	}

	/**
	 * Checks if the input is ambiguous.
	 * <p>
	 * No {@link #getErrorMsg() error message} is available in this case.
	 * 
	 * @return <code>true</code> if the input is ambiguous
	 */
	public final boolean isInputAmbiguous() {
		return (this.getErrorMsg() != null);
	}

	/**
	 * Gets the error message {@link Text} if the input is ambiguous.
	 * 
	 * @return the error message, or <code>null</code> if the input is not ambiguous
	 */
	public final Text getErrorMsg() {
		this.processMatches();
		return errorMsg; // can be null
	}
}
