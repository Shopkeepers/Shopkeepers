package com.nisovin.shopkeepers.player;

import java.util.UUID;

import com.nisovin.shopkeepers.util.Validate;

/**
 * Holds identification information about a player (such as the player's unique id and name).
 */
public class PlayerId {

	private final UUID uniqueId; // not null
	private final String name; // not null or empty

	public PlayerId(UUID playerUUID, String playerName) {
		Validate.notNull(playerUUID, "Player uuid is null!");
		Validate.notEmpty(playerName, "Player name is null or empty!");
		this.uniqueId = playerUUID;
		this.name = playerName;

	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerId [uniqueId=");
		builder.append(uniqueId);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + uniqueId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof PlayerId)) return false;
		PlayerId other = (PlayerId) obj;
		if (!name.equals(other.name)) return false;
		if (!uniqueId.equals(other.uniqueId)) return false;
		return true;
	}
}
