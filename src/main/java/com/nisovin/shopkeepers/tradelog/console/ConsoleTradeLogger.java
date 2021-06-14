package com.nisovin.shopkeepers.tradelog.console;

import com.nisovin.shopkeepers.tradelog.TradeLogger;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.Log;

/**
 * Logs trades to the console.
 */
public class ConsoleTradeLogger implements TradeLogger {

	public ConsoleTradeLogger() {
	}

	@Override
	public void logTrade(TradeRecord trade) {
		String message = "";// TODO
		//Log.info(message);
	}

	@Override
	public void flush() {
		// This TradeLogger does not use buffers.
	}
}
