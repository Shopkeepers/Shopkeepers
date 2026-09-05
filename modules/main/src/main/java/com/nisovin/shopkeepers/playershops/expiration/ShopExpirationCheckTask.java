package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperExpireEvent;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.Ticks;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Periodically checks for expired player shops, handles their expiration, and notifies online shop
 * owners and members about approaching expirations.
 */
class ShopExpirationCheckTask implements Runnable {

	// The check interval:
	// Also used as the initial delay.
	// Expirations and reached notification thresholds are only detected with this granularity.
	// Thresholds shorter than this interval might not be reached before the shop expires.
	private static final long INTERVAL_TICKS = Ticks.fromMinutes(5);

	private final SKShopkeepersPlugin plugin;
	private final ShopExpirationNotifier notifier;

	private @Nullable BukkitTask task = null;

	ShopExpirationCheckTask(
			SKShopkeepersPlugin plugin,
			ShopExpirationNotifier notifier
	) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(notifier, "notifier is null");
		this.plugin = plugin;
		this.notifier = notifier;
	}

	void start() {
		this.stop(); // Stop the task if it is already running

		task = Bukkit.getScheduler().runTaskTimer(plugin, this, INTERVAL_TICKS, INTERVAL_TICKS);
	}

	void stop() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	@Override
	public void run() {
		Instant now = Instant.now();
		var notify = Settings.notifyShopMembersAboutExpiration;

		List<AbstractPlayerShopkeeper> expiredShops = new ArrayList<>();
		// Online players mapped to their shops with a newly reached notification threshold:
		Map<UUID, List<ExpiringShop>> notifications = new HashMap<>();

		for (AbstractPlayerShopkeeper shop : plugin.getShopkeeperRegistry().getAllPlayerShopkeepers()) {
			@Nullable Instant shopExpiration = shop.getExpiration();
			if (shopExpiration == null) {
				// The shop does not expire:
				continue;
			}

			if (now.isAfter(shopExpiration)) {
				// The shop is due to expire.
				// Handled after the iteration, to avoid concurrent modifications.
				expiredShops.add(shop);
				continue;
			}

			if (!notify) {
				continue;
			}

			// Collect the players to remind about this shop's expiration and reached threshold:
			Duration timeLeft = Duration.between(now, shopExpiration);
			@Nullable Duration threshold = this.getReachedThreshold(timeLeft);
			if (threshold != null) {
				this.collectNotifications(shop, shopExpiration, threshold, notifications);
			}
		}

		// Process expired shops:
		for (AbstractPlayerShopkeeper shop : expiredShops) {
			if (!shop.isValid()) {
				// Already deleted:
				continue;
			}
			// Assert: Still expired (this code runs synchronous).

			// Call event:
			var expireEvent = new PlayerShopkeeperExpireEvent(shop);
			Bukkit.getPluginManager().callEvent(expireEvent);

			if (!shop.isValid()) {
				// Removed during event handling:
				Log.debug(() -> shop.getUniqueIdLogPrefix()
						+ "Removed during expiration event handling.");
				continue;
			}

			if (expireEvent.isCancelled()) {
				Log.debug(() -> shop.getUniqueIdLogPrefix()
						+ "Expiration was cancelled by a plugin.");

				// Automatically reset the shop's expiration:
				shop.resetExpiration();
				continue;
			}

			// Note: We do not re-check here if the shop is no longer expired. If a plugin wants to
			// cancel the expiration, they have to cancel the event.

			// Always inform online owners and members that the shop has expired, regardless of the
			// notification setting.
			// Note: Players are only informed if they are currently online when the shop expires.
			this.notifyExpired(shop);

			// Expire the shop:
			shop.expire();
		}

		// Send the reminders and remember the reached thresholds:
		notifications.forEach((playerId, expiringShops) -> {
			@Nullable Player player = Bukkit.getPlayer(playerId);
			if (player == null) return; // Went offline in the meantime

			notifier.sendExpirationReminders(player, expiringShops, now);

			for (ExpiringShop expiringShop : expiringShops) {
				@Nullable Duration threshold = expiringShop.threshold;
				if (threshold == null) continue;

				expiringShop.shopkeeper.setLastExpirationNotified(playerId, threshold);
			}
		});

		plugin.getShopkeeperStorage().saveIfDirty();
	}

	// Returns the most urgent notification threshold that has been reached for the given remaining
	// time, or null if no threshold has been reached yet.
	@Nullable
	private Duration getReachedThreshold(Duration timeLeft) {
		for (Duration threshold : DerivedSettings.playerShopExpirationNotificationThresholds) {
			if (timeLeft.compareTo(threshold) <= 0) {
				return threshold;
			}
		}

		return null;
	}

	// Collects notifications for online members of the given shop that have not yet been reminded
	// about the given (most urgent reached) threshold.
	private void collectNotifications(
			AbstractPlayerShopkeeper shop,
			Instant shopExpiration,
			Duration threshold,
			Map<UUID, List<ExpiringShop>> notifications
	) {
		shop.forEachMember(member -> {
			if (!member.isOnline()) return; // Offline

			// Already reminded about this (or a more urgent) threshold:
			@Nullable Duration lastNotified = shop.getLastExpirationNotified(member.getUniqueId());
			if (lastNotified != null && threshold.compareTo(lastNotified) >= 0) {
				return;
			}

			notifications.computeIfAbsent(member.getUniqueId(), key -> new ArrayList<>())
					.add(new ExpiringShop(shop, shopExpiration, threshold));
		});
	}

	// Informs the shop members that the given shop has expired.
	private void notifyExpired(AbstractPlayerShopkeeper shop) {
		shop.forEachMember(member -> {
			@Nullable Player player = member.getPlayer();
			if (player == null) return; // Offline

			notifier.sendShopExpired(player, shop);
		});
	}
}
