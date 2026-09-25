# Sprint 11 Requirements Traceability

Date: 2026-09-25  
Decision boundary: **S11 SOFTWARE COMPLETE**

| Requirement | Sprint 11 implementation | Verification | Status |
|---|---|---|---|
| FR-011 — severity and confidence remain independent | `AuthorizationRiskAssessment`, `ReviewedFinding`, lifecycle preserves both independently | lifecycle/workspace/Burp/SARIF suites | COMPLETE |
| FR-012 — defined finding lifecycle | `FindingLifecycleState`, `FindingReviewTransition`, `FindingLifecycleService`, `FindingReviewWorkspace` | 40 lifecycle + 29 workspace assertions | COMPLETE |
| FR-013 — reproduction targets JSON, Burp Issue, SARIF | `FindingReproductionPackage`, JSON exporter, SARIF exporter, Burp issue draft + Montoya adapter | 38 package + 34 JSON + 41 SARIF + 39 Burp draft assertions | COMPLETE |
| FR-014 — vulnerable and secure ACRA-Lab ground truth | `GT-S11-AUTHORIZATION-RESEARCH.json` plus secure/vulnerable localhost oracle verifier | 16 cases, 8 positive, 8 negative, 8 dimensions | COMPLETE |
| FR-015 — baseline/A0-A7 research support | EXP-A0…EXP-A7 registry entries and registered dataset/oracle | dataset/software readiness verified; experiments not executed | SOFTWARE READY / RESEARCH NOT RUN |
| SEC credential minimization | domain redaction + minimized reproduction projection; reviewer/reason excluded | security hardening 37 assertions; JSON/SARIF/Burp tests | COMPLETE |
| Deterministic export identity | finding/reproduction fingerprints and SHA-256 | package/JSON/SARIF/security suites | COMPLETE |
| Human review before confirmation | candidate opens as NEEDS_REVIEW; direct candidate/validated skips rejected | lifecycle suite | COMPLETE |
| False-positive disposition | explicit terminal FALSE_POSITIVE | lifecycle/workspace/UI/SARIF tests | COMPLETE |
| Accepted-risk disposition | only reachable from CONFIRMED | lifecycle/workspace/SARIF tests | COMPLETE |
| Project isolation | review workspace bound to project ID | workspace suite | COMPLETE |
| Read-only product UI | Findings, Review History, JSON, SARIF, Burp Draft | headless UI 47 assertions | COMPLETE |
| Burp Montoya materialization | extension-only `S11BurpIssueAdapter` compiles against Montoya 2026.7 | official Maven package PASS | COMPLETE |
| Real Burp publication | deliberately not performed by adapter or CI | direct SiteMap publication source guard | DEFERRED |
| A0-A7 measured metrics | no execution in Sprint 11 | oracle reports A0_A7_NOT_RUN; metrics NOT_MEASURED | DEFERRED TO SPRINT 12 |
| Controlled engineering observation | 1,000 review records + 2,000 JSON/SARIF export repetitions | bounded performance suite PASS | COMPLETE / OBSERVATIONAL |
| Full regression | retained S6-S10 foundations, S2/S3 contracts, multiple headless UI suites | final run `36064082001` | COMPLETE |
| Reproducible checkpoint | deterministic source ZIP, manifest, SHA-256, clean extraction verification | final package gate | COMPLETE |

## Explicit exclusions

The traceability table does not convert deferred validation/research into completion. In particular:

- real Burp desktop load/publication remains UNVERIFIED / DEFERRED;
- no external-target validation is claimed;
- EXP-A0 through EXP-A7 are NOT_RUN;
- Sprint 11 TP/TN/FP/FN, precision, recall and F1 are NOT_MEASURED;
- controlled CI timing values are not production performance guarantees.
