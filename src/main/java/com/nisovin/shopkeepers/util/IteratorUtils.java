package com.nisovin.shopkeepers.util;

import java.util.Iterator;
import java.util.Objects;

public class IteratorUtils {

	private IteratorUtils() {
	}

	/**
	 * Joins the {@link String#valueOf(Object) String representations} of the elements returned by the given
	 * {@link Iterator} to produce a String with the format <code>[element1, element2, ...]</code>.
	 * <p>
	 * If the given Iterator is <code>null</code>, the String <code>"null"</code> is returned.
	 * 
	 * @param iterator
	 *            the iterator
	 * @return the String
	 */
	public static String toString(Iterator<?> iterator) {
		if (iterator == null) return "null";
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		if (iterator.hasNext()) {
			Object element = iterator.next();
			builder.append(element);
		}
		while (iterator.hasNext()) {
			Object element = iterator.next();
			builder.append(", ");
			builder.append(element);
		}
		builder.append(']');
		return builder.toString();
	}

	public static int hashCode(Iterator<?> iterator) {
		if (iterator == null) return 0;
		int hashCode = 1;
		while (iterator.hasNext()) {
			Object element = iterator.next();
			hashCode = 31 * hashCode + ((element == null) ? 0 : element.hashCode());
		}
		return hashCode;
	}

	public static boolean equals(Iterator<?> iterator1, Iterator<?> iterator2) {
		if (iterator1 == iterator2) return true;
		if (iterator1 == null) {
			return (iterator2 == null);
		} else if (iterator2 == null) return false;

		while (iterator1.hasNext()) {
			if (!iterator2.hasNext()) return false;
			Object element1 = iterator1.next();
			Object element2 = iterator2.next();
			if (!Objects.equals(element1, element2)) return false;
		}
		if (iterator2.hasNext()) return false;
		return true;
	}
}
