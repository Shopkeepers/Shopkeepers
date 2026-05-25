package com.nisovin.shopkeepers.ui.members;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractShopkeeperViewProvider;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;

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
		if (!this.getShopkeeper().checkAccess(player, DefaultPlayerShopAccessLevels.FULL(), silent)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Player has no access.");
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
