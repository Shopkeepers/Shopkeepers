package com.nisovin.shopkeepers.history;

import java.util.List;

import com.nisovin.shopkeepers.player.profile.PlayerProfile;
import com.nisovin.shopkeepers.util.Validate;

public class HistoryResult {

	private final PlayerProfile tradingPlayer; // null if unspecified (eg. all players) or not found
	private final PlayerProfile owner; // null if unspecified (eg. all shops or admin shops) or not found
	// TODO shopkeeper data?

	private final List<LoggedTrade> loggedTrades;
	private final int totalTradesCount;

	public HistoryResult(PlayerProfile tradingPlayer, PlayerProfile owner, List<LoggedTrade> loggedTrades, int totalTradesCount) {
		Validate.notNull(loggedTrades, "Logged trades is null!");
		Validate.noNullElements(loggedTrades, "Logged trades cannot contain null!");
		Validate.isTrue(totalTradesCount >= 0, "Total trades count cannot be negative!");
		this.tradingPlayer = tradingPlayer;
		this.owner = owner;
		this.loggedTrades = loggedTrades;
		this.totalTradesCount = totalTradesCount;
	}

	/**
	 * @return the tradingPlayer, or <code>null</code> if no player has been specified in the history request or no
	 *         corresponding player profile has been found
	 */
	public PlayerProfile getTradingPlayer() {
		return tradingPlayer;
	}

	/**
	 * @return the owner, or <code>null</code> if no owner has been specified in the history request or no corresponding
	 *         player profile has been found
	 */
	public PlayerProfile getOwner() {
		return owner;
	}

	/**
	 * @return the loggedTrades, not <code>null</code> but can be empty
	 */
	public List<LoggedTrade> getLoggedTrades() {
		return loggedTrades;
	}

	/**
	 * @return the total number of matching trades
	 */
	public int getTotalTradesCount() {
		return totalTradesCount;
	}
}
