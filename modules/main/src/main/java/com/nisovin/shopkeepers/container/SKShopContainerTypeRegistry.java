package com.nisovin.shopkeepers.container;

import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerTypeRegistry;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;

public final class SKShopContainerTypeRegistry extends AbstractTypeRegistry<SKShopContainerType>
		implements ShopContainerTypeRegistry<SKShopContainerType> {

	public SKShopContainerTypeRegistry() {
	}

	@Override
	protected String getTypeName() {
		return "shop container type";
	}
}
