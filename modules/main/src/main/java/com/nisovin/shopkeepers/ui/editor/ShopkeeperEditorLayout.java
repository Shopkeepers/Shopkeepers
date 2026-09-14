package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.events.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.moving.ShopkeeperMoving;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIState;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperEditorLayout extends EditorLayout {

	private final AbstractShopkeeper shopkeeper;

	public ShopkeeperEditorLayout(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	protected AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	public int getMaxTradesPages() {
		if (shopkeeper instanceof PlayerShopkeeper) {
			return Settings.maxPlayerShopTradesPages;
		}

		return super.getMaxTradesPages();
	}

	@Override
	protected ItemStack createShopInformationIcon() {
		var shopkeeper = this.getShopkeeper();
		String itemName = Messages.shopInformationHeader;
		List<String> itemLore = shopkeeper.getInformation();
		TextUtils.wrap(itemLore, TextUtils.LORE_MAX_LENGTH);
		return ItemUtils.setDisplayNameAndLore(
				Settings.shopInformationItem.createItemStack(),
				itemName,
				itemLore
		);
	}

	@Override
	protected ItemStack createTradeSetupIcon() {
		ShopType<?> shopType = this.getShopkeeper().getType();
		String itemName = StringUtils.replaceArguments(Messages.tradeSetupDescHeader,
				"shopType", shopType.getDisplayName()
		);
		List<? extends String> itemLore = shopType.getTradeSetupDescription();
		return ItemUtils.setDisplayNameAndLore(
				Settings.tradeSetupItem.createItemStack(),
				itemName,
				itemLore
		);
	}

	// EDITOR BUTTONS

	protected void setupShopkeeperButtons() {
		this.addButtonOrIgnore(this.createDeleteButton());
		this.addButtonOrIgnore(this.createOpenButton());
		this.addButtonOrIgnore(this.createNamingButton());
		this.addButtonOrIgnore(this.createMoveButton());
	}

	protected void setupShopObjectButtons() {
		this.addButtons(shopkeeper.getShopObject().createEditorButtons());
	}

	protected Button createDeleteButton() {
		// ActionButton instead of ShopkeeperActionButton: No need to call the edited event and save
		// the shopkeeper when clicked.
		return new ActionButton(true) {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				if (getHireableShop() != null) {
					return DerivedSettings.restoreForHireButtonItem.createItemStack();
				}

				return DerivedSettings.deleteButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				// Check if the player is allowed to delete this shopkeeper:
				if (shopkeeper instanceof AbstractPlayerShopkeeper playerShop
						&& !playerShop.checkAccess(editorView.getPlayer(), DefaultPlayerShopAccessLevels.FULL(), false)) {
					return true;
				}

				// A shop that is already for hire cannot be restored any further:
				var hireableShop = getHireableShop();
				if (hireableShop != null && hireableShop.isForHire()) {
					var player = editorView.getPlayer();
					TextUtils.sendMessage(player, Messages.shopAlreadyForHire);
					sendSetNotForHireToDeleteHint(player);
					return true;
				}

				UIState capturedUIState = editorView.captureState();
				editorView.closeDelayedAndRunTask(() -> {
					requestConfirmationDeleteShop(editorView.getPlayer(), capturedUIState);
				});
				return true;
			}
		};
	}

	// Returns the player shopkeeper if it retains a hire cost item, and can therefore not be
	// deleted, but only be restored to its for-hire state. Returns null otherwise.
	private @Nullable PlayerShopkeeper getHireableShop() {
		if (shopkeeper instanceof PlayerShopkeeper playerShop && playerShop.isHireable()) {
			return playerShop;
		}

		return null;
	}

	// Hints players that are able to set shops for hire that they can remove the shop's hire cost
	// item first in order to then fully delete the shop.
	private static void sendSetNotForHireToDeleteHint(Player player) {
		if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION)) {
			TextUtils.sendMessage(player, Messages.setNotForHireToDeleteHint);
		}
	}

	private void requestConfirmationDeleteShop(Player player, UIState previousUIState) {
		// Note: Shops that are already for hire are rejected before the confirmation is requested.
		boolean restoreForHire = this.getHireableShop() != null;
		var config = new ConfirmationUIState(
				restoreForHire
						? Messages.confirmationUiRestoreShopForHireTitle
						: Messages.confirmationUiDeleteShopTitle,
				restoreForHire
						? Messages.confirmationUiRestoreShopForHireConfirmLore
						: Messages.confirmationUiDeleteShopConfirmLore,
				() -> {
					// Delete confirmed.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) {
						// The shopkeeper has already been removed in the meantime.
						TextUtils.sendMessage(player, Messages.shopAlreadyRemoved);
						return;
					}

					// The player's access permission might have changed in the meantime:
					if (shopkeeper instanceof AbstractPlayerShopkeeper playerShop
							&& !playerShop.checkAccess(player, DefaultPlayerShopAccessLevels.FULL(), false)) {
						return;
					}

					// Shops that retain a hire cost item are restored to their for-hire state
					// instead of being deleted, so that other players can hire them again. This
					// matches how these shops are handled when they expire.
					// Note: This applies to admins as well, so that they do not unknowingly observe
					// a behavior that differs from what other players get.
					if (shopkeeper instanceof AbstractPlayerShopkeeper playerShop
							&& playerShop.isHireable()) {
						playerShop.setForHire();

						// Call shopkeeper edited event:
						Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

						// Save:
						shopkeeper.save();

						TextUtils.sendMessage(player, Messages.shopRestoredForHire);
						sendSetNotForHireToDeleteHint(player);
						return;
					}

					// Call event:
					PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(
							shopkeeper,
							player
					);
					Bukkit.getPluginManager().callEvent(deleteEvent);
					if (!deleteEvent.isCancelled()) {
						// Delete the shopkeeper and save:
						shopkeeper.delete(player);
						shopkeeper.save();

						TextUtils.sendMessage(player, Messages.shopRemoved);
					}
					// Else: Cancelled by another plugin.
					// Note: We don't send a message in this case here, because we expect that the
					// other plugin sends a more specific message anyway if it wants to inform the
					// player.
				}, () -> {
					// Delete cancelled.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) return;

					// Try to open the editor again:
					// We freshly determine the currently configured editor ViewProvider of the
					// shopkeeper, because it might have been replaced in the meantime.
					// We currently assume here that the captured UI state is compatible with the
					// current (potentially different) editor view provider.
					shopkeeper.openWindow(DefaultUITypes.EDITOR(), player, previousUIState);
				}
		);
		ConfirmationUI.requestConfirmation(player, config);
	}

	protected @Nullable Button createOpenButton() {
		if (shopkeeper.getType() instanceof PlayerShopType && !Settings.enableClosingOfPlayerShops) {
			return null;
		}

		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return shopkeeper.isOpen() ? DerivedSettings.shopOpenButtonItem.createItemStack()
						: DerivedSettings.shopClosedButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				var newState = !shopkeeper.isOpen();
				shopkeeper.setOpen(newState);
				return true;
			}
		};
	}

	protected @Nullable Button createNamingButton() {
		boolean useNamingButton = true;
		if (shopkeeper.getType() instanceof PlayerShopType) {
			// Naming via button enabled?
			if (Settings.namingOfPlayerShopsViaItem) {
				useNamingButton = false;
			} else {
				// No naming button for Citizens player shops if renaming is disabled for those.
				// TODO Restructure this to allow for dynamic editor buttons depending on shop
				// (object) types and settings.
				if (!Settings.allowRenamingOfPlayerNpcShops
						&& shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
					useNamingButton = false;
				}
			}
		}
		if (!useNamingButton) return null;

		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return DerivedSettings.nameButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				// Also triggers a save:
				editorView.closeDelayed();

				// Start naming:
				Player player = editorView.getPlayer();
				ShopkeeperNaming shopkeeperNaming = SKShopkeepersPlugin.getInstance().getShopkeeperNaming();
				shopkeeperNaming.startNaming(player, shopkeeper);

				TextUtils.sendMessage(player, Messages.typeNewName);
				return true;
			}
		};
	}

	protected @Nullable Button createMoveButton() {
		if (shopkeeper.getType() instanceof PlayerShopType && !Settings.enableMovingOfPlayerShops) {
			return null;
		}

		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return DerivedSettings.moveButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				Player player = editorView.getPlayer();

				// Prevent players from moving hired shops, unless the player has the setforhire
				// permission:
				if (getHireableShop() != null
						&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.SET_FOR_HIRE_PERMISSION)) {
					TextUtils.sendMessage(player, Messages.cannotMoveHiredShop);
					return true;
				}

				// Also triggers a save:
				editorView.closeDelayed();

				// Start moving:
				ShopkeeperMoving shopkeeperMoving = SKShopkeepersPlugin.getInstance().getShopkeeperMoving();
				shopkeeperMoving.startMoving(player, shopkeeper);

				TextUtils.sendMessage(player, Messages.clickNewShopLocation);
				return true;
			}
		};
	}
}
