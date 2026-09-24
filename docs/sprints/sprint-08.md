# Sprint 8 — Routing Normalization & Authorization-Path Intelligence

Status: IN PROGRESS — PHASE 5 VERIFIED  
Branch: `s8-routing-normalization`  
Immutable Sprint 7 base: `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`

## Dependency decision

The recovered ACRA roadmap assigns Sprint 8 to Advanced URI, Routing, Normalization and Security-Boundary
Differential Analysis. The repository implementation therefore continues the established Routing Analysis Engine
priority and the retained Sprint 3 limitation that syntactic equivalence does not prove identical
proxy/gateway/framework/application resolution.

Sprint 8 extends the existing Sprint 3 route layer. It does not replace `RouteTemplateEngine`,
`RouteEquivalenceEngine`, URI extraction, the security-context graph, or the active-testing engine.

## Sprint boundary

Sprint 8 owns evidence-backed reasoning about how a request path is represented across:

```
RAW URI
  ↓
PROXY
  ↓
GATEWAY
  ↓
FRAMEWORK
  ↓
APPLICATION
```

Only explicit observations/imported/configured facts may populate a processing stage. Missing stages remain
missing; ACRA must not simulate or invent intermediary behavior.

## Phase 1 — staged normalization trace foundation

Implemented:
- `RouteProcessingStage`
- `RouteObservationSource`
- `RouteStageObservation`
- `RouteNormalizationDivergenceKind`
- `RouteNormalizationTraceState`
- `RouteNormalizationTransition`
- `RouteNormalizationTrace`
- `RouteNormalizationAnalyzer`
- reuse of the existing `RouteEquivalenceEngine`
- provenance evidence required for every populated stage
- path-only stage observations; query/fragment material rejected
- deterministic stage ordering
- duplicate-stage rejection
- explicit missing-stage accounting
- stage-gap attribution marked INCONCLUSIVE
- representation change separated from canonical divergence
- no vulnerability/finding promotion in this phase

## Phase 1 acceptance boundary

The foundation must prove:
1. unchanged five-stage traces do not invent divergence;
2. canonical-equivalent normalization is represented as a representation change;
3. encoded-separator/path-shape changes can be recorded as canonical divergence when directly observed;
4. same-family differences remain distinct from canonical divergence;
5. missing intermediary stages prevent attribution;
6. duplicate or provenance-free observations fail closed;
7. the existing Sprint 3 route behavior remains green;
8. retained Sprint 7 foundation remains green on exact Java 21.

No external targets, bypass generation, proxy fingerprinting or vulnerability classification are part of Phase 1.


## Phase 1 verification

GitHub Actions run `35961851498`: **SUCCESS**.

- exact Java 21 compile with `-Xlint:all -Werror`: PASS
- `Sprint8RoutingNormalizationFoundationTestSuite`: PASS
- retained Sprint 3 route/core behavior: PASS
- retained Sprint 7 foundation: PASS

Phase 1 is complete.


## Phase 2 — authorization-path differential intelligence

Implemented:
- `RouteBoundaryObservation` for evidence-backed path/method/host/API-version/authorization observations
- `RouteSecurityBoundaryAnalyzer`
- `RouteBoundaryTransition`
- `RouteSecurityBoundaryTrace`
- explicit states: `STABLE`, `ROUTING_DIVERGENCE`, `AUTHORIZATION_BOUNDARY_CHANGE`,
  `COMBINED_DIVERGENCE`, `INCONCLUSIVE`
- route-only changes separated from authorization-decision changes
- method, host and API-version changes represented independently of path normalization
- combined route + authorization changes preserved without automatic vulnerability promotion
- missing intermediary stages remain `INCONCLUSIVE`
- unknown authorization context remains `INCONCLUSIVE`
- host observations reject schemes, paths and user-info
- route observations remain path-only and provenance-backed

This phase classifies evidence-backed boundary differences. It does not infer proxy/gateway behavior, generate
bypass payloads, scan external targets, or promote a routing differential into a confirmed vulnerability.

### Phase 2 verification

GitHub Actions run `35965612498`: **SUCCESS**.

- Sprint 8 normalization foundation: PASS
- `Sprint8AuthorizationPathDifferentialTestSuite`: PASS
- retained Sprint 3 route/core behavior: PASS
- retained Sprint 7 foundation: PASS
- exact Temurin Java 21 verification: PASS

Phase 2 is complete. Sprint 8 remains IN PROGRESS.


## Phase 3 — controlled route-equivalence differential validation

Implemented:
- controlled Sprint 8 ACRA-Lab routing ground truth
- canonical and duplicate-separator route representations for the same admin route family
- secure fixture preserves authorization across equivalent URI representations
- deliberately vulnerable fixture models a route-normalization authorization mismatch
- existing `EQUIVALENT_ROUTE_REPRESENTATION` mutation type reused
- existing `UriMutationAdapter`, `RequestEquivalenceGuard`, planner, queue, safety validation and `TestExecutor` reused
- explicit authorized localhost scope includes both canonical and equivalent route prefixes
- no scope-guard bypass or external-target execution
- raw bearer material remains excluded from serialized test state

Controlled route pair:

```
/api/v1/s8/admin
/api//v1/s8/admin
```

Ground truth requires the non-admin viewer to remain denied on both representations.

### Phase 3 verification

GitHub Actions run `35966112820`: **SUCCESS**.

- routing ground-truth contract: PASS, 3 cases
- routing normalization foundation: PASS, 16 assertions
- authorization-path differential suite: PASS, 19 assertions
- controlled live routing differential: PASS, 10 assertions
- secure fixture: expected DENY, observed DENY, `NO_CHANGE`
- deliberately vulnerable fixture: expected DENY, observed ALLOW, `UNEXPECTED_CHANGE`
- retained Sprint 3 core: PASS, 47 tests
- retained Sprint 7 foundation/assessment/coverage/reporting/security/performance: PASS

Phase 3 is complete. Sprint 8 remains IN PROGRESS.

## Phase 4 — provenance-gated routing assessment and FindingCandidate

Implemented:
- `RouteAuthorizationAssessmentState`
- `RouteAuthorizationAssessment`
- `S8RoutingAssessmentEvaluator`
- `S8RoutingAnalysisRequest`
- `S8RoutingFindingCandidateEvaluator`
- routing-only candidate rule: an evidence-backed routing change must exist
- candidate rule requires explicit expected `DENY` and observed `ALLOW`
- routing-stable authorization differences are not promoted as Sprint 8 routing candidates
- incomplete routing/authorization attribution remains `INCONCLUSIVE`
- evidence object ownership and observation lineage validated through the existing `EvidenceReferenceValidator`
- cross-project provenance mismatch fails closed
- FindingCandidate remains review-only; no automatic confirmed-vulnerability state is introduced
- secure controlled routing evidence resolves to `REJECTED`
- deliberately vulnerable controlled routing evidence resolves to `CANDIDATE`

### Phase 4 verification

GitHub Actions run `35969974964`: **SUCCESS**.

- routing ground-truth contract: PASS
- routing normalization foundation: PASS, 16 assertions
- authorization-path differential suite: PASS, 19 assertions
- controlled live routing + assessment/provenance integration: PASS, 22 assertions
- secure fixture: DENY / `NO_CHANGE` → routing assessment `NO_VIOLATION` → finding `REJECTED`
- deliberately vulnerable fixture: ALLOW / `UNEXPECTED_CHANGE` → routing assessment `CANDIDATE` → provenance-verified FindingCandidate `CANDIDATE`
- cross-project provenance tampering: `INCONCLUSIVE`
- retained Sprint 3 core: PASS, 47 tests
- retained Sprint 7 foundation/assessment/coverage/reporting/security/performance: PASS

Phase 4 is complete. Sprint 8 remains IN PROGRESS.

## Phase 5 — routing product workspace and Burp UI

Implemented:
- `S8RoutingWorkspace`
- immutable `S8RoutingProductSnapshot`
- `S8RoutingPanel`
- top-level Burp `Routing` product area
- Overview / Stage Traces / Boundary Matrix / Assessments / Candidates views
- backward-compatible `AcraSuiteTab` constructor chain
- read-only projection of normalization traces, boundary transitions, assessments and review-only candidates
- explicit UI language separating routing divergence from vulnerability severity
- explicit UI language preserving unknown processing stages as unknown
- no new active-execution controls introduced by the Routing UI

### Phase 5 verification

GitHub Actions run `35970306700`: **SUCCESS**.

- Sprint 8 core/live verification: PASS
- retained Sprint 7 foundation: PASS
- real extension Maven `test-compile`: PASS
- retained Sprint 4 UI: PASS, 26 checks
- retained Sprint 6 Authorization UI: PASS, 27 assertions
- retained Sprint 7 Workflow UI: PASS, 20 assertions
- Sprint 8 Routing UI: PASS, 17 assertions
- controlled routing logs artifact upload: PASS

Phase 5 is complete. Sprint 8 remains IN PROGRESS.
