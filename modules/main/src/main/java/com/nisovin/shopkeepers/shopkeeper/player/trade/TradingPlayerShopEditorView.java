package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.Set;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorView;
import com.nisovin.shopkeepers.ui.UIHelpers;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.inventory.InventoryViewUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class TradingPlayerShopEditorView extends PlayerShopEditorView {

	protected TradingPlayerShopEditorView(
			TradingPlayerShopEditorViewProvider viewProvider,
			Player player,
			UIState uiState
	) {
		super(viewProvider, player, uiState);
	}

	@Override
	protected TradingRecipeDraft getEmptyTrade() {
		return DerivedSettings.tradingEmptyTrade;
	}

	@Override
	protected TradingRecipeDraft getEmptyTradeSlotItems() {
		return DerivedSettings.tradingEmptyTradeSlotItems;
	}

	@Override
	protected void handlePlayerInventoryClick(InventoryClickEvent event) {
		// Assert: Event cancelled.
		// Clicking in player inventory:
		if (event.isShiftClick()) return; // Ignoring shift clicks

		UIHelpers.swapCursor(event.getView(), event.getRawSlot());
	}

	@Override
	protected void handleTradesClick(InventoryClickEvent event) {
		var layout = this.getLayout();
		int rawSlot = event.getRawSlot();
		assert layout.isTradesArea(rawSlot);

		Inventory inventory = event.getInventory();
		ItemStack cursor = event.getCursor();
		if (!ItemUtils.isEmpty(cursor)) {
			// Place item from cursor:
			ItemStack cursorClone = ItemUtils.copySingleItem(Unsafe.assertNonNull(cursor));
			this.placeCursorInTrades(event.getView(), rawSlot, cursorClone);
		} else {
			// Change the stack size of the clicked item, if this column contains a trade:
			int tradeColumn = layout.getTradeColumn(rawSlot);
			if (this.isEmptyTrade(inventory, tradeColumn)) return;

			int minAmount = 0;
			UnmodifiableItemStack emptySlotItem;
			if (layout.isResultRow(rawSlot)) {
				minAmount = 1;
				emptySlotItem = this.getEmptyTradeSlotItems().getResultItem();
			} else if (layout.isItem1Row(rawSlot)) {
				emptySlotItem = this.getEmptyTradeSlotItems().getItem1();
			} else {
				assert layout.isItem2Row(rawSlot);
				emptySlotItem = this.getEmptyTradeSlotItems().getItem2();
			}
			ItemStack newItem = this.updateItemAmountOnClick(event, minAmount, emptySlotItem);

			// If the trade column might now be completely empty, update it to insert the correct
			// placeholder items:
			if (newItem == null) {
				this.updateTradeColumn(inventory, tradeColumn);
			}
		}
	}

	private void placeCursorInTrades(InventoryView view, int rawSlot, ItemStack cursorClone) {
		assert !ItemUtils.isEmpty(cursorClone);
		cursorClone.setAmount(1);
		// Replace placeholder item, if this is one:
		ItemStack cursorFinal = PlaceholderItems.replace(cursorClone);
		SchedulerUtils.runTaskOrOmit(view.getPlayer(), () -> {
			if (view.getPlayer().getOpenInventory() != view) return;

			Inventory inventory = view.getTopInventory();
			inventory.setItem(rawSlot, cursorFinal); // This copies the item internally

			// Update the trade column (replaces empty slot placeholder items if necessary):
			var layout = this.getLayout();
			this.updateTradeColumn(inventory, layout.getTradeColumn(rawSlot));
		});
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		event.setCancelled(true);
		ItemStack cursorClone = event.getOldCursor(); // Already a copy
		if (ItemUtils.isEmpty(cursorClone)) return;
		assert cursorClone != null;

		Set<Integer> rawSlots = event.getRawSlots();
		if (rawSlots.size() != 1) return;

		InventoryView view = event.getView();

		int rawSlot = rawSlots.iterator().next();
		var layout = this.getLayout();
		if (layout.isTradesArea(rawSlot)) {
			// Place item from cursor:
			this.placeCursorInTrades(view, rawSlot, cursorClone);
		} else {
			if (InventoryViewUtils.isPlayerInventory(view, rawSlot)) {
				// Clicking in player inventory:
				// The cancelled drag event resets the cursor afterwards, so we need this delay:
				UIHelpers.swapCursorDelayed(view, rawSlot);
			}
		}
	}
}
