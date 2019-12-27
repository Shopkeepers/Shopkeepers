package com.nisovin.shopkeepers.storage.sql;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.history.storage.sql.SQLHistoryStorage;
import com.nisovin.shopkeepers.player.profile.storage.sql.SQLPlayerStorage;
import com.nisovin.shopkeepers.storage.SKStorage;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.storage.StorageType;

public abstract class SQLStorage extends SKStorage {

	private SQLConnector connector;

	protected SQLStorage(SKShopkeepersPlugin plugin, StorageType storageType) {
		super(plugin, storageType);
	}

	@Override
	public void setup() throws StorageException {
		// setup and verify database connection:
		this.connector = this.createConnector();
		if (this.connector == null) {
			throw new StorageException("SQLStorage '" + this.getType().getIdentifier() + "' returned null SQLConnector!");
		}
		try {
			connector.ensureConnection(true);
		} catch (Exception e) {
			// could not connect to database, even after several retries:
			throw new StorageException("Could not connect to database!", e);
		}

		// setup common components:
		super.setup();
	}

	protected abstract SQLConnector createConnector();

	protected SQLPlayerStorage createPlayerStorage() {
		return new SQLPlayerStorage(connector);
	}

	protected SQLHistoryStorage createHistoryStorage() {
		return new SQLHistoryStorage(connector, this.getPlayerStorage());
	}

	@Override
	public void shutdown() {
		// reverse order:
		super.shutdown();
		connector.shutdown();
		connector = null;
	}

	// COMPONENTS

	public SQLConnector getConnector() {
		return connector;
	}

	@Override
	public SQLPlayerStorage getPlayerStorage() {
		return (SQLPlayerStorage) super.getPlayerStorage();
	}

	@Override
	public SQLHistoryStorage getHistoryStorage() {
		return (SQLHistoryStorage) super.getHistoryStorage();
	}
}
