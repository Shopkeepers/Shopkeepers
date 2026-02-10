package com.nisovin.shopkeepers.shopkeeper.ticking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class ShopkeeperTicker {

	/**
	 * The ticking period of active shopkeepers and shop objects in ticks.
	 */
	public static final int TICKING_PERIOD_TICKS = 20; // 1 second

	/**
	 * The number of ticking groups.
	 * <p>
	 * For load balancing purposes, we tick more frequently, but then only process a subset of all
	 * active shopkeepers each time. Each of these subsets is called a "ticking group".
	 * <p>
	 * This number is chosen as a balance between {@code 1} group (all shopkeepers are ticked within
	 * the same tick; no load balancing), and the maximum of {@code 20} groups (groups are as small
	 * as possible for the tick rate of once every second, i.e. once every {@code 20} ticks; best
	 * load balancing; but this is associated with a large overhead due to having to do some
	 * processing each Minecraft tick).
	 * <p>
	 * With {@code 4} ticking groups, one fourth of the active shopkeepers are processed every
	 * {@code 5} ticks.
	 */
	public static final int TICKING_GROUPS = 4;
	private static final CyclicCounter tickingGroupCounter = new CyclicCounter(TICKING_GROUPS);

	public static int nextTickingGroup() {
		return tickingGroupCounter.getAndIncrement();
	}

	private static final class TickingGroup {

		private final Set<AbstractShopkeeper> shopkeepers = new LinkedHashSet<>();

		TickingGroup() {
		}

		Collection<? extends AbstractShopkeeper> getShopkeepers() {
			return shopkeepers;
		}

		void addShopkeeper(AbstractShopkeeper shopkeeper) {
			assert shopkeeper != null;
			shopkeepers.add(shopkeeper);
		}

		void removeShopkeeper(AbstractShopkeeper shopkeeper) {
			assert shopkeeper != null;
			shopkeepers.remove(shopkeeper);
		}

		void clear() {
			shopkeepers.clear();
		}
	}

	private final SKShopkeepersPlugin plugin;

	private final List<? extends TickingGroup> tickingGroups;
	{
		List<TickingGroup> tickingGroups = new ArrayList<>(TICKING_GROUPS);
		for (int i = 0; i < TICKING_GROUPS; i++) {
			tickingGroups.add(new TickingGroup());
		}
		this.tickingGroups = tickingGroups;
	}

	private final CyclicCounter activeTickingGroup = new CyclicCounter(TICKING_GROUPS);
	private boolean currentlyTicking = false;
	private boolean dirty;

	// True: Ticking started, False: Ticking stopped
	// Note: The start/stop-ticking callbacks for these pending changes have already been invoked
	// and only the actual registration change is deferred, because if a shopkeeper changes its
	// ticking state multiple times during the same tick we would otherwise lose the callbacks for
	// the intermediate ticking state changes.
	private final Map<AbstractShopkeeper, Boolean> pendingTickingChanges = new LinkedHashMap<>();

	public ShopkeeperTicker(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		// Resetting the ticking group counter ensures that shopkeepers retain their ticking group
		// across reloads (if there are no changes in the order of the loaded shopkeepers). This
		// ensures that the particle colors of our tick visualization remain the same across reloads
		// (avoids possible confusion for users).
		tickingGroupCounter.reset();
		activeTickingGroup.setValue(0);

		// Start shopkeeper ticking task:
		this.startShopkeeperTickTask();
	}

	public void onDisable() {
		// Usually, there should be no need to clean up the registered ticking shopkeepers here,
		// since shopkeepers should stop their ticking automatically once they are deactivated.
		// However, if the plugin is shut down during shopkeeper ticking, we can end up with still
		// pending registration changes.
		if (currentlyTicking) {
			// Reset:
			currentlyTicking = false;
			dirty = false;
			tickingGroups.forEach(TickingGroup::clear);
			pendingTickingChanges.clear();
		} else {
			this.ensureEmpty();
		}
	}

	private void ensureEmpty() {
		boolean anyNonEmptyTickingGroup = tickingGroups.stream()
				.anyMatch(tickingGroup -> !tickingGroup.getShopkeepers().isEmpty());
		if (anyNonEmptyTickingGroup) {
			Log.warning("Some ticking shopkeepers were not properly unregistered!");
			tickingGroups.forEach(TickingGroup::clear);
		}
		if (!pendingTickingChanges.isEmpty()) {
			Log.warning("Unexpected pending shopkeeper ticking changes!");
			pendingTickingChanges.clear();
		}
	}

	private TickingGroup getTickingGroup(int tickingGroupIndex) {
		assert tickingGroupIndex >= 0 && tickingGroupIndex < tickingGroups.size();
		TickingGroup tickingGroup = tickingGroups.get(tickingGroupIndex);
		assert tickingGroup != null;
		return tickingGroup;
	}

	private TickingGroup getTickingGroup(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		int tickingGroupIndex = shopkeeper.getTickingGroup();
		return this.getTickingGroup(tickingGroupIndex);
	}

	// TICKING START / STOP

	// This has no effect if the shopkeeper is already ticking.
	public void startTicking(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (shopkeeper.isTicking()) return; // Already ticking

		Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLogPrefix()
				+ "Ticking started." + (currentlyTicking ? " (Deferred registration)" : ""));

		if (currentlyTicking) {
			// Defer registration until after ticking:
			pendingTickingChanges.put(shopkeeper, true); // Replaces any previous value
		} else {
			this.addShopkeeper(shopkeeper);
		}

		// Inform the shopkeeper:
		try {
			shopkeeper.informStartTicking();
		} catch (Throwable e) {
			Log.severe(shopkeeper.getLogPrefix() + "Error during ticking start!", e);
		}
	}

	// This has no effect if the shopkeeper is already not ticking.
	public void stopTicking(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (!shopkeeper.isTicking()) return; // Already not ticking

		Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLogPrefix()
				+ "Ticking stopped." + (currentlyTicking ? " (Deferred unregistration)" : ""));

		if (currentlyTicking) {
			// Defer unregistration until after ticking:
			pendingTickingChanges.put(shopkeeper, false); // Replaces any previous value
		} else {
			this.removeShopkeeper(shopkeeper);
		}

		// Inform the shopkeeper:
		try {
			shopkeeper.informStopTicking();
		} catch (Throwable e) {
			Log.severe(shopkeeper.getLogPrefix() + "Error during ticking stop!", e);
		}
	}

	private void addShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		TickingGroup tickingGroup = this.getTickingGroup(shopkeeper);
		assert tickingGroup != null;
		tickingGroup.addShopkeeper(shopkeeper);
	}

	private void removeShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		TickingGroup tickingGroup = this.getTickingGroup(shopkeeper);
		assert tickingGroup != null;
		tickingGroup.removeShopkeeper(shopkeeper);
	}

	// TICKING

	private void startShopkeeperTickTask() {
		new ShopkeeperTickTask().start();
	}

	private final class ShopkeeperTickTask implements Runnable {

		private static final int PERIOD = TICKING_PERIOD_TICKS / TICKING_GROUPS;

		void start() {
			SchedulerUtils.runAsyncTaskTimerOrOmit(this, PERIOD, PERIOD);
		}

		@Override
		public void run() {
			tickShopkeepers();
		}
	}

	private void tickShopkeepers() {
		dirty = false;

		currentlyTicking = true;
		TickingGroup tickingGroup = this.getTickingGroup(activeTickingGroup.getValue());
		tickingGroup.getShopkeepers().forEach(this::tickShopkeeper);
		currentlyTicking = false;

		// Process pending shopkeeper ticking registration changes:
		pendingTickingChanges.forEach((shopkeeper, isTicking) -> {
			if (isTicking) {
				this.addShopkeeper(shopkeeper);
			} else {
				this.removeShopkeeper(shopkeeper);
			}
		});
		pendingTickingChanges.clear();

		// Trigger a delayed save if any of the shopkeepers got marked as dirty:
		if (dirty) {
			plugin.getShopkeeperStorage().saveDelayed();
		}

		// Update the active ticking group:
		activeTickingGroup.getAndIncrement();
	}

	private void tickShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		// Skip if the shopkeeper is no longer ticking (e.g. if it got removed or deactivated while
		// it was pending to be ticked):
		if (!shopkeeper.isTicking()) return;

		// Tick the shopkeeper:
		try {
			shopkeeper.tick();
		} catch (Throwable e) {
			Log.severe(shopkeeper.getLogPrefix() + "Error during ticking!", e);
		}

		if (shopkeeper.isDirty()) {
			dirty = true;
		}
	}
}
