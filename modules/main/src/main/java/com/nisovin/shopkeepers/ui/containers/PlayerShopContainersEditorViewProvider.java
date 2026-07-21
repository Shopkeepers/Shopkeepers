package com.nisovin.shopkeepers.ui.containers;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.player.members.DefaultPlayerShopAccessLevels;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractShopkeeperViewProvider;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;

public class PlayerShopContainersEditorViewProvider extends AbstractShopkeeperViewProvider {

	public PlayerShopContainersEditorViewProvider(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.SHOP_CONTAINERS_EDITOR(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canAccess(Player player, boolean silent) {
		// Note: The parent editor itself is already only accessible with access level "edit", so it
		// makes no sense to support "container" access here for viewing the containers.
		if (!this.getShopkeeper().checkAccess(player, DefaultPlayerShopAccessLevels.EDIT(), silent)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Player has no access.");
			}
			return false;
		}

		return true;
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new PlayerShopContainersEditorView(this, player, uiState);
	}
}
