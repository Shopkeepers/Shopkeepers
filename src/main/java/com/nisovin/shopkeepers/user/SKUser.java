package com.nisovin.shopkeepers.user;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKUser implements User {

	private final UUID uniqueId; // not null
	private String name; // null if unknown

	SKUser(UUID uniqueId, String name) {
		Validate.notNull(uniqueId, "uniqueId is null!");
		this.uniqueId = uniqueId;
		this.name = StringUtils.getNotEmpty(name); // empty String is normalized to null
	}

	@Override
	public boolean isValid() {
		return ShopkeepersAPI.isEnabled() && ShopkeepersAPI.getUserManager().getUser(uniqueId) == this;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the player's last known name.
	 * <p>
	 * Called by {@link SKUserManager}, for example when the player joins the server.
	 * 
	 * @param newName
	 *            the new name, <code>null</code> to indicate that the name is unknown
	 */
	void setName(String newName) {
		this.name = StringUtils.getNotEmpty(newName); // empty String is normalized to null
	}

	@Override
	public boolean isOnline() {
		return (this.getPlayer() != null);
	}

	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(uniqueId);
	}

	@Override
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(uniqueId); // non-blocking
	}

	@Override
	public String toPrettyString() {
		return TextUtils.getPlayerString(name, uniqueId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKUser [uniqueId=");
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
		result = prime * result + uniqueId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SKUser)) return false;
		SKUser other = (SKUser) obj;
		if (!uniqueId.equals(other.uniqueId)) return false;
		return true;
	}
}
