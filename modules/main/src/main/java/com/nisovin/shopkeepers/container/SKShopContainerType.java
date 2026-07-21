package com.nisovin.shopkeepers.container;

import java.util.function.Supplier;

import com.nisovin.shopkeepers.api.shopkeeper.container.ShopContainerType;
import com.nisovin.shopkeepers.types.AbstractType;

public final class SKShopContainerType extends AbstractType implements ShopContainerType {

	private final boolean isStock;
	private final boolean isEarnings;
	private final Supplier<String> displayName;
	private final Supplier<String> description;

	public SKShopContainerType(
			String identifier,
			boolean isStock,
			boolean isEarnings,
			Supplier<String> displayName,
			Supplier<String> description
	) {
		super(identifier, null);
		this.isStock = isStock;
		this.isEarnings = isEarnings;
		this.displayName = displayName;
		this.description = description;
	}

	@Override
	public boolean isStock() {
		return isStock;
	}

	@Override
	public boolean isEarnings() {
		return isEarnings;
	}

	@Override
	public String getDisplayName() {
		return displayName.get();
	}

	public String getDescription() {
		return description.get();
	}
}
