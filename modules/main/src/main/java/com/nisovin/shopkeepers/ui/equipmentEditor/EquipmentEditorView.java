package com.nisovin.shopkeepers.ui.equipmentEditor;

import java.util.Set;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.ui.UIHelpers;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.ui.lib.ViewProvider;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.inventory.ChestLayout;
import com.nisovin.shopkeepers.util.inventory.InventoryViewUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class EquipmentEditorView extends View {

	protected EquipmentEditorView(ViewProvider provider, Player player, UIState uiState) {
		super(provider, player, uiState);
	}

	@Override
	public boolean isAcceptedState(UIState uiState) {
		return uiState instanceof EquipmentEditorUIState;
	}

	// Restoring UI State: Not supported.

	protected EquipmentEditorUIState getConfig() {
		return (EquipmentEditorUIState) this.getInitialUIState();
	}

	@Override
	protected @Nullable InventoryView openInventoryView() {
		var config = this.getConfig();
		var supportedSlots = config.getSupportedSlots();
		int inventorySize = ChestLayout.getRequiredSlots(supportedSlots.size());
		var inventory = Bukkit.createInventory(null, inventorySize, Messages.equipmentEditorTitle);
		this.updateInventory(inventory);

		Player player = this.getPlayer();
		return player.openInventory(inventory);
	}

	private void updateInventory(Inventory inventory) {
		var config = this.getConfig();
		var supportedSlots = config.getSupportedSlots();
		var currentEquipment = config.getCurrentEquipment();

		for (int slotIndex = 0; slotIndex < supportedSlots.size(); slotIndex++) {
			EquipmentSlot equipmentSlot = supportedSlots.get(slotIndex);
			if (slotIndex >= inventory.getSize()) break;

			@Nullable UnmodifiableItemStack equipmentItem = currentEquipment.get(equipmentSlot);
			ItemStack editorItem = this.toEditorEquipmentItem(equipmentSlot, ItemUtils.asItemStackOrNull(equipmentItem));
			inventory.setItem(slotIndex, editorItem);
		}
	}

	@Override
	public void updateInventory() {
		var inventory = this.getInventory();
		this.updateInventory(inventory);
		this.syncInventory();
	}

	private @Nullable ItemStack toEditorEquipmentItem(EquipmentSlot equipmentSlot, @ReadOnly @Nullable ItemStack item) {
		ItemStack editorItem;

		if (ItemUtils.isEmpty(item)) {
			editorItem = new ItemStack(Material.ARMOR_STAND);
		} else {
			assert item != null;
			editorItem = item.clone();
		}
		assert editorItem != null;

		this.setEditorEquipmentItemMeta(editorItem, equipmentSlot);

		return editorItem;
	}

	private void setEditorEquipmentItemMeta(@ReadWrite ItemStack item, EquipmentSlot equipmentSlot) {
		String displayName;
		switch (equipmentSlot.name()) {
		case "HAND":
			displayName = Messages.equipmentSlotMainhand;
			break;
		case "OFF_HAND":
			displayName = Messages.equipmentSlotOffhand;
			break;
		case "FEET":
			displayName = Messages.equipmentSlotFeet;
			break;
		case "LEGS":
			displayName = Messages.equipmentSlotLegs;
			break;
		case "CHEST":
			displayName = Messages.equipmentSlotChest;
			break;
		case "HEAD":
			displayName = Messages.equipmentSlotHead;
			break;
		case "BODY": // Added in Bukkit 1.20.5
			displayName = Messages.equipmentSlotBody;
			break;
		case "SADDLE": // Added in Bukkit 1.21.5
			displayName = Messages.equipmentSlotSaddle;
			break;
		default:
			// Fallback:
			displayName = EnumUtils.formatEnumName(equipmentSlot.name());
			break;
		}

		ItemUtils.setDisplayNameAndLore(
				item,
				displayName,
				Messages.equipmentSlotLore
		);
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.isShiftClick()) return; // Ignoring shift clicks
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		int rawSlot = event.getRawSlot();
		if (rawSlot < 0) return;

		InventoryView view = event.getView();

		if (InventoryViewUtils.isTopInventory(view, rawSlot)) {
			this.handleEditorInventoryClick(event);
		} else if (InventoryViewUtils.isPlayerInventory(view, rawSlot)) {
			this.handlePlayerInventoryClick(event);
		}
	}

	private void handlePlayerInventoryClick(InventoryClickEvent event) {
		assert event.isCancelled();
		UIHelpers.swapCursor(event.getView(), event.getRawSlot());
	}

	private void handleEditorInventoryClick(InventoryClickEvent event) {
		assert event.isCancelled();
		InventoryView view = event.getView();
		this.handleEditorInventoryClick(
				view,
				event.getRawSlot(),
				event.isLeftClick(),
				event.isRightClick(),
				() -> ItemUtils.cloneOrNullIfEmpty(view.getCursor())
		);
	}

	private void handleEditorInventoryClick(
			InventoryView view,
			int rawSlot,
			boolean leftClick,
			boolean rightClick,
			Supplier<@Nullable ItemStack> getCursorCopy
	) {
		// Assert: The involved inventory event was cancelled.
		var config = this.getConfig();
		var supportedSlots = config.getSupportedSlots();

		if (rawSlot >= supportedSlots.size()) return;

		EquipmentSlot equipmentSlot = supportedSlots.get(rawSlot);
		Inventory inventory = view.getTopInventory();

		if (rightClick) {
			// Clear the equipment slot:
			SchedulerUtils.runTaskOrOmit(view.getPlayer(), () -> {
				if (!this.isOpen() || this.abortIfContextInvalid()) return;

				inventory.setItem(rawSlot, this.toEditorEquipmentItem(equipmentSlot, null));
				onEquipmentChanged(equipmentSlot, null);
			});
			return;
		}

		ItemStack cursorClone = getCursorCopy.get();
		if (leftClick && !ItemUtils.isEmpty(cursorClone)) {
			assert cursorClone != null;
			// Place the item from the cursor:
			SchedulerUtils.runTaskOrOmit(getPlayer(), () -> {
				if (!this.isOpen() || this.abortIfContextInvalid()) return;

				cursorClone.setAmount(1);

				// Replace placeholder item, if this is one:
				ItemStack substitutedItem = PlaceholderItems.replaceNonNull(cursorClone);

				// Inform about the new equipment item:
				// No item copy required: The item is already a copy, and for the item in the editor
				// we create a separate copy subsequently.
				onEquipmentChanged(equipmentSlot, UnmodifiableItemStack.of(substitutedItem));

				// Update the item in the editor:
				// This copies the item internally (but irrelevant, because we already create a copy
				// for the editor item anyway):
				inventory.setItem(rawSlot, this.toEditorEquipmentItem(equipmentSlot, substitutedItem));
			});
		}
	}

	protected void onEquipmentChanged(EquipmentSlot slot, @Nullable UnmodifiableItemStack item) {
		var config = this.getConfig();
		var onEquipmentChanged = config.getOnEquipmentChanged();
		onEquipmentChanged.accept(slot, item);
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		event.setCancelled(true);
		ItemStack cursorClone = event.getOldCursor(); // Already a copy
		if (ItemUtils.isEmpty(cursorClone)) return;
		assert cursorClone != null;

		Set<Integer> rawSlots = event.getRawSlots();
		if (rawSlots.size() != 1) return;

		int rawSlot = rawSlots.iterator().next();
		if (rawSlot < 0) return;

		InventoryView view = event.getView();

		if (InventoryViewUtils.isTopInventory(view, rawSlot)) {
			boolean isLeftClick = event.getType() == DragType.EVEN;
			boolean isRightClick = event.getType() == DragType.SINGLE;
			this.handleEditorInventoryClick(
					view,
					rawSlot,
					isLeftClick,
					isRightClick,
					() -> cursorClone
			);
		} else {
			if (InventoryViewUtils.isPlayerInventory(view, rawSlot)) {
				// The cancelled drag event resets the cursor afterwards, so we need this delay:
				UIHelpers.swapCursorDelayed(view, rawSlot);
			}
		}
	}

	@Override
	protected void onInventoryClose(@Nullable InventoryCloseEvent closeEvent) {
		// TODO Return to the editor if the user closed the inventory? But we cannot properly detect
		// currently whether the player themselves close the inventory.
		// Nothing to do by default.
	}
}
