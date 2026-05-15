package com.nisovin.shopkeepers.api.shopkeeper.player.members;

import com.nisovin.shopkeepers.api.types.TypeRegistry;

/**
 * A {@link TypeRegistry} of {@link PlayerShopAccessLevel}.
 * 
 * @param <T>
 *            the internal type of {@link PlayerShopAccessLevel} managed by this registry
 */
public interface PlayerShopAccessLevelRegistry<T extends PlayerShopAccessLevel>
		extends TypeRegistry<T> {
}
