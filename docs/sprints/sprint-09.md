# Sprint 9 — Property-Level Authorization & Field Policy Intelligence

Status: IN PROGRESS — Phase 1 VERIFIED  
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
