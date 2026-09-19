package com.nisovin.shopkeepers.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * The layout of an {@link EditorView}, including its buttons.
 * <p>
 * Each {@link EditorView} sets up its own layout. The buttons can therefore take the viewing player
 * into account.
 */
public abstract class EditorLayout {

	private static final SoundEffect PAGE_TURN_SOUND = new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN);

	public static final int COLUMNS_PER_ROW = 9;
	// 9 columns, column = [0,8]
	public static final int TRADES_COLUMNS = COLUMNS_PER_ROW;

	public static final int TRADES_ROW_1_START = 0;
	public static final int TRADES_ROW_1_END = TRADES_ROW_1_START + TRADES_COLUMNS - 1;
	public static final int TRADES_ROW_2_START = TRADES_ROW_1_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	public static final int TRADES_ROW_2_END = TRADES_ROW_2_START + TRADES_COLUMNS - 1;
	public static final int TRADES_ROW_3_START = TRADES_ROW_2_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	public static final int TRADES_ROW_3_END = TRADES_ROW_3_START + TRADES_COLUMNS - 1;

	public static final int TRADES_PAGE_BAR_START = TRADES_ROW_3_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	public static final int TRADES_PAGE_BAR_END = TRADES_PAGE_BAR_START + TRADES_COLUMNS - 1;
	public static final int TRADES_PAGE_ICON = TRADES_PAGE_BAR_START + (TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START) / 2;
	public static final int TRADES_SETUP_ICON = TRADES_PAGE_ICON - 1;
	public static final int SHOP_INFORMATION_ICON = TRADES_PAGE_ICON + 1;

	public static final int BUTTONS_START = TRADES_PAGE_BAR_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	// TODO If there are more buttons than we can fit into two rows, move the excess buttons into a
	// separate (paged) inventory view and add an editor button that opens it.
	public static final int BUTTON_MAX_ROWS = 2;

	// slot = column + offset:
	public static final int RESULT_ITEM_OFFSET = TRADES_ROW_1_START;
	public static final int ITEM_1_OFFSET = TRADES_ROW_3_START;
	public static final int ITEM_2_OFFSET = TRADES_ROW_2_START;

	private final EditorView editorView;

	private final @Nullable Button[] tradesPageBarButtons = new @Nullable Button[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START + 1];
	private final List<Button> buttons = new ArrayList<>();
	private int buttonRows = 1;
	private final @Nullable Button[] bakedButtons = new @Nullable Button[BUTTON_MAX_ROWS * COLUMNS_PER_ROW];
	private boolean dirtyButtons = false;

	public EditorLayout(EditorView editorView) {
		Validate.notNull(editorView, "editorView is null");
		this.editorView = editorView;
	}

	/**
	 * Gets the {@link EditorView} this layout has been created for.
	 * 
	 * @return the editor view, not <code>null</code>
	 */
	public EditorView getEditorView() {
		return editorView;
	}

	// INVENTORY LAYOUT

	public int getInventorySize() {
		return COLUMNS_PER_ROW * (4 + this.getButtonRows());
	}

	public boolean isResultRow(int rawSlot) {
		return rawSlot >= TRADES_ROW_1_START && rawSlot <= TRADES_ROW_1_END;
	}

	public boolean isItem1Row(int rawSlot) {
		return rawSlot >= TRADES_ROW_3_START && rawSlot <= TRADES_ROW_3_END;
	}

	public boolean isItem2Row(int rawSlot) {
		return rawSlot >= TRADES_ROW_2_START && rawSlot <= TRADES_ROW_2_END;
	}

	public boolean isTradesArea(int rawSlot) {
		return this.isResultRow(rawSlot) || this.isItem1Row(rawSlot) || this.isItem2Row(rawSlot);
	}

	// First column has index 0.
	// Only guaranteed to return valid results if the slot is within the trades area.
	public int getTradeColumn(int rawSlot) {
		return rawSlot % 9;
	}

	public boolean isTradeColumn(int column) {
		return column >= 0 && column < TRADES_COLUMNS;
	}

	public int getResultItemSlot(int column) {
		return column + RESULT_ITEM_OFFSET;
	}

	public int getItem1Slot(int column) {
		return column + ITEM_1_OFFSET;
	}

	public int getItem2Slot(int column) {
		return column + ITEM_2_OFFSET;
	}

	public boolean isTradesPageBar(int rawSlot) {
		return rawSlot >= TRADES_PAGE_BAR_START && rawSlot <= TRADES_PAGE_BAR_END;
	}

	public boolean isButtonArea(int rawSlot) {
		return rawSlot >= BUTTONS_START && rawSlot <= this.getButtonsEnd();
	}

	// Depends on the number of buttons rows currently used:
	public int getButtonsEnd() {
		return BUTTONS_START + (this.getButtonRows() * COLUMNS_PER_ROW) - 1;
	}

	// TRADES AREA

	// assert: [1, 10].
	public int getMaxTradesPages() {
		return Settings.maxTradesPages;
	}

	// BUTTONS

	/**
	 * Sets up the buttons of this layout.
	 * <p>
	 * This is called once right after the layout has been created.
	 */
	public void setupButtons() {
		this.setupTradesPageBarButtons();
	}

	// PAGE BAR

	@Nullable
	Button[] getTradesPageBarButtons() {
		return tradesPageBarButtons;
	}

	@Nullable
	Button getTradesPageBarButton(int rawSlot) {
		if (!this.isTradesPageBar(rawSlot)) return null;
		return tradesPageBarButtons[rawSlot - TRADES_PAGE_BAR_START];
	}

	protected void setupTradesPageBarButtons() {
		this.setTradesPageBarButton(TRADES_PAGE_BAR_START, this.createPrevPageButton());
		this.setTradesPageBarButton(TRADES_SETUP_ICON, this.createTradeSetupButton());
		this.setTradesPageBarButton(TRADES_PAGE_ICON, this.createCurrentPageButton());
		this.setTradesPageBarButton(SHOP_INFORMATION_ICON, this.createShopInformationButton());
		this.setTradesPageBarButton(TRADES_PAGE_BAR_END, this.createNextPageButton());
	}

	private void setTradesPageBarButton(int slot, Button button) {
		assert this.isTradesPageBar(slot);
		// Validates that the button is applicable and not yet used in another layout:
		button.setEditorLayout(this);
		button.setSlot(slot);
		tradesPageBarButtons[slot - TRADES_PAGE_BAR_START] = button;
	}

	protected Button createPrevPageButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				int page = this.getEditorView().getCurrentPage();
				if (page <= 1) return null;

				return createPrevPageIcon(page);
			}

			@Override
			protected void playButtonClickSound(boolean actionSuccess) {
				if (actionSuccess) {
					PAGE_TURN_SOUND.play(this.getEditorView().getPlayer());
				}
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				// Ignore double clicks:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return false;

				// Switch to previous page:
				EditorView editorView = this.getEditorView();
				int currentPage = editorView.getCurrentPage();
				return editorView.switchPage(currentPage - 1, true);
			}

			@Override
			protected boolean isUpdateIconOnActionSuccess() {
				// Switching the page already updates the whole view.
				return false;
			}
		};
	}

	protected Button createNextPageButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon() {
				int page = this.getEditorView().getCurrentPage();
				if (page >= getMaxTradesPages()) return null;

				return createNextPageIcon(page);
			}

			@Override
			protected void playButtonClickSound(boolean actionSuccess) {
				if (actionSuccess) {
					PAGE_TURN_SOUND.play(this.getEditorView().getPlayer());
				}
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent) {
				// Ignore double clicks:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return false;

				// Switch to next page:
				EditorView editorView = this.getEditorView();
				int currentPage = editorView.getCurrentPage();
				return editorView.switchPage(currentPage + 1, true);
			}

			@Override
			protected boolean isUpdateIconOnActionSuccess() {
				// Switching the page already updates the whole view.
				return false;
			}
		};
	}

	protected Button createCurrentPageButton() {
		return new Button() {
			@Override
			public @Nullable ItemStack getIcon() {
				int page = this.getEditorView().getCurrentPage();
				return createCurrentPageIcon(page);
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent) {
				// Current page button: Does nothing.
			}
		};
	}

	protected Button createShopInformationButton() {
		return new Button() {
			@Override
			public @Nullable ItemStack getIcon() {
				return createShopInformationIcon();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent) {
				// Shop information button: Does nothing.
			}
		};
	}

	protected Button createTradeSetupButton() {
		return new Button() {
			@Override
			public @Nullable ItemStack getIcon() {
				return createTradeSetupIcon();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent) {
				// Trade setup button: Does nothing.
			}
		};
	}

	protected ItemStack createPrevPageIcon(int page) {
		int prevPage = 1;
		String prevPageText = "-";
		if (page > 1) {
			prevPage = (page - 1);
			prevPageText = String.valueOf(prevPage);
		}
		String itemName = StringUtils.replaceArguments(Messages.buttonPreviousPage,
				"prev_page", prevPageText,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.previousPageItem.createItemStack();
		ItemUtils.setItemMeta(item, itemName, Messages.buttonPreviousPageLore, ItemUtils.MAX_STACK_SIZE_64);
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.clamp(prevPage, 1, ItemUtils.MAX_STACK_SIZE_64));
		return item;
	}

	protected ItemStack createNextPageIcon(int page) {
		int nextPage = 1;
		String nextPageText = "-";
		if (page < getMaxTradesPages()) {
			nextPage = (page + 1);
			nextPageText = String.valueOf(nextPage);
		}
		String itemName = StringUtils.replaceArguments(Messages.buttonNextPage,
				"next_page", nextPageText,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.nextPageItem.createItemStack();
		ItemUtils.setItemMeta(item, itemName, Messages.buttonNextPageLore, ItemUtils.MAX_STACK_SIZE_64);
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.clamp(nextPage, 1, ItemUtils.MAX_STACK_SIZE_64));
		return item;
	}

	protected ItemStack createCurrentPageIcon(int page) {
		String itemName = StringUtils.replaceArguments(Messages.buttonCurrentPage,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.currentPageItem.createItemStack();
		ItemUtils.setItemMeta(item, itemName, Messages.buttonCurrentPageLore, ItemUtils.MAX_STACK_SIZE_64);
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.clamp(page, 1, ItemUtils.MAX_STACK_SIZE_64));
		return item;
	}

	protected abstract ItemStack createShopInformationIcon();

	protected abstract ItemStack createTradeSetupIcon();

	// EDITOR BUTTONS

	public void addButton(Button button) {
		Validate.notNull(button, "button is null");
		// Validates that the button is applicable and not yet used in another layout:
		button.setEditorLayout(this);
		buttons.add(button);
		dirtyButtons = true;
	}

	public final void addButtons(Iterable<? extends Button> buttons) {
		Validate.notNull(buttons, "buttons is null");
		for (Button button : buttons) {
			this.addButton(button);
		}
	}

	public void addButtonOrIgnore(@Nullable Button button) {
		if (button == null) return; // Ignore

		this.addButton(button);
	}

	public final void addButtonsOrIgnore(Iterable<? extends @Nullable Button> buttons) {
		Validate.notNull(buttons, "buttons is null");
		for (Button button : buttons) {
			this.addButtonOrIgnore(button);
		}
	}

	private void bakeButtons() {
		if (!dirtyButtons) return;

		// Reset buttons:
		buttons.forEach(button -> button.setSlot(Button.NO_SLOT));

		// Clear array:
		Arrays.fill(bakedButtons, null);

		// Insert buttons:
		int frontIndex = 0;
		this.buttonRows = Math.min(BUTTON_MAX_ROWS, ((buttons.size() - 1) / COLUMNS_PER_ROW) + 1);
		int endIndex = buttonRows * COLUMNS_PER_ROW - 1;
		for (int i = 0; i < buttons.size(); ++i) {
			Button button = buttons.get(i);
			int buttonIndex;
			if (button.isPlaceAtEnd()) {
				buttonIndex = endIndex;
				endIndex--;
			} else {
				buttonIndex = frontIndex;
				frontIndex++;
			}
			if (bakedButtons[buttonIndex] != null) {
				// There is not enough space for the remaining buttons.
				break;
			}
			bakedButtons[buttonIndex] = button;
			button.setSlot(BUTTONS_START + buttonIndex);
		}
	}

	int getButtonRows() {
		this.bakeButtons();
		return buttonRows;
	}

	@Nullable
	Button[] getBakedButtons() {
		this.bakeButtons();
		return bakedButtons;
	}

	@Nullable
	Button getButton(int rawSlot) {
		if (!this.isButtonArea(rawSlot)) return null;
		return this.getBakedButtons()[rawSlot - BUTTONS_START];
	}

	@Nullable
	Button findButton(Object buttonIdentity) {
		// Uses the baked buttons, so if the caller subsequently accesses the slot of the returned
		// button, it is already filled in:
		Button button = findButton(this.getBakedButtons(), buttonIdentity);
		if (button != null) return button;

		return findButton(this.getTradesPageBarButtons(), buttonIdentity);
	}

	private static @Nullable Button findButton(
			@Nullable Button[] buttons,
			Object buttonIdentity
	) {
		for (int i = 0; i < buttons.length; ++i) {
			Button button = buttons[i];
			if (button == null) continue;

			if (button.getIdentity().equals(buttonIdentity)) {
				return button;
			}
		}

		return null;
	}
}
