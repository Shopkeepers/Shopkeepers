package com.nisovin.shopkeepers.members;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.input.InputRequest;
import com.nisovin.shopkeepers.input.chat.ChatInput;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperMembers {

  private enum MemberAction {
    ADD,
    REMOVE
  }

  private class MemberInputRequest implements InputRequest<String> {

    private final Player player;
    private final AbstractPlayerShopkeeper shopkeeper;
    private final MemberAction action;

    MemberInputRequest(Player player, AbstractPlayerShopkeeper shopkeeper, MemberAction action) {
      assert player != null && shopkeeper != null && action != null;
      this.player = player;
      this.shopkeeper = shopkeeper;
      this.action = action;
    }

    @Override
    public void onInput(String message) {
      if (!shopkeeper.isValid())
        return;

      String input = message.trim();
      if (input.equals("-")) {
        // Cancelled
        return;
      }

      if (action == MemberAction.ADD) {
        handleAddMember(player, shopkeeper, input);
      } else {
        handleRemoveMember(player, shopkeeper, input);
      }
    }
  }

  private final ChatInput chatInput;

  public ShopkeeperMembers(ChatInput chatInput) {
    Validate.notNull(chatInput, "chatInput is null");
    this.chatInput = chatInput;
  }

  public void onEnable() {
  }

  public void onDisable() {
  }

  public void startAddMember(Player player, AbstractPlayerShopkeeper shopkeeper) {
    Validate.notNull(player, "player is null");
    Validate.notNull(shopkeeper, "shopkeeper is null");
    chatInput.request(player, new MemberInputRequest(player, shopkeeper, MemberAction.ADD));
  }

  public void startRemoveMember(Player player, AbstractPlayerShopkeeper shopkeeper) {
    Validate.notNull(player, "player is null");
    Validate.notNull(shopkeeper, "shopkeeper is null");
    chatInput.request(player, new MemberInputRequest(player, shopkeeper, MemberAction.REMOVE));
  }

  public void abortMemberInput(Player player) {
    Validate.notNull(player, "player is null");
    InputRequest<String> request = chatInput.getRequest(player);
    if (request instanceof MemberInputRequest) {
      chatInput.abortRequest(player, request);
    }
  }

  @SuppressWarnings("deprecation")
  private void handleAddMember(Player player, AbstractPlayerShopkeeper shopkeeper, String playerName) {
    // Try to resolve the player:
    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
    String targetName = targetPlayer.getName();
    UUID targetUUID = targetPlayer.getUniqueId();

    if (targetName == null) {
      TextUtils.sendMessage(player, Messages.memberPlayerNotFound, "player", playerName);
      return;
    }

    // Check if this is the owner:
    if (targetUUID.equals(shopkeeper.getOwnerUUID())) {
      TextUtils.sendMessage(player, Messages.memberIsOwner);
      return;
    }

    // Check if already a member:
    if (shopkeeper.isMember(targetUUID)) {
      TextUtils.sendMessage(player, Messages.memberAlreadyAdded, "member", targetName);
      return;
    }

    // Try to add:
    boolean added = shopkeeper.addMember(targetUUID, targetName);
    if (!added) {
      TextUtils.sendMessage(player, Messages.memberLimitReached);
      return;
    }

    TextUtils.sendMessage(player, Messages.memberAdded, "member", targetName);

    // Call event:
    Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

    // Save:
    shopkeeper.save();
  }

  private void handleRemoveMember(Player player, AbstractPlayerShopkeeper shopkeeper, String playerName) {
    // Find the member by name:
    List<? extends User> members = shopkeeper.getMembers();
    User targetMember = null;
    for (User member : members) {
      if (member.getLastKnownName().equalsIgnoreCase(playerName)) {
        targetMember = member;
        break;
      }
    }

    if (targetMember == null) {
      TextUtils.sendMessage(player, Messages.memberNotFound, "member", playerName);
      return;
    }

    shopkeeper.removeMember(targetMember.getUniqueId());
    TextUtils.sendMessage(player, Messages.memberRemoved, "member", targetMember.getLastKnownName());

    // Call event:
    Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

    // Save:
    shopkeeper.save();
  }

  public void listMembers(Player player, PlayerShopkeeper shopkeeper) {
    List<? extends User> members = shopkeeper.getMembers();
    if (members.isEmpty()) {
      TextUtils.sendMessage(player, Messages.membersListEmpty);
      return;
    }

    TextUtils.sendMessage(player, Messages.membersListHeader);
    for (User member : members) {
      TextUtils.sendMessage(player, Messages.membersListEntry, "member", member.getLastKnownName());
    }
  }
}
