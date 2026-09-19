package com.nisovin.shopkeepers.ui.editor;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.lib.UISessionManager;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.ui.villager.editor.VillagerEditorView;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.inventory.InventoryViewUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for editor UIs that support editing multiple pages of trades and offer additional
 * editor buttons.
 * <p>
 * For example used by {@link ShopkeeperEditorView} and {@link VillagerEditorView}.
 */
public abstract class EditorView extends View {

	protected static final String AREA_BUTTONS = "buttons";

	private @Nullable EditorLayout layout; // Lazily setup

	private @Nullable List<TradingRecipeDraft> recipes;
	private @Nullable Inventory inventory;

	private int currentPage = 1; // Starts at 1.

	public EditorView(AbstractEditorViewProvider provider, Player player, UIState uiState) {
		super(provider, player, uiState);
	}

	protected AbstractEditorViewProvider getEditorViewProvider() {
		return (AbstractEditorViewProvider) this.getProvider();
	}

	/**
	 * Gets the {@link EditorLayout}, creating it if not yet set up.
	 * 
	 * @return the editor layout, not <code>null</code>
	 */
	protected final EditorLayout getLayout() {
		var layout = this.layout;
		if (layout == null) {
			layout = this.createLayout();
			this.layout = layout;
			layout.setupButtons();
		}

		return layout;
	}

	/**
	 * Creates the {@link EditorLayout} for this view.
	 * <p>
	 * The layout is created lazily once it is requested for the first time.
	 * 
	 * @return the editor layout, not <code>null</code>
	 */
	protected abstract EditorLayout createLayout();

	protected final TradingRecipesAdapter getTradingRecipesAdapter() {
		return this.getEditorViewProvider().tradingRecipesAdapter;
	}

	// STATE

	@Override
	public final Inventory getInventory() {
		return Validate.State.notNull(inventory, "Inventory not yet set up!");
	}

	// This list of recipes is edited directly (not a copy or readonly view currently).
	/**
	 * Gets the list of recipes being edited.
	 * 
	 * @return the recipes, not <code>null</code>
	 */
	public final List<TradingRecipeDraft> getRecipes() {
		return Validate.State.notNull(recipes, "Recipes not yet set up!");
	}

	// UI STATE

	@Override
	public UIState captureState() {
		return new EditorUIState(this.getCurrentPage());
	}

	@Override
	public boolean isAcceptedState(UIState uiState) {
		return uiState == UIState.EMPTY || uiState instanceof EditorUIState;
	}

	@Override
	public void restoreState(UIState uiState) {
		super.restoreState(uiState);

		EditorUIState editorState = (EditorUIState) uiState;
		this.switchPage(editorState.getCurrentPage(), true);
	}

	// SETUP

	@Override
	protected @Nullable InventoryView openInventoryView() {
		Player player = this.getPlayer();

		// Lazily setup when first requested:
		var layout = this.getLayout();

		List<TradingRecipeDraft> recipes = this.getTradingRecipesAdapter().getTradingRecipes();

		// Create inventory:
		Inventory inventory = Bukkit.createInventory(
				null,
				layout.getInventorySize(),
				this.getTitle()
		);

		this.inventory = inventory;
		// The recipes are not copied, but edited directly:
		this.recipes = recipes;

		// Determine the initial page:
		int page = 1;
		if (this.getInitialUIState() instanceof EditorUIState editorState) {
			page = this.getValidPage(editorState.getCurrentPage());
		}

		// Set up and open the initial page:
		this.setPage(page);
		this.setupCurrentPage();

		return player.openInventory(inventory);
	}

	protected abstract String getTitle();

	protected void setupCurrentPage() {
		// Setup inventory:
		this.setupTradeColumns();
		this.setupTradesPageBar();
		this.setupButtons();
	}

	protected void setupTradeColumns() {
		// Insert trades (this replaces all previous items inside the trades area):
		Inventory inventory = this.getInventory();
		int page = this.getCurrentPage();
		assert page >= 1;
		List<TradingRecipeDraft> recipes = this.getRecipes();
		int recipeStartIndex = (page - 1) * EditorLayout.TRADES_COLUMNS;
		for (int column = 0; column < EditorLayout.TRADES_COLUMNS; column++) {
			int recipeIndex = recipeStartIndex + column;
			if (recipeIndex < recipes.size()) {
				// Insert trading recipe:
				TradingRecipeDraft recipe = recipes.get(recipeIndex);
				this.setTradeColumn(inventory, column, recipe);
			} else {
				// Insert empty slot placeholders:
				this.setTradeColumn(inventory, column, TradingRecipeDraft.EMPTY);
			}
		}
	}

	protected void setupTradesPageBar() {
		Inventory inventory = this.getInventory();
		// Clear page bar area:
		for (int i = EditorLayout.TRADES_PAGE_BAR_START; i <= EditorLayout.TRADES_PAGE_BAR_END; ++i) {
			inventory.setItem(i, null);
		}

		// Insert buttons:
		@Nullable Button[] buttons = this.getLayout().getTradesPageBarButtons();
		for (int i = 0; i < buttons.length; ++i) {
			Button button = buttons[i];
			if (button == null) continue;
			ItemStack icon = button.getIcon();
			if (icon == null) continue;
			inventory.setItem(button.getSlot(), icon);
		}
	}

	// Also used to refresh all button icons in an already open inventory.
	protected void setupButtons() {
		Inventory inventory = this.getInventory();
		final int inventorySize = inventory.getSize();
		@Nullable Button[] buttons = this.getLayout().getBakedButtons();
		for (int buttonIndex = 0; buttonIndex < buttons.length; ++buttonIndex) {
			int slot = EditorLayout.BUTTONS_START + buttonIndex;
			if (slot >= inventorySize) {
				// This can be reached if called on a previously set up inventory.
				break;
			}

			ItemStack icon = null;
			Button button = buttons[buttonIndex];
			if (button != null) {
				icon = button.getIcon();
			}

			// Null will clear the slot (which is required if this is called to refresh the buttons
			// in an already set up inventory):
			inventory.setItem(slot, icon);
		}
	}

	// TRADES AREA

	/**
	 * Gets the {@link TradingRecipeDraft} that is used for trade columns that don't contain any
	 * trade yet.
	 * <p>
	 * This is expected to always return the same placeholder items.
	 * <p>
	 * The placeholder items are expected to not match any items that players are able to set up
	 * trades with.
	 * 
	 * @return the {@link TradingRecipeDraft} to use for empty trade columns, not <code>null</code>
	 */
	protected TradingRecipeDraft getEmptyTrade() {
		return TradingRecipeDraft.EMPTY;
	}

	/**
	 * Gets the items that are used for empty slots of partially set up trades.
	 * <p>
	 * This is expected to always return the same placeholder items.
	 * <p>
	 * The placeholder items are expected to not match any items that players are able to set up
	 * trades with.
	 * 
	 * @return a {@link TradingRecipeDraft} with the items to use for empty slots of partially set
	 *         up trades, not <code>null</code>
	 */
	protected TradingRecipeDraft getEmptyTradeSlotItems() {
		return TradingRecipeDraft.EMPTY;
	}

	private boolean isEmptyResultItem(@ReadOnly @Nullable ItemStack slotItem) {
		ItemStack item = ItemUtils.getNullIfEmpty(slotItem);
		if (item == null) return true;
		if (ItemUtils.equals(this.getEmptyTrade().getResultItem(), item)) return true;
		if (ItemUtils.equals(this.getEmptyTradeSlotItems().getResultItem(), item)) return true;
		return false;
	}

	private boolean isEmptyItem1(@ReadOnly @Nullable ItemStack slotItem) {
		ItemStack item = ItemUtils.getNullIfEmpty(slotItem);
		if (item == null) return true;
		if (ItemUtils.equals(this.getEmptyTrade().getItem1(), item)) return true;
		if (ItemUtils.equals(this.getEmptyTradeSlotItems().getItem1(), item)) return true;
		return false;
	}

	private boolean isEmptyItem2(@ReadOnly @Nullable ItemStack slotItem) {
		ItemStack item = ItemUtils.getNullIfEmpty(slotItem);
		if (item == null) return true;
		if (ItemUtils.equals(this.getEmptyTrade().getItem2(), item)) return true;
		if (ItemUtils.equals(this.getEmptyTradeSlotItems().getItem2(), item)) return true;
		return false;
	}

	// Returns null if the slot is empty or if the item matches an empty slot placeholder item.
	protected final @Nullable ItemStack getTradeResultItem(Inventory inventory, int column) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		ItemStack item = inventory.getItem(this.getLayout().getResultItemSlot(column));
		if (this.isEmptyResultItem(item)) return null;
		return item;
	}

	// Returns null if the slot is empty or if the item matches an empty slot placeholder item.
	protected final @Nullable ItemStack getTradeItem1(Inventory inventory, int column) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		ItemStack item = inventory.getItem(this.getLayout().getItem1Slot(column));
		if (this.isEmptyItem1(item)) return null;
		return item;
	}

	// Returns null if the slot is empty or if the item matches an empty slot placeholder item.
	protected final @Nullable ItemStack getTradeItem2(Inventory inventory, int column) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		ItemStack item = inventory.getItem(this.getLayout().getItem2Slot(column));
		if (this.isEmptyItem2(item)) return null;
		return item;
	}

	// Use TradingRecipeDraft#EMPTY to insert an empty trade column.
	protected void setTradeColumn(Inventory inventory, int column, TradingRecipeDraft recipe) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		assert recipe != null;

		TradingRecipeDraft emptySlotItems;
		if (recipe.isEmpty()) {
			emptySlotItems = this.getEmptyTrade();
		} else {
			emptySlotItems = this.getEmptyTradeSlotItems();
		}

		// Insert placeholders for empty slots:
		UnmodifiableItemStack resultItem = ItemUtils.getFallbackIfNull(
				recipe.getResultItem(),
				emptySlotItems.getResultItem()
		);
		UnmodifiableItemStack item1 = ItemUtils.getFallbackIfNull(
				recipe.getItem1(),
				emptySlotItems.getItem1()
		);
		UnmodifiableItemStack item2 = ItemUtils.getFallbackIfNull(
				recipe.getItem2(),
				emptySlotItems.getItem2()
		);

		// The inventory implementations create NMS copies of the items, so we do not need to copy
		// them ourselves here:
		var layout = this.getLayout();
		inventory.setItem(layout.getResultItemSlot(column), ItemUtils.asItemStackOrNull(resultItem));
		inventory.setItem(layout.getItem1Slot(column), ItemUtils.asItemStackOrNull(item1));
		inventory.setItem(layout.getItem2Slot(column), ItemUtils.asItemStackOrNull(item2));
	}

	// TODO Avoid creating new TradingRecipeDraft objects here and instead update the drafts of the
	// session?
	// This replaces items matching the empty slot placeholders with null items in the returned
	// TradingRecipeDraft.
	protected TradingRecipeDraft getTradingRecipe(Inventory inventory, int column) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		ItemStack resultItem = this.getTradeResultItem(inventory, column);
		ItemStack item1 = this.getTradeItem1(inventory, column);
		ItemStack item2 = this.getTradeItem2(inventory, column);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	protected final void updateTradeColumn(Inventory inventory, int column) {
		TradingRecipeDraft recipe = this.getTradingRecipe(inventory, column);
		this.setTradeColumn(inventory, column, recipe);
	}

	protected final boolean isEmptyTrade(Inventory inventory, int column) {
		assert inventory != null;
		assert this.getLayout().isTradeColumn(column);
		ItemStack resultItem = this.getTradeResultItem(inventory, column);
		if (resultItem != null) return false;
		ItemStack item1 = this.getTradeItem1(inventory, column);
		if (item1 != null) return false;
		ItemStack item2 = this.getTradeItem2(inventory, column);
		if (item2 != null) return false;
		return true;
	}

	// PAGINATION

	/**
	 * Gets the current page, starting at {@code 1} for the first page.
	 * 
	 * @return the current page
	 */
	public final int getCurrentPage() {
		return currentPage;
	}

	void setPage(int newPage) {
		this.currentPage = newPage;
	}

	protected final int getValidPage(int targetPage) {
		var maxPage = this.getLayout().getMaxTradesPages();
		return Math.max(1, Math.min(maxPage, targetPage));
	}

	// Returns true if the page has changed.
	protected boolean switchPage(int targetPage, boolean saveCurrentPage) {
		int newPage = this.getValidPage(targetPage);
		int currentPage = this.getCurrentPage();
		if (newPage == currentPage) return false; // Page has not changed

		// Save the current page:
		if (saveCurrentPage) {
			this.saveEditorPage();
		}

		// Update page:
		this.setPage(newPage);
		this.setupCurrentPage();
		this.syncInventory();
		return true;
	}

	// INVENTORY UPDATES

	@Override
	public void updateInventory() {
		// Note: The trades area is not updated here, because it can contain unsaved changes of the
		// editing player, which would get discarded.
		this.setupTradesPageBar();
		this.updateButtons();
		this.syncInventory();
	}

	@Override
	public void updateArea(String area) {
		if (AREA_BUTTONS.equals(area)) {
			this.updateButtons();
			this.syncInventory();
			return;
		}

		super.updateArea(area);
	}

	// Note: This cannot deal with new button rows being required due to newly added buttons (which
	// would require creating and freshly open a new inventory, resulting in flicker).
	protected void updateButtons() {
		this.setupButtons();
	}

	void updateButtonsInAllViews() {
		this.updateAreaInAllViews(AREA_BUTTONS);
	}

	// Updates the icon of the button with the given identity, if this view has such a button.
	void updateButton(Object buttonIdentity) {
		Button button = this.getLayout().findButton(buttonIdentity);
		if (button == null) return;

		int slot = button.getSlot();
		if (slot == Button.NO_SLOT) return;

		ItemStack icon = button.getIcon();
		this.getInventory().setItem(slot, icon);
		this.syncInventory();
	}

	// Each view sets up its own layout, so the same button can occupy different slots across views:
	// The buttons are therefore matched by their identity instead of by slot.
	void updateButtonInAllViews(Object buttonIdentity) {
		UISessionManager.getInstance()
				.getUISessionsForContext(this.getContext().getObject(), this.getUIType())
				.forEach(view -> {
					if (view instanceof EditorView editorView) {
						editorView.updateButton(buttonIdentity);
					}
				});
	}

	// VIEW INTERACTIONS

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		// Dragging is allowed by default only inside the player inventory and the trades area:
		if (event.isCancelled()) return; // Already cancelled

		InventoryView view = event.getView();
		Set<Integer> slots = Unsafe.castNonNull(event.getRawSlots());
		for (Integer rawSlotInteger : slots) {
			int rawSlot = rawSlotInteger;
			if (this.getLayout().isTradesArea(rawSlot)) continue;
			if (InventoryViewUtils.isPlayerInventory(view, rawSlot)) continue;

			event.setCancelled(true);
			break;
		}
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		var layout = this.getLayout();
		int rawSlot = event.getRawSlot();
		if (layout.isTradesArea(rawSlot)) {
			// Trades area:
			this.handleTradesClick(event);
		} else if (layout.isTradesPageBar(rawSlot)) {
			// Trades page bar:
			this.handleTradesPageBarClick(event);
		} else if (layout.isButtonArea(rawSlot)) {
			// Editor buttons:
			this.handleButtonClick(event);
		} else if (InventoryViewUtils.isPlayerInventory(event.getView(), rawSlot)) {
			// Player inventory:
			this.handlePlayerInventoryClick(event);
		}
	}

	protected void handleTradesClick(InventoryClickEvent event) {
		assert this.getLayout().isTradesArea(event.getRawSlot());
	}

	protected void handleTradesPageBarClick(InventoryClickEvent event) {
		assert this.getLayout().isTradesPageBar(event.getRawSlot());
		event.setCancelled(true);
		int rawSlot = event.getRawSlot();
		Button button = this.getLayout().getTradesPageBarButton(rawSlot);
		if (button != null) {
			button.onClick(event);
		}
	}

	protected void handleButtonClick(InventoryClickEvent event) {
		assert this.getLayout().isButtonArea(event.getRawSlot());
		event.setCancelled(true);
		int rawSlot = event.getRawSlot();
		Button button = this.getLayout().getButton(rawSlot);
		if (button != null) {
			button.onClick(event);
		}
	}

	protected void handlePlayerInventoryClick(InventoryClickEvent event) {
		assert InventoryViewUtils.isPlayerInventory(event.getView(), event.getRawSlot());
	}

	protected int getNewAmountAfterEditorClick(
			InventoryClickEvent event,
			int currentAmount,
			int minAmount,
			int maxAmount
	) {
		// Validate bounds:
		if (minAmount > maxAmount) return currentAmount; // No valid value possible
		if (minAmount == maxAmount) return minAmount; // Only one valid value possible

		int newAmount = currentAmount;
		ClickType clickType = event.getClick();
		switch (clickType) {
		case LEFT:
			newAmount += 1;
			break;
		case SHIFT_LEFT:
			newAmount += 10;
			break;
		case RIGHT:
			newAmount -= 1;
			break;
		case SHIFT_RIGHT:
			newAmount -= 10;
			break;
		case MIDDLE:
			newAmount = minAmount;
			break;
		case NUMBER_KEY:
			assert event.getHotbarButton() >= 0;
			newAmount = event.getHotbarButton() + 1;
			break;
		default:
			break;
		}
		// Bounds:
		if (newAmount < minAmount) newAmount = minAmount;
		if (newAmount > maxAmount) newAmount = maxAmount;
		return newAmount;
	}

	@Override
	protected void onInventoryClose(@Nullable InventoryCloseEvent closeEvent) {
		if (closeEvent != null) {
			// Only save if caused by an inventory close event (i.e. if the session has not been
			// 'aborted'):
			this.saveEditor();
		}
	}

	// SAVING

	/**
	 * Saves the current page of the editor view to this session.
	 */
	protected void saveEditorPage() {
		Inventory inventory = this.getInventory();
		int page = this.getCurrentPage();
		assert page >= 1;
		List<TradingRecipeDraft> recipes = this.getRecipes();

		int recipesPerPage = EditorLayout.COLUMNS_PER_ROW;
		int startIndex = (page - 1) * recipesPerPage;
		int endIndex = startIndex + EditorLayout.TRADES_COLUMNS - 1;
		// Add empty recipes to support the recipes of the current page:
		for (int i = recipes.size(); i <= endIndex; ++i) {
			recipes.add(TradingRecipeDraft.EMPTY);
		}

		// Replace recipes:
		for (int column = 0; column < EditorLayout.TRADES_COLUMNS; column++) {
			TradingRecipeDraft recipeDraft = this.getTradingRecipe(inventory, column);
			int recipeIndex = startIndex + column;
			recipes.set(recipeIndex, recipeDraft);
		}
	}

	/**
	 * Saves the current state of the editor view.
	 */
	protected void saveEditor() {
		// Save the current editor page from the UI to this session:
		this.saveEditorPage();

		// Save the recipes:
		this.saveRecipes();
	}

	/**
	 * Saves (i.e. applies) the trading recipes of this editor session.
	 */
	protected abstract void saveRecipes();
}
