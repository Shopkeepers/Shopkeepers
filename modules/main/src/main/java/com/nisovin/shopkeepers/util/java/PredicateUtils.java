package com.nisovin.shopkeepers.util.java;

import java.util.function.Predicate;

public class PredicateUtils {

	private PredicateUtils() {
	}

	private static final Predicate<Object> ALWAYS_TRUE = (object) -> true;
	private static final Predicate<Object> ALWAYS_FALSE = (object) -> false;

	/**
	 * Returns a {@link Predicate} that always returns <code>true</code>.
	 * 
	 * @param <T>
	 *            the predicate type
	 * @return the predicate
	 */
	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> alwaysTrue() {
		return (Predicate<T>) ALWAYS_TRUE;
	}

	/**
	 * Returns a {@link Predicate} that always returns <code>false</code>.
	 * 
	 * @param <T>
	 *            the predicate type
	 * @return the predicate
	 */
	@SuppressWarnings("unchecked")
	public static <T> Predicate<T> alwaysFalse() {
		return (Predicate<T>) ALWAYS_FALSE;
	}

	/**
	 * Returns the given {@link Predicate} if it is not <code>null</code>, and otherwise a Predicate that always returns
	 * <code>true</code>.
	 * 
	 * @param <T>
	 *            the predicate type
	 * @param predicate
	 *            a predicate, possibly <code>null</code>
	 * @return the predicate
	 */
	public static <T> Predicate<T> orAlwaysTrue(Predicate<T> predicate) {
		if (predicate != null) return predicate;
		return alwaysTrue();
	}

	/**
	 * Returns the given {@link Predicate} if it is not <code>null</code>, and otherwise a Predicate that always returns
	 * <code>false</code>.
	 * 
	 * @param <T>
	 *            the predicate type
	 * @param predicate
	 *            a predicate, possibly <code>null</code>
	 * @return the predicate
	 */
	public static <T> Predicate<T> orAlwaysFalse(Predicate<T> predicate) {
		if (predicate != null) return predicate;
		return alwaysFalse();
	}
}
