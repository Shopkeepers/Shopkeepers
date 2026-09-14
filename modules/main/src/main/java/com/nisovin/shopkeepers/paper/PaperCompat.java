package com.nisovin.shopkeepers.paper;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Provides access to the {@link PaperCompatProvider} implementation.
 */
public final class PaperCompat {

	private static final String PROVIDER_CLASS_NAME
			= "com.nisovin.shopkeepers.paper.PaperCompatProviderImpl";

	private static @Nullable PaperCompatProvider provider;

	public static boolean hasProvider() {
		return (provider != null);
	}

	/**
	 * Gets the {@link PaperCompatProvider}.
	 * <p>
	 * The provider is only set up on servers that provide the Paper API. Use {@link #hasProvider()}
	 * to check for its availability.
	 * 
	 * @return the provider, not <code>null</code>
	 */
	public static PaperCompatProvider getProvider() {
		return Validate.State.notNull(provider, "Paper compat provider is not set up!");
	}

	/**
	 * Loads the {@link PaperCompatProvider}, if the server provides the Paper API.
	 * 
	 * @return <code>true</code> on success, or if the server does not provide the Paper API;
	 *         <code>false</code> if the compat module could not be loaded
	 */
	public static boolean load() {
		if (provider != null) {
			throw new IllegalStateException("Provider already loaded!");
		}

		if (!ServerUtils.isPaper()) {
			return true;
		}

		try {
			Class<?> clazz = Class.forName(PROVIDER_CLASS_NAME);
			provider = (PaperCompatProvider) clazz.getConstructor().newInstance();
			Log.info("Paper compatibility provider loaded.");
			return true; // Success
		} catch (Exception e) {
			Log.severe("Failed to load the Paper compatibility provider!", e);
			return false;
		}
	}

	private PaperCompat() {
	}
}
