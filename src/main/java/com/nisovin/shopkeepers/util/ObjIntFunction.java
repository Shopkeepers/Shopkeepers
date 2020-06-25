package com.nisovin.shopkeepers.util;

import java.util.function.BiFunction;

/**
 * A function that accepts an object-valued and an int-valued argument and produces a result. This is a specialization
 * of {@link BiFunction}.
 *
 * @param <T>
 *            the type of the object argument
 * @param <R>
 *            the type of the result of the function
 */
@FunctionalInterface
public interface ObjIntFunction<T, R> {

	/**
	 * Applies this function to the given arguments.
	 * 
	 * @param t
	 *            the first function argument
	 * @param value
	 *            the second function argument
	 * @return the function result
	 */
	R apply(T t, int value);
}
