package benchmark;

import cache.CountMinSketch;
import cache.LRUCache;
import cache.StreamCache;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Benchmark {

    public static void main(String[] args) {
        System.out.println("=== StreamCache vs Plain LRU ===");
        System.out.println("(500 hot keys, cache size 100, 500k accesses, Zipfian distribution)\n");
        runPure("Pure Zipfian — no scan",          100, 500, 500_000);
        runMixed("Mixed: 30% scan traffic",         100, 500, 0.30, 500_000);

        demoCMS();
    }

    // Scenario 1: pure Zipfian hot-key traffic, no scan.
    static void runPure(String name, int capacity, int hotKeys, int totalOps) {
        LRUCache<Integer, String> lru = new LRUCache<>(capacity);
        StreamCache<Integer, String> sc  = new StreamCache<>(capacity, 3);
        int[] zipf = precomputeZipf(hotKeys, totalOps, new Random(42));
        int lruHits = 0, scHits = 0;

        for (int i = 0; i < totalOps; i++) {
            int key = zipf[i];
            if (lru.get(key) != null) lruHits++; else lru.put(key, "v");
            if (sc.get(key)  != null) scHits++;  else sc.put(key, "v");
        }
        System.out.printf("%-35s  LRU: %5.1f%%   StreamCache: %5.1f%%%n",
            name, 100.0 * lruHits / totalOps, 100.0 * scHits / totalOps);
    }

    // Scenario 2: scanFraction of each op is a unique one-time key (the adversarial case).
    // LRU admits every scan key, constantly evicting hot keys to make room.
    // StreamCache blocks them — scan keys are never seen enough to cross the threshold.
    static void runMixed(String name, int capacity, int hotKeys, double scanFraction, int totalOps) {
        LRUCache<Integer, String> lru = new LRUCache<>(capacity);
        StreamCache<Integer, String> sc  = new StreamCache<>(capacity, 3);
        int[] zipf   = precomputeZipf(hotKeys, totalOps, new Random(42));
        Random rng   = new Random(99);
        int scanKey  = hotKeys;
        int lruHits = 0, scHits = 0;

        for (int i = 0; i < totalOps; i++) {
            int key = rng.nextDouble() < scanFraction ? scanKey++ : zipf[i];
            if (lru.get(key) != null) lruHits++; else lru.put(key, "v");
            if (sc.get(key)  != null) scHits++;  else sc.put(key, "v");
        }
        System.out.printf("%-35s  LRU: %5.1f%%   StreamCache: %5.1f%%%n",
            name, 100.0 * lruHits / totalOps, 100.0 * scHits / totalOps);
    }

    // Shows Count-Min Sketch accuracy on a 100k-event Zipfian stream.
    // CMS never undercounts; error is bounded by ε × N with high probability.
    static void demoCMS() {
        int n = 1000, events = 100_000;
        CountMinSketch cms = new CountMinSketch(0.001, 0.99);
        Map<Integer, Integer> exact = new HashMap<>();
        int[] zipf = precomputeZipf(n, events, new Random(7));

        for (int key : zipf) { cms.increment(key); exact.merge(key, 1, Integer::sum); }

        System.out.println("\n=== Count-Min Sketch accuracy (top 10 keys, 100k-event stream) ===");
        System.out.printf("  ε=0.001 → max error ≤ %.0f  (ε × N = 0.001 × %,d)%n%n", 0.001 * events, events);
        exact.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> {
                int est = cms.estimate(e.getKey());
                System.out.printf("  key %4d: true=%-6d  est=%-6d  error=%+d%n",
                    e.getKey(), e.getValue(), est, est - e.getValue());
            });
        System.out.println("\n  Errors are always ≥ 0 (CMS only overcounts, never undercounts).");
    }

    // Precomputes Zipf-distributed samples via CDF binary search. O(n) build, O(log n) per sample.
    static int[] precomputeZipf(int n, int count, Random rng) {
        double harmonic = 0;
        for (int i = 1; i <= n; i++) harmonic += 1.0 / i;

        double[] cdf = new double[n];
        double acc = 0;
        for (int i = 0; i < n; i++) { acc += 1.0 / (i + 1); cdf[i] = acc / harmonic; }

        int[] samples = new int[count];
        for (int s = 0; s < count; s++) {
            double r = rng.nextDouble();
            int lo = 0, hi = n - 1;
            while (lo < hi) { int mid = (lo + hi) / 2; if (cdf[mid] < r) lo = mid + 1; else hi = mid; }
            samples[s] = lo;
        }
        return samples;
    }
}
