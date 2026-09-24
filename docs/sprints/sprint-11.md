# Sprint 11 — Finding Review Lifecycle, Reproduction & Research Ground-Truth Closure

Status: **S11 SOFTWARE COMPLETE — final executable closure verified**  
Branch: `s11-finding-lifecycle-reproduction`  
Base: verified Sprint 10 software-complete line `s10-batch-indirect-authorization`

## Final decision

Sprint 11 closes the product gap between review-only `FindingCandidate` output and a human-reviewed,
reproducible finding record. It also closes the labelled ACRA-Lab ground-truth contract needed for the
later A0-A7 comparative research campaign.

The final executable closure passed in GitHub Actions run `36064082001`.

This decision is limited to the verified software/local-synthetic boundary. It does **not** claim:

- real Burp desktop issue publication;
- external-target authorization testing;
- A0-A7 experiment execution;
- Sprint 11 TP/TN/FP/FN, precision, recall or F1;
- real-world scanner accuracy;
- automatic vulnerability confirmation.

## Verified phases

### Phase 1 — finding review lifecycle

Implemented and verified:

- `FindingLifecycleState`;
- `FindingReviewTransition`;
- `ReviewedFinding`;
- `FindingLifecycleService`;
- deterministic finding identity;
- mandatory reviewer/evidence-backed transitions;
- `NEEDS_REVIEW -> VALIDATED -> CONFIRMED` review path;
- explicit `FALSE_POSITIVE` and post-confirmation `ACCEPTED_RISK` dispositions;
- no direct candidate-to-confirmed promotion;
- immutable severity and confidence through lifecycle transitions.

Verification: `Sprint11FindingLifecycleFoundationTestSuite` — **40 assertions PASS**.

### Phase 2 — project-isolated finding review workspace

Implemented and verified:

- `FindingReviewCase`;
- `FindingReviewSnapshot`;
- `FindingReviewWorkspace`;
- project isolation;
- idempotent candidate intake;
- risk/candidate consistency checks;
- deterministic snapshot ordering;
- explicit lifecycle state counts.

Verification: `Sprint11FindingReviewWorkspaceTestSuite` — **29 assertions PASS**.

### Phase 3 — minimized reproduction package

Implemented and verified:

- versioned `s11-finding-reproduction-v1` package;
- finding/candidate/provenance references;
- expected and observed authorization decisions;
- minimized review trail;
- explicit limitations;
- no fabricated raw HTTP;
- no reviewer identity or review-reason export;
- no raw credential export.

Verification: `Sprint11FindingReproductionPackageTestSuite` — **38 assertions PASS**.

### Phase 4 — canonical JSON reproduction export

Implemented and verified:

- deterministic JSON export;
- stable SHA-256;
- Reporter plugin adapter `s11-finding-reproduction-json-v1`;
- explicit `confirmedFinding` state;
- minimized/redacted export boundary.

Verification: `Sprint11FindingReproductionJsonExportTestSuite` — **34 assertions PASS**.

### Phase 5 — SARIF 2.1.0 export

Implemented and verified:

- SARIF 2.1.0 log generation;
- stable rule and fingerprint mapping;
- review state mapped to SARIF review semantics;
- confirmed finding mapped to fail semantics;
- accepted risk represented as accepted external suppression;
- false positive represented without confirmation;
- deterministic SHA-256.

Verification: `Sprint11FindingReproductionSarifExportTestSuite` — **41 assertions PASS**.

### Phase 6 — Burp Issue adapter contract

Implemented and verified:

- core-only `FindingBurpIssueDraft` projection;
- independent severity/confidence mapping;
- confirmed-state publication eligibility;
- Montoya `AuditIssue` materialization adapter in the Burp extension;
- local contract stubs synchronized for legacy Sprint 2/3 offline builds;
- source guard prevents direct `SiteMap.add` publication from the adapter.

Verification: `Sprint11FindingBurpIssueDraftTestSuite` — **39 assertions PASS**.

The adapter compiles against the official Montoya dependency in Maven. Real Burp desktop issue publication
is **UNVERIFIED / DEFERRED**.

### Phase 7 — read-only Findings & Reproduction UI

Implemented and verified:

- `Findings & Reproduction` main ACRA tab;
- Overview;
- Findings;
- Review History;
- JSON Reproduction;
- SARIF;
- Burp Issue Draft;
- no lifecycle mutation or publication actions on this surface;
- secret/reviewer material excluded.

Verification: `Sprint11FindingReviewUiTestSuite` — **47 assertions PASS**.

### Phase 8 — ACRA-Lab authorization research ground truth

Registered and verified:

- dataset: `GT-S11-AUTHORIZATION-RESEARCH`;
- 16 controlled cases;
- 8 authorization dimensions;
- exactly one positive and one negative control per dimension;
- secure/vulnerable expected decisions declared independently of ACRA output;
- dimensions: object, tenant, RBAC, workflow, routing, property, batch and indirect reference;
- secure and intentionally vulnerable localhost ACRA-Lab servers verified against the oracle.

Final ground-truth gate:

```text
SPRINT11_GROUND_TRUTH PASS
cases=16
positive=8
negative=8
dimensions=8
a0_a7=NOT_RUN
metrics=NOT_MEASURED
```

### Phase 9 — security hardening and bounded engineering observation

Security verification:

- secret-bearing candidate/risk/review inputs do not appear in JSON, SARIF or Burp draft outputs;
- reviewer identity/reasons remain structurally excluded;
- deterministic reproduction identities/fingerprints;
- immutable workspace/reproduction projections;
- lifecycle identity validation hardened.

`Sprint11FindingSecurityHardeningTestSuite` — **37 assertions PASS**.

Bounded engineering observation in final run:

```text
cases=1000
exportIterations=2000
intakeMillis=246
exportMillis=1597
jsonBytes=1256
sarifBytes=2196
scope=CONTROLLED_CI_ENGINEERING_OBSERVATION
```

These timing/size values are CI observations, not benchmarks, SLOs or capacity guarantees.

### Phase 10 — regression, packaging and final closure

Final executable closure run: `36064082001` — **SUCCESS**.

Verified in that run:

- Sprint 11 Phase 1-9 gates: PASS;
- Sprint 10 / 9 / 8 / 7 / 6 foundations: PASS;
- official Maven package: PASS;
- Sprint 2 local-contract regression: PASS;
- Sprint 3 local-contract regression: PASS;
- Sprint 4 / 6 / 7 / 8 / 9 / 10 / 11 headless UI regressions: PASS;
- exact Temurin Java 21 execution: PASS;
- deterministic checkpoint packaging: PASS.

Closure checkpoint from executable run:

- archive: `acra-sprint-11-final.zip`;
- SHA-256: `3ca60a82888a0b7649da8433ad670df4dc62f0f168bd88dc814199731ff4fcfd`;
- source entries: 974;
- unsafe paths: 0;
- duplicate entries: 0;
- clean extraction equality: PASS;
- per-file SHA-256 equality: PASS.

The digest above identifies the executable closure head before this documentation synchronization. A
documentation-correct head is revalidated by the same final workflow after source-of-truth updates.

## Research boundary after Sprint 11

The registered later comparison remains:

```text
A0  naive differential baseline
A1  + identity
A2  + ownership
A3  + tenant
A4  + role
A5  + workflow
A6  + semantic evidence
A7  full ACRA correlation
```

All `EXP-A0` through `EXP-A7` remain:

```text
SOFTWARE_READY / RESEARCH_NOT_RUN
metrics = NOT_MEASURED
```

Sprint 12 is the research-execution/evaluation boundary; Sprint 11 does not fabricate those results.
