package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperHireEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.playershops.PlayerShopsLimit;
import com.nisovin.shopkeepers.ui.hiring.HiringView;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class PlayerShopHiringView extends HiringView {

	protected static final int HIRE_COST_SLOT = 4;
	protected static final int HIRE_BUTTON_1_SLOT = 2;
	protected static final int HIRE_BUTTON_2_SLOT = 6;

	public PlayerShopHiringView(PlayerShopHiringViewProvider provider, Player player, UIState uiState) {
		super(provider, player, uiState);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeperNonNull() {
		return (AbstractPlayerShopkeeper) super.getShopkeeperNonNull();
	}

	@Override
	protected @Nullable InventoryView openInventoryView() {
		var inventory = Bukkit.createInventory(null, 9, Messages.forHireTitle);
		this.updateInventory(inventory);

		Player player = this.getPlayer();
		return player.openInventory(inventory);
	}

	private void updateInventory(Inventory inventory) {
		PlayerShopkeeper shopkeeper = this.getShopkeeperNonNull();

		ItemStack hireItem = DerivedSettings.hireButtonItem.createItemStack();
		inventory.setItem(HIRE_BUTTON_1_SLOT, hireItem);
		inventory.setItem(HIRE_BUTTON_2_SLOT, hireItem);

		UnmodifiableItemStack hireCost = shopkeeper.getHireCost();
		if (hireCost != null) {
			// Inventory#setItem copies the item, so we do not need to copy it ourselves here.
			inventory.setItem(HIRE_COST_SLOT, ItemUtils.asItemStack(hireCost));
		}
	}

	@Override
	public void updateInventory() {
		var inventory = this.getInventory();
		this.updateInventory(inventory);
		this.syncInventory();
	}

	private boolean canPlayerHireShopType(Player player, Shopkeeper shopkeeper) {
		if (!Settings.hireRequireCreationPermission) return true;
		if (!shopkeeper.getType().hasPermission(player)) return false;
		if (!shopkeeper.getShopObject().getType().hasPermission(player)) return false;
		return true;
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event) {
		super.onInventoryClickEarly(event);
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		Player player = this.getPlayer();
		AbstractPlayerShopkeeper shopkeeper = this.getShopkeeperNonNull();
		int slot = event.getRawSlot();
		if (slot == HIRE_BUTTON_1_SLOT || slot == HIRE_BUTTON_2_SLOT) {
			// Handle hiring:

			// Note: Players can also hire their own shops. This is relevant when a shop reverted to
			// its for-hire state, for example after it expired, and its owner wants to hire it
			// again, or when an admin who set up the shop for hire wants to hire it themselves in
			// their normal "player" role.

			// Check if the player can hire (create) this type of shopkeeper:
			if (!this.canPlayerHireShopType(player, shopkeeper)) {
				// Missing permission to hire this type of shopkeeper:
				TextUtils.sendMessage(player, Messages.cannotHireShopType);
				this.abortDelayed();
				return;
			}

			if (!shopkeeper.isForHire()) {
				// The shopkeeper is no longer for hire.
				// We usually already close any hiring UIs whenever the for-hire state or the hire
				// cost item changes, so this is only a safeguard (view closing may be delayed).
				// TODO Send a feedback message to the player?
				this.abortDelayed();
				return;
			}

			UnmodifiableItemStack hireCost = Unsafe.assertNonNull(shopkeeper.getHireCost());

			// Check if the player can afford to hire the shopkeeper, and calculate the resulting
			// player inventory:
			PlayerInventory playerInventory = player.getInventory();
			@Nullable ItemStack[] newPlayerInventoryContents = Unsafe.castNonNull(playerInventory.getContents());
			if (InventoryUtils.removeItems(newPlayerInventoryContents, hireCost) != 0) {
				// The player cannot afford to hire the shopkeeper:
				TextUtils.sendMessage(player, Messages.cannotHire);
				// Close the view for this player:
				this.abortDelayed();
				return;
			}

			// Call event:
			int maxShopsLimit = PlayerShopsLimit.getMaxShopsLimit(player);
			PlayerShopkeeperHireEvent hireEvent = new PlayerShopkeeperHireEvent(
					shopkeeper,
					player,
					newPlayerInventoryContents,
					maxShopsLimit
			);
			Bukkit.getPluginManager().callEvent(hireEvent);
			if (hireEvent.isCancelled()) {
				Log.debug("PlayerShopkeeperHireEvent was cancelled!");
				// Close the view for this player:
				this.abortDelayed();
				return;
			}

			// Check max shops limit:
			// Note: This shopkeeper is currently for hire and therefore not counted, even if the
			// player already owns it.
			maxShopsLimit = hireEvent.getMaxShopsLimit();
			if (maxShopsLimit != Integer.MAX_VALUE) {
				int ownedShopsCount = PlayerShopsLimit.getOwnedShopsCount(player.getUniqueId());
				if (ownedShopsCount >= maxShopsLimit) {
					TextUtils.sendMessage(player, Messages.tooManyShops);
					this.abortDelayed();
					return;
				}
			}

			// Hire the shopkeeper:
			// Apply player inventory changes:
			InventoryUtils.setContents(playerInventory, newPlayerInventoryContents);

			// Reset the trade offers if they were set up by a different previous owner:
			if (!shopkeeper.isOwner(player)) {
				shopkeeper.clearOffers();
			}

			// Note: This preserves the hire cost item in case the shopkeeper later reverts to the
			// for-hire state.
			shopkeeper.setHired();
			shopkeeper.setOwner(player);
			shopkeeper.save();
			TextUtils.sendMessage(player, Messages.hired);

			// Additionally inform the new owner about the shop's expiration, if applicable:
			SKShopkeepersPlugin.getInstance()
					.getPlayerShops()
					.getPlayerShopsExpiration()
					.getNotifier()
					.informAboutExpiration(player, shopkeeper);

			// Close all open windows for the shopkeeper:
			shopkeeper.abortUISessionsDelayed();
		}
	}
}
