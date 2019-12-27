package com.nisovin.shopkeepers.history;

import java.time.Instant;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.util.Validate;

public class LoggedTrade {

	public static LoggedTrade newLoggedTrade(ShopkeeperTradeEvent tradeEvent) {
		Validate.notNull(tradeEvent, "Trade event is null!");
		Instant timestamp = Instant.now();
		// new profile, gets used if missing in storage:
		PlayerProfile player = PlayerProfile.newProfile(tradeEvent.getPlayer());
		Shopkeeper shopkeeper = tradeEvent.getShopkeeper();
		ShopInfo shopInfo = ShopInfo.newShopInfo(shopkeeper);
		ItemInfo item1 = ItemInfo.newItemInfo(tradeEvent.getOfferedItem1());
		ItemInfo item2 = ItemInfo.newItemInfo(tradeEvent.getOfferedItem2());
		ItemInfo resultItem = ItemInfo.newItemInfo(tradeEvent.getTradingRecipe().getResultItem());
		return new LoggedTrade(timestamp, player, shopInfo, item1, item2, resultItem);
	}

	private final Instant timestamp;
	private final PlayerProfile player;
	private final ShopInfo shop; // not null
	// The offered items matching the first and second items required by the trade. The items might not necessarily be
	// equal the items required by the trade (they only have to 'match' / be accepted).
	// The order in which the player supplied the items in the trading interface is not getting recorded.
	private final ItemInfo item1; // not null
	private final ItemInfo item2; // can be null
	private final ItemInfo resultItem; // not null

	public LoggedTrade(Instant timestamp, PlayerProfile player, ShopInfo shop, ItemInfo item1, ItemInfo item2, ItemInfo resultItem) {
		Validate.notNull(timestamp);
		Validate.notNull(player);
		Validate.notNull(shop);
		Validate.notNull(item1);
		Validate.notNull(resultItem);
		this.timestamp = timestamp;
		this.player = player;
		this.shop = shop;
		this.item1 = item1;
		this.item2 = item2;
		this.resultItem = resultItem;
	}

	/**
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the trading player
	 */
	public PlayerProfile getPlayer() {
		return player;
	}

	/**
	 * @return the shop info
	 */
	public ShopInfo getShop() {
		return shop;
	}

	/**
	 * @return the first item involved in the trade, not <code>null</code>
	 */
	public ItemInfo getItem1() {
		return item1;
	}

	/**
	 * @return the second item involved in the trade, can be <code>null</code>
	 */
	public ItemInfo getItem2() {
		return item2;
	}

	/**
	 * @return the result item, not <code>null</code>
	 */
	public ItemInfo getResultItem() {
		return resultItem;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoggedTrade [timestamp=");
		builder.append(timestamp);
		builder.append(", player=");
		builder.append(player);
		builder.append(", shop=");
		builder.append(shop);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append(", resultItem=");
		builder.append(resultItem);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item1 == null) ? 0 : item1.hashCode());
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
		result = prime * result + ((shop == null) ? 0 : shop.hashCode());
		result = prime * result + ((resultItem == null) ? 0 : resultItem.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((player == null) ? 0 : player.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof LoggedTrade)) return false;
		LoggedTrade other = (LoggedTrade) obj;
		if (item1 == null) {
			if (other.item1 != null) return false;
		} else if (!item1.equals(other.item1)) return false;
		if (item2 == null) {
			if (other.item2 != null) return false;
		} else if (!item2.equals(other.item2)) return false;
		if (shop == null) {
			if (other.shop != null) return false;
		} else if (!shop.equals(other.shop)) return false;
		if (resultItem == null) {
			if (other.resultItem != null) return false;
		} else if (!resultItem.equals(other.resultItem)) return false;
		if (timestamp == null) {
			if (other.timestamp != null) return false;
		} else if (!timestamp.equals(other.timestamp)) return false;
		if (player == null) {
			if (other.player != null) return false;
		} else if (!player.equals(other.player)) return false;
		return true;
	}
}
