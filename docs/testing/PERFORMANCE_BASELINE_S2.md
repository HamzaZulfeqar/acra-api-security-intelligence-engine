# Sprint 2 Performance Baseline

The canonical retained Sprint 2 measurements are engineering observations only. They depend on JVM state, GC, CPU scheduling, UI event timing and the benchmark harness. They are not scientific benchmarks or release thresholds.

| Transactions | Total analysis | Approx. memory delta | Graph edges | Evidence records |
|---:|---:|---:|---:|---:|
| 100 | ~208 ms | ~5.3 MB | 900 | 1,000 |
| 1,000 | ~327 ms | ~19.1 MB | 9,000 | 10,000 |
| 10,000 | ~2,587 ms | ~148.2 MB | 90,000 | 100,000 |

Fresh verification runs may vary. New observations are written to `docs/testing/artifacts/performance-baseline-s2-rerun.txt` so the canonical RC1 baseline is not overwritten.
