package com.nisovin.shopkeepers.history.storage;

import java.time.Duration;

import com.nisovin.shopkeepers.history.HistoryRequest;
import com.nisovin.shopkeepers.history.HistoryResult;
import com.nisovin.shopkeepers.history.LoggedTrade;
import com.nisovin.shopkeepers.storage.StorageException;

public interface HistoryStorage {

	/**
	 * Removes logged trades (and their associated data) that are older than the specified duration.
	 * TODO
	 * 
	 * @param duration
	 *            the duration
	 * @throws StorageException
	 *             in case of failure
	 */
	public void purgeTradesOlderThan(Duration duration) throws StorageException;

	/**
	 * Removes any data that is no longer referenced and therefore no longer needed.
	 * TODO
	 * 
	 * @throws StorageException
	 *             in case of failure
	 */
	public void performCleanup() throws StorageException;

	/**
	 * Combines various startup cleanup and migration tasks.
	 * TODO remove from this interface?
	 * 
	 * @throws StorageException
	 *             in case of failure
	 */
	public void onStartup() throws StorageException;

	public void logTrade(LoggedTrade trade) throws StorageException;

	/**
	 * Searches for logged trades matching the given request criteria.
	 * 
	 * @param request
	 *            the request
	 * @return the history result
	 * @throws StorageException
	 *             in case of failure
	 */
	public HistoryResult getTradingHistory(HistoryRequest request) throws StorageException;
}
