package com.nisovin.shopkeepers.api.shopkeeper.container;

import com.nisovin.shopkeepers.api.types.Type;

/**
 * A type of {@link ShopContainer}, defining the role a container fulfills for a shopkeeper.
 * <p>
 * A container's type determines for example whether the shop stores its stock and/or the earnings
 * of its trades in the container.
 */
public interface ShopContainerType extends Type {

	/**
	 * Checks whether containers of this type store the shop's stock, i.e. the items the shop hands
	 * out during trades.
	 * 
	 * @return <code>true</code> if this type stores stock
	 */
	public boolean isStock();

	/**
	 * Checks whether containers of this type store the earnings of trades, i.e. the items that
	 * customers hand over to the shop.
	 * 
	 * @return <code>true</code> if this type stores earnings
	 */
	public boolean isEarnings();
}
