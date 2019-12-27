package com.nisovin.shopkeepers.history;

import org.bukkit.World;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.Validate;

public class WorldInfo {

	// world can be null (for virtual shopkeepers)
	public static WorldInfo newWorldInfo(World world) {
		return newWorldInfo(world == null ? null : world.getName());
	}

	public static WorldInfo newWorldInfo(String worldName) {
		return new WorldInfo(Settings.serverId, worldName);
	}

	private final String serverId;
	// name can be null for 'virtual' shopkeepers, that are not located in any world
	private final String worldName;

	public WorldInfo(String serverId, String worldName) {
		Validate.notEmpty(serverId, "Server id is empty!");
		if (worldName != null) {
			Validate.notEmpty(worldName, "World name is empty!");
		}
		this.serverId = serverId;
		this.worldName = worldName;
	}

	/**
	 * @return the serverId
	 */
	public String getServerId() {
		return serverId;
	}

	public boolean hasWorld() {
		return (worldName != null);
	}

	/**
	 * @return the worldName, can be <code>null</code>
	 */
	public String getWorldName() {
		return worldName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WorldInfo [serverId=");
		builder.append(serverId);
		builder.append(", worldName=");
		builder.append(worldName);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
		result = prime * result + ((worldName == null) ? 0 : worldName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof WorldInfo)) return false;
		WorldInfo other = (WorldInfo) obj;
		if (serverId == null) {
			if (other.serverId != null) return false;
		} else if (!serverId.equals(other.serverId)) return false;
		if (worldName == null) {
			if (other.worldName != null) return false;
		} else if (!worldName.equals(other.worldName)) return false;
		return true;
	}
}
