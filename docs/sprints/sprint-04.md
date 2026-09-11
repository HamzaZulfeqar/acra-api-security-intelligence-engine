# Sprint 4 — Controlled Active Differential Testing Engine

**Candidate:** `0.4.0-rc1`  
**State:** S4 SOFTWARE COMPLETE — local product core and source checkpoint verified  
**Execution boundary:** authorized ACRA-Lab / synthetic localhost only

## Phase 3 continuation status — 2026-09-04

The local product core now passes executable profile, selection/user mode, queue, local-consent, backoff/error, resolver, coverage/efficiency, active UI source/binding and research-record verification. The verified graph-hydrating localhost milestone is preserved.

The configured product workspace now proves plan → queue → existing executor → result/coverage. The default Burp extension remains intentionally disabled until explicit configuration/consent; real Burp passive-to-active provisioning is deferred adapter/runtime validation, not a missing local S4 engine requirement. Broad research/performance metrics remain NOT MEASURED; Maven/Montoya and real Burp validation remain blocked/deferred. No Sprint 5 work has begun.

## Completed evidence-backed slices

### S4-00 repository reconciliation

PASS. The current Sprint 4 work was reconciled against the immutable Sprint 3 baseline without restarting or replacing the working tree.

### S4 core verification

PASS. `SecurityTest`, mutation/request construction, planner, queue, safety controls, executor orchestration, semantic outcomes, differential model, Observation/Evidence and replay foundations are covered by 54 core + 41 engine-security assertions.

### S4 localhost transport + ACRA-Lab integration

PASS. `LocalhostHttpTransport` executes only a configured authorized loopback `LAB` target and reuses the existing S4 executor and S3 semantic comparison infrastructure.

Independent ground truth is `lab/ground-truth/GT-EXEC-S4.json`.

The final live secure execution established:

```text
Baseline                ALLOW
Expected-ALLOW control  ALLOW
Expected-DENY control   DENY
Resource mutation       DENY
Differential            EXPECTED_CHANGE
```

The deliberately vulnerable fixture preserved the expected-DENY control but returned ALLOW for the single controlled resource mutation. The engine preserved the mismatch as an Observation and returned `UNEXPECTED_CHANGE`. No vulnerability finding was generated.

Minimum semantic preparation also verifies HTTP-200 application denial, dynamic timestamps/request IDs, formatting/order variation, operational timeout, and credential/cookie redaction.

### Final software audit and dispatch safety

PASS for the scoped safety repair. `RequestEquivalenceGuard` now independently verifies the declared single mutation and preserved request dimensions before dispatch. The executor-level proof records valid mutation dispatch=1, invalid mutation dispatch=0 and contaminated mutation dispatch=0.

The recovery initially produced a historical 50-row audit with an **S4 SOFTWARE PARTIAL** decision. The current source-driven 52-requirement audit in `docs/sprints/sprint-04-final-software-audit.md` supersedes that checkpoint and concludes **S4 SOFTWARE COMPLETE** for the authorized local/synthetic product boundary; this is not Burp, external-target, research-accuracy or final-release validation.

## Current verification counts

- Sprint 4 core: 54 PASS.
- Sprint 4 engine security: 52 PASS.
- Sprint 4 localhost integration: 51 PASS.
- Core regression: 37 PASS.
- Sprint 3 core: 47 PASS.
- Sprint 3 adapter: 11 PASS.
- Sprint 3 security/architecture: PASS.
- Sprint 4 architecture: PASS.

## Research record

`research/EXP-EXEC-001.md` records the live controlled execution. This is Level 4 evidence for the localhost execution-substrate claim only. FP/FN rates, real-world accuracy and novelty are not measured by this experiment.

## Pending Sprint 4 work

- active Observation/Evidence → existing Security Context Graph integration;
- context-completeness blocking through that graph path;
- explicit Beginner/Professional modes and complete selection/profile tests;
- larger labelled FP/FN and baseline comparison campaign;
- request-efficiency and performance measurements;
- active Test Plan/Queue/Detail/Execution/Differential/Evidence/Safety/Experiment UI surfaces;
- complete consent/backoff/queue/resolver branch verification;
- S4 CI/package verification and missing architecture/security documentation;
- documentation/release closure and final candidate package validation;
- current Burp validation only when separately scheduled and executable.

## Historical Burp evidence

S2 Level 3/4 and S3 Level 3/4 remain BLOCKED / UNVERIFIED. Localhost Sprint 4 evidence does not rewrite those records.

## Next task

Implement and verify only the active Observation/Evidence → existing `SecurityContextGraph` adapter. Do not start Sprint 5 analyzers.
