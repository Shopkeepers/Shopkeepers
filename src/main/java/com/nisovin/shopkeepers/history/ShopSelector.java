package com.nisovin.shopkeepers.history;

import java.util.UUID;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public interface ShopSelector {

	public static final ShopSelector ALL = new ShopSelector() {
		@Override
		public String toString() {
			return "ShopSelector.ALL";
		}
	};

	public static final ShopSelector ADMIN_SHOPS = new ShopSelector() {
		@Override
		public String toString() {
			return "ShopSelector.ADMIN_SHOPS";
		}
	};

	public static final ShopSelector PLAYER_SHOPS = new ShopSelector() {
		@Override
		public String toString() {
			return "ShopSelector.PLAYER_SHOPS";
		}
	};

	public static abstract class ByShop implements ShopSelector {

		private final UUID ownerUUID; // null to ignore owner

		public ByShop(UUID ownerUUID) {
			this.ownerUUID = ownerUUID;
		}

		/**
		 * The owner uuid.
		 * 
		 * @return the owner uuid, or <code>null</code> to ignore the owner
		 */
		public UUID getOwnerUUID() {
			return ownerUUID;
		}

		public abstract String getShopIdentifier();
	}

	public static class ByShopUUID extends ByShop {

		private final UUID shopUUID;

		public ByShopUUID(UUID shopUUID) {
			this(shopUUID, null);
		}

		public ByShopUUID(UUID shopUUID, UUID ownerUUID) {
			super(ownerUUID);
			Validate.notNull(shopUUID, "Shop uuid is null!");
			this.shopUUID = shopUUID;
		}

		public UUID getShopUUID() {
			return shopUUID;
		}

		@Override
		public String getShopIdentifier() {
			return shopUUID.toString();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ShopSelector.ByShopUUID [shopUUID=");
			builder.append(shopUUID);
			builder.append(", ownerUUID=");
			builder.append(this.getOwnerUUID());
			builder.append("]");
			return builder.toString();
		}
	}

	public static class ByExistingShop extends ByShopUUID {

		private final Shopkeeper shopkeeper;

		public ByExistingShop(Shopkeeper shopkeeper) {
			this(shopkeeper, null);
		}

		public ByExistingShop(Shopkeeper shopkeeper, UUID ownerUUID) {
			super(Validate.notNull(shopkeeper).getUniqueId(), ownerUUID);
			this.shopkeeper = shopkeeper;
		}

		public Shopkeeper getShopkeeper() {
			return shopkeeper;
		}

		// TODO use the identifier from the command instead?
		@Override
		public String getShopIdentifier() {
			// name (if set), else the shop's session id:
			String id = shopkeeper.getName(); // can be empty
			if (id.isEmpty()) {
				id = String.valueOf(shopkeeper.getId());
			}
			return id;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ShopSelector.ByExistingShop [shopUUID=");
			builder.append(shopkeeper.getUniqueId());
			builder.append(", shopName=");
			builder.append(shopkeeper.getName());
			builder.append(", shopId=");
			builder.append(shopkeeper.getId());
			builder.append(", ownerUUID=");
			builder.append(this.getOwnerUUID());
			builder.append("]");
			return builder.toString();
		}
	}

	// Since shop names are not unique, this might return trades corresponding to different shops.
	// TODO Print a note if the result contains trades belonging to different shops with that name?
	public static class ByShopName extends ByShop {

		private final String shopName;

		public ByShopName(String shopName) {
			this(shopName, null);
		}

		public ByShopName(String shopName, UUID ownerUUID) {
			super(ownerUUID);
			Validate.notEmpty(shopName, "Shop name is empty!");
			this.shopName = shopName;
		}

		public String getShopName() {
			return shopName;
		}

		@Override
		public String getShopIdentifier() {
			return shopName;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ShopSelector.ByShopName [shopName=");
			builder.append(shopName);
			builder.append(", ownerUUID=");
			builder.append(this.getOwnerUUID());
			builder.append("]");
			return builder.toString();
		}
	}

	public static interface ByOwner extends ShopSelector {

		public String getOwnerIdentifier();
	}

	public static class ByOwnerUUID implements ByOwner {

		private final UUID ownerUUID;

		public ByOwnerUUID(UUID ownerUUID) {
			Validate.notNull(ownerUUID, "Owner uuid is null!");
			this.ownerUUID = ownerUUID;
		}

		public UUID getOwnerUUID() {
			return ownerUUID;
		}

		@Override
		public String getOwnerIdentifier() {
			return ownerUUID.toString();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ShopSelector.ByOwnerUUID [ownerUUID=");
			builder.append(ownerUUID);
			builder.append("]");
			return builder.toString();
		}
	}

	// Shortcut to first lookup the owner by name. If multiple players used that name in the past, this will only show
	// results for the latest known player with that name.
	// TODO perform the name lookup manually and print a warning / uuid list if there are multiple matching players?
	public static class ByOwnerName implements ByOwner {

		private final String ownerName;

		public ByOwnerName(String ownerName) {
			Validate.notEmpty(ownerName, "Owner name is empty!");
			this.ownerName = ownerName;
		}

		public String getOwnerName() {
			return ownerName;
		}

		@Override
		public String getOwnerIdentifier() {
			return ownerName;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ShopSelector.ByOwnerName [ownerName=");
			builder.append(ownerName);
			builder.append("]");
			return builder.toString();
		}
	}
}
