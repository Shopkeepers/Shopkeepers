package com.nisovin.shopkeepers.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

	private TimeUtils() {
	}

	// Calendar is not thread-safe
	public static final ThreadLocal<Calendar> UTC_CALENDAR = ThreadLocal.withInitial(() -> Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT));

	public static String getTimeAgoString(Instant instant) {
		Duration duration = Duration.between(instant, Instant.now());
		boolean negative = duration.isNegative(); // instant is in the future
		duration = duration.abs();
		long remainingSeconds = duration.getSeconds();
		StringBuilder timeAgoString = new StringBuilder();
		if (negative) {
			timeAgoString.append('-');
		}
		int days = (int) (remainingSeconds / 86400);
		if (days > 0) {
			timeAgoString.append(days).append("d ");
			remainingSeconds %= 86400;
		}
		int hours = (int) (remainingSeconds / 3600);
		if (hours > 0) {
			timeAgoString.append(hours).append("h ");
			remainingSeconds %= 3600;
		}
		int minutes = (int) (remainingSeconds / 60);
		if (minutes > 0) {
			timeAgoString.append(minutes).append("m ");
			remainingSeconds %= 60;
		}
		// only print seconds part for durations less than 1 minute:
		if (timeAgoString.length() < 2) { // length 0 or 1 (due to '-' sign)
			timeAgoString.append(remainingSeconds).append("s ");
		}
		// return string without trailing space:
		return timeAgoString.substring(0, timeAgoString.length() - 1);
	}
}
