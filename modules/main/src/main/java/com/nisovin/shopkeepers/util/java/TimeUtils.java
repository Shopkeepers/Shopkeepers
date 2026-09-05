package com.nisovin.shopkeepers.util.java;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

	/**
	 * The number of nanoseconds in one second.
	 */
	public static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

	/**
	 * Converts a duration between the given {@link TimeUnit time units} while preserving double
	 * precision.
	 * 
	 * @param duration
	 *            the duration in the source time unit
	 * @param from
	 *            the source time unit
	 * @param to
	 *            the target time unit
	 * @return the duration in the target time unit
	 */
	public static double convert(double duration, TimeUnit from, TimeUnit to) {
		if (from == to) {
			return duration;
		}
		// Smaller ordinal indicates the smaller time unit:
		if (from.ordinal() < to.ordinal()) {
			return duration / from.convert(1, to);
		} else {
			return duration * to.convert(1, from);
		}
	}

	/**
	 * Gets a display string representing the given duration, omitting all components that are zero
	 * or smaller than the given minimum unit.
	 * <p>
	 * Example: "2d 3h 4m", or "5s" if the minimum unit is {@link ChronoUnit#SECONDS}. Negative
	 * durations are prefixed with a '-'.
	 * <p>
	 * A duration whose magnitude is smaller than the minimum unit is represented as "&lt;1"
	 * followed by the minimum unit's suffix (for example "&lt;1m"), or as "0" followed by that
	 * suffix if the duration is zero. Its sign is omitted in that case.
	 * 
	 * @param duration
	 *            the duration
	 * @param minimumUnit
	 *            the smallest unit to display: One of {@link ChronoUnit#DAYS},
	 *            {@link ChronoUnit#HOURS}, {@link ChronoUnit#MINUTES}, or
	 *            {@link ChronoUnit#SECONDS}
	 * @return the duration display string
	 */
	public static String getDurationString(Duration duration, ChronoUnit minimumUnit) {
		// This also validates the minimum unit:
		String minimumUnitSuffix = getDurationUnitSuffix(minimumUnit);

		var negative = duration.isNegative();
		var absDuration = duration.abs();

		var durationString = new StringBuilder();
		appendDurationComponent(durationString, absDuration.toDays(), "d");

		if (minimumUnit.compareTo(ChronoUnit.HOURS) <= 0) {
			appendDurationComponent(durationString, absDuration.toHoursPart(), "h");
		}

		if (minimumUnit.compareTo(ChronoUnit.MINUTES) <= 0) {
			appendDurationComponent(durationString, absDuration.toMinutesPart(), "m");
		}

		if (minimumUnit.compareTo(ChronoUnit.SECONDS) <= 0) {
			appendDurationComponent(durationString, absDuration.toSecondsPart(), "s");
		}

		if (durationString.length() == 0) {
			// The duration is smaller than the minimum unit. We omit its sign in this case:
			return (absDuration.isZero() ? "0" : "<1") + minimumUnitSuffix;
		}

		if (negative) {
			durationString.insert(0, '-');
		}

		return durationString.toString();
	}

	private static void appendDurationComponent(
			StringBuilder durationString,
			long value,
			String suffix
	) {
		if (value <= 0L) return;

		if (durationString.length() > 0) {
			durationString.append(' ');
		}

		durationString.append(value).append(suffix);
	}

	private static String getDurationUnitSuffix(ChronoUnit unit) {
		switch (unit) {
		case DAYS:
			return "d";
		case HOURS:
			return "h";
		case MINUTES:
			return "m";
		case SECONDS:
			return "s";
		default:
			throw new IllegalArgumentException("Unsupported duration unit: " + unit);
		}
	}

	/**
	 * Gets a display string representing the time elapsed since the given instant.
	 * <p>
	 * Example: "2d 3h 4m", or "5s" for durations less than one minute.
	 * 
	 * @param instant
	 *            the instant to calculate the elapsed time from
	 * @return the string representing the elapsed time
	 */
	public static String getTimeAgoString(Instant instant) {
		// Negative if the instant is in the future:
		return getDurationString(Duration.between(instant, Instant.now()), ChronoUnit.SECONDS);
	}

	private TimeUtils() {
	}
}
