package com.nisovin.shopkeepers.paper;

import com.nisovin.shopkeepers.compat.CompatProvider;

/**
 * Provides Paper-specific functionality.
 * <p>
 * Unlike the server version specific {@link CompatProvider}, this only uses Paper API that is
 * available on all supported server versions.
 */
public interface PaperCompatProvider {

	/**
	 * This is called when the plugin is enabled.
	 */
	public void onEnable();

	/**
	 * This is called when the plugin is disabled.
	 */
	public void onDisable();
}
