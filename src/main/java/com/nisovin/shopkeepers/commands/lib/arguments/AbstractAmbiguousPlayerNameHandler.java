package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.text.Text;

// Allows for reuse among any type of matched object that represents players and is able to provide the player's name
// and unique id.
public abstract class AbstractAmbiguousPlayerNameHandler<M> extends AmbiguousInputHandler<M> {

	public AbstractAmbiguousPlayerNameHandler(String input, Iterable<? extends M> matches) {
		this(input, matches, DEFAULT_MAX_ENTRIES);
	}

	public AbstractAmbiguousPlayerNameHandler(String input, Iterable<? extends M> matches, int maxEntries) {
		super(input, matches, maxEntries);
	}

	// TODO Note: We don't copy the Texts here, so they are only valid if used immediately.

	@Override
	protected Text getHeaderText() {
		Text header = Settings.msgAmbiguousPlayerName;
		header.setPlaceholderArguments("name", input);
		return header;
	}

	protected abstract String getName(M match);

	protected abstract UUID getUniqueId(M match);

	@Override
	protected Text getEntryText(M match, int index) {
		assert match != null;
		String matchName = this.getName(match);
		assert matchName != null;
		UUID matchUUID = this.getUniqueId(match);
		assert matchUUID != null;
		String matchUUIDString = matchUUID.toString();
		Text entry = Settings.msgAmbiguousPlayerNameEntry;
		entry.setPlaceholderArguments(
				"index", index,
				"name", Text.insertion(matchName).childText(matchName).buildRoot(),
				"uuid", Text.insertion(matchUUIDString).childText(matchUUIDString).buildRoot()
		);
		return entry;
	}

	@Override
	protected Text getMoreText() {
		return Settings.msgAmbiguousPlayerNameMore;
	}
}
