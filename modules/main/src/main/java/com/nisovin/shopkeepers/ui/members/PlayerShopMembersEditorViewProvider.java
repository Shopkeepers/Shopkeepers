package com.nisovin.shopkeepers.ui.members;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractShopkeeperViewProvider;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public class PlayerShopMembersEditorViewProvider extends AbstractShopkeeperViewProvider {

	public PlayerShopMembersEditorViewProvider(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.SHOP_MEMBERS_EDITOR(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canAccess(Player player, boolean silent) {
		// Check access:
		if (!this.getShopkeeper().hasAccessLevel(player, DefaultPlayerShopAccessLevels.FULL())
				&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Missing member permission.");
				TextUtils.sendMessage(player, Messages.notAllowedToEditShopMembers);
			}
			return false;
		}

		return true;
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new PlayerShopMembersEditorView(this, player, uiState);
	}
}
