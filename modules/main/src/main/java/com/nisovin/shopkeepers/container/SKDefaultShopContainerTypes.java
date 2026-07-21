package com.nisovin.shopkeepers.container;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.container.DefaultShopContainerTypes;
import com.nisovin.shopkeepers.lang.Messages;

/**
 * The {@link DefaultShopContainerTypes}.
 */
public class SKDefaultShopContainerTypes implements DefaultShopContainerTypes {

	private final SKShopContainerType stock = new SKShopContainerType(
			"stock",
			true,
			false,
			() -> Messages.shopContainerTypeStock,
			() -> Messages.shopContainerTypeDescriptionStock
	);
	private final SKShopContainerType earnings = new SKShopContainerType(
			"earnings",
			false,
			true,
			() -> Messages.shopContainerTypeEarnings,
			() -> Messages.shopContainerTypeDescriptionEarnings
	);
	private final SKShopContainerType stockAndEarnings = new SKShopContainerType(
			"stock-and-earnings",
			true,
			true,
			() -> Messages.shopContainerTypeStockAndEarnings,
			() -> Messages.shopContainerTypeDescriptionStockAndEarnings
	);

	public SKDefaultShopContainerTypes() {
	}

	@Override
	public List<? extends SKShopContainerType> getAllContainerTypes() {
		List<SKShopContainerType> defaults = new ArrayList<>();
		defaults.add(stock);
		defaults.add(earnings);
		defaults.add(stockAndEarnings);
		return defaults;
	}

	@Override
	public SKShopContainerType getStock() {
		return stock;
	}

	@Override
	public SKShopContainerType getEarnings() {
		return earnings;
	}

	@Override
	public SKShopContainerType getStockAndEarnings() {
		return stockAndEarnings;
	}

	// STATICS (for convenience):

	public static SKDefaultShopContainerTypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultShopContainerTypes();
	}

	public static SKShopContainerType STOCK() {
		return getInstance().getStock();
	}

	public static SKShopContainerType EARNINGS() {
		return getInstance().getEarnings();
	}

	public static SKShopContainerType STOCK_AND_EARNINGS() {
		return getInstance().getStockAndEarnings();
	}
}
