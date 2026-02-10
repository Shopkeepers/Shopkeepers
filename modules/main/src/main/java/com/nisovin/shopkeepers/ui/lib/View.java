package com.nisovin.shopkeepers.ui.lib;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * A user interface instance that represents a {@link UISession} and handles the user interactions
 * with an underlying {@link InventoryView}.
 * <p>
 * A {@link View} is responsible for setting up, opening, and handling the interactions with an
 * underlying {@link InventoryView} for a player. Once opened, the {@link View} is only valid as
 * long as the underlying {@link InventoryView} is still valid. If the view wants to open a new
 * {@link InventoryView} in response to a user interaction, the new {@link InventoryView} is
 * represented by a new {@link View} instance. Any state that should be preserved between the views
 * needs to be passed to the new {@link View}, for example in the form of a {@link UIState}.
 * <p>
 * A {@link View} is associated with the {@link #getProvider() ViewProvider} that instantiated it,
 * which also provides information about the its {@link ViewProvider#getContext() view context}: The
 * context can be any arbitrary object, but is usually the primary object for which the view is
 * visualizing data or providing controls. The context can be used to query the active views that
 * share the same context, for example to update each other.
 */
public abstract class View implements UISession {

	private final ViewProvider provider;
	private final Player player;
	private final UIState initialUIState;

	private boolean valid = true;
	private boolean uiActive = true;

	private @Nullable InventoryView inventoryView;

	// Heuristic detection of automatically triggered shift left-clicks:
	private static final long AUTOMATIC_SHIFT_LEFT_CLICK_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);
	private long lastManualClickNanos = 0L;
	private int lastManualClickedSlotId = -1;
	private boolean isAutomaticShiftLeftClick = false;

	/**
	 * Creates a new {@link View}.
	 * 
	 * @param provider
	 *            the {@link ViewProvider} that instantiates this view, not <code>null</code>
	 * @param player
	 *            the player for whom the view is created, not <code>null</code>
	 * @param uiState
	 *            the initial {@link UIState}, not <code>null</code>, can be {@link UIState#EMPTY}
	 *            if accepted by the view
	 */
	protected View(ViewProvider provider, Player player, UIState uiState) {
		Validate.notNull(provider, "provider is null");
		Validate.notNull(player, "player is null");
		Validate.notNull(uiState, "uiState is null");

		this.provider = provider;
		this.player = player;

		// Note: We intentionally don't offer a flag for callers to silently ignore incompatible UI
		// states, since certain views always require a specific UI state to be provided in order
		// to function correctly. It is up to the view implementation to decide whether it accepts
		// the given UI state.
		this.validateState(uiState);
		this.initialUIState = uiState;
	}

	@Override
	public final AbstractUIType getUIType() {
		return provider.getUIType();
	}

	/**
	 * Gets the {@link ViewProvider} that instantiated this view.
	 * 
	 * @return the {@link ViewProvider}, not <code>null</code>
	 */
	public final ViewProvider getProvider() {
		return provider;
	}

	@Override
	public final Player getPlayer() {
		return player;
	}

	/**
	 * Gets the {@link ViewContext}.
	 * 
	 * @return the context, not <code>null</code>
	 */
	public final ViewContext getContext() {
		return provider.getContext();
	}

	// Can be null for UIs that are not associated with any shopkeeper:
	@Override
	public final @Nullable AbstractShopkeeper getShopkeeper() {
		if (this.getContext().getObject() instanceof AbstractShopkeeper shopkeeper) {
			return shopkeeper;
		}

		return null;
	}

	/**
	 * Gets the {@link Shopkeeper} associated with this view.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 * @throws IllegalStateException
	 *             if there is no associated shopkeeper
	 */
	public AbstractShopkeeper getShopkeeperNonNull() {
		return Validate.State.notNull(this.getShopkeeper(), "Shopkeeper is null!");
	}

	/**
	 * Gets the initially supplied and validated {@link UIState}.
	 * 
	 * @return the initially supplied {@link UIState}, not <code>null</code>
	 */
	protected final UIState getInitialUIState() {
		return initialUIState;
	}

	@Override
	public final boolean isValid() {
		return valid;
	}

	/**
	 * Verifies that this view is still {@link #isValid() valid}.
	 */
	protected final void validateIsValid() {
		Validate.State.isTrue(this.isValid(),
				"This operation is not allowed because this view is no longer valid!");
	}

	/**
	 * This is called at the end of the UI session and marks this view as {@link #isValid()
	 * invalid}.
	 */
	final void onSessionEnd() {
		this.markInvalid();
	}

	private final void markInvalid() {
		valid = false;
	}

	@Override
	public final boolean isUIActive() {
		return uiActive;
	}

	@Override
	public final void deactivateUI() {
		uiActive = false;
	}

	@Override
	public final void activateUI() {
		uiActive = true;
	}

	@Override
	public final void close() {
		if (!this.isValid()) return;

		// This triggers an InventoryCloseEvent which ends the UI session:
		player.closeInventory();
	}

	@Override
	public final void closeDelayed() {
		this.closeDelayedAndRunTask(null);
	}

	@Override
	public final void closeDelayedAndRunTask(@Nullable Runnable task) {
		if (!this.isValid()) return;

		this.deactivateUI();

		// This fails during plugin disable. However, all UIs will be closed anyway.
		SchedulerUtils.runTaskOrOmit(player, () -> {
			if (!this.isValid()) return;

			this.close();
			if (task != null) {
				task.run();
			}
		});
	}

	@Override
	public final void abort() {
		UISessionManager.getInstance().abort(this);
	}

	@Override
	public final void abortDelayed() {
		this.abortDelayedAndRunTask(null);
	}

	@Override
	public final void abortDelayedAndRunTask(@Nullable Runnable task) {
		if (!this.isValid()) return;

		this.deactivateUI();

		// This fails during plugin disable. However, all UIs will be closed anyway.
		SchedulerUtils.runTaskOrOmit(player, () -> {
			if (!this.isValid()) return;
			this.abort();
			if (task != null) {
				task.run();
			}
		});
	}

	/**
	 * Sets up and opens this view.
	 * <p>
	 * The view can only be opened once. If opening the view fails, this view is marked as
	 * {@link #isValid() invalid}.
	 * 
	 * @return <code>true</code> if the view was successfully opened
	 * @throws IllegalStateException
	 *             if this view is no longer valid or was already opened before
	 */
	public final boolean open() {
		this.validateIsValid();
		Validate.State.isTrue(this.inventoryView == null, "Already opened!");

		// Set up and open the inventory view:
		InventoryView inventoryView = this.openInventoryView();
		if (inventoryView == null) {
			Log.debug("Failed to open the inventory view.");
			this.markInvalid();

			// We don't expect the player to have any inventory open currently. But just in case
			// that opening the inventory view partially succeeded but we got back a null
			// InventoryView nevertheless, close any currently open InventoryView for the player.
			player.closeInventory();
			return false;
		}

		this.inventoryView = inventoryView;

		return true;
	}

	/**
	 * Sets up and opens the {@link InventoryView}.
	 * <p>
	 * Depending on the view implementation and the type of menu being opened, this operation can
	 * consist of several steps:
	 * <ul>
	 * <li>Perform any pending one-time setup that needs to happen prior to the view being opened.
	 * <li>Creating the {@link Inventory}.
	 * <li>Setting up the initial inventory contents. Consider sharing the implementation with
	 * {@link #updateInventory()}.
	 * <li>Opening the {@link InventoryView} for the {@link #getPlayer() player}, e.g. via
	 * {@link Player#openInventory(Inventory)}.
	 * <li>Setting up the opened {@link InventoryView}.
	 * </ul>
	 * For some menu types, e.g. merchants, the {@link Inventory} is not created up-front, but as
	 * part of opening the {@link InventoryView}, e.g. via
	 * {@link Player#openMerchant(org.bukkit.inventory.Merchant, boolean)}.
	 * <p>
	 * Instead of creating a new {@link Inventory}, this may also open an existing {@link Inventory}
	 * or reuse an inventory instance from a pool. However, be aware that if the used inventory is
	 * shared in other contexts, or across multiple {@link InventoryView} instances, the behavior
	 * may be unexpected, since this {@link View} instance is intended to only handle inventory
	 * interactions and keep track of the UI state for a single player.
	 * <p>
	 * Consider overriding {@link #getInventory()} if necessary.
	 * 
	 * @return the inventory view, or <code>null</code> if the view cannot be opened (aborts the
	 *         opening of this view)
	 */
	protected abstract @Nullable InventoryView openInventoryView();

	/**
	 * Gets the {@link InventoryView} associated with this view.
	 * 
	 * @return the inventory view, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the view has not been opened yet
	 */
	public final InventoryView getInventoryView() {
		return Validate.State.notNull(inventoryView, "The view has not been opened yet!");
	}

	/**
	 * Checks if the {@link #getInventoryView() InventoryView} of this view has already been
	 * successfully created.
	 * 
	 * @return <code>true</code> if the inventory view has already been successfully created
	 */
	public final boolean hasInventoryView() {
		return inventoryView != null;
	}

	/**
	 * Gets the {@link Inventory} associated with this view.
	 * <p>
	 * By default, the inventory is retrieved from the opened {@link #getInventoryView()
	 * InventoryView} via {@link InventoryView#getTopInventory()}, but view implementations can
	 * override this if necessary.
	 * 
	 * @return the inventory, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the inventory cannot be retrieved because the view was not opened yet
	 */
	protected Inventory getInventory() {
		var inventoryView = this.getInventoryView();
		return inventoryView.getTopInventory();
	}

	protected final void debugNotOpeningUI(Player player, String reason) {
		Validate.notEmpty(reason, "reason is null or empty");
		this.getProvider().debugNotOpeningUI(this.getPlayer(), reason);
	}

	/**
	 * Check if this view handles interactions for the given {@link InventoryView}.
	 * <p>
	 * The {@link UIRegistry} already keeps track of a player's currently open UI. This additional
	 * check verifies that the inventory view the player is interacting with actually matches the
	 * inventory view expected by this {@link View}. The result of this method is checked before any
	 * inventory events are passed through to this view.
	 * 
	 * @param view
	 *            an inventory view, not <code>null</code>
	 * @return <code>true</code> if the given inventory view matches the inventory view handled by
	 *         this view
	 */
	public boolean isHandling(InventoryView view) {
		return this.inventoryView == view;
	}

	/**
	 * Checks if the given player has this view open currently.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if this view is open currently
	 */
	public final boolean isOpenFor(Player player) {
		if (this.inventoryView == null) return false; // Not opened yet

		// Check if still open:
		View view = UISessionManager.getInstance().getUISession(player);
		if (view != this) return false;

		// Optional custom verification:
		return Settings.disableInventoryVerification || this.isHandling(player.getOpenInventory());
	}

	/**
	 * Checks if this view is currently open.
	 * 
	 * @return <code>true</code> if this view is currently open, <code>false</code> if this view has
	 *         not been opened yet or has already been closed again
	 */
	public final boolean isOpen() {
		return this.isOpenFor(this.getPlayer());
	}

	/**
	 * {@link #abortDelayed() Aborts} this view if the {@link #getContext() ViewContext} is no
	 * longer {@link ViewContext#isValid() valid}.
	 * <p>
	 * If the view is already no longer {@link #isValid() valid}, this still sends the
	 * {@link ViewContext#getNoLongerValidMessage()} to the player.
	 * 
	 * @return <code>true</code> if the context is no longer valid
	 */
	public final boolean abortIfContextInvalid() {
		var viewContext = this.getContext();
		if (viewContext.isValid()) {
			return false;
		}

		TextUtils.sendMessage(player, viewContext.getNoLongerValidMessage());

		if (this.isValid()) {
			Log.debug(() -> viewContext.getLogPrefix() + "Closing '"
					+ this.getUIType().getIdentifier() + "' for " + player.getName()
					+ ": View context is no longer valid.");
			this.abortDelayed();
		}

		return true;
	}

	// INVENTORY UPDATES

	/**
	 * Updates the displayed inventory contents for the viewing {@link #getPlayer() player}.
	 * 
	 * @see Player#updateInventory()
	 */
	public final void syncInventory() {
		// Note: We omit checking isOpen here: We don't expect any negative side effects (other than
		// redundant inventory updates being sent to the client) if the view has not yet been opened
		// or is no longer open. In most cases, the view is expected to actually be open when this
		// is called and the checks performed by isOpen are redundant.
		this.getPlayer().updateInventory();
	}

	// TODO Ideally, we should realize UI updates by some kind of lightweight data binding mechanism
	// (i.e. by listening to changes in the underlying context object and triggering selective UI
	// updates based on which property changed and which UI element is affected by that). But for
	// the time being, these direct view update methods are simpler and suffice our current needs.

	/**
	 * Request to update the content of the specified slot.
	 * <p>
	 * By default, this falls back to {@link #updateInventory() updating the whole inventory}. It is
	 * up to each UI to implement this in a more fine-grained manner if needed.
	 * <p>
	 * This may need to {@link #syncInventory() synchronize} the inventory for the viewing player.
	 * 
	 * @param slot
	 *            the slot
	 */
	public void updateSlot(int slot) {
		this.updateInventory();
	}

	/**
	 * Request to update the content of the specified slot for this {@link View} and all other
	 * active views that share the same {@link #getContext() context object}.
	 * 
	 * @param slot
	 *            the slot
	 */
	public final void updateSlotInAllViews(int slot) {
		UISessionManager.getInstance()
				.getUISessionsForContext(this.getContext().getObject(), this.getUIType())
				.forEach(view -> view.updateSlot(slot));
	}

	/**
	 * Request to update the contents of the inventory slots of the specified "area".
	 * <p>
	 * It is up to each UI to define and expose "areas" of inventory slots that are logically
	 * related in some way and that external components may want to selectively request updates for.
	 * For any unrecognized areas this should fallback to {@link #updateInventory() updating the
	 * whole view}.
	 * <p>
	 * This may need to {@link #syncInventory() synchronize} the inventory for the viewing player.
	 * 
	 * @param area
	 *            the area identifier, not <code>null</code>
	 */
	public void updateArea(String area) {
		// There are no defined areas by default, so we always trigger an update of the whole
		// inventory:
		this.updateInventory();
	}

	/**
	 * Request to update the contents of the inventory slots of the specified "area" for this
	 * {@link View} and all other active views that share the same {@link #getContext() context
	 * object}.
	 * 
	 * @param area
	 *            the area identifier, not <code>null</code>
	 */
	public final void updateAreaInAllViews(String area) {
		UISessionManager.getInstance()
				.getUISessionsForContext(this.getContext().getObject(), this.getUIType())
				.forEach(view -> view.updateArea(area));
	}

	/**
	 * Request to update the contents of the whole inventory.
	 * <p>
	 * Once the inventory contents have been updated, this may need to also call
	 * {@link #syncInventory()} to update the inventory for the viewing player.
	 */
	public abstract void updateInventory();

	/**
	 * Request to update the contents of the whole inventory for this {@link View} and all other
	 * active views that share the same {@link #getContext() context object}.
	 */
	public final void updateAllViews() {
		UISessionManager.getInstance()
				.getUISessionsForContext(this.getContext().getObject(), this.getUIType())
				.forEach(view -> view.updateInventory());
	}

	// UI STATE

	/**
	 * Captures the current {@link UIState}.
	 * <p>
	 * Not all types of UIs may support this, or may be able to fully {@link #restoreState(UIState)
	 * restore} the state of the current view.
	 * 
	 * @return the {@link UIState}, not <code>null</code>, can be {@link UIState#EMPTY}
	 */
	public UIState captureState() {
		// By default, this does not capture any dynamic UI state but just returns the initially
		// supplied state.
		return this.getInitialUIState();
	}

	/**
	 * Checks if the given {@link UIState} is accepted by this view.
	 * <p>
	 * By default, only {@link UIState#EMPTY} is accepted.
	 * <p>
	 * View implementations may also decide to not accept {@link UIState#EMPTY} but always require a
	 * specific type of UI state to be provided when the UI is requested.
	 * 
	 * @param uiState
	 *            the {@link UIState}, not <code>null</code>
	 * @return <code>true</code> if the {@link UIState} is accepted
	 */
	public boolean isAcceptedState(UIState uiState) {
		if (uiState == UIState.EMPTY) return true;
		return false;
	}

	/**
	 * Validates the given {@link UIState} according to {@link #isAcceptedState(UIState)}.
	 * 
	 * @param uiState
	 *            the {@link UIState}, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given {@link UIState} is not {@link #isAcceptedState(UIState) accepted} by
	 *             this view
	 */
	public final void validateState(UIState uiState) {
		Validate.notNull(uiState, "uiState is null");
		Validate.isTrue(this.isAcceptedState(uiState),
				() -> "uiState of type '" + uiState.getClass().getName()
						+ "' is not accepted by view '" + this.getClass().getName() + "'");
	}

	/**
	 * Tries to restore a previously {@link #captureState() captured} {@link UIState} in a
	 * best-effort manner.
	 * <p>
	 * Any current state is silently replaced with the provided state.
	 * <p>
	 * If this operation is not supported by the view, calls to this method are silently ignored.
	 * 
	 * @param uiState
	 *            the {@link UIState}, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given {@link UIState} is not {@link #isAcceptedState(UIState) accepted} by
	 *             this view
	 */
	public void restoreState(UIState uiState) {
		this.validateState(uiState);
		// Not supported by default.
	}

	// SESSION ENDING

	/**
	 * This is called when this {@link UISession} has ended.
	 * <p>
	 * If the {@link UISession} has ended not due to a received {@link InventoryCloseEvent} but for
	 * another reason (e.g. due to a call to {@link #abort()}), the provided
	 * {@link InventoryCloseEvent} argument is <code>null</code>.
	 * <p>
	 * This is also called when the UI session has ended because {@link #open()} could not
	 * successfully open the view.
	 * 
	 * @param closeEvent
	 *            the corresponding {@link InventoryCloseEvent}, can be <code>null</code>
	 */
	protected void onInventoryClose(@Nullable InventoryCloseEvent closeEvent) {
		// Callback for subclasses.
	}

	// VIEW INTERACTIONS

	/**
	 * Additionally requested types of {@link InventoryEvent}s.
	 * <p>
	 * By default, only {@link InventoryClickEvent}, {@link InventoryDragEvent}, and
	 * {@link InventoryCloseEvent} are forwarded to the {@link View}s. This method can be overridden
	 * to request callbacks for additional types of inventory events via
	 * {@link #onInventoryEventEarly(InventoryEvent)} and
	 * {@link #onInventoryEventLate(InventoryEvent)}.
	 * <p>
	 * It is only effective to request event types for which a corresponding normally registered
	 * event handler would also be able to receive respective events. For example, it has no effect
	 * to request the base type {@link InventoryEvent}, because the various subtypes use their own
	 * {@link HandlerList}s to keep track of registered event handlers. The way the Bukkit event
	 * system works is that any called event is only forwarded to event handlers that have been
	 * registered at the closest {@link HandlerList} in the event's type hierarchy.
	 * <p>
	 * The returned {@link Set} of requested event types is expected to be fixed.
	 * 
	 * @return the additionally requested inventory event types, not <code>null</code>
	 */
	protected Set<? extends Class<? extends InventoryEvent>> getAdditionalInventoryEvents() {
		return Collections.emptySet();
	}

	/**
	 * Called by {@link UIListener} during {@link EventPriority#LOW} for any of the handled
	 * inventory events while this view is open.
	 * 
	 * @param event
	 *            the inventory event, not <code>null</code>
	 * @return <code>false</code> if the event is rejected and not handled by this view for some
	 *         reason, e.g. because the inventory event is unexpected in the current state, the view
	 *         is {@link #isUIActive() deactivated}, or the {@link #getContext() context} is no
	 *         longer be {@link ViewContext #isValid() valid}. The view will then also not receive
	 *         the corresponding {@link #informOnInventoryEventLate(InventoryEvent)}.
	 */
	boolean informOnInventoryEventEarly(InventoryEvent event) {
		if (!this.isInventoryEventHandled(event)) {
			return false;
		}

		debugInventoryEvent(event);

		this.onInventoryEventEarly(event);

		// Invoke dedicated event handling methods:
		if (event instanceof InventoryClickEvent) {
			this.informOnInventoryClickEarly((InventoryClickEvent) event);
		} else if (event instanceof InventoryDragEvent) {
			this.informOnInventoryDragEarly((InventoryDragEvent) event);
		}

		return true;
	}

	// Returns true if the view is able to process the event.
	private boolean isInventoryEventHandled(InventoryEvent event) {
		var player = this.getPlayer();
		var viewContext = this.getContext();
		var inventoryView = event.getView();
		assert player.equals(inventoryView.getPlayer());

		// Check if the UI has been deactivated:
		if (!this.isUIActive()) {
			Log.debug(() -> viewContext.getLogPrefix() + "Ignoring " + event.getEventName() + " of "
					+ player.getName() + ": UI '" + this.getUIType().getIdentifier()
					+ "' has been deactivated and is probably about to get closed.");
			EventUtils.setCancelled(event, true);
			return false;
		}

		// Check if the view context is still valid:
		if (this.abortIfContextInvalid()) {
			EventUtils.setCancelled(event, true);
			return false;
		}

		// Check if the inventory view matches the expected inventory view:
		// Note: No need to check view.isOpen() here: We retrieved the view from the
		// UISessionManager, so the view can be assumed to be open.
		// Unlike view.isOpen(), we verify the InventoryView retrieved from the event here, which is
		// usually assumed to be the same, but this verification is intended to catch unexpected
		// cases.
		if (!Settings.disableInventoryVerification && !this.isHandling(inventoryView)) {
			// Something went wrong: The player seems to have an unexpected inventory open. Let's
			// close it to prevent any potential damage:
			Log.debug(() -> viewContext.getLogPrefix() + "Closing inventory of type "
					+ inventoryView.getType() + " with title '"
					+ inventoryView.getTitle() + "' for " + player.getName()
					+ ", because a different open inventory was expected for '"
					+ this.getUIType().getIdentifier() + "'.");
			EventUtils.setCancelled(event, true);
			this.abortDelayed();
			return false;
		}

		return true;
	}

	private static void debugInventoryEvent(InventoryEvent event) {
		if (event instanceof InventoryClickEvent) {
			debugInventoryClickEvent((InventoryClickEvent) event);
		} else if (event instanceof InventoryDragEvent) {
			debugInventoryDragEvent((InventoryDragEvent) event);
		} else {
			debugOtherInventoryEvent(event);
		}
	}

	private static void debugInventoryClickEvent(InventoryClickEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory click: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", raw-slot-id=" + event.getRawSlot() + ", slot-id=" + event.getSlot()
				+ ", slot-type=" + event.getSlotType() + ", shift=" + event.isShiftClick()
				+ ", hotbar key=" + event.getHotbarButton() + ", left-or-right="
				+ (event.isLeftClick() ? "left" : (event.isRightClick() ? "right" : "unknown"))
				+ ", click-type=" + event.getClick() + ", action=" + event.getAction()
				+ ", time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
	}

	private static void debugInventoryDragEvent(InventoryDragEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory dragging: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", drag-type=" + event.getType());
	}

	private static void debugOtherInventoryEvent(InventoryEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory event (" + event.getClass().getSimpleName()
				+ "): player=" + player.getName() + ", view-type=" + view.getType()
				+ ", view-title=" + view.getTitle());
	}

	/**
	 * Called early ({@link EventPriority#LOW}) for all {@link InventoryEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * <p>
	 * This method is only guaranteed to be called for inventory events that are either handled by
	 * default, or that have been explicitly requested by this view via
	 * {@link #getAdditionalInventoryEvents()}. However, this method may also be called for
	 * inventory events that have not been explicitly requested by this view. The view should
	 * therefore take care to ignore any unexpected types of inventory events.
	 * 
	 * @param event
	 *            the inventory event, not <code>null</code>
	 * @see #onInventoryEventLate(InventoryEvent)
	 */
	protected void onInventoryEventEarly(InventoryEvent event) {
		// Callback for subclasses.
	}

	/**
	 * Called by {@link UIListener} during {@link EventPriority#HIGH} for any of the handled
	 * inventory events while this view is open.
	 * 
	 * @param event
	 *            the inventory event, not <code>null</code>
	 */
	void informOnInventoryEventLate(InventoryEvent event) {
		// Check if the view is still valid. This is usually expected to be the case, but this
		// assumption can be violated by plugins that (incorrectly) close the inventory view during
		// the handling of this event.
		if (!this.isValid()) {
			Log.debug(() -> this.getContext().getLogPrefix() + "Ignoring late inventory event ("
					+ event.getEventName() + "): UI '" + this.getUIType().getIdentifier()
					+ "' of player " + this.getPlayer().getName() + " is no longer valid. Some "
					+ "plugin might have unexpectedly closed the inventory while the event was "
					+ "still being processed!");
			return;
		}

		this.onInventoryEventLate(event);

		// Invoke dedicated event handling methods:
		if (event instanceof InventoryClickEvent) {
			this.informOnInventoryClickLate((InventoryClickEvent) event);
		} else if (event instanceof InventoryDragEvent) {
			this.informOnInventoryDragLate((InventoryDragEvent) event);
		}
	}

	/**
	 * Called late ({@link EventPriority#HIGH}) for all {@link InventoryEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * <p>
	 * This method is only guaranteed to be called for inventory events that are either handled by
	 * default, or that have been explicitly requested by this view via
	 * {@link #getAdditionalInventoryEvents()}. However, his method may also be called for inventory
	 * events that have not been explicitly requested by this view. The view should therefore take
	 * care to ignore any unexpected types of inventory events.
	 * 
	 * @param event
	 *            the inventory event, not <code>null</code>
	 * @see #onInventoryEventEarly(InventoryEvent)
	 */
	protected void onInventoryEventLate(InventoryEvent event) {
		// Callback for subclasses.
	}

	/**
	 * Returns whether the currently handled inventory click is, according to our heuristic, an
	 * automatically triggered shift left-click due to a shift double left-click by the player.
	 * <p>
	 * Shift double left-clicks are supposed to move all matching items to the other inventory.
	 * Minecraft implements this by triggering a shift left-click with
	 * {@link InventoryAction#MOVE_TO_OTHER_INVENTORY} for all inventory slots that contain a
	 * matching item. Plugins cannot differentiate between these automatically triggered clicks and
	 * normal shift left-clicks by the player.
	 * <p>
	 * We use a heuristic to detect (and then possibly ignore) these automatically triggered clicks:
	 * We assume that any shift left-clicks that occur within
	 * {@link #AUTOMATIC_SHIFT_LEFT_CLICK_NANOS} on a slot different to the previously clicked slot
	 * are automatically triggered.
	 * <p>
	 * Limitations (TODO): We cannot use a much lower time span (e.g. limiting it to 1 or 2 ticks),
	 * because the automatically triggered clicks may arrive quite some time later (up to 150 ms
	 * later on a local server and possibly more with network delay involved). Also, this does not
	 * work for automatic clicks triggered for the same slot. Since the automatically triggered
	 * clicks may arrive quite some time later, we cannot differentiate them from manual fast
	 * clicking.
	 * 
	 * @return <code>true</code> if we detected an automatically triggered shift left-click
	 */
	protected final boolean isAutomaticShiftLeftClick() {
		return isAutomaticShiftLeftClick;
	}

	private void informOnInventoryClickEarly(InventoryClickEvent event) {
		// Heuristic detection of automatically triggered shift left-clicks:
		isAutomaticShiftLeftClick = false; // Reset
		final long nowNanos = System.nanoTime();
		if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (event.getRawSlot() != lastManualClickedSlotId
					&& (nowNanos - lastManualClickNanos) < AUTOMATIC_SHIFT_LEFT_CLICK_NANOS) {
				isAutomaticShiftLeftClick = true;
				Log.debug("  Detected automatically triggered shift left-click! (on different slot)");
			}
		}
		// Note: We reset these for all types of clicks, because when quickly switching between
		// shift and non-shift clicking we sometimes receive non-shift clicks that are followed by
		// the automatic shift-clicks:
		if (!isAutomaticShiftLeftClick) {
			lastManualClickNanos = nowNanos;
			lastManualClickedSlotId = event.getRawSlot();
		}

		this.onInventoryClickEarly(event);
	}

	/**
	 * Called early ({@link EventPriority#LOW}) for all {@link InventoryClickEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * <p>
	 * Any view potentially canceling the event should consider doing so early in order for other
	 * plugins to ignore the event.
	 * 
	 * @param event
	 *            the inventory click event, not <code>null</code>
	 * @see #onInventoryClickLate(InventoryClickEvent)
	 */
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		// Callback for subclasses.
	}

	private void informOnInventoryClickLate(InventoryClickEvent event) {
		this.onInventoryClickLate(event);
	}

	/**
	 * Called late ({@link EventPriority#HIGH}) for all {@link InventoryClickEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * 
	 * @param event
	 *            the inventory click event, not <code>null</code>
	 * @see #onInventoryClickEarly(InventoryClickEvent)
	 */
	protected void onInventoryClickLate(InventoryClickEvent event) {
		// Callback for subclasses.
	}

	private void informOnInventoryDragEarly(InventoryDragEvent event) {
		this.onInventoryDragEarly(event);
	}

	/**
	 * Called early ({@link EventPriority#LOW}) for all {@link InventoryDragEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * <p>
	 * Any view potentially canceling the event should consider doing so early in order for other
	 * plugins to ignore the event.
	 * 
	 * @param event
	 *            the inventory drag event, not <code>null</code>
	 * @see #onInventoryDragLate(InventoryDragEvent)
	 */
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		// Callback for subclasses.
	}

	private void informOnInventoryDragLate(InventoryDragEvent event) {
		this.onInventoryDragLate(event);
	}

	/**
	 * Called late ({@link EventPriority#HIGH}) for all {@link InventoryDragEvent}s handled by this
	 * view. The view can be assumed to be {@link #isOpen() open}.
	 * 
	 * @param event
	 *            the inventory drag event, not <code>null</code>
	 * @see #onInventoryDragEarly(InventoryDragEvent)
	 */
	protected void onInventoryDragLate(InventoryDragEvent event) {
		// Callback for subclasses.
	}
}
