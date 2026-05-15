package com.nisovin.shopkeepers.util.java;

import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;

public final class StreamUtils {

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		// Only supports single-threaded iteration:
		var seen = new HashSet<Object>();
		return x -> seen.add(keyExtractor.apply(x));
	}

	private StreamUtils() {
	}
}
