package com.nisovin.shopkeepers.ui.containers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainer;
import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerType;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.SKShopContainer;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.input.interaction.InteractionInput;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopcreation.ContainerSelection;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIState;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ChestLayout;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class PlayerShopContainersEditorView extends View {

	// Reserve two inventory rows for potential future additions (page navigation or additional
	// buttons).
	// Maximum supported containers for now: 9 * 4 = 36
	private final static int MAX_INVENTORY_SIZE = ChestLayout.SLOTS_PER_ROW * (ChestLayout.MAX_ROWS - 2);

	// Cached for the lifetime of this view:
	private final List<? extends ShopContainerType> allContainerTypes = new ArrayList<>(
			SKShopkeepersPlugin.getInstance().getShopContainerTypeRegistry().getRegisteredTypes()
	);
	private List<? extends SKShopContainer> containers = new ArrayList<>();
	private int addContainerButtonSlot = -1;

	protected PlayerShopContainersEditorView(
			PlayerShopContainersEditorViewProvider provider,
			Player player,
			UIState uiState
	) {
		super(provider, player, uiState);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeperNonNull() {
		return (AbstractPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected @Nullable InventoryView openInventoryView() {
		var playerShop = this.getShopkeeperNonNull();
		var containerCount = playerShop.getContainers().size();
		var inventorySize = Math.min(
				ChestLayout.getRequiredSlots(Math.max(containerCount, Settings.maxContainersPerPlayerShop)),
				MAX_INVENTORY_SIZE
		);
		var inventory = Bukkit.createInventory(null, inventorySize, Messages.shopContainersEditorTitle);
		this.updateInventory(inventory);

		Player player = this.getPlayer();
		return player.openInventory(inventory);
	}

	private void updateInventory(Inventory inventory) {
		inventory.clear();

		var playerShop = this.getShopkeeperNonNull();
		containers = new ArrayList<>(playerShop.getContainers());

		// Note: We don't support dynamically resizing the inventory currently.
		var inventorySize = inventory.getSize();
		var slotIndex = 0;
		for (var container : containers) {
			if (slotIndex >= inventorySize) {
				break;
			}

			var editorItem = ShopContainerEditorUtils.createContainerItem(playerShop, container, false);
			inventory.setItem(slotIndex, editorItem);

			slotIndex += 1;
		}

		if (containers.size() >= Settings.maxContainersPerPlayerShop
				|| containers.size() >= inventorySize) {
			addContainerButtonSlot = -1;
		} else {
			addContainerButtonSlot = containers.size();
			var addContainerButtonItem = this.createAddContainerItem();
			inventory.setItem(slotIndex, addContainerButtonItem);
		}
	}

	@Override
	public void updateInventory() {
		var inventory = this.getInventory();
		this.updateInventory(inventory);
		this.syncInventory();
	}

	private @Nullable ItemStack createAddContainerItem() {
		return ItemUtils.setDisplayNameAndLore(
				Settings.addContainerItem.createItemStack(),
				Messages.buttonAddShopContainer,
				StringUtils.replaceArguments(Messages.buttonAddShopContainerLore,
						"containerCount", this.getShopkeeperNonNull().getContainers().size(),
						"maxContainers", Settings.maxContainersPerPlayerShop
				)
		);
	}

	@Override
	protected void onInventoryClose(@Nullable InventoryCloseEvent closeEvent) {
		// Nothing to do by default.
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event) {
		event.setCancelled(true);
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		event.setCancelled(true);

		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		int slot = event.getRawSlot();

		// Ignore outside clicks:
		// Also ensures that the add-container-button is not clickable when it is not assigned to
		// any positive slot number.
		if (slot < 0) {
			return;
		}

		if (slot == addContainerButtonSlot) {
			this.addContainerClicked(event);
		} else if (slot < containers.size()) {
			var container = containers.get(slot);
			this.containerClicked(event, container);
		}
	}

	private void containerClicked(InventoryClickEvent event, SKShopContainer container) {
		var player = this.getPlayer();
		var shopkeeper = this.getShopkeeperNonNull();

		// Shift-right-click: Open the container contents, if remote opening is enabled.
		if (Settings.enablePlayerShopOpenContainer
				&& event.isShiftClick()
				&& event.isRightClick()) {
			ShopContainerEditorUtils.openContainerContents(this, shopkeeper, player, container);
			return;
		}

		// Shift-left-click, or any shift click if remote container opening is disabled: Remove the
		// container after confirmation.
		if (event.isShiftClick()) {
			if (!ShopContainerEditorUtils.isRemovalSupported(shopkeeper)) {
				return;
			}

			this.confirmRemoveContainer(shopkeeper, player, container);
			return;
		}

		// Left-/right-click: Cycle the container type.
		// The container type is only editable if the shop can use multiple containers. Otherwise, a
		// single container always serves for both stock and the earnings.
		if (Settings.maxContainersPerPlayerShop <= 1) {
			return;
		}

		this.cycleContainerType(shopkeeper, player, container, event.isRightClick());
	}

	private void cycleContainerType(
			AbstractPlayerShopkeeper shopkeeper,
			Player player,
			SKShopContainer container,
			boolean backwards
	) {
		var nextContainerType = CollectionUtils.cycleValue(
				allContainerTypes,
				container.getType(),
				backwards,
				value -> true
		);

		shopkeeper.updateContainer(container.getLocation(), nextContainerType);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();

		// Update any other open container views:
		this.updateAllViews();
		// Note: Updating any open editor views: Not required currently.
	}

	private void confirmRemoveContainer(
			AbstractPlayerShopkeeper shopkeeper,
			Player player,
			ShopContainer container
	) {
		ConfirmationUI.requestConfirmation(player, new ConfirmationUIState(
				Messages.confirmationUiRemoveShopContainerTitle,
				StringUtils.replaceArguments(Messages.confirmationUiRemoveShopContainerLore,
						"container_world", container.getWorldName(),
						"container_x", container.getX(),
						"container_y", container.getY(),
						"container_z", container.getZ()
				),
				() -> {
					// Removal confirmed.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) {
						TextUtils.sendMessage(player, Messages.shopNoLongerExists);
						return;
					}

					// The player's access permission might have changed in the meantime:
					var viewProvider = shopkeeper.getViewProvider(DefaultUITypes.SHOP_CONTAINERS_EDITOR());
					if (viewProvider == null || !viewProvider.canAccess(player, false)) {
						return;
					}

					// The number of containers might have changed in the meantime: Silently abort
					// if the container can no longer be removed (e.g. it is now the last one).
					if (!ShopContainerEditorUtils.isRemovalSupported(shopkeeper)) {
						return;
					}

					// Remove the container:
					shopkeeper.removeContainer(
							container.getWorldName(),
							container.getX(),
							container.getY(),
							container.getZ()
					);

					TextUtils.sendMessage(player, Messages.shopContainerRemoved,
							"container_world", container.getWorldName(),
							"container_x", container.getX(),
							"container_y", container.getY(),
							"container_z", container.getZ()
					);

					// Call event:
					Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

					// Save:
					shopkeeper.save();

					// Update any other open container views:
					this.updateAllViews();

					// Re-open the containers editor:
					shopkeeper.openContainersEditorWindow(player);
				}, () -> {
					// Removal cancelled.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) return;

					// Re-open the containers editor:
					shopkeeper.openContainersEditorWindow(player);
				}
		));
	}

	private void addContainerClicked(InventoryClickEvent event) {
		this.closeDelayedAndRunTask(() -> {
			Player player = this.getPlayer();
			if (!player.isValid()) return;
			if (this.abortIfContextInvalid()) {
				return;
			}

			var shopkeeper = this.getShopkeeperNonNull();

			SKShopkeepersPlugin.getInstance().getInteractionInput().request(
					player,
					new AddShopContainerRequest(shopkeeper, player)
			);
			TextUtils.sendMessage(player, Messages.clickShopContainerToAdd);
		});
	}

	private class AddShopContainerRequest implements InteractionInput.Request {

		private final AbstractPlayerShopkeeper shopkeeper;
		private final Player player;

		AddShopContainerRequest(AbstractPlayerShopkeeper shopkeeper, Player player) {
			assert shopkeeper != null;
			assert player != null;
			this.shopkeeper = shopkeeper;
			this.player = player;
		}

		private boolean isAbortAction(Action action) {
			// Note: Right-click air is not called if the player is not holding any item in their
			// hand. We therefore completely ignore it.
			return action == Action.LEFT_CLICK_AIR
					|| action == Action.LEFT_CLICK_BLOCK;
		}

		@Override
		public boolean accepts(PlayerInteractEvent event) {
			Action action = event.getAction();
			if (this.isAbortAction(action)) {
				return true;
			}

			// Only right-clicking a block adds a container:
			if (action != Action.RIGHT_CLICK_BLOCK) {
				return false;
			}

			// Ignore interactions with certain types of blocks:
			Block clickedBlock = Unsafe.assertNonNull(event.getClickedBlock());
			if (this.isInteractionIgnored(clickedBlock.getType())) {
				Log.debug(() -> "Container selection: Ignoring interaction with block of type "
						+ clickedBlock.getType());
				return false;
			}

			return true;
		}

		private boolean isInteractionIgnored(Material clickedBlockType) {
			// We ignore interactions with some types of blocks that are relevant for navigation
			// (doors, buttons, etc.):
			if (ItemUtils.isClickableDoor(clickedBlockType)) return true;
			if (ItemUtils.isClickableSwitch(clickedBlockType)) return true;
			return false;
		}

		@Override
		public void onInteract(PlayerInteractEvent event) {
			// Cancel the event, including when the selection is aborted:
			event.setCancelled(true);

			if (this.isAbortAction(event.getAction())) {
				this.onAborted();
				return;
			}

			Block clickedBlock = Unsafe.assertNonNull(event.getClickedBlock());
			this.addContainer(clickedBlock);
		}

		private void addContainer(Block containerBlock) {
			if (!shopkeeper.isValid()) {
				TextUtils.sendMessage(player, Messages.shopNoLongerExists);
				return;
			}

			// Check for a supported container type:
			if (!ShopContainers.isSupportedContainer(containerBlock.getType())) {
				TextUtils.sendMessage(player, Messages.unsupportedContainer);
				return;
			}

			// Validate the selected container:
			ContainerSelection containerSelection = SKShopkeepersPlugin.getInstance()
					.getShopkeeperCreation()
					.getContainerSelection();
			if (!containerSelection.validateContainer(player, containerBlock)) {
				// Validation already sent a feedback message.
				return;
			}

			// Check the maximum container distance:
			var shopLocation = shopkeeper.getBlockLocation();
			if (shopLocation != null) {
				var containerLocation = BlockLocation.of(containerBlock);
				double maxContainerDistanceSq = Settings.maxContainerDistance * Settings.maxContainerDistance;
				if (containerLocation.getDistanceSquared(shopLocation) > maxContainerDistanceSq) {
					TextUtils.sendMessage(player, Messages.containerTooFarAway);
					return;
				}
			}

			// Add the container, defaulting to the "stock and earnings" type:
			shopkeeper.addContainer(
					containerBlock.getWorld().getName(),
					containerBlock.getX(),
					containerBlock.getY(),
					containerBlock.getZ(),
					DefaultShopContainerTypes.STOCK_AND_EARNINGS()
			);

			TextUtils.sendMessage(player, Messages.shopContainerAdded,
					"container_world", containerBlock.getWorld().getName(),
					"container_x", containerBlock.getX(),
					"container_y", containerBlock.getY(),
					"container_z", containerBlock.getZ()
			);

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// Save:
			shopkeeper.save();

			// Update any other open container views:
			updateAllViews();

			// Re-open the containers editor:
			shopkeeper.openContainersEditorWindow(player);
		}

		@Override
		public void onAborted() {
			if (!player.isValid()) return;

			TextUtils.sendMessage(player, Messages.containerSelectionAborted);
		}
	}
}
