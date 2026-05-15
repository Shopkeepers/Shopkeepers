package com.nisovin.shopkeepers.ui.confirmations;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.util.java.Validate;

public final class ConfirmationUI {

	private static @Nullable ConfirmationViewProvider VIEW_PROVIDER;

	private static ConfirmationViewProvider getViewProvider() {
		// Initialized lazily because the default UI types are not setup until after the plugin has
		// been enabled.
		if (VIEW_PROVIDER == null) {
			VIEW_PROVIDER = new ConfirmationViewProvider();
		}
		assert VIEW_PROVIDER != null;
		return VIEW_PROVIDER;
	}

	// TODO Add the involved shopkeeper/villager as context, so any open views can be automatically
	// aborted when needed (e.g. on concurrent edits by other players)?
	public static void requestConfirmation(Player player, ConfirmationUIState config) {
		Validate.notNull(player, "player is null");
		Validate.notNull(config, "config is null");

		// Note: This also closes any previous UI and thereby also aborts any previously active UI
		// confirmation request.
		UISessionManager.getInstance().requestUI(getViewProvider(), player, config);
	}

	private ConfirmationUI() {
	}
}
