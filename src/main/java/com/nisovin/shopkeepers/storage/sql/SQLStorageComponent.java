package com.nisovin.shopkeepers.storage.sql;

import com.nisovin.shopkeepers.storage.StorageComponent;
import com.nisovin.shopkeepers.storage.StorageException;
import com.nisovin.shopkeepers.util.Validate;

public abstract class SQLStorageComponent implements StorageComponent {

	private final String name;
	protected final SQLConnector connector;
	protected final SQL sql;
	protected final String logPrefix;

	private boolean setup = false;
	private boolean shutdown = false;

	public SQLStorageComponent(String name, SQLConnector connector) {
		Validate.notEmpty(name, "Empty name!");
		Validate.notNull(connector, "Connector is null!");
		this.name = name;
		this.connector = connector;
		this.sql = connector.getSQL();
		this.logPrefix = connector.logPrefix;
	}

	@Override
	public final String getName() {
		return name;
	}

	// SETUP

	public final boolean isSetup() {
		return setup;
	}

	protected final void validateSetup() {
		Validate.State.isTrue(setup, "Storage component '" + this.getName() + "' has not yet been set up!");
	}

	protected final void validateNotSetup() {
		Validate.State.isTrue(!setup, "Storage component '" + this.getName() + "' has already been set up!");
	}

	@Override
	public final void setup() throws StorageException {
		this.validateNotSetup();
		// setup, performed as transaction:
		try {
			connector.execute(() -> {
				return connector.performTransaction(connector.getConnection(), () -> {
					this.onSetup();
					return null;
				});
			});
		} catch (Exception e) {
			throw new StorageException("Error during setup of storage component '" + this.getName() + "'!", e);
		}
		setup = true;
	}

	protected void onSetup() throws StorageException {
	}

	// SHUTDOWN

	@Override
	public final boolean isShutdown() {
		return shutdown;
	}

	protected final void validateNotShutdown() {
		Validate.State.isTrue(!shutdown, "Storage component '" + this.getName() + "' has already been shutdown!");
	}

	@Override
	public final void shutdown() {
		this.validateNotShutdown();
		synchronized (connector) {
			this.onShutdown();
			shutdown = true;
		}
	}

	protected void onShutdown() {
	}
}
