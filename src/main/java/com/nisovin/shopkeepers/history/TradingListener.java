package com.nisovin.shopkeepers.history;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;

class TradingListener implements Listener {

	private final TradingHistory tradingHistory;

	TradingListener(TradingHistory tradingHistory) {
		this.tradingHistory = tradingHistory;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTradeCompleted(ShopkeeperTradeEvent event) {
		tradingHistory.logTrade(event);
	}
}
