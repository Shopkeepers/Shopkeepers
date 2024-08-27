package com.nisovin.shopkeepers.tradelog;

import com.nisovin.shopkeepers.tradelog.data.TradeRecord;

public interface TradeLogger {

	/**
	 * This is invoked once to perform any required one-time setup prior to the first trades being
	 * logged.
	 */
	public void setup();

	/**
	 * Logs the given {@link TradeRecord}.
	 * <p>
	 * The actual writing to storage might happen asynchronously or in batches. Use {@link #flush()}
	 * to force an immediate write of all buffered trade records.
	 * 
	 * @param trade
	 *            the trade
	 */
	public void logTrade(TradeRecord trade);

	/**
	 * Writes any buffered {@link TradeRecord trade records} to storage and waits (blocking!) for
	 * any pending writes to complete.
	 */
	public void flush();
}
