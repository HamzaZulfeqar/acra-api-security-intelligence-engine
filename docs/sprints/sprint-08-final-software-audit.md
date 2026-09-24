# Sprint 8 Final Software Audit

Audit state: **FINAL CANDIDATE — closure verification pending**  
Branch: `s8-routing-normalization`

## Current decision

**DO NOT mark S8 SOFTWARE COMPLETE yet.**

Phases 1–7 of the defined routing-normalization and authorization-path intelligence scope have executable
verification evidence. The remaining decision gate is the dedicated final closure workflow covering retained
regressions, official Maven packaging, extension/UI compilation, deterministic source packaging, safe archive
paths and clean-extraction/per-file SHA-256 equality.

## Implemented software boundary

Sprint 8 currently includes:
- staged RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION route evidence;
- deterministic route normalization/equivalence reasoning;
- routing/authorization boundary differential intelligence;
- controlled localhost route-equivalence validation;
- provenance-gated routing assessments and review-only FindingCandidate projection;
- Routing product workspace and Burp UI;
- deterministic JSON/SHA-256/Markdown report export;
- shared redaction hardening;
- security-hardening and bounded performance-observation suites.

## Verified pre-final evidence

- Phase 1 run `35961851498`: SUCCESS
- Phase 2 run `35965612498`: SUCCESS
- Phase 3 run `35966112820`: SUCCESS
- Phase 4 run `35969974964`: SUCCESS
- Phase 5 run `35970306700`: SUCCESS
- Phase 6 run `35970917885`: SUCCESS
- Phase 7 run `35971212640`: SUCCESS

Latest Phase 7 evidence includes:
- Sprint 8 routing security hardening: 17 assertions PASS
- Sprint 8 routing performance observations: 13 assertions PASS
- 100 / 1,000 / 10,000 routing contexts completed
- retained Sprint 7 foundation: PASS
- Maven extension test compilation: PASS
- retained Sprint 4 / Sprint 6 / Sprint 7 / Sprint 8 headless UI lanes: PASS

The performance values are engineering observations only, not benchmarks, SLOs, release thresholds or
real-world capacity claims.

## Required final gates

1. exact Temurin Java 21 Sprint 8 core/test compilation with warnings as errors;
2. all Sprint 8 normalization/boundary/live/assessment/report/security/performance suites;
3. controlled secure/vulnerable localhost route-equivalence validation;
4. retained Sprint 7 foundation;
5. retained Sprint 6 foundation;
6. official Maven package;
7. retained Sprint 2 local-contract regression;
8. retained Sprint 3 local-contract regression;
9. Sprint 4, Sprint 6, Sprint 7 and Sprint 8 headless UI regressions;
10. deterministic Sprint 8 source checkpoint generation;
11. archive safe-path validation;
12. clean extraction equality and per-file SHA-256 equality.

## Explicit non-blocking validation debt

Real Burp desktop load/handler/UI runtime and external-target routing validation remain
**UNVERIFIED / DEFERRED**. They are not replaced by localhost execution or headless Swing verification.

No real-world routing/proxy accuracy, production-authentication, capacity, external-target safety or automatic
confirmed-vulnerability claim is made.

## Promotion rule

Only after all twelve final gates pass may the repository state be promoted from
**S8 FINAL CANDIDATE** to **S8 SOFTWARE COMPLETE**.
