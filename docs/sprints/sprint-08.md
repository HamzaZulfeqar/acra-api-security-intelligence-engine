# Sprint 8 — Routing Normalization & Authorization-Path Intelligence

Status: IN PROGRESS  
Branch: `s8-routing-normalization`  
Immutable Sprint 7 base: `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`

## Dependency decision

The repository had no previously assigned Sprint 8 scope. The next dependency is defined from ACRA's established
Routing Analysis Engine priority and the retained routing limitation that syntactic equivalence does not prove
identical proxy/gateway/framework/application resolution.

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
