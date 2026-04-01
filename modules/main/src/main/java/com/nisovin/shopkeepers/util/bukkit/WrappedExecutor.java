package com.nisovin.shopkeepers.util.bukkit;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Location;
import org.eclipse.jdt.annotation.Nullable;

import java.util.concurrent.Executor;

public interface WrappedExecutor extends Executor {

    void execute(Location location, Runnable command);

    @Override
    default void execute(Runnable command) {
        throw new UnsupportedOperationException("Use execute(Location, Runnable) instead");
    }
}
