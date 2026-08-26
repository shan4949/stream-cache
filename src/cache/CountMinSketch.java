package cache;

import java.util.Random;

public class CountMinSketch {
    private final int[][] table;
    private final int[] seeds;
    private final int width;
    private final int depth;

    // epsilon: max relative error (e.g. 0.001), delta: confidence (e.g. 0.99)
    public CountMinSketch(double epsilon, double delta) {
        this.width = (int) Math.ceil(Math.E / epsilon);
        this.depth = (int) Math.ceil(Math.log(1.0 / (1.0 - delta)));
        this.table = new int[depth][width];
        this.seeds = new int[depth];
        Random rng = new Random(42);
        for (int i = 0; i < depth; i++) seeds[i] = rng.nextInt();
    }

    public void increment(Object key) {
        for (int i = 0; i < depth; i++) table[i][col(key, i)]++;
    }

    public int estimate(Object key) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < depth; i++) min = Math.min(min, table[i][col(key, i)]);
        return min;
    }

    // Wang hash mixing to spread bits before modding into column index
    private int col(Object key, int row) {
        int h = key.hashCode() ^ seeds[row];
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return Math.floorMod(h, width);
    }
}
