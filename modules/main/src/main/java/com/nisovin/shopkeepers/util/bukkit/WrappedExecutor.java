package com.nisovin.shopkeepers.util.bukkit;

import java.util.concurrent.Executor;

import org.bukkit.Location;

public interface WrappedExecutor extends Executor {

	void execute(Location location, Runnable command);

	@Override
	default void execute(Runnable command) {
		throw new UnsupportedOperationException("Use execute(Location, Runnable) instead");
	}
}
