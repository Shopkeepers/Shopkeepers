package com.nisovin.shopkeepers.shopkeeper.player.members;

import java.util.function.Supplier;

import com.nisovin.shopkeepers.api.shopkeeper.player.members.PlayerShopAccessLevel;
import com.nisovin.shopkeepers.types.AbstractType;

public final class SKPlayerShopAccessLevel extends AbstractType implements PlayerShopAccessLevel {

	private final int accessLevel;
	private final Supplier<String> displayName;
	private final Supplier<String> description;

	public SKPlayerShopAccessLevel(
			String identifier,
			int value,
			Supplier<String> displayName,
			Supplier<String> description
	) {
		super(identifier, null);
		this.accessLevel = value;
		this.displayName = displayName;
		this.description = description;
	}

	private int getAccessLevel() {
		return accessLevel;
	}

	@Override
	public String getDisplayName() {
		return displayName.get();
	}

	public String getDescription() {
		return description.get();
	}

	@Override
	public boolean includes(PlayerShopAccessLevel accessLevel) {
		return accessLevel instanceof SKPlayerShopAccessLevel skAccessLevel
				&& this.getAccessLevel() >= skAccessLevel.getAccessLevel();
	}
}
