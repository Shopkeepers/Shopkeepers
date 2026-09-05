package com.nisovin.shopkeepers.util.bukkit;

import java.util.concurrent.TimeUnit;

import org.bukkit.util.NumberConversions;

/**
 * Utilities related to Minecraft ticks.
 */
public final class Ticks {

	/**
	 * The expected number of ticks per second.
	 */
	public static final int PER_SECOND = 20;
	/**
	 * The expected duration of a single tick in seconds.
	 */
	public static final double DURATION_SECONDS = 1.0D / PER_SECOND;
	/**
	 * The expected duration of a single tick in minutes.
	 */
	public static final double DURATION_MINUTES = DURATION_SECONDS / TimeUnit.MINUTES.toSeconds(1);
	/**
	 * The expected duration of a single tick in milliseconds.
	 */
	public static final long DURATION_MILLIS = TimeUnit.SECONDS.toMillis(1L) / PER_SECOND;
	/**
	 * The expected duration of a single tick in nanoseconds.
	 */
	public static final long DURATION_NANOS = TimeUnit.SECONDS.toNanos(1L) / PER_SECOND;

	/**
	 * Converts the given duration in ticks into seconds.
	 * 
	 * @param ticks
	 *            the duration in ticks
	 * @return the duration in seconds
	 */
	public static double toSeconds(long ticks) {
		return ticks * DURATION_SECONDS;
	}

	/**
	 * Converts the given duration in seconds into ticks, rounding to the nearest tick.
	 * 
	 * @param seconds
	 *            the duration in seconds
	 * @return the duration in ticks
	 */
	public static long fromSeconds(double seconds) {
		return NumberConversions.round(seconds / DURATION_SECONDS);
	}

	/**
	 * Converts the given duration in ticks into minutes.
	 * 
	 * @param ticks
	 *            the duration in ticks
	 * @return the duration in minutes
	 */
	public static double toMinutes(long ticks) {
		return ticks * DURATION_MINUTES;
	}

	/**
	 * Converts the given duration in minutes into ticks, rounding to the nearest tick.
	 * 
	 * @param minutes
	 *            the duration in minutes
	 * @return the duration in ticks
	 */
	public static long fromMinutes(double minutes) {
		return NumberConversions.round(minutes / DURATION_MINUTES);
	}

	/**
	 * Converts the given duration in ticks into milliseconds.
	 * 
	 * @param ticks
	 *            the duration in ticks
	 * @return the duration in milliseconds
	 */
	public static long toMillis(long ticks) {
		return ticks * DURATION_MILLIS;
	}

	/**
	 * Converts the given duration in milliseconds into ticks, rounding to the nearest tick.
	 * 
	 * @param millis
	 *            the duration in milliseconds
	 * @return the duration in ticks
	 */
	public static long fromMillis(long millis) {
		return NumberConversions.round((double) millis / DURATION_MILLIS);
	}

	/**
	 * Converts the given duration in ticks into nanoseconds.
	 * 
	 * @param ticks
	 *            the duration in ticks
	 * @return the duration in nanoseconds
	 */
	public static long toNanos(long ticks) {
		return ticks * DURATION_NANOS;
	}

	/**
	 * Converts the given duration in nanoseconds into ticks, rounding to the nearest tick.
	 * 
	 * @param nanos
	 *            the duration in nanoseconds
	 * @return the duration in ticks
	 */
	public static long fromNanos(long nanos) {
		return NumberConversions.round((double) nanos / DURATION_NANOS);
	}

	private Ticks() {
	}
}
