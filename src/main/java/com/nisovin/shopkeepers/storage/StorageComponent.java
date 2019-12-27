package com.nisovin.shopkeepers.storage;

public interface StorageComponent {

	/**
	 * A name for identification purposes, eg. in error messages.
	 * 
	 * @return the name of this component
	 */
	public String getName();

	/**
	 * Called once during startup.
	 * 
	 * @throws Exception
	 *             in case of severe failure
	 */
	public void setup() throws StorageException;

	/**
	 * Checks if this component has already been {@link #shutdown()}.
	 *
	 * @return <code>true</code> if shut down
	 */
	public boolean isShutdown();

	/**
	 * Called once on shutdown.
	 */
	public void shutdown();
}
