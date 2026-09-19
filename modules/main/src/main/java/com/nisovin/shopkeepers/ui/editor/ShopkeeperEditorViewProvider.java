package com.nisovin.shopkeepers.ui.editor;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.ShopkeeperViewContext;
import com.nisovin.shopkeepers.ui.ShopkeeperViewProvider;
import com.nisovin.shopkeepers.ui.lib.AbstractUIType;

public abstract class ShopkeeperEditorViewProvider extends AbstractEditorViewProvider
		implements ShopkeeperViewProvider {

	protected ShopkeeperEditorViewProvider(
			AbstractUIType uiType,
			AbstractShopkeeper shopkeeper,
			TradingRecipesAdapter tradingRecipesAdapter
	) {
		super(uiType, new ShopkeeperViewContext(shopkeeper), tradingRecipesAdapter);
	}

	@Override
	public ShopkeeperViewContext getContext() {
		return (ShopkeeperViewContext) super.getContext();
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return this.getContext().getObject();
	}
}
