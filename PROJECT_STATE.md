# ACRA Project State

## Current state — 2026-09-23

**Current sprint:** Sprint 6 — Tenant Isolation, Advanced RBAC and Policy Intelligence.  
**Decision:** S6 IN PROGRESS.  
**Working branch:** `s6-tenant-rbac`.  
**Immutable Sprint 5 base:** `89ceb1eec0aca7ebb2c5db60b4bfce43251d0f84`.  
**Sprint 5:** SOFTWARE COMPLETE and preserved separately.  
**Sprint 7:** NOT STARTED.

### Implemented S6 capabilities

- authorization scope taxonomy
- tenant membership and multi-role assignment
- role hierarchy with cycle-safe inheritance
- permission and role-permission models
- explicit ALLOW / DENY rules
- explicit default decision and evidence-backed precedence
- global, shared and delegated authorization
- effective role and permission resolution
- tenant relationship classification
- tenant-isolation and RBAC assessments
- explicit privileged-action role-escalation assessment
- policy-conflict assessment
- policy coverage and effective authorization matrix
- S6 composition into the existing S5 FindingCandidate and severity/risk architecture
- policy hydration into the existing SecurityContextGraph with atomic evidence preflight
- deterministic root-cause grouping foundation
- policy-import plugin boundary and manual policy builder
- controlled S6 ACRA-Lab ground truth and secure/vulnerable tenant/RBAC fixtures
- policy-aware planning recommendations
- live controlled tenant/RBAC experiment and measurement pipeline
- policy-aware S6 planning bridge into the existing S4 planner/queue/executor
- deterministic safe read-only CROSS_TENANT test generation
- live generated-test execution against secure localhost ACRA-Lab
- ROLE_COMPARISON active generation intentionally fail-closed pending credential-safe authenticated-context substitution

### Newly measured S6 evidence

GitHub Actions run `35878508170` completed successfully on exact Temurin JDK 21.0.12.1.

`EXP-S6-TENANT-RBAC-001` executed ten labelled localhost cases.

- naive baseline: TP=2, TN=1, FP=7, FN=0, precision=0.222222, recall=1.000000, F1=0.363636
- ACRA S6: TP=2, TN=8, FP=0, FN=0, precision=1.000000, recall=1.000000, F1=1.000000

These are controlled-fixture measurements only. They do not establish real-world scanner accuracy.

The campaign also exposed and closed a SHARED-scope isolation defect before the successful run.

### Remaining S6 closure work

- fuller planner→controlled execution product integration
- UI for policy, role hierarchy, tenant map, effective permissions, conflicts and coverage
- report/export extension for S6 policy evidence
- larger performance workloads and memory profiling
- expanded negative-security and serialization audit
- requirements traceability / ADR closure
- full S1–S6 regression and Maven/package verification
- final Sprint 6 audit
- reproducible S6 ZIP + SHA-256 + manifest

Historical S2/S3 real Burp runtime validation remains separate and is not promoted by Sprint 6 evidence.
