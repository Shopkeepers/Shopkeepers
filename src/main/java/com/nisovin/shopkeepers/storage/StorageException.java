package com.nisovin.shopkeepers.storage;

/**
 * This exception is thrown when an error occurs during storage operations.
 */
public class StorageException extends Exception {

	private static final long serialVersionUID = -3529082452345820954L;

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
