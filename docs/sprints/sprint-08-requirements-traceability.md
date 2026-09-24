# Sprint 8 Requirements Traceability

Status: **SOFTWARE COMPLETE — final closure verified**  
Branch: `s8-routing-normalization`  
Immutable Sprint 7 base: `0a42558e1aadfd31a0dd17ba2479ee99635fbafb`

PASS means source implementation plus executable evidence exists. Sprint 8 software completion is established by
the dedicated final closure workflow. Real Burp desktop runtime remains a separate validation lane and is not
inferred from localhost or headless UI evidence.

| ID | Requirement | Implementation evidence | Verification evidence | Status |
|---|---|---|---|---|
| S8-00 | Start from frozen S7 software-complete base | branch base / project state | repository history | PASS |
| S8-01 | Explicit staged route-processing model | `RouteProcessingStage`, `RouteStageObservation` | Phase 1 / run `35961851498` | PASS |
| S8-02 | Deterministic normalization trace | `RouteNormalizationAnalyzer`, trace/transition models | `Sprint8RoutingNormalizationFoundationTestSuite` | PASS |
| S8-03 | Missing intermediary stages remain unknown/inconclusive | analyzer stage-gap rules | Phase 1/2 suites | PASS |
| S8-04 | Route representation vs family vs canonical divergence taxonomy | `RouteNormalizationDivergenceKind` | Phase 1 suite | PASS |
| S8-05 | Evidence/provenance required for populated routing stages | route observation contracts | Phase 1 + Phase 7 hardening | PASS |
| S8-06 | Path-only routing boundary; query/fragment rejected | route-stage/boundary validation | Phase 1 + Phase 7 hardening | PASS |
| S8-07 | Authorization-path boundary model | `RouteBoundaryObservation`, `RouteBoundaryTransition` | Phase 2 / run `35965612498` | PASS |
| S8-08 | Routing-only vs authorization-only vs combined differential states | `RouteSecurityBoundaryAnalyzer` | `Sprint8AuthorizationPathDifferentialTestSuite` | PASS |
| S8-09 | Method/host/API-version boundary comparison | boundary observation/analyzer | Phase 2 suite | PASS |
| S8-10 | Unknown authorization context fails closed | boundary analyzer | Phase 2 + Phase 7 security suite | PASS |
| S8-11 | Controlled route-normalization ground truth | `GT-S8-ROUTING-NORMALIZATION.json` | Phase 3 lab contract | PASS |
| S8-12 | Safe read-only equivalent-route active validation | existing `EQUIVALENT_ROUTE_REPRESENTATION`, `UriMutationAdapter`, active engine | Phase 3 / run `35966112820` and later gates | PASS |
| S8-13 | Explicit localhost scope for canonical and equivalent forms | `TargetDescriptor` + existing `HardScopeGuard` | controlled live suite | PASS |
| S8-14 | Secure route-equivalence control preserves DENY | secure ACRA-Lab S8 fixture | Phase 3 / later gates | PASS |
| S8-15 | Deliberately vulnerable route-equivalence mismatch produces evidence-backed differential | vulnerable ACRA-Lab S8 fixture | Phase 3 / later gates | PASS |
| S8-16 | Routing assessment separated from raw differential | `RouteAuthorizationAssessment`, `S8RoutingAssessmentEvaluator` | Phase 4 / run `35969974964` | PASS |
| S8-17 | Candidate requires routing change + explicit DENY→ALLOW mismatch | S8 assessment rules | Phase 4 + Phase 7 hardening | PASS |
| S8-18 | Evidence/project/test/execution/observation lineage gate | `S8RoutingFindingCandidateEvaluator` + existing `EvidenceReferenceValidator` | Phase 4 live provenance integration | PASS |
| S8-19 | Cross-project provenance mismatch fails closed | existing evidence validator | Phase 4 controlled suite | PASS |
| S8-20 | FindingCandidate remains review-only | S8 finding evaluator / report boundary | Phase 4 + Phase 6 | PASS |
| S8-21 | Routing product workspace | `S8RoutingWorkspace`, immutable product snapshot | Phase 5 | PASS |
| S8-22 | Burp Routing product area | `S8RoutingPanel`, `AcraSuiteTab` integration | Phase 5 / run `35970306700` | PASS |
| S8-23 | Routing UI preserves unknown-stage and candidate boundaries | Routing Overview/tables | `Sprint8RoutingUiTestSuite` | PASS |
| S8-24 | Deterministic routing report/export | S8 report generator/exporter | Phase 6 / run `35970917885` | PASS |
| S8-25 | Canonical JSON + SHA-256 + Markdown | `S8RoutingReportExporter` | reporting/export suite | PASS |
| S8-26 | Reporter plugin integration | `S8RoutingJsonReporter` | reporting/export suite | PASS |
| S8-27 | Confirmed-finding count remains zero | report summary/export contract | Phase 6/7 tests | PASS |
| S8-28 | Secret-safe export without JSON truncation | shared `UniversalRedactor` hardening | Phase 6 regression + Phase 7 security | PASS |
| S8-29 | Routing mutation/security hardening | URI adapter + route/boundary validation | run `35971212640`, 17 assertions | PASS |
| S8-30 | 100/1k/10k bounded engineering observations | `Sprint8RoutingPerformanceObservationTestSuite` | run `35971212640`, 13 assertions | PASS |
| S8-31 | Retained S2/S3/S4/S6/S7 plus S8 final regression and official Maven package | `scripts/verify-sprint8-final.sh` | run `35971753523` | PASS |
| S8-32 | Reproducible S8 ZIP + manifest + SHA-256 + clean extraction | `scripts/package-sprint8.sh` | run `35971753523` | PASS |
| S8-33 | Real Burp desktop load/handler/UI runtime | separate runtime gate | no current desktop Burp execution | UNVERIFIED / DEFERRED |

## Verified phase gates

- Phase 1: `35961851498` — SUCCESS
- Phase 2: `35965612498` — SUCCESS
- Phase 3: `35966112820` — SUCCESS; current-head revalidation also passed
- Phase 4: `35969974964` — SUCCESS
- Phase 5: `35970306700` — SUCCESS
- Phase 6: `35970917885` — SUCCESS
- Phase 7: `35971212640` — SUCCESS

## Non-blocking exclusions

Sprint 8 does not claim:
- real external proxy/gateway/framework fingerprinting;
- external-target route-bypass scanning;
- real-world scanner accuracy;
- production authentication/session behavior;
- automatic confirmed vulnerabilities;
- real Burp desktop runtime validation;
- property-level authorization (later roadmap);
- OAuth/OIDC/session-refresh intelligence (later roadmap);
- GraphQL/gRPC/WebSocket active validation unless separately implemented and verified.

## Final closure gate

GitHub Actions run `35971753523` completed successfully at source commit
`f6a0c19358b00672711532ec7effe1eed3800e3e`.

Final closure evidence:
- exact Temurin Java 21 build/verification: PASS
- official Maven package: PASS
- Sprint 2 local-contract regression: PASS, 52 tests
- Sprint 3 core regression: PASS, 47 tests
- Sprint 3 adapter regression: PASS, 11 tests
- Sprint 4 UI: PASS, 26 tests
- Sprint 6 Authorization UI: PASS, 27 assertions
- Sprint 7 Workflow UI: PASS, 20 assertions
- Sprint 8 Routing UI: PASS, 23 assertions
- deterministic checkpoint entries: 836
- unsafe paths: 0
- duplicate entries: 0
- clean extraction equality: PASS
- per-file SHA-256 equality: PASS
- closure-candidate source ZIP SHA-256:
  `e063217d3c8795a59ce1cd7e052a2c9b0475a86836239973ef1d470a6c627af7`

The canonical final-status package digest is emitted in the external `.sha256` sidecar after final-status
documentation changes, avoiding self-referential package-hash mutation.
