package com.nisovin.shopkeepers.playershops.expiration;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Formats the remaining time until a player shop's expiration, color-coded by urgency.
 */
public final class ShopExpirationTimeFormat {

	private static final Duration ONE_HOUR = Duration.ofHours(1);
	private static final Duration ONE_DAY = Duration.ofDays(1);
	private static final Duration THREE_DAYS = Duration.ofDays(3);

	/**
	 * Gets a {@link Text} that displays the remaining time until the given expiration timestamp as
	 * a color-coded, human-readable duration, with the absolute expiration timestamp as hover text.
	 * 
	 * @param expiration
	 *            the expiration timestamp, not <code>null</code>
	 * @param now
	 *            the current timestamp, not <code>null</code>
	 * @return the remaining time text
	 */
	public static Text getTimeLeftText(Instant expiration, Instant now) {
		Validate.notNull(expiration, "expiration is null");
		Validate.notNull(now, "now is null");

		var formattedExpiration = DerivedSettings.dateTimeFormatter.format(expiration);
		var timeLeft = Duration.between(now, expiration);
		return Text.hoverEvent(Text.of(formattedExpiration))
				.childText(formatTimeLeft(timeLeft))
				.buildRoot();
	}

	// Formats the given remaining time until a shop's expiration as a color-coded, human-readable
	// duration.
	private static String formatTimeLeft(Duration timeLeft) {
		// An already reached expiration is displayed as no remaining time, instead of as a negative
		// duration:
		if (timeLeft.isNegative()) {
			timeLeft = Duration.ZERO;
		}

		// The color reflects the urgency of the approaching expiration:
		String duration = TimeUtils.getDurationString(timeLeft, ChronoUnit.MINUTES);
		return TextUtils.colorize(color(timeLeft) + duration);
	}

	private static String color(Duration timeLeft) {
		if (timeLeft.compareTo(ONE_HOUR) <= 0) return "&4"; // Dark red
		if (timeLeft.compareTo(ONE_DAY) <= 0) return "&c"; // Red
		if (timeLeft.compareTo(THREE_DAYS) <= 0) return "&6"; // Gold
		return "&a"; // Green
	}

	private ShopExpirationTimeFormat() {
	}
}
