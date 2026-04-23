package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.members.ShopkeeperMembers;
import com.nisovin.shopkeepers.ui.editor.ActionButton;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorView;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperEditorLayout;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
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

		this.addButtonOrIgnore(this.createContainerButton());
		this.addButtonOrIgnore(this.createTradeNotificationsButton());
		this.addButtonOrIgnore(this.createMembersButton());
	}

	protected @Nullable Button createContainerButton() {
		if (!Settings.enableContainerOptionOnPlayerShop) {
			return null;
		}

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
					if (!player.isValid() || !shopkeeper.isValid())
						return;

					shopkeeper.openContainerWindow(player);
				});
				return true;
			}
		};
	}

	protected @Nullable Button createTradeNotificationsButton() {
		if (!Settings.notifyShopOwnersAboutTrades) {
			return null;
		}

		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				var shopkeeper = (AbstractPlayerShopkeeper) this.getShopkeeper();
				ItemStack iconItem = Settings.tradeNotificationsItem.createItemStack();
				String state = shopkeeper.isNotifyOnTrades() ? Messages.stateEnabled : Messages.stateDisabled;
				String displayName = StringUtils.replaceArguments(Messages.buttonTradeNotifications,
						"state", state);
				List<? extends String> lore = StringUtils.replaceArguments(
						Messages.buttonTradeNotificationsLore,
						"state", state);
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

	protected @Nullable Button createMembersButton() {
		return new ActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorView editorView) {
				ItemStack iconItem = DerivedSettings.membersButtonItem.createItemStack();
				List<? extends String> lore = Messages.buttonMembersLore;
				ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonMembers, lore);
				return iconItem;
			}

			@Override
			protected boolean runAction(EditorView editorView, InventoryClickEvent clickEvent) {
				Player player = editorView.getPlayer();
				AbstractPlayerShopkeeper shopkeeper = getShopkeeper();

				// Only the owner can manage members:
				if (!shopkeeper.isOwner(player))
					return false;

				if (clickEvent.isLeftClick()) {
					// Add member: close editor, prompt for name via chat
					editorView.closeDelayed();
					ShopkeeperMembers shopkeeperMembers = SKShopkeepersPlugin.getInstance().getShopkeeperMembers();
					shopkeeperMembers.startAddMember(player, shopkeeper);
					TextUtils.sendMessage(player, Messages.typeMemberName);
				} else if (clickEvent.isRightClick()) {
					if (clickEvent.isShiftClick()) {
						// List members
						ShopkeeperMembers shopkeeperMembers = SKShopkeepersPlugin.getInstance().getShopkeeperMembers();
						shopkeeperMembers.listMembers(player, shopkeeper);
					} else {
						// Remove member: close editor, prompt for name via chat
						editorView.closeDelayed();
						ShopkeeperMembers shopkeeperMembers = SKShopkeepersPlugin.getInstance().getShopkeeperMembers();
						shopkeeperMembers.startRemoveMember(player, shopkeeper);
						TextUtils.sendMessage(player, Messages.typeRemoveMemberName);
					}
				}
				return true;
			}
		};
	}
}
