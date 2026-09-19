package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorView;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class PlayerShopEditorView extends ShopkeeperEditorView {

	protected PlayerShopEditorView(
			PlayerShopEditorViewProvider viewProvider,
			Player player,
			UIState uiState
	) {
		super(viewProvider, player, uiState);
	}

	@Override
	protected PlayerShopEditorLayout createLayout() {
		return new PlayerShopEditorLayout(this);
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		// Cancel all inventory clicks and handle everything on our own:
		// TODO Maybe allow certain inventory actions which only affect the player's inventory?
		event.setCancelled(true);
		super.onInventoryDragEarly(event);
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		// Cancel all inventory clicks and handle everything on our own:
		// TODO Maybe allow certain inventory actions which only affect the player's inventory (like
		// moving items around)?
		event.setCancelled(true);
		super.onInventoryClickEarly(event);
	}

	// Returns the new item.
	// Returns null if the new item is empty or matches the empty slot item.
	protected @Nullable ItemStack updateItemAmountOnClick(
			InventoryClickEvent event,
			int minAmount,
			@Nullable UnmodifiableItemStack emptySlotItem
	) {
		Validate.isTrue(minAmount >= 0, "minAmount cannot be negative");
		assert event.isCancelled();
		// Ignore in certain situations:
		ItemStack clickedItem = event.getCurrentItem();
		if (ItemUtils.isEmpty(clickedItem) || ItemUtils.equals(emptySlotItem, clickedItem)) {
			return null;
		}

		clickedItem = Unsafe.assertNonNull(clickedItem);

		// Get new item amount:
		int currentItemAmount = clickedItem.getAmount();
		int newItemAmount = this.getNewAmountAfterEditorClick(
				event,
				currentItemAmount,
				minAmount,
				clickedItem.getMaxStackSize()
		);
		assert newItemAmount >= minAmount;
		assert newItemAmount <= clickedItem.getMaxStackSize();

		// Update item in inventory:
		if (newItemAmount == 0) {
			// Place empty slot item:
			event.setCurrentItem(ItemUtils.asItemStackOrNull(emptySlotItem));
			return null;
		} else {
			clickedItem.setAmount(newItemAmount);
			return clickedItem;
		}
	}

	protected void updateTradeCostItemOnClick(
			InventoryClickEvent event,
			@Nullable Currency currency,
			@Nullable UnmodifiableItemStack emptySlotItem
	) {
		assert event != null;
		assert event.isCancelled();
		// Ignore in certain situations:
		if (currency == null) return;

		// Get new item amount:
		ItemStack clickedItem = event.getCurrentItem(); // Can be null
		int currentItemAmount = 0;
		boolean isCurrencyItem = currency.getItemData().matches(clickedItem);
		if (isCurrencyItem) {
			assert clickedItem != null;
			currentItemAmount = clickedItem.getAmount();
		}

		int newItemAmount = this.getNewAmountAfterEditorClick(
				event,
				currentItemAmount,
				0,
				currency.getMaxStackSize()
		);
		assert newItemAmount >= 0;
		assert newItemAmount <= currency.getMaxStackSize();

		// Update item in inventory:
		if (newItemAmount == 0) {
			// Place empty slot item:
			event.setCurrentItem(ItemUtils.asItemStackOrNull(emptySlotItem));
		} else {
			if (isCurrencyItem) {
				assert clickedItem != null;
				// Only update the amount of the already existing currency item:
				clickedItem.setAmount(newItemAmount);
			} else {
				// Place a new currency item:
				ItemStack currencyItem = currency.getItemData().createItemStack(newItemAmount);
				event.setCurrentItem(currencyItem);
			}
		}
	}
}
