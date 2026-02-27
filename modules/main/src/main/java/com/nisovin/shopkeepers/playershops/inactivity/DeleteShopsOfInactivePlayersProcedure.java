package com.nisovin.shopkeepers.playershops.inactivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerInactiveEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Identifies and deletes the shops that are owned by inactive players.
 */
class DeleteShopsOfInactivePlayersProcedure {

	private static class InactivePlayerData {

		private final int lastSeenDaysAgo;
		private final List<PlayerShopkeeper> shopkeepers = new ArrayList<>();

		InactivePlayerData(int lastSeenDaysAgo) {
			this.lastSeenDaysAgo = lastSeenDaysAgo;
		}

		int getLastSeenDaysAgo() {
			return lastSeenDaysAgo;
		}

		List<PlayerShopkeeper> getShopkeepers() {
			return shopkeepers;
		}
	}

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;
	private final int playerInactivityDays;

	private boolean started = false;
	// Retrieved once and then reused for all inactivity checks of this procedure:
	private final long currentTimeMillis = System.currentTimeMillis();
	private final Map<User, @Nullable InactivePlayerData> inactivePlayers = new HashMap<>();

	public DeleteShopsOfInactivePlayersProcedure(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
		// Local copy, because this setting might change while we use it, and we might access it
		// asynchronously:
		this.playerInactivityDays = Settings.playerShopkeeperInactiveDays;
	}

	/**
	 * Starts this procedure.
	 * <p>
	 * This is expected to be called right after construction. Each procedure instance can only be
	 * run once.
	 */
	public void start() {
		Validate.State.isTrue(!started, "Already started!");
		started = true;
		if (playerInactivityDays <= 0) return; // Feature is disabled

		Log.info("Checking for shopkeepers of inactive players.");

		this.collectShopOwners();
		if (inactivePlayers.isEmpty()) {
			return; // There are no player shops
		}

		this.asyncCheckInactivityOfAllShopOwnersAndContinue();
	}

	// This initially collects all shop owners into the inactivePlayers Map, which is subsequently
	// pruned from shop owners that are not actually inactive.
	private void collectShopOwners() {
		shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
			// In this first step, we only collect the existing shop owners, and don't store their
			// shopkeepers yet. Later, we collect the shopkeepers of only the inactive shop owners.
			inactivePlayers.put(playerShop.getOwnerUser(), null);
		});
	}

	private void asyncCheckInactivityOfAllShopOwnersAndContinue() {
		// We retrieve the OfflinePlayers and their 'last played' times asynchronously:
		SchedulerUtils.runAsyncTaskOrOmit(() -> {
			// Set up the data for all inactive shop owners, and remove all shop owners that are
			// not inactive:
			setUpInactiveShopOwners();

			// Abort if no inactive players were found:
			if (inactivePlayers.isEmpty()) return;

			// Abort if the task has been cancelled in the meantime (e.g. if the plugin has been
			// disabled or reloaded):
			//if (this.isCancelled()) return;

			SchedulerUtils.runTaskGloballyOrOmit(() -> continueWithInactiveShopOwners());
		});
	}

	// This may be called asynchronously.
	// Sets up the data for all inactive shop owners, and removes all shop owners that are not
	// inactive.
	private void setUpInactiveShopOwners() {
		Iterator<Entry<User, @Nullable InactivePlayerData>> iterator = inactivePlayers.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<User, @Nullable InactivePlayerData> entry = iterator.next();
			User user = entry.getKey();
			InactivePlayerData data = this.setUpIfInactive(user);
			if (data == null) {
				// The user is not inactive:
				iterator.remove();
			} else {
				entry.setValue(data);
			}
		}
		assert !CollectionUtils.containsNull(inactivePlayers.values());
	}

	// This may be called asynchronously.
	// Returns null if the given user is not inactive.
	private @Nullable InactivePlayerData setUpIfInactive(User user) {
		assert user != null;
		OfflinePlayer offlinePlayer = user.getOfflinePlayer();
		// Some servers may delete player data files, either regularly for all players (which breaks
		// this feature), or for particular players (for example to reset or fix some issue with
		// their data). If this is the case, we cannot reliably determine when the player was last
		// seen on the server, and therefore do not delete their shopkeepers.
		if (!offlinePlayer.hasPlayedBefore()) return null;

		long lastPlayedMillis = offlinePlayer.getLastPlayed();
		if (lastPlayedMillis == 0) return null; // 0 if unknown (see reasoning above)

		long millisSinceLastPlayed = currentTimeMillis - lastPlayedMillis;
		int daysSinceLastPlayed = (int) TimeUnit.MILLISECONDS.toDays(millisSinceLastPlayed);
		if (daysSinceLastPlayed < playerInactivityDays) return null;

		return new InactivePlayerData(daysSinceLastPlayed);
	}

	private void continueWithInactiveShopOwners() {
		assert SchedulerUtils.isGlobalThread();
		assert !inactivePlayers.isEmpty();
		assert !CollectionUtils.containsNull(inactivePlayers.values());
		this.collectShopsOfInactivePlayers();
		this.deleteShopsOfInactivePlayers();
	}

	private void collectShopsOfInactivePlayers() {
		shopkeeperRegistry.getAllPlayerShopkeepers().forEach(playerShop -> {
			// If the shop is owned by an inactive player, remember it for removal:
			User shopOwner = playerShop.getOwnerUser();
			InactivePlayerData inactivePlayerData = inactivePlayers.get(shopOwner);
			if (inactivePlayerData != null) {
				inactivePlayerData.getShopkeepers().add(playerShop);
			}
		});
		// Note: For some inactive shop owners we might no longer find any shopkeepers. Their
		// entries will then not contain any shopkeepers.
	}

	private void deleteShopsOfInactivePlayers() {
		inactivePlayers.forEach((user, nullableInactivePlayerData) -> {
			InactivePlayerData inactivePlayerData = Unsafe.assertNonNull(nullableInactivePlayerData);
			List<? extends PlayerShopkeeper> shopkeepers = inactivePlayerData.getShopkeepers();
			if (shopkeepers.isEmpty()) {
				// We initially found this shop owner and identified them as inactive, but were then
				// subsequently no longer able to find any shopkeepers that are still owned by them.
				return;
			}

			int originalShopkeepersCount = shopkeepers.size();

			// Call event:
			PlayerInactiveEvent event = new PlayerInactiveEvent(user, shopkeepers);
			Bukkit.getPluginManager().callEvent(event);

			if (event.isCancelled() || shopkeepers.isEmpty()) {
				Log.debug(() -> "Ignoring inactive player " + TextUtils.getPlayerString(user)
						+ " (last seen " + inactivePlayerData.getLastSeenDaysAgo() + " days ago)"
						+ " and their " + originalShopkeepersCount + " shopkeepers"
						+ (shopkeepers.size() != originalShopkeepersCount
								? " (reduced to " + shopkeepers.size() + ")" : "")
						+ ": Cancelled by a plugin.");
				return;
			}

			// Delete the shopkeepers:
			shopkeepers.forEach(playerShop -> {
				if (!playerShop.isValid()) {
					// The shopkeeper has already been removed in the meantime.
					Log.debug(() -> playerShop.getUniqueIdLogPrefix()
							+ "Deletion due to inactivity of owner " + playerShop.getOwnerString()
							+ " (last seen " + inactivePlayerData.getLastSeenDaysAgo()
							+ " days ago)" + " skipped: The shopkeeper has already been removed.");
					return;
				}

				Log.info(playerShop.getUniqueIdLogPrefix() + "Deletion due to inactivity of owner "
						+ playerShop.getOwnerString() + " (last seen "
						+ inactivePlayerData.getLastSeenDaysAgo() + " days ago).");
				playerShop.delete();
			});
		});

		// Save if necessary:
		plugin.getShopkeeperStorage().saveIfDirty();
	}
}
