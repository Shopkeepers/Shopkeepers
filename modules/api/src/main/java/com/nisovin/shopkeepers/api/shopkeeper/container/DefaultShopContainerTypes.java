package com.nisovin.shopkeepers.api.shopkeeper.container;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * The default built-in {@link ShopContainerType ShopContainerTypes}.
 */
public interface DefaultShopContainerTypes {

	/**
	 * Gets all default {@link ShopContainerType ShopContainerTypes}.
	 * 
	 * @return all default container types
	 */
	public List<? extends ShopContainerType> getAllContainerTypes();

	/**
	 * Gets the "stock" container type: The shop stores its stock in the container, but not the
	 * earnings of its trades.
	 * 
	 * @return the "stock" container type
	 */
	public ShopContainerType getStock();

	/**
	 * Gets the "earnings" container type: The shop stores the earnings of its trades in the
	 * container, but not its stock.
	 * 
	 * @return the "earnings" container type
	 */
	public ShopContainerType getEarnings();

	/**
	 * Gets the "stock and earnings" container type: The shop stores both its stock and the earnings
	 * of its trades in the container.
	 * 
	 * @return the "stock and earnings" container type
	 */
	public ShopContainerType getStockAndEarnings();

	// STATIC ACCESSORS (for convenience)

	/**
	 * Gets the {@link DefaultShopContainerTypes} instance.
	 * 
	 * @return the instance
	 */
	public static DefaultShopContainerTypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultShopContainerTypes();
	}

	/**
	 * Gets the "stock" {@link ShopContainerType}.
	 * 
	 * @return the "stock" {@link ShopContainerType}
	 * @see #getStock()
	 */
	public static ShopContainerType STOCK() {
		return getInstance().getStock();
	}

	/**
	 * Gets the "earnings" {@link ShopContainerType}.
	 * 
	 * @return the "earnings" {@link ShopContainerType}
	 * @see #getEarnings()
	 */
	public static ShopContainerType EARNINGS() {
		return getInstance().getEarnings();
	}

	/**
	 * Gets the "stock and earnings" {@link ShopContainerType}.
	 * 
	 * @return the "stock and earnings" {@link ShopContainerType}
	 * @see #getStockAndEarnings()
	 */
	public static ShopContainerType STOCK_AND_EARNINGS() {
		return getInstance().getStockAndEarnings();
	}
}
