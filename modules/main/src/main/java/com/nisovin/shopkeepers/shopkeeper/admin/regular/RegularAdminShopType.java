package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopType;
import com.nisovin.shopkeepers.text.Text;

public final class RegularAdminShopType
		extends AbstractAdminShopType<SKRegularAdminShopkeeper> {

	public RegularAdminShopType() {
		super(
				"admin",
				Collections.emptyList(),
				ShopkeepersPlugin.ADMIN_PERMISSION,
				SKRegularAdminShopkeeper.class
		);
	}

	@Override
	public String getDisplayName() {
		return Messages.shopTypeAdminRegular;
	}

	@Override
	public Text getDescriptionText() {
		return Messages.shopTypeDescAdminRegular;
	}

	@Override
	public Text getSetupDescriptionText() {
		return Messages.shopSetupDescAdminRegular;
	}

	@Override
	public List<? extends String> getTradeSetupDescription() {
		return Messages.tradeSetupDescAdminRegular;
	}

	@Override
	protected SKRegularAdminShopkeeper createNewShopkeeper() {
		return new SKRegularAdminShopkeeper();
	}
}
