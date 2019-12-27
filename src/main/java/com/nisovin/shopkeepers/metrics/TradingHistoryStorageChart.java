package com.nisovin.shopkeepers.metrics;

import java.util.Locale;

import org.bstats.bukkit.Metrics;

import com.nisovin.shopkeepers.Settings;

/**
 * Reports the storage type being used for the trading history.
 */
public class TradingHistoryStorageChart extends Metrics.SimplePie {

	public TradingHistoryStorageChart() {
		super("trading_history_storage", () -> {
			if (!Settings.enableTradingHistory) {
				return "Disabled";
			}
			return Settings.storageType.toLowerCase(Locale.ROOT);
		});
	}
}
