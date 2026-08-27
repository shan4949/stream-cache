package test;

import cache.LRUCache;
import cache.StreamCache;

public class StreamCacheTest {

    public static void main(String[] args) {
        testLRUEviction();
        testLRUOrdering();
        testAdmissionPolicy();
        testScanResistance();
        System.out.println("\nAll tests passed.");
    }

    static void testLRUEviction() {
        LRUCache<Integer, String> c = new LRUCache<>(3);
        c.put(1, "a"); c.put(2, "b"); c.put(3, "c");
        c.put(4, "d"); // evicts 1, the LRU entry
        assert c.get(1) == null : "key 1 should be evicted";
        assert "d".equals(c.get(4));
        System.out.println("✓ LRU eviction");
    }

    static void testLRUOrdering() {
        LRUCache<Integer, String> c = new LRUCache<>(3);
        c.put(1, "a"); c.put(2, "b"); c.put(3, "c");
        c.get(1); // promotes 1 to MRU; 2 is now LRU
        c.put(4, "d"); // should evict 2
        assert c.get(2) == null : "key 2 should be evicted after access reorders";
        assert "a".equals(c.get(1));
        System.out.println("✓ LRU ordering after access");
    }

    static void testAdmissionPolicy() {
        // threshold=3: a key needs 3 increments before entering the LRU
        StreamCache<Integer, String> sc = new StreamCache<>(10, 3);
        sc.put(1, "a");           // count=1, not admitted
        assert sc.get(1) == null; // count=2, LRU miss
        sc.put(1, "a");           // count=3, admitted to LRU
        assert sc.get(1) != null : "should be admitted once count >= threshold";
        System.out.println("✓ Admission policy");
    }

    static void testScanResistance() {
        int capacity = 100, hotKeys = 50, scanSize = 2000;
        LRUCache<Integer, String> lru = new LRUCache<>(capacity);
        StreamCache<Integer, String> sc  = new StreamCache<>(capacity, 3);

        // warm up hot keys — 5 accesses each so all are admitted to StreamCache
        for (int i = 0; i < hotKeys; i++)
            for (int j = 0; j < 5; j++) { lru.put(i, "h"); sc.put(i, "h"); }

        // sequential scan: 2000 unique keys each seen exactly once
        // LRU admits all of them and evicts hot keys; StreamCache blocks them
        for (int i = hotKeys; i < hotKeys + scanSize; i++) { lru.put(i, "s"); sc.put(i, "s"); }

        int lruHits = 0, scHits = 0;
        for (int i = 0; i < hotKeys; i++) {
            if (lru.get(i) != null) lruHits++;
            if (sc.get(i)  != null) scHits++;
        }

        double lruRate = (double) lruHits / hotKeys;
        double scRate  = (double) scHits  / hotKeys;
        System.out.printf("Post-scan hot-key hits — LRU: %.0f%%  StreamCache: %.0f%%%n",
            lruRate * 100, scRate * 100);

        assert scRate > lruRate + 0.3
            : String.format("StreamCache (%.0f%%) should beat LRU (%.0f%%) by 30%%+ after scan",
                scRate * 100, lruRate * 100);
        System.out.println("✓ Scan resistance");
    }
}
