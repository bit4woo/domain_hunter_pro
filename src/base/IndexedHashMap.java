package base;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe indexed map: keeps insertion order and supports get by index.
 * Uses a single monitor to synchronize updates that touch both the map and the index list.
 */
public class IndexedHashMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final long serialVersionUID = 1L;

    // index list - protected by `indexLock` for operations that must be atomic with the map
    private final List<K> index = new ArrayList<>();
    // single lock to protect cross-structure invariants
    private final Object indexLock = new Object();

    @Override
    public V put(K key, V val) {
        // We need to ensure the map update and index update are atomic w.r.t each other.
        synchronized (indexLock) {
            V previous = super.put(key, val);
            if (previous == null) {
                // new mapping: append to index list
                index.add(key);
            } else {
                // existing mapping replaced: keep index list unchanged
            }
            return previous;
        }
    }

    @Override
    public V remove(Object key) {
        synchronized (indexLock) {
            V previous = super.remove(key);
            if (previous != null) {
                // remove first occurrence from index list
                index.remove(key);
            }
            return previous;
        }
    }

    /**
     * Remove by index (returns the removed value)
     */
    public V remove(int idx) {
        K key;
        synchronized (indexLock) {
            key = index.get(idx);
            // reuse remove(Object) which already synchronizes
        }
        return remove(key);
    }

    /**
     * Get by index
     */
    public V get(int idx) {
        K key;
        synchronized (indexLock) {
            key = index.get(idx);
        }
        return super.get(key);
    }

    @Override
    public V get(Object key) {
        return super.get(key);
    }

    /**
     * Return index of key. This method is synchronized on indexLock to keep view consistent.
     */
    public int indexOfKey(K key) {
        synchronized (indexLock) {
            return index.indexOf(key);
        }
    }

    @Override
    public int size() {
        synchronized (indexLock) {
            int idxSize = index.size();
            int mapSize = super.size();
            if (idxSize != mapSize) {
                // keep the error behavior for early detection, but provide helpful message
                throw new IllegalStateException("IndexedHashMap error: size not match!!! index.size="
                        + idxSize + " map.size=" + mapSize);
            }
            return mapSize;
        }
    }

    // optional helpers
    public K keyAt(int idx) {
        synchronized (indexLock) {
            return index.get(idx);
        }
    }
}

