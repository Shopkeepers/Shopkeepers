package com.nisovin.shopkeepers.compat;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 26.3 upwards.
public final class MC_26_3 {

	public static final @Nullable Material POPLAR_SIGN = CompatUtils.getMaterial("POPLAR_SIGN");
	public static final @Nullable Material POPLAR_WALL_SIGN = CompatUtils.getMaterial("POPLAR_WALL_SIGN");
	public static final @Nullable Material POPLAR_HANGING_SIGN = CompatUtils.getMaterial("POPLAR_HANGING_SIGN");
	public static final @Nullable Material POPLAR_WALL_HANGING_SIGN = CompatUtils.getMaterial("POPLAR_WALL_HANGING_SIGN");

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 26.3 exclusive features are enabled.");
		} else {
			Log.debug("MC 26.3 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		return POPLAR_SIGN != null;
	}

	private MC_26_3() {
	}
}
