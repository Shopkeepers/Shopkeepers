package com.nisovin.shopkeepers.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An array-based list that can provide a snapshot which can be iterated without the risk of running into a
 * {@link ConcurrentModificationException} in case the list is modified during the iteration. Any such modifications are
 * not reflected in the currently ongoing iteration(s) over the returned snapshot(s).
 * <p>
 * Regular iterations, which use one of the iterators returned by this list instead of iterating over an snapshot, do
 * not provide this guarantee.
 * <p>
 * Unlike {@link CopyOnWriteArrayList} this implementation is not thread-safe and only creates a new snapshot if there
 * have been modifications since the last time a snapshot has been created.
 * <p>
 * The array returned by {@link #getSnapshotArray()} is not safe to be modified and therefore this class is only meant
 * to be used internally.
 * 
 * @param <E>
 *            the element type
 */
public class SnapshotList<E> extends AbstractList<E> {

	private final Class<E> elementClass;
	private final List<E> delegateList;
	private E[] snapshotArray = null; // null if dirty
	private List<E> snapshotList = null; // null if dirty

	public SnapshotList(Class<E> elementClass) {
		this.elementClass = elementClass;
		this.delegateList = new ArrayList<>();
	}

	public SnapshotList(Class<E> elementClass, int initialCapacity) {
		this.elementClass = elementClass;
		this.delegateList = new ArrayList<>(initialCapacity);
	}

	public SnapshotList(Class<E> elementClass, Collection<? extends E> collection) {
		this.elementClass = elementClass;
		this.delegateList = new ArrayList<>(collection.size());
		this.addAll(collection);
	}

	@Override
	public E get(int index) {
		return delegateList.get(index);
	}

	@Override
	public int size() {
		return delegateList.size();
	}

	@Override
	public E set(int index, E element) {
		this.markDirty();
		return delegateList.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		this.markDirty();
		super.modCount++;
		delegateList.add(index, element);
	}

	@Override
	public E remove(int index) {
		this.markDirty();
		super.modCount++;
		return delegateList.remove(index);
	}

	private void markDirty() {
		snapshotArray = null;
		snapshotList = null;
	}

	/**
	 * Gets a snapshot array of the list's contents.
	 * <p>
	 * A new snapshot is only created if there have been modifications since the last time a snapshot has been created.
	 * <p>
	 * Since the returned array may be reused in future calls to this method, it is not meant to be modified! This
	 * method is only provided for internal use (such as very fast iteration with low overhead).
	 * 
	 * @return the snapshot array
	 */
	@SuppressWarnings("unchecked")
	public E[] getSnapshotArray() {
		if (snapshotArray == null) {
			snapshotArray = delegateList.toArray((E[]) Array.newInstance(elementClass, 0));
		}
		return snapshotArray;
	}

	/**
	 * Gets an unmodifiable snapshot of the list's contents.
	 * <p>
	 * A new snapshot is only created if there have been modifications since the last time a snapshot has been created.
	 * 
	 * @return an unmodifiable snapshot of the list's contents
	 */
	public List<E> getSnapshotList() {
		if (snapshotList == null) {
			snapshotList = Collections.unmodifiableList(Arrays.asList(this.getSnapshotArray()));
		}
		return snapshotList;
	}
}
