# StreamCache: Scan-Resistant LRU + Count-Min Sketch

## The problem with plain LRU

A plain LRU cache has a known failure mode: a sequential scan (nightly batch job,
full-table read, one-time data load) that's larger than the cache evicts every hot
key to make room for data that's never touched again. Hit rate on the real working
set collapses for the duration of the scan.

## The fix: frequency-based admission

`StreamCache` wraps an LRU cache with an admission gate: a key must be seen at
least `admitThreshold` times before it's allowed into the LRU.

- **Hot Zipfian keys** (accessed repeatedly) cross the threshold quickly and stay cached.
- **Scan keys** (each unique, seen exactly once) never cross the threshold, so they
  never enter the LRU and can never evict hot keys.

## Data structures

| Class | Structure | Why |
|---|---|---|
| `LRUCache` | HashMap + doubly-linked list | O(1) get/put/evict. HashMap for O(1) lookup; linked list to maintain recency order with O(1) move-to-front |
| `CountMinSketch` | 2D array of counters + hash functions | O(1) frequency estimation over an unbounded stream in fixed memory, with bounded error |
| `StreamCache` | LRUCache + HashMap frequency counter | Exact admission counting (swap for CMS when key cardinality is unbounded — see `Benchmark.demoCMS`) |

## How the doubly-linked list works

```
head (MRU sentinel) <-> [most recent] <-> ... <-> [least recent] <-> tail (LRU sentinel)
```

- `get(key)`: look up node in HashMap, move to front → O(1)
- `put(key)`: insert at front; if full, remove from tail → O(1)
- Dummy sentinels eliminate null checks on every pointer operation

## Count-Min Sketch

A CMS with parameters (ε, δ) uses a `depth × width` counter table where:
- `width = ceil(e / ε)` — controls error magnitude
- `depth = ceil(ln(1 / (1 - δ)))` — controls confidence

For any key X: `estimate(X) ≤ trueCount(X) + ε × N` with probability ≥ δ,
where N is total increments across all keys. CMS never undercounts.

With ε=0.001 on a 100k-event stream: max error ≤ 100. In practice, errors on
popular keys are 0 (see benchmark output).

## Results

```
Pure Zipfian — no scan              LRU:  66.6%   StreamCache:  66.6%
Mixed: 30% scan traffic             LRU:  39.2%   StreamCache:  46.6%
```

Without scans, both caches perform identically. Under 30% scan load, StreamCache
holds 7 percentage points more hit rate — because its LRU slots stay reserved for
keys that have proven they'll be needed again.

The test suite (`StreamCacheTest`) also includes a worst-case assertion: after a
scan of 2000 unique keys injected into a cache of size 100 with 50 warm hot keys,
LRU retains 0% of hot keys and StreamCache retains 100%.

## Run

```
bash run.sh
```

Requires Java 8+. No dependencies.
