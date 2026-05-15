package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.editor.ActionButton;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorLayout;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class PlayerShopEditorLayout extends ShopkeeperEditorLayout {

	public PlayerShopEditorLayout(AbstractPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	protected AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected void setupShopkeeperButtons() {
		super.setupShopkeeperButtons();

		this.addButtonOrIgnore(this.createMembersButton());
		this.addButtonOrIgnore(this.createContainerButton());
		this.addButtonOrIgnore(this.createTradeNotificationsButton());
	}

	protected @Nullable Button createMembersButton() {
		if (Settings.maxMembersPerShop <= 0) {
			return null;
		}

		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				var shopkeeper = (PlayerShopkeeper) this.getShopkeeper();
				ItemStack iconItem = Settings.membersItem.createItemStack();
				String memberCount = String.valueOf(shopkeeper.getMembers().size());
				String displayName = StringUtils.replaceArguments(Messages.buttonMembers,
						"memberCount", memberCount
				);
				List<? extends String> lore = StringUtils.replaceArguments(
						Messages.buttonMembersLore,
						"memberCount", memberCount
				);
				ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
				return iconItem;
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				var shopkeeper = (AbstractPlayerShopkeeper) this.getShopkeeper();
				// Check if the player is allowed to edit the shop members:
				if (!shopkeeper.canEditMembers(editorView.getPlayer(), false)) {
					return true;
				}

				// Closing the UI also triggers a save of the current editor state:
				editorView.closeDelayedAndRunTask(() -> {
					// Open the shop members editor:
					Player player = editorView.getPlayer();
					if (!player.isValid() || !shopkeeper.isValid()) return;

					shopkeeper.openMembersEditorWindow(player);
				});
				return true;
			}

			@Override
			protected void onActionSuccess(EditorView editorView, InventoryClickEvent clickEvent) {
				// Skip the edit event and saving of the shopkeeper.
			}
		};
	}

	protected @Nullable Button createContainerButton() {
		if (!Settings.enableContainerOptionOnPlayerShop) {
			return null;
		}

		// ActionButton instead of ShopkeeperActionButton: No need to call the edited event and save
		// the shopkeeper when clicked.
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				return DerivedSettings.containerButtonItem.createItemStack();
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				// Closing the UI also triggers a save of the current editor state:
				editorView.closeDelayedAndRunTask(() -> {
					// Open the shop container inventory:
					Player player = editorView.getPlayer();
					PlayerShopkeeper shopkeeper = getShopkeeper();
					if (!player.isValid() || !shopkeeper.isValid()) return;

					shopkeeper.openContainerWindow(player);
				});
				return true;
			}
		};
	}

	protected @Nullable Button createTradeNotificationsButton() {
		if (!Settings.notifyShopMembersAboutTrades) {
			return null;
		}

		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				var shopkeeper = (AbstractPlayerShopkeeper) this.getShopkeeper();
				ItemStack iconItem = Settings.tradeNotificationsItem.createItemStack();
				String state = shopkeeper.isNotifyOnTrades() ? Messages.stateEnabled : Messages.stateDisabled;
				String displayName = StringUtils.replaceArguments(Messages.buttonTradeNotifications,
						"state", state
				);
				List<? extends String> lore = StringUtils.replaceArguments(
						Messages.buttonTradeNotificationsLore,
						"state", state
				);
				ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
				return iconItem;
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				var shopkeeper = (AbstractPlayerShopkeeper) this.getShopkeeper();
				shopkeeper.setNotifyOnTrades(!shopkeeper.isNotifyOnTrades());
				return true;
			}
		};
	}
}
