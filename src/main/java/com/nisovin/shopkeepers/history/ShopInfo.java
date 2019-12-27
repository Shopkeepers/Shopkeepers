package com.nisovin.shopkeepers.history;

import java.util.UUID;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.util.Validate;

public class ShopInfo {

	public static ShopInfo newShopInfo(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return null;
		UUID shopUUID = shopkeeper.getUniqueId();
		String shopTypeId = shopkeeper.getType().getIdentifier();
		PlayerShopkeeper playerShop = (shopkeeper instanceof PlayerShopkeeper) ? (PlayerShopkeeper) shopkeeper : null;
		// new profile, gets used if missing in storage:
		PlayerProfile owner = (playerShop == null) ? null : PlayerProfile.newProfile(playerShop.getOwnerUUID(), playerShop.getOwnerName());
		String shopName = shopkeeper.getName(); // can be empty
		WorldInfo world = WorldInfo.newWorldInfo(shopkeeper.getWorldName());
		int x = shopkeeper.getX();
		int y = shopkeeper.getY();
		int z = shopkeeper.getZ();
		return new ShopInfo(shopUUID, shopTypeId, owner, shopName, world, x, y, z);
	}

	// Note: Any shopkeeper data might no longer be accurate, because the shop might have been modified or even deleted
	private final UUID uniqueId;
	private final String typeId;
	// The owner might no longer match the current owner of the shopkeeper, if it got transfered in the meantime.
	private final PlayerProfile owner; // can be null (eg. for admin shops)
	private final String name; // can be empty
	private final WorldInfo world; // not null, but may not have a world name
	private final int x;
	private final int y;
	private final int z;

	public ShopInfo(UUID shopUUID, String shopTypeId, PlayerProfile owner, String shopName, WorldInfo world, int x, int y, int z) {
		Validate.notNull(shopUUID, "Shop uuid is null!");
		Validate.notNull(shopTypeId, "Shop type id is null!");
		Validate.notNull(shopName, "Shop name is null!"); // can be empty
		Validate.notNull(world, "World info is null!");
		this.uniqueId = shopUUID;
		this.typeId = shopTypeId;
		this.owner = owner;
		this.name = shopName;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * @return the uniqueId
	 */
	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * @return the shop's type id
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @return the shop's name, can be empty
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the owner, can be <code>null</code>
	 */
	public PlayerProfile getOwner() {
		return owner;
	}

	/**
	 * @return the world info
	 */
	public WorldInfo getWorld() {
		return world;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}

	/**
	 * @return the z
	 */
	public int getZ() {
		return z;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ShopInfo [uniqueId=");
		builder.append(uniqueId);
		builder.append(", typeId=");
		builder.append(typeId);
		builder.append(", owner=");
		builder.append(owner);
		builder.append(", name=");
		builder.append(name);
		builder.append(", world=");
		builder.append(world);
		builder.append(", x=");
		builder.append(x);
		builder.append(", y=");
		builder.append(y);
		builder.append(", z=");
		builder.append(z);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
		result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
		result = prime * result + ((world == null) ? 0 : world.hashCode());
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ShopInfo)) return false;
		ShopInfo other = (ShopInfo) obj;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (owner == null) {
			if (other.owner != null) return false;
		} else if (!owner.equals(other.owner)) return false;
		if (typeId == null) {
			if (other.typeId != null) return false;
		} else if (!typeId.equals(other.typeId)) return false;
		if (uniqueId == null) {
			if (other.uniqueId != null) return false;
		} else if (!uniqueId.equals(other.uniqueId)) return false;
		if (world == null) {
			if (other.world != null) return false;
		} else if (!world.equals(other.world)) return false;
		if (x != other.x) return false;
		if (y != other.y) return false;
		if (z != other.z) return false;
		return true;
	}
}
