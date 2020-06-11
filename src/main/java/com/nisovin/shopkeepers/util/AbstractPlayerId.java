package com.nisovin.shopkeepers.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Abstract base class for implementations of {@link PlayerId}.
 */
public abstract class AbstractPlayerId implements PlayerId {

	@Override
	public abstract UUID getUniqueId();

	@Override
	public abstract String getName();

	@Override
	public abstract String getDisplayName();

	@Override
	public String getDisplayNameOrName() {
		String displayName = this.getDisplayName();
		if (displayName != null) {
			return displayName;
		} else {
			return this.getName(); // may also be null
		}
	}

	@Override
	public boolean isOnline() {
		return (this.getPlayer() != null);
	}

	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(this.getUniqueId());
	}

	@Override
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(this.getUniqueId()); // non-blocking
	}

	@Override
	public String toPrettyString() {
		return TextUtils.getPlayerString(this.getName(), this.getUniqueId());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerId [uniqueId=");
		builder.append(this.getUniqueId());
		builder.append(", name=");
		builder.append(this.getName());
		builder.append(", displayName=");
		builder.append(this.getDisplayName());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getUniqueId().hashCode();
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof PlayerId)) return false;
		PlayerId other = (PlayerId) obj;
		if (!this.getUniqueId().equals(other.getUniqueId())) return false;
		return true;
	}
}
