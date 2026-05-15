package com.nisovin.shopkeepers.shopkeeper.player.members;

import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevelRegistry;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;

public final class SKPlayerShopAccessLevelRegistry extends AbstractTypeRegistry<SKPlayerShopAccessLevel>
		implements PlayerShopAccessLevelRegistry<SKPlayerShopAccessLevel> {

	public SKPlayerShopAccessLevelRegistry() {
	}

	@Override
	protected String getTypeName() {
		return "player shop access level";
	}
}
