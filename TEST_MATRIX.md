# Test Matrix

Current S5 defensive verification: `docs/testing/artifacts/verification-s5-defensive.json` and `docs/sprints/sprint-05-final-software-closure.md`. The S5 suite is partial; missing modules have no executable evidence. Historical records below are retained.

Canonical test matrix: [`docs/testing/TEST_MATRIX.md`](docs/testing/TEST_MATRIX.md).

The canonical matrix includes the newly executed `S4-PHASE3` product profiles, selection/user modes, queue, local consent, backoff/error, resolver, coverage/efficiency, research-software and active UI source/binding results, plus the retained verified graph and ACRA-Lab milestones.


S5-03 function-level authorization reasoning foundation added.

## S5 Batch 1 evidence integrity — newly executed status

`Sprint5EvidenceIntegrityTestSuite` was added for evidence-store validation, ownership, project scope, replay separation, immutability, correlation fail-closed behavior, and serialization leakage. The requested `javac --release 21 -Xlint:all -Werror` compile and test run is **BLOCKED / NOT EXECUTED**: the only discovered compiler is OpenJDK 17.0.8, Java 21 release support is unavailable, `javac` is not on PATH, and Maven is unavailable. No new pass count is claimed.

Checkpoint packaging was newly verified separately: 604 files, no unsafe paths, no build/cache entries, clean extraction equality, and a matching external SHA-256 sidecar.

## S5 Batch 2 policy validation — newly executed status

`Sprint5PolicyValidationTestSuite` was added for tenant, workflow, property, conflict, determinism, immutability, provenance, and serialization-security coverage. The Java 21 compile and requested Batch 2/affected test execution are **BLOCKED / NOT EXECUTED** because no Java 21 compiler is available; the available OpenJDK 17.0.8 compiler exits 2 for `--release 21`, and Maven is unavailable. No new test pass count is claimed.

Batch 2 checkpoint packaging was newly verified separately: 609 entries, no unsafe paths, no build/cache entries, clean extraction equality, and a matching external SHA-256 sidecar.

## S5 Batch 3 final integration — current execution status

| Area | Result | Evidence |
|---|---|---|
| Evidence → context → BOLA/BFLA → policy → correlation source integration | COMPLETE | Source trace and fail-closed consumer review |
| Policy observation-reference binding | COMPLETE | `PolicyValidationEvaluator` calls `validateObservation`; focused regression source added |
| Batch 1 evidence integrity tests | BLOCKED | Java 21 compiler unavailable; no test count claimed |
| Batch 2 policy validation tests | BLOCKED | Java 21 compiler unavailable; no test count claimed |
| Batch 3 integration tests | UNVERIFIED | No executable Batch 3 suite available; Java 21 execution blocked |
| Affected S5/S4 regression | UNVERIFIED | Maven unavailable and no configured core test project |
| Java `--release 21 -Xlint:all -Werror` | BLOCKED | Discovered `javac 17.0.8`; exits 2: `release version 21 not supported` |
| Maven | BLOCKED | `mvn` unavailable |
| Exact JDK 21 runtime | UNVERIFIED | Not present in the environment |

No newly executed test pass count is claimed.
