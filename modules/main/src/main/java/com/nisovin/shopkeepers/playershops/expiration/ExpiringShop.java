package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Duration;
import java.time.Instant;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;

/**
 * A player shop that is about to expire, together with its expiration time and the most urgent
 * reached notification threshold, or <code>null</code> if not applicable.
 */
class ExpiringShop {

	final AbstractPlayerShopkeeper shopkeeper;
	final Instant expiration;
	final @Nullable Duration threshold; // Null if not applicable

	ExpiringShop(
			AbstractPlayerShopkeeper shopkeeper,
			Instant expiration,
			@Nullable Duration threshold
	) {
		this.shopkeeper = shopkeeper;
		this.expiration = expiration;
		this.threshold = threshold;
	}
}
