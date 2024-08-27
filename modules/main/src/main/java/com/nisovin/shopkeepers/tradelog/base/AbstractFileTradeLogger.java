package com.nisovin.shopkeepers.tradelog.base;

import java.nio.file.Path;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.tradelog.TradeLogStorageType;
import com.nisovin.shopkeepers.tradelog.TradeLogger;

/**
 * Base class for file-based {@link TradeLogger}s with a single concurrent writer.
 */
public abstract class AbstractFileTradeLogger extends AbstractSingleWriterTradeLogger {

	/**
	 * The directory inside the plugin folder to store trade logs in.
	 */
	public static final String TRADE_LOGS_FOLDER = "trade-logs";

	/**
	 * The path of the trade logs directory.
	 */
	protected final Path tradeLogsFolder;

	public AbstractFileTradeLogger(Plugin plugin, TradeLogStorageType storageType) {
		super(plugin, storageType);

		this.tradeLogsFolder = plugin.getDataFolder().toPath().resolve(TRADE_LOGS_FOLDER);
	}
}
