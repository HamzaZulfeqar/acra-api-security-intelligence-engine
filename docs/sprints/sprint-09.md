# Sprint 9 — Property-Level Authorization & Field Policy Intelligence

Status: IN PROGRESS — Phase 1 candidate implemented, verification pending  
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

## Verification

Pending GitHub Actions execution on `s9-property-authorization`.

Until that workflow passes, Phase 1 remains IMPLEMENTED / UNVERIFIED.
