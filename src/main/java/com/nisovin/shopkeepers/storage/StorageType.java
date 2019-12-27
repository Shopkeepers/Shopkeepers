package com.nisovin.shopkeepers.storage;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.types.AbstractType;

public abstract class StorageType extends AbstractType {

	public StorageType(String identifier) {
		this(identifier, null);
	}

	public StorageType(String identifier, List<String> aliases) {
		super(identifier, aliases); // does not use permissions
	}

	public abstract SKStorage createStorage(ShopkeepersPlugin plugin);
}
