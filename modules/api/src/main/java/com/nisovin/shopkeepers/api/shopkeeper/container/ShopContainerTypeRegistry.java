package com.nisovin.shopkeepers.api.shopkeeper.container;

import com.nisovin.shopkeepers.api.types.TypeRegistry;

/**
 * A {@link TypeRegistry} of {@link ShopContainerType}.
 * 
 * @param <T>
 *            the internal type of {@link ShopContainerType} managed by this registry
 */
public interface ShopContainerTypeRegistry<T extends ShopContainerType> extends TypeRegistry<T> {
}
