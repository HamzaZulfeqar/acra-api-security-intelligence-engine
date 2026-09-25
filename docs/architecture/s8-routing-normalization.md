# Sprint 8 Routing Normalization Architecture

## Existing layer reused

Sprint 8 builds on:
- `DefaultUriExtractor` for raw/decoded/normalized/canonical URI representations;
- `RouteTemplateEngine` for route syntax/canonical templates;
- `RouteEquivalenceEngine` for syntactic/canonical/family comparison.

Those components remain the only route-template/equivalence engines.

## New evidence-backed layer

```
RouteStageObservation
  stage + path + source + evidence
        ↓
RouteNormalizationAnalyzer
        ↓
existing RouteEquivalenceEngine
        ↓
RouteNormalizationTransition
        ↓
RouteNormalizationTrace
```

Processing order is fixed:

```
RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION
```

A trace may omit stages. The analyzer compares known observations in order, but a comparison crossing a missing
intermediary stage is marked `INCONCLUSIVE` for attribution even when the endpoint representations appear equal.

## Divergence taxonomy

- `NONE`: exact path equality at adjacent observed stages.
- `REPRESENTATION_CHANGE`: different strings with canonical equivalence.
- `FAMILY_VARIATION`: same static route family but non-identical canonical representation.
- `CANONICAL_DIVERGENCE`: directly adjacent evidence resolves to different route structures.
- `INCONCLUSIVE`: attribution cannot be established, including stage gaps.

These are routing observations. None is a vulnerability verdict.

## Provenance and security

Every populated stage requires at least one evidence identifier. Stage values are path-only; query and fragment
data are rejected at this boundary to avoid conflating routing with secret-bearing query state.

The analyzer does not infer proxy, gateway, framework or application behavior from product names, headers,
common defaults or heuristics. Later phases may import evidence from controlled fixtures or supported connectors,
but unknown stages remain unknown.


## Phase 2 authorization-path differential layer

The second layer extends the Phase 1 routing trace with explicit HTTP and authorization context:

```
RouteBoundaryObservation
  stage
  path
  method
  host
  apiVersion
  expectedDecision
  observedDecision
  policyReference
  evidence
        ↓
RouteSecurityBoundaryAnalyzer
        ↓
Phase 1 RouteNormalizationAnalyzer
        +
method / host / version comparison
        +
authorization-decision comparison
        ↓
RouteBoundaryTransition
        ↓
RouteSecurityBoundaryTrace
```

Boundary states:
- `STABLE`: adjacent observations agree and authorization evidence is complete.
- `ROUTING_DIVERGENCE`: path representation/structure, method, host or API version changes while authorization remains stable.
- `AUTHORIZATION_BOUNDARY_CHANGE`: authorization decision changes without a routing change.
- `COMBINED_DIVERGENCE`: routing and authorization both change across the same directly observed transition.
- `INCONCLUSIVE`: stage attribution is incomplete, path equivalence is unknown, or authorization evidence is incomplete.

A combined divergence is not a vulnerability verdict. It is an evidence-backed security-boundary observation that
can be consumed by later controlled differential experiments.


## Phase 3 controlled active-validation path

```
explicit route-equivalence ground truth
        ↓
canonical viewer request
        +
duplicate-separator viewer mutation
        ↓
EQUIVALENT_ROUTE_REPRESENTATION
        ↓
UriMutationAdapter
        ↓
RequestEquivalenceGuard
        ↓
HardScopeGuard
        ↓
authorized localhost ACRA-Lab
        ↓
secure / deliberately vulnerable comparison
        ↓
Observation + MultiWayDifferential
```

The active test reuses the existing Sprint 4 engine. No new transport or parallel mutation engine is introduced.

The HardScopeGuard remains authoritative. Both canonical and equivalent route prefixes must be explicitly
authorized in the localhost target descriptor before the mutation can dispatch.

Secure behavior preserves DENY across equivalent representations. The deliberately vulnerable fixture returns
ALLOW only for the duplicate-separator representation, creating an evidence-backed `UNEXPECTED_CHANGE`.
This remains a controlled research observation and is not automatically promoted to a confirmed vulnerability.

## Phase 4 assessment and finding gate

```
RouteBoundaryTransition
        +
expected policy decision
        +
observed authorization outcome
        ↓
S8RoutingAssessmentEvaluator
        ↓
RouteAuthorizationAssessment
        ↓
EvidenceReferenceValidator
  execution ownership
  test ownership
  observation lineage
  project ownership
        ↓
S8RoutingFindingCandidateEvaluator
        ↓
FindingCandidate
```

Promotion is deliberately conservative. A Sprint 8 routing candidate requires both a routing change and an
explicit DENY→ALLOW policy mismatch. Missing stage attribution, unknown authorization state, invalid evidence,
or cross-project provenance produces `INCONCLUSIVE`. A route-stable authorization difference is outside this
Sprint 8 routing-candidate gate.

The resulting `FindingCandidate` is review-only. It does not represent an automatically confirmed vulnerability.
