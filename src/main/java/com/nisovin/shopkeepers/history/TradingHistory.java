package com.nisovin.shopkeepers.history;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.history.storage.HistoryStorage;
import com.nisovin.shopkeepers.storage.SKStorage;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.VoidCallable;

public class TradingHistory {

	private final SKShopkeepersPlugin plugin;
	private final TradingListener tradingListener = new TradingListener(this);

	private SKStorage storage;
	private HistoryStorage historyStorage = null;

	public TradingHistory(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin);
		this.plugin = plugin;
	}

	public void onEnable() {
		// get storage:
		this.storage = plugin.getStorage();
		Validate.State.notNull(storage, "Cannot enable TradingHistory if storage is disabled!");
		this.historyStorage = storage.getHistoryStorage();

		// register listener:
		Bukkit.getPluginManager().registerEvents(tradingListener, plugin);

		// startup tasks: TODO lock database while this is on progress? Run sync?
		// TODO what if multiple servers try to perform these tasks at the same time? Can they interfere?
		// mysql: https://stackoverflow.com/questions/31819282/select-for-update-to-lock-database-during-migration
		// sqlite: https://www.sqlite.org/lang_transaction.html
		storage.addTask((VoidCallable) () -> historyStorage.onStartup());
	}

	public void onDisable() {
		this.validateEnabled();
		// unregister listener:
		HandlerList.unregisterAll(tradingListener);

		this.storage = null;
		this.historyStorage = null;
	}

	public boolean isEnabled() {
		return (storage != null);
	}

	private void validateEnabled() {
		Validate.State.isTrue(this.isEnabled(), "Trading history is disabled!");
	}

	void logTrade(ShopkeeperTradeEvent tradeEvent) {
		LoggedTrade loggedTrade = LoggedTrade.newLoggedTrade(tradeEvent);
		this.logTrade(loggedTrade);
	}

	public void logTrade(LoggedTrade loggedTrade) {
		this.validateEnabled();
		Validate.notNull(loggedTrade);
		storage.addTask((VoidCallable) () -> historyStorage.logTrade(loggedTrade));
	}

	public CompletableFuture<Void> purgeTradesOlderThan(Duration duration) {
		this.validateEnabled();
		return storage.addTask((VoidCallable) () -> historyStorage.purgeTradesOlderThan(duration));
	}

	/**
	 * Searches for logged trades matching the given request criteria.
	 * 
	 * @param request
	 *            the request
	 * @return the history result
	 */
	public CompletableFuture<HistoryResult> getTradingHistory(HistoryRequest request) {
		this.validateEnabled();
		return storage.addTask(() -> historyStorage.getTradingHistory(request));
	}
}
