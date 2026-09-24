# Sprint 9 — Property-Level Authorization & Field Policy Intelligence

Status: SOFTWARE COMPLETE  
Branch: `s9-property-authorization`  
Immutable Sprint 8 base: `21635d900ad80cd27e4f9212b8448bbf5b4cd7f2`

## Dependency decision

Sprint 8 is SOFTWARE COMPLETE. Its final traceability explicitly leaves property-level authorization as later
roadmap scope.

Repository inspection shows that Sprint 5 already implemented the typed property-policy foundation:
`PropertyAuthorizationAssessment`, `PropertyPolicy`, property READ/UPDATE policy review, and correlation into
the authorization analysis pipeline. Sprint 9 therefore extends that existing foundation instead of replacing it.

The product/research scope is aligned with OWASP API3:2023 Broken Object Property Level Authorization:
property-level read exposure and unauthorized property modification share the root problem of absent or
incorrect authorization at an individual object property.

## Sprint boundary

Sprint 9 is responsible for making property authorization a complete evidence-backed ACRA capability while
preserving the project's existing review-only finding boundary.

Planned dependency order:

1. evidence-backed property observation and deterministic policy correlation;
2. controlled ACRA-Lab property read/update ground truth;
3. safe property test planning/execution through the existing S4 engine;
4. provenance-gated assessment/finding integration;
5. property coverage accounting;
6. Burp product workspace/UI;
7. deterministic report/export;
8. security hardening and bounded performance observations;
9. final regression, reproducible checkpoint and software audit.

Later phases remain subject to repository evidence and successful preceding gates.

## Phase 1 — evidence-backed property policy correlation

Candidate implementation:

- `PropertyAccessObservation`;
- `S9PropertyAuthorizationAnalyzer`;
- `S9PropertyAuthorizationAnalysis`;
- reuse of `PropertyAuthorizationAssessment`;
- reuse of `PolicyValidationEvaluator.PropertyPolicy`;
- reuse of `AuthorizationDimensionEvaluator`;
- existing `EvidenceReferenceValidator` for project/test/execution/observation/evidence lineage;
- exact endpoint + property + operation policy matching;
- explicit role/tenant applicability;
- no inferred policy precedence;
- missing policy → INCONCLUSIVE;
- multiple applicable policies → explicit ambiguity;
- property-specific expected decision separated from object-level request authorization;
- deterministic analysis identity;
- no property values stored;
- no vulnerability auto-confirmation.

## Phase 1 acceptance boundary

Phase 1 must prove:

1. explicit property DENY + observed ALLOW produces a review-only property candidate;
2. explicit property DENY + observed DENY does not produce a candidate;
3. missing property policy fails closed;
4. ambiguous applicable policies are not promoted;
5. cross-project provenance cannot produce a candidate;
6. input ordering does not change the analysis identity;
7. secret-bearing observation metadata is rejected;
8. retained Sprint 5 property/orchestrator behavior remains green;
9. retained Sprint 6 policy foundation remains green;
10. retained Sprint 7 workflow foundation remains green;
11. retained Sprint 8 routing-normalization foundation remains green;
12. exact Java 21 compilation with warnings as errors and Maven core test compilation pass.

## Explicit Phase 1 exclusions

Phase 1 does not:

- enumerate hidden fields;
- fuzz arbitrary request properties;
- mutate production/external targets;
- infer that a property is sensitive from its name;
- infer authorization policy from response shape;
- perform GraphQL field mutation;
- perform OAuth/OIDC scope manipulation;
- auto-confirm an API3 vulnerability;
- replace the existing S4 safety/consent/scope gates;
- claim real Burp desktop runtime validation.

## Phase 1 verification

GitHub Actions run `35985518829`: **SUCCESS** at commit
`2487f8600049c6805eda60550e0fb29ec54e5309`.

Verified gates:

- exact Temurin Java 21 setup: PASS;
- all core source/test compilation with `-Xlint:all -Werror`: PASS;
- `Sprint9PropertyAuthorizationFoundationTestSuite`: PASS;
- retained Sprint 5 final closure suite: PASS;
- retained Sprint 6 policy foundation: PASS;
- retained Sprint 7 workflow-authorization foundation: PASS;
- retained Sprint 8 routing-normalization foundation: PASS;
- Maven core `test-compile`: PASS.

Phase 1 is therefore **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. No Phase 2 controlled ACRA-Lab property execution is claimed yet.


## Phase 2 — controlled ACRA-Lab property ground truth

Implemented:

- `GT-S9-PROPERTY-AUTHORIZATION.json` with five explicit policy cases;
- secure and deliberately vulnerable localhost-only profile fixtures;
- secure property READ behavior that omits denied `salary_band` and `is_admin`;
- vulnerable property READ behavior that exposes those explicitly declared fixture properties;
- secure UPDATE behavior that denies `is_admin`;
- vulnerable UPDATE behavior that applies `is_admin`;
- positive control allowing `display_name` update;
- cross-object control preserved as DENY in both lab modes so Sprint 9 does not introduce a separate BOLA fixture;
- `Sprint9ControlledPropertyLabTestSuite`;
- Sprint 9 verification script starts both isolated lab modes and validates the ground-truth contract.

The fixture uses only explicit synthetic properties. It does not enumerate hidden fields or infer sensitivity from names.

### Phase 2 verification

GitHub Actions run `35986020761`: **SUCCESS** at commit
`824aa74ea451f52321c4c7663c961ea7cafbd9c9`.

Verified:

- Python lab syntax: PASS;
- five-case Sprint 9 ground-truth contract: PASS;
- secure/vulnerable localhost lab readiness: PASS;
- `Sprint9PropertyAuthorizationFoundationTestSuite`: PASS;
- `Sprint9ControlledPropertyLabTestSuite`: PASS;
- retained Sprint 5 final closure: PASS;
- retained Sprint 6 policy foundation: PASS;
- retained Sprint 7 workflow foundation: PASS;
- retained Sprint 8 routing-normalization foundation: PASS;
- Maven core `test-compile`: PASS.

Phase 2 is **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. Phase 3 must reuse the existing S4 planner/safety/executor path for controlled
property mutation; it must not introduce an ad-hoc HTTP execution engine.


## Phase 3 — existing active-engine property execution integration

Implemented:

- reuse of existing `TestContract.PROPERTY`;
- reuse of existing `MutationType.PROPERTY`;
- reuse of existing BODY mutation support and exact-one replacement;
- reuse of `RequestEquivalenceGuard`, planner, queue, consent, scope, budget, concurrency, rate-limit and kill-switch gates;
- `STATE_CHANGING` classification for the controlled privileged property update;
- authorized localhost LAB target only;
- no destructive-operation approval path used;
- `Sprint9ControlledPropertyExecutionTestSuite`;
- secure fixture: explicit property policy DENY, observed DENY;
- deliberately vulnerable fixture: explicit property policy DENY, observed ALLOW;
- executor evidence converted to object references for property-policy validation;
- cross-project live evidence fails closed;
- raw synthetic bearer material excluded from serialized test state.

During Phase 3 integration, a provenance-boundary mismatch was identified: executor `Observation.evidenceIds()`
contain evidence-chain IDs, while `EvidenceReferenceValidator.validateEvidenceReferences` validates stored object
references. Sprint 9 now follows the existing Sprint 8 pattern by validating evidence object references and
Observation lineage independently rather than weakening either invariant.

### Phase 3 verification

GitHub Actions run `35986668193`: **SUCCESS** at commit
`abfb40e4e0b754d231a15b136576ac5474b8f380`.

Verified:

- exact Java 21 compilation with warnings as errors: PASS;
- Sprint 9 Phase 1 foundation: PASS;
- controlled property lab: PASS;
- controlled active property execution: PASS;
- secure DENY path: PASS;
- vulnerable ALLOW / unexpected differential path: PASS;
- live evidence → property assessment correlation: PASS;
- cross-project provenance rejection: PASS;
- retained Sprint 5/6/7/8 foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 3 is **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. Phase 4 must project provenance-verified property assessments into the existing
review-only `FindingCandidate` model without adding an automatic confirmed-vulnerability state.


## Phase 4 — provenance-gated property FindingCandidate projection

Implemented:

- `S9PropertyFindingRequest`;
- `S9PropertyFindingCandidateEvaluator`;
- independent evidence-object and Observation-lineage validation;
- endpoint/policy/expected/observed consistency checks between assessment and finding request;
- dimensions `PROPERTY` plus operation-specific `PROPERTY_READ` / `PROPERTY_UPDATE`;
- secure verified property assessment → `REJECTED`;
- verified DENY→ALLOW property mismatch → review-only `CANDIDATE`;
- missing/invalid provenance or inconsistent attribution → `INCONCLUSIVE`;
- cross-project evidence → `INCONCLUSIVE`;
- deterministic candidate/fingerprint generation;
- explicit rationale that a candidate is not an automatically confirmed vulnerability.

### Phase 4 verification

GitHub Actions run `35986888660`: **SUCCESS** at commit
`91a6ed9b4ac4bfb9609324b09e5f2f0fa540babe`.

Verified:

- full Sprint 9 verification script: PASS;
- secure live property evidence → FindingCandidate REJECTED: PASS;
- vulnerable live DENY→ALLOW property evidence → FindingCandidate CANDIDATE: PASS;
- cross-project finding provenance → INCONCLUSIVE: PASS;
- review-only wording: PASS;
- retained Sprint 5/6/7/8 regression foundations: PASS;
- Maven core `test-compile`: PASS.

Phase 4 is **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. Phase 5 owns property-authorization coverage accounting; absence of an observed
property/policy combination must remain visible rather than being treated as tested or secure.


## Phase 5 — property-authorization coverage accounting

Implemented:

- `PropertyCoverageDisposition`;
- `PropertyAuthorizationCoverageEntry`;
- `PropertyAuthorizationCoverageSummary`;
- `S9PropertyCoverageTracker`;
- deterministic policy-context identity based on explicit policy reference, endpoint, property, operation, role and tenant;
- separate READ and UPDATE coverage counts;
- policy-only / unobserved contexts preserved;
- observed-but-unassessed contexts preserved;
- assessed contexts classified as CANDIDATE, REJECTED or INCONCLUSIVE;
- duplicate policy registration does not inflate the denominator;
- policy drift for an existing coverage identity is rejected;
- observation, assessment and finding-projection identity checks;
- deterministic coverage ordering and discoverable missing/inconclusive coverage IDs.

The denominator is the explicit supplied property-policy universe. Missing execution evidence is never interpreted
as a secure result.

### Phase 5 verification

GitHub Actions run `35987236797`: **SUCCESS** at commit
`e76ac4457fbc7343ff8a918bb9d15342847d845b`.

Verified:

- Sprint 9 property coverage test suite: PASS;
- READ/UPDATE policy-context separation: PASS;
- unobserved context accounting: PASS;
- observed-unassessed accounting: PASS;
- candidate/rejected/inconclusive accounting: PASS;
- deterministic ordering: PASS;
- operation-mismatched observation rejection: PASS;
- full retained Sprint 9 + S5/S6/S7/S8 verification script: PASS;
- Maven core `test-compile`: PASS.

Phase 5 is **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. Phase 6 owns the property-authorization product workspace / Burp UI projection.
Real Burp desktop runtime validation remains a separate deferred gate.


## Phase 6 — property product workspace and Burp UI

Implemented:

- `S9PropertyProductSnapshot`;
- `S9PropertyWorkspace` as the single read-only product projection source;
- `S9PropertyPanel`;
- top-level Burp `Properties` product area;
- Overview / Policies / Observations / Assessments / Candidates / Coverage views;
- explicit READ versus UPDATE coverage counts;
- explicit unobserved and observed-unassessed coverage states;
- candidate / rejected / inconclusive projections without confirmed-vulnerability promotion;
- property-value minimization boundary: property values are not rendered by the workspace;
- backward-compatible `AcraSuiteTab` integration;
- `Sprint9PropertyUiTestSuite`.

### Phase 6 verification

GitHub Actions run `36001111468`: **SUCCESS** at commit
`18f3cb03dddf59b0cd054b9184e607be934127f6`.

Verified:

- full Sprint 9 core/lab/property verification: PASS;
- Maven core test compilation: PASS;
- Maven Burp extension test compilation: PASS;
- retained Sprint 4 UI: PASS;
- retained Sprint 6 Authorization UI: PASS;
- retained Sprint 7 Workflow UI: PASS;
- retained Sprint 8 Routing UI: PASS;
- Sprint 9 Property UI: PASS;
- candidate != confirmed vulnerability wording: PASS;
- unobserved != secure wording: PASS;
- property-value non-rendering checks: PASS.

Phase 6 is **VERIFIED COMPLETE**.

Real Burp desktop runtime/load/handler/UI validation remains **UNVERIFIED / DEFERRED**. Headless Swing verification
does not replace real Burp desktop validation.

Sprint 9 remains IN PROGRESS. Phase 7 owns deterministic property report/export from the same S9 workspace source
of truth.


## Phase 7 — deterministic property report/export

Implemented:

- versioned `S9PropertyReport`;
- `S9PropertyReportSummary`;
- deterministic `S9PropertyReportGenerator`;
- canonical secret-safe JSON export + SHA-256;
- deterministic Markdown review report;
- `S9PropertyJsonReporter` on the existing Reporter plugin boundary;
- report/export methods on the single `S9PropertyWorkspace` source of truth;
- Properties UI `Report` / `JSON Export` views;
- explicit `confirmedFindingCount = 0`;
- unobserved property-policy coverage retained in report/export;
- review-only FindingCandidate reporting;
- property values absent from report model and UI projection.

Core reporting verification: GitHub Actions run `36001581140` — **SUCCESS** at commit
`d9ab3e14a7012da8eded4b00fe1ba8fe2b639c98`.

Final Phase 7 UI/report verification: GitHub Actions run `36001743072` — **SUCCESS** at commit
`bf7b222245f6699d5f3dc778ac249475914e2b92`.

Verified:

- deterministic canonical JSON: PASS;
- stable SHA-256 digest for identical report content: PASS;
- Markdown review export: PASS;
- Reporter plugin adapter: PASS;
- secret exclusion: PASS;
- property-value minimization: PASS;
- unobserved coverage preservation: PASS;
- `confirmedFindingCount = 0`: PASS;
- Maven core and extension compilation: PASS;
- retained S4/S6/S7/S8 UI regressions: PASS;
- S9 Property UI Report / JSON Export views: PASS.

Phase 7 is **VERIFIED COMPLETE**.

Sprint 9 remains IN PROGRESS. Phase 8 owns security hardening and bounded performance observations before final
traceability, reproducible packaging and software closure.


## Phase 8 — security hardening and bounded performance observations

Security-hardening coverage:

- property observation query material rejected;
- property observation fragment material rejected;
- provenance-free property observations rejected;
- duplicate evidence references rejected;
- secret-bearing property metadata rejected;
- operation-mismatched coverage observations rejected;
- policy decision drift for an existing coverage identity rejected;
- controlled property BODY mutation requires exact-one replacement;
- duplicate matching property occurrences fail closed;
- privileged property mutation remains `STATE_CHANGING`;
- property workspace rejects non-property FindingCandidate projections;
- property report cannot auto-confirm findings;
- embedded bearer secret redaction preserves later report fields.

Bounded engineering observations cover 100 / 1,000 / 10,000 explicit property-policy contexts across:

- property workspace / coverage population;
- deterministic property report generation.

### Phase 8 verification

GitHub Actions run `36002106088`: **SUCCESS** at commit
`6fd524f349ff77005efac42e8ae5b96201ab7c1b`.

- `Sprint9PropertySecurityHardeningTestSuite`: PASS, 14 assertions;
- `Sprint9PropertyPerformanceObservationTestSuite`: PASS;
- Maven core and extension compilation: PASS;
- retained S4/S6/S7/S8 UI regressions: PASS;
- S9 Property UI regression: PASS;
- performance CSV artifact upload: PASS.

Observed CI values:

| Property-policy contexts | Workspace population | Report generation | Approx JVM memory delta |
|---:|---:|---:|---:|
| 100 | 98 ms | 27 ms | 3,187,608 bytes |
| 1,000 | 109 ms | 6 ms | 2,381,752 bytes |
| 10,000 | 228 ms | 40 ms | 114,832,128 bytes |

These values are engineering observations from one CI environment. They are not benchmarks, SLOs, release
thresholds, real-world capacity claims, or evidence of scanner accuracy.

Phase 8 is **VERIFIED COMPLETE**.

Remaining Sprint 9 work is final requirements traceability, retained regression, reproducible source packaging
and final software audit. Real Burp desktop runtime remains a separate **UNVERIFIED / DEFERRED** lane.


## Final closure — S9 SOFTWARE COMPLETE

GitHub Actions run `36002545177` passed the dedicated Sprint 9 final closure workflow at source commit
`9bffa59c1b360b3e74e0b5e97d2ce22a06dc73f5`.

Final verification included:

- exact Temurin Java 21 compilation and all Sprint 9 verification suites;
- controlled secure/vulnerable localhost property authorization validation;
- property FindingCandidate, coverage, reporting, security and performance suites;
- retained Sprint 8 / Sprint 7 / Sprint 6 foundations;
- official Maven package;
- retained Sprint 2 local-contract regression: 52 tests;
- retained Sprint 3 core regression: 47 tests;
- retained Sprint 3 adapter regression: 11 tests;
- Sprint 4 UI: 26 tests;
- Sprint 6 Authorization UI: 27 assertions;
- Sprint 7 Workflow UI: 20 assertions;
- Sprint 8 Routing UI: 23 assertions;
- Sprint 9 Property UI: 59 assertions;
- deterministic source checkpoint generation;
- archive safe-path and duplicate-entry validation;
- clean-extraction equality;
- per-file SHA-256 equality.

Closure-candidate checkpoint: `acra-sprint-09-final.zip`  
Closure-candidate SHA-256: `04eaadbfb2e132eb386ab52ff775fb5e1c56f0313b610721a28f2fb63bf8e531`  
Entries: **873**  
Unsafe paths: **0**  
Duplicate entries: **0**  
Clean extraction: **PASS**  
Per-file SHA-256 equality: **PASS**

Real Burp desktop runtime/load/handler/UI validation remains a separate **UNVERIFIED / DEFERRED** validation
lane. Controlled localhost and headless UI evidence do not establish production authentication behavior,
real-world scanner accuracy/capacity, or external-target safety.

Sprint 10 is **NOT STARTED** by this closure.


## Phase 2 — controlled property-level ACRA-Lab ground truth

Implemented and verified:

- `GT-S9-PROPERTY-AUTHORIZATION.json` with five independently declared cases;
- explicit allowed `display_name` READ control;
- explicit denied `salary_band` READ case;
- explicit allowed `display_name` UPDATE control;
- explicit denied `is_admin` UPDATE case;
- cross-object control remains denied so the property fixture does not introduce a separate BOLA defect;
- secure localhost fixture omits denied properties and rejects privileged property updates;
- deliberately vulnerable localhost fixture exposes denied properties and accepts the explicit privileged update;
- no hidden-property discovery, schema fuzzing or external-target execution.

Verification run `35986020761`: **SUCCESS** at commit
`824aa74ea451f52321c4c7663c961ea7cafbd9c9`.

The full Sprint 9 verification script and Maven core `test-compile` passed after repair of an intermediate
lab-file patching defect. The two failed intermediate workflow runs are retained as development history and do
not represent the verified Phase 2 state.

Phase 2 is **VERIFIED COMPLETE**.

## Phase 3 — property test planning and controlled execution

Candidate implementation now uses the existing S4 active engine:

```text
Explicit PROPERTY TestSeed
        ↓
Existing TestPlanner
        ↓
ExecutionQueue
        ↓
MutationValidator
  + Scope / Environment / Consent
  + Request Equivalence
  + Budget / Concurrency / Rate Gates
        ↓
Existing TestExecutor
        ↓
Secure / Vulnerable ACRA-Lab
        ↓
Observation + Evidence + Differential
```

The candidate test changes only one explicit request-body property, uses `SafetyClass.STATE_CHANGING`,
requires explicit confirmation, permits only localhost `PATCH`, and does not add automatic production execution.

Phase 3 verification is pending the GitHub Actions gate.
