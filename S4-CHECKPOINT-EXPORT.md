# ACRA Sprint 4 Checkpoint Export

**Export type:** exact interrupted working-state transfer; no Sprint 4 implementation was performed during this export request.  
**Checkpoint identifier:** `S4-RESUME-2026-08-31-CORE-INTEGRATED`

## 1. Actual repository located

YES. The current modified tree was located by its `PROJECT_STATE.md`, completed `docs/sprints/sprint-04-reconciliation.md`, and 90 recovered `core/active` Java sources. It is not the clean Sprint 3 archive.

## 2. Repository path

`C:\Users\hamzi\Documents\Codex\2026-08-31\referenced-chatgpt-conversation-this-is-an-2\work\acra-sprint-04-v0.4.0-rc1\acra`

Immutable comparison baseline: `work/sprint-03-baseline/acra`. Original archive SHA-256 recorded by reconciliation: `EF9EC889ABA9C7C6FBCE4FB3078140901143BAD56F31852169AD486DF143C4F`.

## 3. Git state

Git commit: UNKNOWN. Git branch: UNKNOWN. No `.git` metadata is present. Working state: DIRTY relative to the immutable baseline, established by SHA-256 comparison.

## 4. Last known checkpoint

The last completed S4 slice was the integrated Java 21 compile of the active-engine core. The pre-implementation S4 gate and reconciliation were complete before that slice. No Sprint 4 test suite or active lab execution had run.

## 5. Verified S4 components

Verified present and structurally implemented: `SecurityTest`; typed mutation/category/profile contracts; deterministic test and plan signatures; request construction; scope, environment, consent and kill-switch guards; hierarchical budgets; concurrency; rate limiting; bounded backoff; safety audit; deterministic planner; queue; priority and deduplication; semantic outcome model; expected-decision resolution; differential model; immutable request/response snapshots; observations; evidence chain/store; replay foundations. Exact class paths are in `docs/sprints/sprint-04-checkpoint.json`.

These are source/compile verifications. They are not claims that the full behavioral contracts have passed Sprint 4 tests.

## 6. Partial S4 components

Planner, baseline/controls, request builder, safety guards, queue, executor orchestration, outcome normalization, differential analysis, observations, evidence, and replay are present but not directly tested in Sprint 4. Active graph hydration is still missing.

## 7. Unverified components

S4 unit/security tests; adapter compilation/tests; concrete localhost transport; ACRA-Lab active execution; secure/vulnerable controls; FP/FN and baseline cases; request-efficiency/performance measurements; active UI; research experiment; full Maven package; and current Burp runtime.

## 8. Files changed

Compared with the immutable Sprint 3 baseline: **95 added, 2 modified, 0 deleted, 0 renamed**. Added files are 90 active-core Java sources, four Sprint 4/checkpoint documents under `docs/sprints/`, and this export report. Modified files are `core/src/main/java/io/acra/core/domain/testing/TestState.java` and `PROJECT_STATE.md`. The exact path inventory is machine-readable in `docs/sprints/sprint-04-checkpoint.json`; build output is excluded.

## 9. Tests and exact status

- **NEWLY EXECUTED 2026-08-31:** Java 21 core main-source compile with `--release 21 -Xlint:all -Werror`: PASS, 266 class files in disposable `build/compile-check-core/`.
- **HISTORICAL/REPRODUCED:** Sprint 1–3 baseline assertions PASS, 147; Sprint 3 secure ACRA-Lab reconnaissance PASS, 10/10; Sprint 3 dry-run dispatch PASS, 0 requests; Sprint 3 controlled metrics reproduced within fixture scope.
- Sprint 4 tests: UNVERIFIED / PENDING. No new tests were run for this export.

## 10. Build status

Core main source: PASS. Full Maven build: BLOCKED / UNVERIFIED because Maven is absent and dependency resolution is unavailable. Bash verifier: BLOCKED on Windows due to Unix classpath syntax.

## 11. ACRA-Lab status

Existing Sprint 3 secure local lab evidence: PASS, 10/10 reconnaissance operations. Sprint 4 active execution, vulnerable/secure control results, and FP/FN results: UNVERIFIED. No new lab process or experiment was started.

## 12. Outstanding blockers

- Maven and external artifact resolution are unavailable.
- Burp is installed on the current machine, but current runtime validation was not executed.
- Historical S2 Burp Level 3/4 and S3 Burp Level 3/4 remain BLOCKED / UNVERIFIED.
- No concrete localhost transport, S4 tests, active UI, graph adapter, or experiment record exists.
- `PROJECT_STATE.md` still has its pre-implementation S4 marker, S3-era next action, and historical “Burp not installed” environment wording; these fields need a narrow continuation update, not a broad rewrite.

## 13. Next exact implementation task

Add, compile, and run the focused Sprint 4 core contract/security test slice listed in `docs/sprints/sprint-04-resumption.md`. Continue afterward only from observed test evidence; do not begin Sprint 5 or infer findings.

## 14. Archive name

`acra-sprint-04-current-checkpoint.zip`

## 15. Archive size

Recorded after packaging in `acra-sprint-04-checkpoint-verification.txt`.

## 16. SHA-256

Recorded after packaging in `acra-sprint-04-current-checkpoint.zip.sha256` and linked from the final response. It is computed over the archive created from this current modified tree after excluding only disposable build artifacts.
