package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.lib.UIState;
import com.nisovin.shopkeepers.ui.lib.View;
import com.nisovin.shopkeepers.ui.trading.TradingViewProvider;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public abstract class PlayerShopTradingViewProvider extends TradingViewProvider {

	protected PlayerShopTradingViewProvider(AbstractPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;

		var shopkeeper = this.getShopkeeper();

		// Stop opening if trading shall be prevented while a member is online:
		if (Settings.preventTradingWhileMemberIsOnline
				&& !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			@Nullable Player memberPlayer = shopkeeper.getFirstOnlineMember();
			if (memberPlayer != null) {
				if (!silent) {
					this.debugNotOpeningUI(player, "Shop member is online: "
							+ Unsafe.assertNonNull(memberPlayer.getName()));
					TextUtils.sendMessage(player, Messages.cannotTradeWhileMemberOnline,
							"member", Unsafe.assertNonNull(memberPlayer.getName())
					);
				}
				return false;
			}
		}

		return true;
	}

	@Override
	protected @Nullable View createView(Player player, UIState uiState) {
		return new PlayerShopTradingView(this, player, uiState);
	}
}
