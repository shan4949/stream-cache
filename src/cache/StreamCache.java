package cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Frequency-filtered LRU cache. Keys must be seen at least `admitThreshold`
 * times before entering the LRU. One-time sequential scans never cross the
 * threshold, so they can't evict hot keys.
 *
 * Admission uses an exact HashMap counter. A Count-Min Sketch would work here
 * too (see CountMinSketch.java + Benchmark.demoCMS), but only when key
 * cardinality is so large the HashMap itself becomes the bottleneck.
 * ponytail: exact counting; swap for CMS if key space is unbounded
 */
public class StreamCache<K, V> {
    private final LRUCache<K, V> lru;
    private final Map<K, Integer> freq = new HashMap<>();
    private final int admitThreshold;

    public StreamCache(int capacity, int admitThreshold) {
        this.lru = new LRUCache<>(capacity);
        this.admitThreshold = admitThreshold;
    }

    public V get(K key) {
        freq.merge(key, 1, Integer::sum);
        return lru.get(key);
    }

    public void put(K key, V value) {
        if (freq.merge(key, 1, Integer::sum) >= admitThreshold) lru.put(key, value);
    }

    public int size()     { return lru.size(); }
    public int capacity() { return lru.capacity(); }
}
