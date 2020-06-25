package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.nisovin.shopkeepers.util.TextUtils;

public class AmbiguousPlayerNameHandler<P extends OfflinePlayer> extends AbstractAmbiguousPlayerNameHandler<P> {

	public AmbiguousPlayerNameHandler(String input, Iterable<? extends P> matches) {
		this(input, matches, DEFAULT_MAX_ENTRIES);
	}

	public AmbiguousPlayerNameHandler(String input, Iterable<? extends P> matches, int maxEntries) {
		super(input, matches, maxEntries);
	}

	@Override
	protected String getName(P match) {
		assert match != null;
		// getName should not be null since its a name-based match. However, we handle this case as well just in case:
		return TextUtils.getPlayerNameOrUnknown(match.getName());
	}

	@Override
	protected UUID getUniqueId(P match) {
		assert match != null;
		return match.getUniqueId();
	}
}
