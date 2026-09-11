# ACRA Sprint 4 Checkpoint Manifest

**Checkpoint identifier:** `S4-RESUME-2026-08-31-CORE-INTEGRATED`  
**Project:** ACRA — API Access Control & Routing Auditor  
**Sprint:** Sprint 4  
**Candidate:** `0.4.0-rc1`  
**Repository root:** `C:\Users\hamzi\Documents\Codex\2026-08-31\referenced-chatgpt-conversation-this-is-an-2\work\acra-sprint-04-v0.4.0-rc1\acra`  
**Git commit:** UNKNOWN — no `.git` metadata is present in the current tree  
**Git branch:** UNKNOWN — no `.git` metadata is present in the current tree  
**Working-tree state:** DIRTY relative to the immutable Sprint 3 baseline; 95 files added, 2 files modified, 0 deleted, 0 renamed. The exact machine-readable inventory is in `docs/sprints/sprint-04-checkpoint.json`.

## Last completed S4 requirement

The recovered implementation slice completed and compiled the reusable controlled-active core: typed `SecurityTest`/mutation/profile contracts, deterministic signatures, request construction, safety guards, hierarchical budgets, concurrency/rate controls, bounded backoff, safety audit, deterministic planner and queue, semantic outcomes, expected-decision resolution, four-way differential structures, immutable evidence/observations, and replay foundations. This is a source-and-compile checkpoint, not a completed Sprint 4 verification release.

## Current active S4 requirement

`Sprint 4 core verification slice`: add and run focused unit/security tests for the recovered contracts and safety invariants. This is the next exact action recorded by `sprint-04-resumption.md`.

## Completed implementation (verified present)

- `core/src/main/java/io/acra/core/active/model/` — 21 typed test, mutation, target, profile, configuration, safety, signature, plan, and reproducibility classes.
- `core/src/main/java/io/acra/core/active/planning/` — deterministic planning input, explicit seed, and planner classes.
- `core/src/main/java/io/acra/core/active/execution/` — request variants/building, queue, transport boundary, cancellation, backoff, executor, and result classes.
- `core/src/main/java/io/acra/core/active/safety/` — consent, environment, hard scope, kill switch, hierarchical budgets/concurrency, rate limiter, validation, and mutation-budget classes.
- `core/src/main/java/io/acra/core/active/analysis/` — outcome normalization, expected-decision resolution, pairwise and multi-way differential classes.
- `core/src/main/java/io/acra/core/active/evidence/` — snapshots, evidence chain, safety audit, execution fingerprint, observation, and append-only evidence store.
- `core/src/main/java/io/acra/core/active/replay/` — replay descriptor, result, and service.
- `docs/sprints/sprint-04-reconciliation.md` — repository-first S4 gate.
- `docs/sprints/sprint-04-resumption.md` — recovered continuation state.

## Partial implementation

Planner, request builder, controls, safety controls, queue, executor orchestration, normalization, differential, observation, evidence, and replay source are present and compile, but lack direct Sprint 4 executable evidence. Existing S3 graph integration has not been added for active observations.

## Unverified implementation

Sprint 4 test source, test compilation, adapter compilation, localhost transport, ACRA-Lab active execution, secure/vulnerable controls, FP/FN cases, performance workloads, research experiment, active UI, packaging build, and current Burp runtime remain unverified. No finding or metric is inferred from source presence.

## Tests passed

- **NEWLY EXECUTED 2026-08-31:** core main-source Java 21 compile with `--release 21 -Xlint:all -Werror`; PASS, 266 class files produced in disposable `build/compile-check-core/`.
- **HISTORICAL/REPRODUCED BEFORE THIS EXPORT:** Sprint 1–3 baseline assertions PASS, 147 total; Sprint 3 secure ACRA-Lab reconnaissance PASS, 10/10; Sprint 3 dry-run dispatch PASS, 0 requests; Sprint 3 controlled fixture metrics reproduced.

## Tests pending

Sprint 4 core contract/security tests, adapter tests, local lab execution, secure/vulnerable controls, FP/FN and baseline comparison, request-efficiency measurement, deterministic research record, and synthetic performance workloads.

## Build status

Core main source: PASS under the bundled Java 21-capable toolchain. Full Maven build: BLOCKED / UNVERIFIED because Maven is absent and dependency resolution is unavailable. The repository Bash verifier is BLOCKED on Windows by Unix `:` classpath syntax.

## Lab status

Existing Sprint 3 secure local lab evidence remains PASS for 10 reconnaissance operations. No Sprint 4 active lab execution has occurred at this checkpoint. Vulnerable/secure active-control results are UNVERIFIED.

## Known blockers

- Maven executable absent; official Montoya dependency build and packaging are BLOCKED / UNVERIFIED.
- Outbound artifact/DNS resolution is unavailable.
- Current Burp installation was observed at `C:\Users\hamzi\AppData\Local\Programs\BurpSuite`, but no current runtime validation was executed.
- Historical S2/S3 Burp Level 3 and Level 4 evidence remains BLOCKED / UNVERIFIED and is not rewritten.
- No concrete localhost transport, active UI, S4 tests, S4 lab ground truth, or S4 experiment record exists yet.

## Known technical debt

The state file retains its pre-implementation marker and S3-era next action; it needs a narrow progress update in the continuation task. The Bash verifier is not portable to Windows. OpenAPI support, persistence, semantic inference, route equivalence, and passive UI behavior retain S3 limitations recorded in the reconciliation.

## Next exact action

Add only the focused Sprint 4 core verification tests described in `docs/sprints/sprint-04-resumption.md`, compile them with Java 21, and run them. Do not add a transport, UI, experiment, release package, vulnerability analyzer, or Sprint 5 work until that test slice is evidenced.

## Exact file inventory

`docs/sprints/sprint-04-checkpoint.json` contains the exact added, modified, deleted, and renamed path arrays generated from a SHA-256 comparison against `work/sprint-03-baseline/acra`. Build output is excluded from that inventory and from the checkpoint archive.
