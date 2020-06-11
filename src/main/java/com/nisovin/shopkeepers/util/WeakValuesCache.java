package com.nisovin.shopkeepers.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * A cache that uses {@link WeakReference}s for its values.
 * <p>
 * Stale entries get cleaned up whenever the cache is accessed or when {@link #cleanup()} is invoked.
 * {@link RemovalListener} can be used to get informed whenever an entry is removed from the cache.
 * <p>
 * Does not support <code>null</code> values.
 * <p>
 * Not thread-safe.
 *
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 */
public class WeakValuesCache<K, V> {

	public interface RemovalListener<K> {

		/**
		 * Called whenever an entry has been removed from the cache.
		 * <p>
		 * This is also called whenever the value for a certain key has been replaced.
		 * 
		 * @param key
		 *            the key
		 */
		public void onEntryRemoved(K key);
	}

	private static class WeakEntry<K, V> extends WeakReference<V> {

		final K key;

		WeakEntry(K key, V value, ReferenceQueue<? super V> queue) {
			super(value, queue);
			this.key = key;
		}
	}

	private final Map<K, WeakEntry<K, V>> values = new HashMap<>();
	// Note: Already unmodifiable.
	private final Collection<? extends V> valuesView = new AbstractCollection<V>() {
		@Override
		public Iterator<V> iterator() {
			cleanupStaleEntries();
			return values.values().stream()
					.map(entry -> entry.get())
					.filter(Objects::nonNull)
					.iterator();
		}

		@Override
		public int size() {
			cleanupStaleEntries();
			return values.size();
		}
	};
	private final ReferenceQueue<V> cleanupQueue = new ReferenceQueue<>();
	// Note: We expect the number of listeners to be low, so adding, finding, and removing listeners inside the list
	// should be fast enough.
	private final SnapshotList<RemovalListener<K>> removalListeners = new SnapshotList<>(castClassType(RemovalListener.class));

	// Required to cast from Class<T> to Class<T<X>>:
	@SuppressWarnings("unchecked")
	private static <T, I extends T> Class<T> castClassType(Class<I> clazz) {
		return (Class<T>) clazz;
	}

	public WeakValuesCache() {
	}

	/**
	 * Registers the given {@link RemovalListener}.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void registerRemovalListener(RemovalListener<K> listener) {
		Validate.notNull(listener, "listener is null");
		Validate.isTrue(!removalListeners.contains(listener), "listener is already registered");
		removalListeners.add(listener);
	}

	/**
	 * Unregisters the given {@link RemovalListener}.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void unregisterRemovalListener(RemovalListener<K> listener) {
		removalListeners.remove(listener);
	}

	private void informRemovalListeners(K key) {
		for (RemovalListener<K> listener : removalListeners.getSnapshotArray()) {
			listener.onEntryRemoved(key);
		}
	}

	/**
	 * Gets the number of entries inside the cache.
	 * 
	 * @return the number of cache entries
	 */
	public int getSize() {
		cleanupStaleEntries();
		return values.size();
	}

	/**
	 * Gets all cached values.
	 * 
	 * @return an unmodifiable view on all cached values
	 */
	public Collection<? extends V> getValues() {
		return valuesView;
	}

	/**
	 * Checks if the cache contains an entry for the given key.
	 * 
	 * @param key
	 *            the key
	 * @return <code>true</code> if the cache contains an entry for the given key
	 */
	public boolean containsKey(K key) {
		return (get(key) != null);
	}

	/**
	 * Gets the value to which the given key is currently mapped.
	 * 
	 * @param key
	 *            the key
	 * @return the value, or <code>null</code> if there is no mapping for the given key
	 */
	public V get(K key) {
		WeakEntry<K, V> entry = values.get(key);
		V value = null;
		if (entry != null) {
			value = entry.get(); // can be null
		}
		// Perform cleanup:
		cleanupStaleEntries();
		return value;
	}

	/**
	 * Maps the given key to the given value.
	 * <p>
	 * This replaces any previous mapping for the given key.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value, not <code>null</code>
	 * @return the previous value, or <code>null</code> if there was no mapping for the key previously
	 */
	public V put(K key, V value) {
		Validate.notNull(value, "value is null");
		WeakEntry<K, V> entry = new WeakEntry<K, V>(key, value, cleanupQueue);
		WeakEntry<K, V> oldEntry = values.put(key, entry);
		V oldValue = null;
		if (oldEntry != null) {
			// Entry has been replaced:
			oldValue = oldEntry.get(); // can be null
			// Enqueue it if it hasn't been enqueued yet (see also #remove(K)):
			oldEntry.enqueue();
		}
		// Perform cleanup and inform removal listeners:
		cleanupStaleEntries();
		return oldValue;
	}

	/**
	 * Removes the mapping for the given key.
	 * 
	 * @param key
	 *            the key
	 * @return the old value, or <code>null</code> if there was no mapping for the key
	 */
	public V remove(K key) {
		WeakEntry<K, V> entry = values.remove(key);
		V oldValue = null;
		if (entry != null) {
			oldValue = entry.get(); // can be null
			// The value might not yet been GC'ed (eg. because it is still referenced somewhere). The GC will therefore
			// enqueue the entry later, even though we are no longer interested in the entry. The entry may then remain
			// inside the queue until maintenance is performed (possibly indefinitely if the cache is no longer used,
			// eg. after the cache has been cleared). To prevent this, we explicitly enqueue the entry now already, if
			// it hasn't been enqueued yet. The following cleanup will then inform the removal listeners.
			entry.enqueue();
		}
		// Perform cleanup and inform removal listeners:
		cleanupStaleEntries();
		return oldValue;
	}

	/**
	 * Removes all entries from the cache.
	 */
	public void clear() {
		// We explicitly enqueue all references which have not yet been enqueued. This ensures that we don't end up with
		// stale references inside the queue later. See also #remove(K).
		for (WeakEntry<K, V> entry : values.values()) {
			entry.enqueue();
		}
		values.clear();

		// Empty the queue and inform removal listeners:
		cleanupStaleEntries();
	}

	/**
	 * Triggers a manual cleanup of any stale entries.
	 */
	public void cleanup() {
		this.cleanupStaleEntries();
	}

	@SuppressWarnings("unchecked")
	private void cleanupStaleEntries() {
		WeakEntry<K, V> entry = null;
		while ((entry = (WeakEntry<K, V>) cleanupQueue.poll()) != null) {
			K key = entry.key;
			// Only remove the current mapping if the key is still mapped to the same entry:
			if (values.remove(key, entry)) {
				// Inform removal listeners:
				informRemovalListeners(key);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WeakValuesCache [values=");
		builder.append(values);
		builder.append("]");
		return builder.toString();
	}
}
