package com.nisovin.shopkeepers.ui.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.PlayerOpenUIEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class UISessionManager {

	/**
	 * Adapter to hook into and alter certain behaviors of the {@link UISessionManager}.
	 */
	public interface SessionHandler {

		/**
		 * A {@link SessionHandler} with the default behavior.
		 */
		public static final SessionHandler DEFAULT = new SessionHandler() {
		};

		public default PlayerOpenUIEvent createPlayerOpenUIEvent(
				ViewProvider viewProvider,
				Player player,
				boolean silentRequest,
				UIState uiState
		) {
			return new PlayerOpenUIEvent(viewProvider.getUIType(), player, silentRequest);
		}
	}

	private static @Nullable UISessionManager instance;

	public static UISessionManager getInstance() {
		return Validate.State.notNull(instance, "Not yet initialized!");
	}

	// Note: We allow subsequent re-initialization. Although not currently expected, this allows
	// handling cases in which the plugin instance is re-created multiple times, potentially leading
	// to multiple initializations of the UISessionManager.
	public static void initialize(Plugin plugin, SessionHandler sessionHandler) {
		if (instance != null) {
			// Has no effect if already disabled:
			instance.onDisable();
		}

		instance = new UISessionManager(plugin, sessionHandler);
	}

	private final Plugin plugin;
	private final SessionHandler sessionHandler;
	private final UIListener uiListener;

	// Player id -> View
	private final Map<UUID, View> uiSessions = new HashMap<>();
	private final Collection<? extends View> uiSessionsView
			= Collections.unmodifiableCollection(uiSessions.values());

	private UISessionManager(Plugin plugin, SessionHandler sessionHandler) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(sessionHandler, "sessionHandler is null");
		this.plugin = plugin;
		this.sessionHandler = sessionHandler;
		this.uiListener = new UIListener(plugin, Unsafe.initialized(this));
	}

	public void onEnable() {
		uiListener.onEnable();
	}

	// Note: This method may also be called even when onEnabled has not been called yet, e.g.
	// because the plugin is being shut down during enabling.
	// Note: Further calls to abort UI sessions are allowed even after disabling.
	public void onDisable() {
		// Close all open views:
		this.abortUISessions();
		uiListener.onDisable();
	}

	public boolean requestUI(ViewProvider viewProvider, Player player) {
		return this.requestUI(viewProvider, player, false, UIState.EMPTY);
	}

	public boolean requestUI(ViewProvider viewProvider, Player player, UIState uiState) {
		return this.requestUI(viewProvider, player, false, uiState);
	}

	public boolean requestUI(ViewProvider viewProvider, Player player, boolean silentRequest) {
		return this.requestUI(viewProvider, player, silentRequest, UIState.EMPTY);
	}

	public boolean requestUI(
			ViewProvider viewProvider,
			Player player,
			boolean silentRequest,
			UIState uiState
	) {
		Validate.notNull(viewProvider, "viewProvider is null");
		Validate.notNull(player, "player is null");
		UIType uiType = viewProvider.getUIType();
		String uiIdentifier = uiType.getIdentifier();
		String playerName = player.getName();

		if (!player.isValid()) {
			Log.debug(() -> "Player " + playerName + " cannot open UI '" + uiIdentifier
					+ "': Player not connected.");
			return false;
		}

		if (!viewProvider.canOpen(player, silentRequest)) {
			Log.debug(() -> "Player " + playerName + " cannot open UI '" + uiIdentifier + "'.");
			return false;
		}

		View oldSession = this.getUISession(player);
		// Filter out duplicate open requests:
		if (oldSession != null && oldSession.getProvider().equals(viewProvider)) {
			Log.debug(() -> "UI '" + uiIdentifier + "'" + " is already open for player "
					+ playerName + ".");
			return false;
		}

		// Call event:
		PlayerOpenUIEvent openUIEvent = sessionHandler.createPlayerOpenUIEvent(
				viewProvider,
				player,
				silentRequest,
				uiState
		);
		Validate.notNull(openUIEvent, "SessionHandler returned null PlayerOpenUIEvent!");
		Bukkit.getPluginManager().callEvent(openUIEvent);
		if (openUIEvent.isCancelled()) {
			Log.debug(() -> "A plugin cancelled the opening of UI '" + uiIdentifier
					+ "' for player " + playerName + ".");
			return false;
		}

		// Close any previous inventory view before we start a new UI session:
		// Opening a new inventory view should already automatically close any previous inventory.
		// However, we need to do this before we start the new UI session, otherwise the closing of
		// the previous inventory view is incorrectly interpreted as the new UI being closed, which
		// immediately ends the new UI session again (possibly even with the UI remaining open
		// without an active UI session).
		// Also, we cannot be sure what the view provider and view implementation actually do.
		// This will call a PlayerCloseInventoryEvent, which also ends any previous UI session.
		player.closeInventory();
		assert this.getUISession(player) == null;

		// Instantiate the new view:
		Log.debug(() -> "Opening UI '" + uiIdentifier + "' for player " + player.getName()
				+ " ...");
		View view = viewProvider.createView(player, uiState);
		if (view == null) {
			Log.debug(() -> "Failed to instantiate UI '" + uiIdentifier + "'!");
			return false;
		}

		// Register event handlers for any not yet handled types of inventory events:
		view.getAdditionalInventoryEvents().forEach(uiListener::registerEventType);

		// Register the new UI session:
		uiSessions.put(player.getUniqueId(), view);

		// Open the view for the player:
		if (!view.open()) {
			Log.debug(() -> "Failed to open UI '" + uiIdentifier + "'!");
			this.endUISession(player, null);
			return false;
		}

		return true;
	}

	public Collection<? extends View> getUISessions() {
		return uiSessionsView;
	}

	public Collection<? extends View> getUISessionsForContext(Object contextObject) {
		Validate.notNull(contextObject, "contextObject is null");
		List<View> sessions = new ArrayList<>();
		uiSessionsView.forEach(uiSession -> {
			if (uiSession.getContext().getObject() == contextObject) {
				sessions.add(uiSession);
			}
		});
		return sessions;
	}

	public Collection<? extends View> getUISessionsForContext(Object contextObject, UIType uiType) {
		Validate.notNull(contextObject, "contextObject is null");
		Validate.notNull(uiType, "uiType is null");
		List<View> sessions = new ArrayList<>();
		uiSessionsView.forEach(uiSession -> {
			if (uiSession.getContext().getObject() == contextObject
					&& uiSession.getUIType() == uiType) {
				sessions.add(uiSession);
			}
		});
		return sessions;
	}

	public Collection<? extends View> getUISessions(UIType uiType) {
		Validate.notNull(uiType, "uiType is null");
		List<View> sessions = new ArrayList<>();
		uiSessionsView.forEach(uiSession -> {
			if (uiSession.getUIType() == uiType) {
				sessions.add(uiSession);
			}
		});
		return sessions;
	}

	public @Nullable View getUISession(Player player) {
		Validate.notNull(player, "player is null");
		return uiSessions.get(player.getUniqueId());
	}

	void onInventoryClose(InventoryCloseEvent closeEvent) {
		assert closeEvent != null;
		if (!(closeEvent.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) closeEvent.getPlayer();

		UISession session = this.getUISession(player);
		if (session == null) return;

		Log.debug(() -> "Player " + player.getName() + " closed UI '"
				+ session.getUIType().getIdentifier() + "'.");
		this.endUISession(player, closeEvent);
	}

	void onPlayerQuit(Player player) {
		// TODO This might have no effect, because CraftBukkit triggers an inventory close event
		// prior to the player quitting.
		this.endUISession(player, null);
	}

	// closeEvent can be null.
	void endUISession(Player player, @Nullable InventoryCloseEvent closeEvent) {
		assert player != null;
		View session = uiSessions.remove(player.getUniqueId());
		if (session == null) return;

		this.onSessionEnded(session, closeEvent);
	}

	// closeEvent can be null.
	private void onSessionEnded(View session, @Nullable InventoryCloseEvent closeEvent) {
		Log.debug(() -> "UI session '" + session.getUIType().getIdentifier()
				+ "' ended for player " + session.getPlayer().getName() + ".");
		session.onSessionEnd(); // Invalidate the session
		session.onInventoryClose(closeEvent); // Inform the view
	}

	// Called by View.
	void abort(View uiSession) {
		assert uiSession != null;
		if (!uiSession.isValid()) return;

		Player player = uiSession.getPlayer();
		this.endUISession(player, null);
		player.closeInventory();
	}

	public void abortUISessions() {
		// Copy to prevent concurrent modifications:
		new ArrayList<>(this.getUISessions()).forEach(View::abort);
		assert uiSessions.isEmpty();
	}

	public void abortUISessionsForContext(Object contextObject) {
		// Note: The returned collection is already a copy.
		this.getUISessionsForContext(contextObject).forEach(View::abort);
	}

	public void abortUISessionsForContext(Object contextObject, UIType uiType) {
		// Note: The returned collection is already a copy.
		this.getUISessionsForContext(contextObject, uiType).forEach(View::abort);
	}

	public void abortUISessionsForContextDelayed(@Nullable Location location, Object contextObject) {
		Validate.notNull(contextObject, "context is null");

		// Deactivate currently active UIs for this subject:
		this.deactivateUIsForContext(contextObject);

		if (location != null) {
			SchedulerUtils.runTaskOrOmit(location, () -> {
				this.abortUISessionsForContext(contextObject);
			});
		} else {
			this.abortUISessionsForContext(contextObject);
		}
	}

	public void abortUISessionsForContextDelayed(@Nullable Location location, Object contextObject, UIType uiType) {
		Validate.notNull(contextObject, "context is null");
		Validate.notNull(uiType, "uiType is null");

		// Deactivate currently active UIs for this subject:
		this.deactivateUIsForContext(contextObject, uiType);

		if (location != null) {
			SchedulerUtils.runTaskOrOmit(location, () -> {
				this.abortUISessionsForContext(contextObject, uiType);
			});
		} else {
			this.abortUISessionsForContext(contextObject, uiType);
		}
	}

	private void deactivateUIsForContext(Object contextObject) {
		assert contextObject != null;
		uiSessionsView.forEach(uiSession -> {
			if (uiSession.getContext().getObject() == contextObject) {
				uiSession.deactivateUI();
			}
		});
	}

	private void deactivateUIsForContext(Object contextObject, UIType uiType) {
		assert contextObject != null;
		uiSessionsView.forEach(uiSession -> {
			if (uiSession.getContext().getObject() == contextObject
					&& uiSession.getUIType() == uiType) {
				uiSession.deactivateUI();
			}
		});
	}
}
