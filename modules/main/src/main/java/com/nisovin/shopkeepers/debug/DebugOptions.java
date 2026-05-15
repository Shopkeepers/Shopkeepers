package com.nisovin.shopkeepers.debug;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DebugOptions {

	private static final Set<String> allOptions = new LinkedHashSet<>();
	private static final Set<? extends String> allOptionsView = Collections.unmodifiableSet(allOptions);

	// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get
	// combined into a single logging entry to slightly reduce spam.
	public static final String logAllEvents = add("log-all-events");
	// Prints the registered listeners for the first call of each event.
	public static final String printListeners = add("print-listeners");
	// Enables debugging output related to shopkeeper activation.
	public static final String shopkeeperActivation = add("shopkeeper-activation");
	// Enables debug output related to shopkeeper ticking activities that may be considered
	// non-exceptional, and might occur regularly and therefore cause debug spam otherwise. This
	// includes, for example, the activity of teleporting shopkeepers back into place, or updating a
	// shopkeeper's location when a mobile shopkeeper NPC moved around.
	public static final String regularTickActivities = add("regular-tick-activities");
	// Visualizes the ticking activities of shopkeepers in-game.
	public static final String visualizeShopkeeperTicks = add("visualize-shopkeeper-ticks");
	// Enables additional commands related debugging output.
	public static final String commands = add("commands");
	// Logs information when updating stored player names, such as of shop owners or shop members.
	public static final String playerNameUpdates = add("player-name-updates");
	// TODO Remove this once all servers are expected to have updated to our new item stack
	// serialization. Logging item migrations during item stack serialization would be quite spammy,
	// occur frequently during server updates, and not provide context information about the
	// involved shopkeeper or trade.
	// Logs whenever a shopkeeper performs item migrations (e.g. for trading offers).
	public static final String itemMigrations = add("item-migrations");
	// Logs whenever items are updated via the UpdateItemEvent.
	public static final String itemUpdates = add("item-updates");
	// Logs detailed item information for the selected trade and the items in the input slots
	// whenever a player clicks an empty trading result slot.
	public static final String emptyTrades = add("empty-trades");
	// Logs additional debug output whenever component-based text is sent.
	public static final String textComponents = add("text-components");
	// Logs additional debug output whenever a teleport is considered unsafe.
	public static final String unsafeTeleports = add("unsafe-teleports");
	// Logs additional debug output whenever a shopkeeper's data is marked as 'dirty'.
	public static final String shopkeeperDirty = add("shopkeeper-dirty");

	private static String add(String debugOption) {
		allOptions.add(debugOption);
		return debugOption;
	}

	public static Set<? extends String> getAll() {
		return allOptionsView;
	}

	private DebugOptions() {
	}
}
