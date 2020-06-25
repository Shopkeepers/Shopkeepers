package com.nisovin.shopkeepers.util;

import java.util.Arrays;

public class ArrayUtils {

	private ArrayUtils() {
	}

	// TODO Unused?
	// Note: Does not work for primitive arrays.
	@SafeVarargs
	public static <T> T[] concat(T[] array1, T... array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;

		int length1 = array1.length;
		int length2 = array2.length;
		T[] result = Arrays.copyOf(array1, length1 + length2);
		System.arraycopy(array2, 0, result, length1, length2);
		return result;
	}
}
