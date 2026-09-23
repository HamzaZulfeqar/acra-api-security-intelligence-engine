# Sprint 6 Performance Observation

Run: `35890873379`  
Commit: `76389e13b996a02c9f7011b262364b5b03a9231d`  
Environment: GitHub Actions Ubuntu runner, Temurin JDK 21.0.12.1

## Scope

The suite measures three deterministic in-process workloads:
1. effective authorization policy resolution;
2. S6 policy-aware planning recommendations; and
3. S6 authorization report generation after projecting each unique resolution into the product workspace.

No network target is involved in this benchmark.

## Observed values

| Count | Resolve | Planning | Report | Approx JVM memory delta | Recommendations |
|---:|---:|---:|---:|---:|---:|
| 100 | 34 ms | 1 ms | 9 ms | 2,948,768 B | 150 |
| 1,000 | 57 ms | 2 ms | 6 ms | 19,825,512 B | 1,500 |
| 10,000 | 169 ms | 4 ms | 29 ms | 10,003,728 B | 15,000 |

All three sizes completed and the generated report contained the expected number of unique analysis records.

## Interpretation boundary

These values are engineering observations only. They depend on GitHub runner CPU scheduling, JVM warmup,
allocation behavior and garbage collection. The memory delta is approximate and can be non-monotonic. No JMH
benchmark, production SLO, throughput guarantee or release threshold is claimed.
