package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Sends shop expiration notifications.
 */
public class ShopExpirationNotifier {

	// The maximum number of shops listed in a single notification:
	private static final int MAX_LISTED_SHOPS = 8;

	private final SKShopkeepersPlugin plugin;

	ShopExpirationNotifier(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	/**
	 * Informs the given player about the given shop's expiration, if it expires.
	 * <p>
	 * This is invoked when a player creates, hires, or is transferred a shop. Unlike the reminders
	 * about approaching expirations, this is sent regardless of the
	 * {@link Settings#notifyShopMembersAboutExpiration} setting.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param shop
	 *            the newly created/acquired player shopkeeper, not <code>null</code>
	 */
	public void informAboutExpiration(Player player, AbstractPlayerShopkeeper shop) {
		Validate.notNull(player, "player is null");
		Validate.notNull(shop, "shop is null");
		@Nullable Instant expiration = shop.getExpiration();
		if (expiration == null) {
			// The shop does not expire:
			return;
		}

		Instant now = Instant.now();

		// Note: An already reached expiration is unexpected here, because the expiration is usually
		// reset when a shop is acquired. If encountered, it is displayed as no remaining time.
		TextUtils.sendMessage(player, Messages.shopExpirationInfo,
				"timeLeft", ShopExpirationTimeFormat.getTimeLeftText(expiration, now)
		);
	}

	/**
	 * Sends the given recipient the list of the given user's shops that are about to expire.
	 * <p>
	 * Unlike the automatic notifications, this lists all of the user's expiring shops (up to a
	 * limit) and does not affect the notification state.
	 * <p>
	 * Shops that have already reached their expiration, but that have not been processed by the
	 * expiration check yet, are listed with no remaining time.
	 * 
	 * @param recipient
	 *            the recipient, not <code>null</code>
	 * @param user
	 *            the user whose expiring shops to list, not <code>null</code>
	 */
	public void sendExpiringShopsList(CommandSender recipient, User user) {
		Validate.notNull(recipient, "recipient is null");
		Validate.notNull(user, "user is null");
		Instant now = Instant.now();

		var playerText = TextUtils.getPlayerText(user);
		List<ExpiringShop> expiringShops = this.collectExpiringShops(user.getUniqueId());
		if (expiringShops.isEmpty()) {
			TextUtils.sendMessage(recipient, Messages.noExpiringShops, "player", playerText);
			return;
		}

		TextUtils.sendMessage(recipient, Messages.listExpiringShopsHeader,
				"player", playerText,
				"shopsCount", expiringShops.size()
		);
		this.sendExpiringShopsListEntries(recipient, expiringShops, now);
	}

	// Collects the shops that expire and that the specified player owns or is a member of.
	// This also includes shops that have already reached their expiration, but that have not been
	// processed by the expiration check yet.
	// The returned entries have no notification threshold set.
	private List<ExpiringShop> collectExpiringShops(UUID playerId) {
		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<ExpiringShop> expiringShops = new ArrayList<>();
		for (AbstractPlayerShopkeeper shop : shopkeeperRegistry.getAllPlayerShopkeepers()) {
			if (!shop.isMember(playerId)) {
				continue;
			}

			@Nullable Instant expiration = shop.getExpiration();
			if (expiration == null) {
				// The shop does not expire:
				continue;
			}

			expiringShops.add(new ExpiringShop(shop, expiration, null));
		}
		return expiringShops;
	}

	/**
	 * Reminds the given player about their shops that are about to expire.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param expiringShops
	 *            the expiring shops, will be sorted in-place in ascending order by their expiration
	 * @param now
	 *            the current timestamp
	 */
	void sendExpirationReminders(Player player, List<ExpiringShop> expiringShops, Instant now) {
		TextUtils.sendMessage(player, Messages.shopExpirationNotificationHeader);

		this.sendExpiringShopsListEntries(player, expiringShops, now);

		if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.EXPIRATION_OWN_PERMISSION)) {
			TextUtils.sendMessage(player, Messages.shopExpirationNotificationHint);
		}
	}

	// Sends the given expiring shops, sorted by soonest expiration.
	// The given list is sorted in-place in ascending order by expiration.
	// Only up to MAX_LISTED_SHOPS shops are listed.
	private void sendExpiringShopsListEntries(
			CommandSender recipient,
			List<ExpiringShop> expiringShops,
			Instant now
	) {
		expiringShops.sort(Comparator.comparing(expiringShop -> expiringShop.expiration));

		int listedShops = Math.min(expiringShops.size(), MAX_LISTED_SHOPS);
		for (int i = 0; i < listedShops; i++) {
			ExpiringShop expiringShop = expiringShops.get(i);
			AbstractPlayerShopkeeper shop = expiringShop.shopkeeper;
			Instant expiration = expiringShop.expiration;
			String shopName = shop.getName();
			TextUtils.sendMessage(recipient, Messages.shopExpirationNotificationEntry,
					"shopId", shop.getId(),
					"shopName", (shopName.isEmpty() ? "" : (shopName + " ")),
					"location", shop.getPositionString(),
					"timeLeft", ShopExpirationTimeFormat.getTimeLeftText(expiration, now)
			);
		}

		int remaining = expiringShops.size() - listedShops;
		if (remaining > 0) {
			TextUtils.sendMessage(recipient, Messages.shopExpirationNotificationMore,
					"count", remaining
			);
		}
	}

	// Informs the given player that the given shop has expired.
	void sendShopExpired(Player player, AbstractPlayerShopkeeper shop) {
		String shopName = shop.getName();
		TextUtils.sendMessage(player, Messages.shopExpired,
				"shopName", (shopName.isEmpty() ? "" : (shopName + " ")),
				"location", shop.getPositionString()
		);
	}
}
