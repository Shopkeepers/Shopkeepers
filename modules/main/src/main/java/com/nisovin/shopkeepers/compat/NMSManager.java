package com.nisovin.shopkeepers.compat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class NMSManager {

	private static final Map<String, CompatVersion> SUPPORTED_MAPPINGS_VERSIONS = new LinkedHashMap<>();

	private static void register(CompatVersion version) {
		SUPPORTED_MAPPINGS_VERSIONS.put(version.getMappingsVersion(), version);
	}

	// Note: Although they look similar, our compat versions do not necessarily match CraftBukkit's
	// 'Minecraft Version'.
	// Our revision number (behind the 'R') is incremented with every mappings version change,
	// whereas CraftBukkit may increment it less frequently.
	// TODO Since 1.17, we have to update and rebuild our compat code whenever the mappings are
	// changed. However, Spigot does not necessarily align mappings changes with CraftBukkit version
	// bumps. The BuildTools also does not allow to build specific commits. We therefore cannot
	// build our compat modules against specific mappings versions of CraftBukkit yet and can
	// consequently only support the latest mappings version for every CraftBukkit version.
	// Versions before 1.17 are not affected by this, because they don't depend on the NMS code
	// remapping.
	static {
		// Registered in the order from latest to oldest.
		// Different CB revision:
		register(new CompatVersion("1_20_R5", "1.20.4", "60a2bb6bf2684dc61c56b90d7c41bddc"));
		register(new CompatVersion("1_20_R3", "1.20.2", "3478a65bfd04b15b431fe107b3617dfc"));
		register(new CompatVersion("1_20_R2", "1.20.1", "bcf3dcb22ad42792794079f9443df2c0"));
		register(new CompatVersion("1_19_R5", "1.19.4", "3009edc0fff87fa34680686663bd59df"));
		register(new CompatVersion("1_19_R4", "1.19.3", "1afe2ffe8a9d7fc510442a168b3d4338"));
		register(new CompatVersion("1_19_R3", "1.19.2", "69c84c88aeb92ce9fa9525438b93f4fe"));
		register(new CompatVersion("1_19_R1", "1.19", "7b9de0da1357e5b251eddde9aa762916"));
		// Different CB revision:
		register(new CompatVersion("1_18_R3", "1.18.2", "eaeedbff51b16ead3170906872fda334"));
		// Different CB revision:
		register(new CompatVersion("1_17_R2", "1.17.1", "f0e3dfc7390de285a4693518dd5bd126"));
		register(new CompatVersion("1_16_R3", "1.16.5", "d4b392244df170796f8779ef0fc1f2e9"));
		register(new CompatVersion("1_16_R3", "1.16.5", "54e89c47309b53737f894f1bf1b0edbe"));
	}

	public static @Nullable CompatVersion getCompatVersion(String compatVersion) {
		// If there are multiple entries for the same compat version, we return the latest one.
		for (CompatVersion version : SUPPORTED_MAPPINGS_VERSIONS.values()) {
			if (version.getCompatVersion().equals(compatVersion)) {
				return version;
			}
		}
		return null;
	}

	// ----

	private static @Nullable NMSCallProvider provider;

	public static boolean hasProvider() {
		return (provider != null);
	}

	public static NMSCallProvider getProvider() {
		return Validate.State.notNull(provider, "NMS provider is not set up!");
	}

	// Returns true if the NMS provider (or fallback handler) has been successfully set up.
	public static boolean load(Plugin plugin) {
		String mappingsVersion = ServerUtils.getMappingsVersion();
		CompatVersion compatVersion = SUPPORTED_MAPPINGS_VERSIONS.get(mappingsVersion);
		if (compatVersion != null) {
			String compatVersionString = compatVersion.getCompatVersion();
			try {
				Class<?> clazz = Class.forName(
						"com.nisovin.shopkeepers.compat.v" + compatVersionString + ".NMSHandler"
				);
				if (NMSCallProvider.class.isAssignableFrom(clazz)) {
					NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
					return true; // Success
				} else {
					// Unexpected: NMSHandler does not implement NMSCallProvider. Continue with
					// fallback.
				}
			} catch (Exception e) {
				// Something went wrong. Continue with fallback.
			}
		}

		// Incompatible server version detected:
		String cbVersion = ServerUtils.getCraftBukkitVersion();
		Log.warning("Incompatible server version: " + cbVersion + " (mappings: "
				+ mappingsVersion + ")");
		Log.warning("Shopkeepers is trying to run in 'compatibility mode'.");
		Log.info("Check for updates at: " + plugin.getDescription().getWebsite());

		try {
			NMSManager.provider = new FailedHandler();
			return true; // Success
		} catch (Exception e) {
			Log.severe("Failed to enable 'compatibility mode'!", e);
		}
		return false;
	}
}
