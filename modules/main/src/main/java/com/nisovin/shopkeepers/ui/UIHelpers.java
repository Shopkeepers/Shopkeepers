package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Common user interface helpers and behaviors.
 */
public final class UIHelpers {

	// The delay is for example required during the handling of inventory drag events, because the
	// cancelled drag event resets the cursor afterwards.
	public static void swapCursorDelayed(InventoryView view, int rawSlot) {
		SchedulerUtils.runTaskOrOmit(view.getPlayer(), () -> {
			// Check that the player still has the same view open and then freshly get and check the
			// involved items to make sure that players don't abuse this delay:
			if (view.getPlayer().getOpenInventory() != view) return;

			swapCursor(view, rawSlot);
		});
	}

	// Assumes that any involved inventory event was cancelled.
	public static void swapCursor(InventoryView view, int rawSlot) {
		ItemStack cursor = ItemUtils.getNullIfEmpty(view.getCursor());
		ItemStack current = ItemUtils.getNullIfEmpty(view.getItem(rawSlot));

		// Place, pick, or swap the cursor and slot item:
		view.setItem(rawSlot, cursor);
		view.setCursor(current);
	}

	private UIHelpers() {
	}
}
