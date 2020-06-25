package com.nisovin.shopkeepers.util;

import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

	private StreamUtils() {
	}

	public static <T> Stream<T> toStream(Iterator<T> iterator) {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, false);
	}

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<T>) iterable).stream();
		} else {
			return StreamSupport.stream(iterable.spliterator(), false);
		}
	}

	// Note: The returned Iterable can only be iterated once!
	public static <T> Iterable<T> toIterable(Stream<T> stream) {
		return stream::iterator;
	}

	public static <R> Stream<R> mapReferents(Stream<? extends Reference<R>> referenceStream) {
		return referenceStream.map(ref -> ref.get()).filter(Objects::nonNull);
	}
}
