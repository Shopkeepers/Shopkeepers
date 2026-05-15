package com.nisovin.shopkeepers.util.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class CollectionUtils {

	/**
	 * Replaces the first occurrence of the given element inside the given list.
	 * 
	 * @param <E>
	 *            the element type
	 * @param list
	 *            the list, not <code>null</code>
	 * @param element
	 *            the element to replace
	 * @param replacement
	 *            the replacement
	 * @return <code>true</code> if the element has been found inside the list
	 */
	public static <E> boolean replace(List<E> list, E element, E replacement) {
		if (list instanceof RandomAccess) { // Also checks for null
			int index = list.indexOf(Unsafe.nullableAsNonNull(element));
			if (index != -1) {
				list.set(index, replacement);
				return true;
			}
			return false;
		} else {
			Validate.notNull(list, "list is null");
			ListIterator<E> iterator = list.listIterator();
			while (iterator.hasNext()) {
				E next = iterator.next();
				if (Objects.equals(element, next)) {
					iterator.set(replacement);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Creates a fixed-sized list backed by the given array.
	 * <p>
	 * Unlike {@link Arrays#asList(Object...)} this returns an {@link Collections#emptyList() empty
	 * list} if the given array is <code>null</code>.
	 * 
	 * @param <E>
	 *            the element type
	 * @param array
	 *            the array
	 * @return the list backed by the given array, or an empty list if the given array is
	 *         <code>null</code>
	 */
	@SafeVarargs
	public static <E> List<E> asList(E @Nullable... array) {
		return (array == null) ? Collections.emptyList() : Arrays.asList(array);
	}

	/**
	 * Sorts the given list using the given {@link Comparator} and then returns the list.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <L>
	 *            the type of the list
	 * @param list
	 *            the list to sort
	 * @param comparator
	 *            the comparator
	 * @return the given list sorted
	 * @see List#sort(Comparator)
	 */
	public static <E, L extends List<? extends E>> L sort(
			L list,
			Comparator<? super E> comparator
	) {
		Validate.notNull(list, "list is null");
		Validate.notNull(comparator, "comparator is null");
		// TODO Cast required due to a limitation of CheckerFramework
		list.sort(Unsafe.castNonNull(comparator));
		return list;
	}

	/**
	 * Adds the given elements to the given collection and then returns the collection.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <C>
	 *            the type of the collection
	 * @param collection
	 *            the collection
	 * @param toAdd
	 *            the elements to add
	 * @return the given collection with the elements added
	 * @see Collection#addAll(Collection)
	 */
	public static <E, C extends Collection<? super E>> C addAll(
			C collection,
			Collection<? extends E> toAdd
	) {
		Validate.notNull(collection, "collection is null");
		Validate.notNull(toAdd, "toAdd is null");
		// TODO Cast required due to a limitation of CheckerFramework
		collection.addAll(Unsafe.<Collection<E>>castNonNull(toAdd));
		return collection;
	}

	/**
	 * Searches through the given {@link Iterable} for an element that is accepted by the given
	 * {@link Predicate}.
	 * 
	 * @param <E>
	 *            the element type
	 * @param iterable
	 *            the elements to search through
	 * @param predicate
	 *            the Predicate, not <code>null</code>
	 * @return the first found element accepted by the Predicate, or <code>null</code> if either no
	 *         such element was found, or if the Predicate accepted a <code>null</code> element
	 */
	public static <E> @Nullable E findFirst(
			Iterable<? extends E> iterable,
			Predicate<? super E> predicate
	) {
		Validate.notNull(predicate, "predicate is null");
		for (E element : iterable) {
			if (predicate.test(element)) {
				return element;
			}
		}
		return null;
	}

	public static boolean containsNull(Collection<?> collection) {
		try {
			return collection.contains(Unsafe.uncheckedNull());
		} catch (ClassCastException | NullPointerException e) {
			// Some collection implementations disallow null elements
			// and may throw an exception when we check for null elements.
			return false;
		}
	}

	/**
	 * Checks if the given {@link Iterable} contains an element that is accepted by the given
	 * {@link Predicate}.
	 * 
	 * @param <E>
	 *            the element type
	 * @param iterable
	 *            the elements to search through
	 * @param predicate
	 *            the Predicate, not <code>null</code>
	 * @return <code>true</code> if an element is found that is accepted by the given Predicate
	 */
	public static <E> boolean contains(
			Iterable<? extends E> iterable,
			Predicate<? super E> predicate
	) {
		Validate.notNull(predicate, "predicate is null");
		for (E element : iterable) {
			if (predicate.test(element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a new list that contains both the elements of the given {@link Collection} and the
	 * given object.
	 * 
	 * @param <E>
	 *            the element type
	 * @param collection
	 *            the collection, not <code>null</code>
	 * @param toAdd
	 *            the object to add, can be <code>null</code>
	 * @return the new list, not <code>null</code>
	 */
	public static <E> List<E> copyAndAdd(Collection<? extends E> collection, E toAdd) {
		Validate.notNull(collection, "collection is null");
		List<E> newList = new ArrayList<>(collection.size() + 1);
		newList.addAll(collection);
		newList.add(toAdd);
		return newList;
	}

	/**
	 * Returns a new unmodifiable list that contains both the elements of the given
	 * {@link Collection} and the given object.
	 * 
	 * @param <E>
	 *            the element type
	 * @param collection
	 *            the collection, not <code>null</code>
	 * @param toAdd
	 *            the object to add, can be <code>null</code>
	 * @return the new list, not <code>null</code>
	 */
	public static <E> List<E> unmodifiableCopyAndAdd(Collection<? extends E> collection, E toAdd) {
		List<E> newList = copyAndAdd(collection, toAdd);
		return Collections.unmodifiableList(newList);
	}

	/**
	 * Returns a new list that contains the elements of both of the given {@link Collection}s.
	 * 
	 * @param <E>
	 *            the element type
	 * @param collection
	 *            the collection, not <code>null</code>
	 * @param toAdd
	 *            the objects to add, not <code>null</code>
	 * @return the new list, not <code>null</code>
	 */
	public static <E> List<E> copyAndAddAll(
			Collection<? extends E> collection,
			Collection<? extends E> toAdd
	) {
		Validate.notNull(collection, "collection is null");
		Validate.notNull(toAdd, "toAdd is null");
		List<E> newList = new ArrayList<>(collection.size() + toAdd.size());
		newList.addAll(collection);
		newList.addAll(toAdd);
		return newList;
	}

	/**
	 * Returns a new unmodifiable list that contains the elements of both of the given
	 * {@link Collection}s.
	 * 
	 * @param <E>
	 *            the element type
	 * @param collection
	 *            the collection, not <code>null</code>
	 * @param toAdd
	 *            the objects to add, not <code>null</code>
	 * @return the new list, not <code>null</code>
	 */
	public static <E> List<E> unmodifiableCopyAndAddAll(
			Collection<? extends E> collection,
			Collection<? extends E> toAdd
	) {
		List<E> newList = copyAndAddAll(collection, toAdd);
		return Collections.unmodifiableList(newList);
	}

	// Note: Does not work for primitive arrays.
	@SafeVarargs
	public static <T> T @Nullable [] concat(T @Nullable [] array1, T @Nullable... array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;

		int length1 = array1.length;
		int length2 = array2.length;
		T @NonNull [] result = Unsafe.assertNonNull(Arrays.copyOf(array1, length1 + length2));
		System.arraycopy(array2, 0, result, length1, length2);
		return result;
	}

	// Note: Does not work for primitive arrays.
	@SafeVarargs
	public static <T> T[] concat(T @Nullable []... arrays) {
		if (arrays == null || arrays.length == 0) {
			return Unsafe.castNonNull(new Object[0]);
		}

		int length = 0;
		for (T @Nullable [] array : arrays) {
			Validate.notNull(array, "array element is null");
			length += Unsafe.assertNonNull(array).length;
		}

		T @NonNull [] result = Unsafe.assertNonNull(Arrays.copyOf(arrays[0], length));
		int resultLength = 0;
		boolean first = true;
		for (T @Nullable [] array : arrays) {
			T[] nonNullArray = Unsafe.assertNonNull(array);
			resultLength += nonNullArray.length;

			if (first) {
				first = false;
				continue;
			}

			System.arraycopy(nonNullArray, 0, result, resultLength, nonNullArray.length);
		}
		return result;
	}

	public static <T> Stream<T> stream(Iterable<T> iterable) {
		if (iterable instanceof Collection) {
			Collection<T> collection = (Collection<T>) iterable;
			return collection.stream();
		} else {
			return StreamSupport.stream(iterable.spliterator(), false);
		}
	}

	// Note: The returned Iterable can only be iterated once!
	public static <T> Iterable<T> toIterable(Stream<T> stream) {
		return stream::iterator;
	}

	@SuppressWarnings("unchecked")
	public static <T> @Nullable T getFirstOrNull(Stream<? extends @Nullable T> stream) {
		// TODO Unnecessary cast: CheckerFramework complains when we use a wildcard here.
		return ((Stream<@Nullable T>) stream).findFirst().orElse(null);
	}

	public static <T> @NonNull T cycleValue(
			List<? extends @NonNull T> values,
			@NonNull T current,
			boolean backwards
	) {
		return cycleValue(values, current, backwards, PredicateUtils.alwaysTrue());
	}

	public static <T> @NonNull T cycleValue(
			List<? extends @NonNull T> values,
			@NonNull T current,
			boolean backwards,
			Predicate<? super @NonNull T> predicate
	) {
		return cycleValue(
				values,
				false,
				current,
				backwards,
				predicate
		);
	}

	public static <T> @Nullable T cycleValueNullable(
			List<? extends @NonNull T> values,
			@Nullable T current,
			boolean backwards
	) {
		return cycleValueNullable(
				values,
				current,
				backwards,
				PredicateUtils.<@Nullable T>alwaysTrue()
		);
	}

	public static <T> @Nullable T cycleValueNullable(
			List<? extends @NonNull T> values,
			@Nullable T current,
			boolean backwards,
			Predicate<? super @Nullable T> predicate
	) {
		return cycleValue(
				values,
				true,
				Unsafe.nullableAsNonNull(current),
				backwards,
				predicate
		);
	}

	// nullable: Uses null as first value.
	// current==null: nullable has to be true.
	// Cycled through all values but none got accepted: Returns current value (can be null).
	public static <T> T cycleValue(
			List<? extends @NonNull T> values,
			boolean nullable,
			T current,
			boolean backwards,
			Predicate<? super T> predicate
	) {
		Validate.notNull(values, "values is null");
		Validate.isTrue(current != null || nullable, "Not nullable, but current is null");
		Validate.notNull(predicate, "predicate is null");
		assert values != null;
		int currentId = (current == null ? -1 : values.indexOf(current));
		int nextId = currentId;
		while (true) {
			if (backwards) {
				nextId -= 1;
				if (nextId < (nullable ? -1 : 0)) {
					nextId = (values.size() - 1);
				}
			} else {
				nextId += 1;
				if (nextId >= values.size()) {
					nextId = (nullable ? -1 : 0);
				}
			}
			if (nextId == currentId) {
				return current;
			}
			T next = (nextId == -1) ? Unsafe.cast(null) : values.get(nextId);
			if (predicate.test(next)) {
				return next;
			}
		}
	}

	private CollectionUtils() {
	}
}
