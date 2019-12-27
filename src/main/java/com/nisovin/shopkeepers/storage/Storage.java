package com.nisovin.shopkeepers.storage;

import com.nisovin.shopkeepers.history.storage.HistoryStorage;
import com.nisovin.shopkeepers.player.profile.storage.PlayerStorage;

public interface Storage {

	/**
	 * Gets the {@link StorageType}.
	 * 
	 * @return the storage type
	 */
	public StorageType getType();

	// COMPONENTS

	/**
	 * Gets the {@link PlayerStorage}.
	 * 
	 * @return the player storage
	 */
	public PlayerStorage getPlayerStorage();

	/**
	 * Gets the {@link HistoryStorage}.
	 * 
	 * @return the history storage
	 */
	public HistoryStorage getHistoryStorage();
}
