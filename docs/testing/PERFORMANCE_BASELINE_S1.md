# Sprint 1 Performance Baseline

This is an engineering observation, not a performance claim. Values depend on the execution environment, JVM warmup, GC, CPU scheduling, and the benchmark harness.

Environment observed during Sprint 1 verification:

- Java: 21.0.11
- available processors: 4

| Transactions | URI parse ms | graph construction ms | serialization ms | approximate memory delta bytes |
|---:|---:|---:|---:|---:|
| 1,000 | 94 | 63 | 341 | 10,504,808 |
| 10,000 | 127 | 64 | 1,428 | 23,627,152 |
| 100,000 | 769 | 188 | 12,940 | 26,779,072 |

The memory delta is intentionally labelled approximate because GC makes point-in-time heap deltas non-monotonic. No pass/fail threshold or sub-millisecond assertion is derived from these values.

Raw benchmark output is generated at `build/performance-baseline.txt` and is not required to be committed.
