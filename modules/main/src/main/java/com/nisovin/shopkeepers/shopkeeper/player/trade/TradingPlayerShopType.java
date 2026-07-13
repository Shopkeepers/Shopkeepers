package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopType;
import com.nisovin.shopkeepers.text.Text;

public final class TradingPlayerShopType extends AbstractPlayerShopType<SKTradingPlayerShopkeeper> {

	public TradingPlayerShopType() {
		super(
				"trade",
				Arrays.asList("trading"),
				ShopkeepersPlugin.PLAYER_TRADE_PERMISSION,
				SKTradingPlayerShopkeeper.class
		);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeTrading;
	}

	@Override
	public Text getDescriptionText() {
		return Messages.shopTypeDescTrading;
	}

	@Override
	public Text getSetupDescriptionText() {
		return Messages.shopSetupDescTrading;
	}

	@Override
	public List<? extends String> getTradeSetupDescription() {
		return Messages.tradeSetupDescTrading;
	}

	@Override
	protected SKTradingPlayerShopkeeper createNewShopkeeper() {
		return new SKTradingPlayerShopkeeper();
	}
}
