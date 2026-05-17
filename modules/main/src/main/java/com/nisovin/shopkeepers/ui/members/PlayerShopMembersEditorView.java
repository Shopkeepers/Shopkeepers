package com.nisovin.shopkeepers.ui.members;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopMember;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.commands.util.UserArgumentUtils;
import com.nisovin.shopkeepers.commands.util.UserArgumentUtils.UserNameMatcher;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.player.members.SKPlayerShopAccessLevel;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIState;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ChestLayout;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class PlayerShopMembersEditorView extends View {

	// Reserve two inventory rows for potential future additions (page navigation or additional
	// buttons).
	// Maximum supported members for now: 9 * 4 = 36
	private final static int MAX_INVENTORY_SIZE = ChestLayout.SLOTS_PER_ROW * (ChestLayout.MAX_ROWS - 2);

	// Cached for the lifetime of this view:
	private final List<? extends PlayerShopAccessLevel> allAccessLevels = new ArrayList<>(
			SKShopkeepersPlugin.getInstance().getPlayerShopAccessLevelRegistry().getRegisteredTypes()
	);
	private List<? extends PlayerShopMember> members = new ArrayList<>();
	private int addMemberButtonSlot = -1;

	protected PlayerShopMembersEditorView(
			PlayerShopMembersEditorViewProvider provider,
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
		var memberCount = playerShop.getMembers().size();
		var inventorySize = Math.min(
				ChestLayout.getRequiredSlots(Math.max(memberCount, Settings.maxMembersPerShop)),
				MAX_INVENTORY_SIZE
		);
		var inventory = Bukkit.createInventory(null, inventorySize, Messages.shopMembersEditorTitle);
		this.updateInventory(inventory);

		Player player = this.getPlayer();
		return player.openInventory(inventory);
	}

	private void updateInventory(Inventory inventory) {
		inventory.clear();

		var playerShop = this.getShopkeeperNonNull();
		members = new ArrayList<>(playerShop.getMembers());

		// Note: We don't support dynamically resizing the inventory currently.
		var inventorySize = inventory.getSize();
		var slotIndex = 0;
		for (var member : members) {
			if (slotIndex >= inventorySize) {
				break;
			}

			var editorItem = this.createEditorItem(member);
			inventory.setItem(slotIndex, editorItem);

			slotIndex += 1;
		}

		if (members.size() >= Settings.maxMembersPerShop
				|| members.size() >= inventorySize) {
			addMemberButtonSlot = -1;
		} else {
			addMemberButtonSlot = members.size();
			var addMemberButtonItem = this.createAddMemberItem();
			inventory.setItem(slotIndex, addMemberButtonItem);
		}
	}

	@Override
	public void updateInventory() {
		var inventory = this.getInventory();
		this.updateInventory(inventory);
		this.syncInventory();
	}

	private @Nullable ItemStack createEditorItem(PlayerShopMember member) {
		ItemStack editorItem = ItemUtils.createPlayerHead(member.getUser().getUniqueId());

		var accessLevel = (SKPlayerShopAccessLevel) member.getAccessLevel();

		var displayName = StringUtils.replaceArguments(Messages.shopMemberTitle,
				"memberName", member.getUser().getName()
		);
		var lore = StringUtils.flattenLines(StringUtils.replaceArguments(
				Messages.shopMemberLore,
				"memberName", member.getUser().getName(),
				"accessLevel", accessLevel.getDisplayName(),
				"accessLevelDescription", accessLevel.getDescription()
		), true);

		ItemUtils.setDisplayNameAndLore(editorItem, displayName, lore);
		return editorItem;
	}

	private @Nullable ItemStack createAddMemberItem() {
		return ItemUtils.setDisplayNameAndLore(
				new ItemData(Material.GRAY_STAINED_GLASS_PANE).createItemStack(),
				Messages.buttonAddShopMember,
				StringUtils.replaceArguments(Messages.buttonAddShopMemberLore,
						"memberCount", this.getShopkeeperNonNull().getMembers().size(),
						"maxMembers", Settings.maxMembersPerShop)
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
		// Also ensures that the add-member-button is not clickable when it is not assigned to any
		// positive slot number.
		if (slot < 0) {
			return;
		}

		if (slot == addMemberButtonSlot) {
			this.addMemberClicked(event);
		} else if (slot < members.size()) {
			var member = members.get(slot);
			this.memberClicked(event, member);
		}
	}

	private void addMemberClicked(InventoryClickEvent event) {
		this.closeDelayedAndRunTask(() -> {
			Player player = this.getPlayer();
			if (!player.isValid()) return;
			if (this.abortIfContextInvalid()) {
				return;
			}

			var shopkeeper = this.getShopkeeperNonNull();

			// Start naming:
			SKShopkeepersPlugin.getInstance().getChatInput().request(player, message -> {
				this.addShopMember(shopkeeper, player, message);
			});
			TextUtils.sendMessage(player, Messages.enterNewShopMember);
		});
	}

	private void addShopMember(PlayerShopkeeper shopkeeper, Player player, String input) {
		if (!shopkeeper.isValid()) {
			return;
		}

		String normalizedInput = input.trim();
		normalizedInput = TextUtils.stripColor(normalizedInput);

		if (input.isEmpty() || input.equals("!")) {
			TextUtils.sendMessage(player, Messages.textInputAborted);
			return;
		}

		// Parse the target player by uuid or name:
		@Nullable User targetUser = null;
		@Nullable UUID memberUuid = ConversionUtils.parseUUID(normalizedInput);
		if (memberUuid != null) {
			targetUser = UserArgumentUtils.findUser(memberUuid);
		} else {
			var matchingUsers = UserNameMatcher.EXACT.match(normalizedInput, true).toList();
			if (matchingUsers.size() > 1) {
				UserArgumentUtils.handleAmbiguousUserName(
						player,
						normalizedInput,
						matchingUsers
				);
				return;
			}

			if (!matchingUsers.isEmpty()) {
				targetUser = matchingUsers.getFirst();
			}
		}

		if (targetUser == null) {
			TextUtils.sendMessage(player, Messages.commandPlayerArgumentInvalid,
					"argument", input
			);
			return;
		}

		if (shopkeeper.isOwner(targetUser.getUniqueId())) {
			TextUtils.sendMessage(player, Messages.cannotAddShopMemberAlreadyOwner,
					"player_name", targetUser.getName()
			);
			return;
		}

		// Also accounts for the shop owner:
		if (shopkeeper.isMember(targetUser.getUniqueId())) {
			TextUtils.sendMessage(player, Messages.cannotAddShopMemberAlreadyMember,
					"player_name", targetUser.getName()
			);
			return;
		}

		// Add the player as a new shop member, with the minimal access level by default:
		shopkeeper.addMember(
				targetUser.getUniqueId(),
				targetUser.getName(),
				DefaultPlayerShopAccessLevels.CONTAINER()
		);

		TextUtils.sendMessage(player, Messages.shopMemberAdded,
				"member_name", targetUser.getName()
		);

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();

		// Update any other open editors:
		this.updateAllViews();

		// Re-open the shop members editor:
		shopkeeper.openMembersEditorWindow(player);
	}

	private void memberClicked(InventoryClickEvent event, PlayerShopMember member) {
		var player = this.getPlayer();
		var shopkeeper = this.getShopkeeperNonNull();

		var memberId = member.getUser().getUniqueId();

		// Players with full access rights: Prevent editing their own membership.
		if (memberId.equals(player.getUniqueId())) {
			TextUtils.sendMessage(player, Messages.cannotEditShopMemberSelf);
			return;
		}

		// The shop shop members or owner might have changed in the meantime:
		if (shopkeeper.isOwner(memberId) || !shopkeeper.isMember(memberId)) {
			TextUtils.sendMessage(player, Messages.cannotEditShopMemberNoLongerAMember);
			this.updateAllViews();
			return;
		}

		if (event.isShiftClick()) {
			// Remove the member after confirmation:
			this.confirmRemoveMember(shopkeeper, player, member);
		} else {
			// Cycle the member's access level:
			var backwards = event.isRightClick();
			var currentAccessLevel = member.getAccessLevel();
			var nextAccessLevel = CollectionUtils.cycleValue(
					allAccessLevels,
					currentAccessLevel,
					backwards,
					value -> value != DefaultPlayerShopAccessLevels.NONE()
							&& (Settings.allowMembersWithFullAccess
									|| value != DefaultPlayerShopAccessLevels.FULL())
			);

			shopkeeper.setAccessLevel(memberId, nextAccessLevel);

			// Close any open view for the affected shop member:
			var memberPlayer = member.getUser().getPlayer();
			if (memberPlayer != null && shopkeeper.closeView(memberPlayer)) {
				TextUtils.sendMessage(player, Messages.yourShopMembershipHasChanged);
			}

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// Save:
			shopkeeper.save();

			// Update any other open member views:
			this.updateAllViews();
			// Note: Updating any open editor views: Not required currently.
		}
	}

	private void confirmRemoveMember(
			AbstractPlayerShopkeeper shopkeeper,
			Player player,
			PlayerShopMember member
	) {
		ConfirmationUI.requestConfirmation(player, new ConfirmationUIState(
				Messages.confirmationUiRemoveShopMemberTitle,
				StringUtils.replaceArguments(Messages.confirmationUiRemoveShopMemberLore,
						"member_name", member.getUser().getName()
				),
				() -> {
					// Removal confirmed.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) {
						// The shopkeeper has already been removed in the meantime.
						TextUtils.sendMessage(player, Messages.shopNoLongerExists);
						return;
					}

					// The player's access permission might have changed in the meantime:
					// Freshly retrieve the view provider, because it might have changed in the
					// meantime:
					var viewProvider = shopkeeper.getViewProvider(DefaultUITypes.SHOP_MEMBERS_EDITOR());
					if (viewProvider == null || viewProvider.canAccess(player, false)) {
						return;
					}

					var memberId = member.getUser().getUniqueId();

					// The shop shop members or owner might have changed in the meantime:
					if (shopkeeper.isOwner(memberId) || !shopkeeper.isMember(memberId)) {
						TextUtils.sendMessage(player, Messages.cannotEditShopMemberNoLongerAMember);
						return;
					}

					// Remove the shop member:
					shopkeeper.removeMember(memberId);

					// Close any open view for the affected shop member:
					var memberPlayer = member.getUser().getPlayer();
					if (memberPlayer != null && shopkeeper.closeView(memberPlayer)) {
						TextUtils.sendMessage(memberPlayer, Messages.yourShopMembershipHasChanged);
					}

					TextUtils.sendMessage(player, Messages.shopMemberRemoved,
							"member_name", member.getUser().getName()
					);

					// Call event:
					Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

					// Save:
					shopkeeper.save();

					// Update any other open member views:
					this.updateAllViews();

					// Re-open the members view:
					shopkeeper.openMembersEditorWindow(player);
				}, () -> {
					// Removal cancelled.
					if (!player.isValid()) return;
					if (!shopkeeper.isValid()) return;

					// Re-open the members view:
					shopkeeper.openMembersEditorWindow(player);
				}
		));
	}
}
