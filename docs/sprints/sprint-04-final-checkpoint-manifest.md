# Sprint 4 Final Software Checkpoint Manifest

**Checkpoint:** `S4-FINAL-SOFTWARE-CHECKPOINT-2026-09-05`  
**Candidate:** `0.4.0-rc1`  
**Decision:** S4 SOFTWARE COMPLETE  
**Repository:** current in-place recovered S4 working tree  
**Git metadata:** unavailable; this recovered tree has no `.git` directory  
**Baseline checkpoint:** `acra-sprint-04-graph-integration-verified-checkpoint.zip`  
**Package:** `acra-sprint-04-final-software-checkpoint.zip`

## Software verification

- Core and all core tests compile with `--release 21 -Xlint:all -Werror`.
- Full extension main/test source compiles warning-clean against the retained Montoya contract stubs.
- Represented assertions: 558 PASS.
- Configured workspace plan → queue → existing executor → result/coverage: PASS.
- Existing graph hydration/provenance/replay/isolation/atomicity milestone: PASS.
- Fresh secure/vulnerable localhost ACRA-Lab pipeline: PASS, 60 assertions; services stopped afterward.
- POM XML and architecture isolation checks: PASS.

## Final 52-requirement classification

| Status | Count |
|---|---:|
| COMPLETE | 45 |
| PARTIAL | 0 |
| MISSING | 0 |
| WEAK | 0 |
| UNVERIFIED | 1 |
| BLOCKED | 1 |
| DEFERRED | 5 |
| NOT APPLICABLE | 0 |

The UNVERIFIED row is exact JDK 21 runtime execution. The BLOCKED row is Maven/official Montoya artifact build because `mvn` is unavailable. Deferred rows are the broad labelled research campaign, S4 performance campaign, real Burp provisioning/runtime and authorized external validation. These are validation/research lanes, not missing local S4 software.

## Package policy

The checkpoint contains source, tests, UI, configuration, ACRA-Lab, scripts, documentation and research records. It excludes disposable `build/`, `target/`, `.class`, `__pycache__` and temporary cache artifacts. The external SHA-256 sidecar and verification report provide the archive hash and clean-unpack comparison so the archive does not self-reference its own digest.

## Boundary

No Sprint 5, BOLA, BFLA, vulnerability classifier or external-target scanner is present. Observations remain distinct from confirmed findings. Sprint 5 was not started.
