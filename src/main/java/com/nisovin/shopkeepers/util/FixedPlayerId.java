package com.nisovin.shopkeepers.util;

import java.util.UUID;

import org.bukkit.entity.Player;

public class FixedPlayerId extends AbstractPlayerId {

	private final UUID uniqueId; // not null
	private final String name; // null if unknown
	private final String displayName; // null if unknown

	public FixedPlayerId(Player player) {
		this(player.getUniqueId(), player.getName(), player.getDisplayName());
	}

	public FixedPlayerId(UUID uniqueId, String name, String displayName) {
		Validate.notNull(uniqueId, "uniqueId is null");
		this.uniqueId = uniqueId;
		this.name = name;
		this.displayName = displayName;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}
}
