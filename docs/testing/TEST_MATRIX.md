# Test Matrix

## Sprint 6 controlled tenant/RBAC validation — 2026-09-23

GitHub Actions run `35878508170`: **PASS** on exact Temurin JDK 21.0.12.1.

| Test / Experiment | Coverage | Result |
|---|---|---|
| Sprint6PolicyFoundationTestSuite | scope, membership, multi-role, hierarchy, policy snapshot, secret safety | PASS |
| Sprint6PolicyResolutionTestSuite | effective permissions, global/delegated/shared scope, precedence/conflict, shared-scope isolation | PASS |
| Sprint6OrchestrationTestSuite | S5→S6 orchestration, tenant/RBAC/finding/risk composition | PASS |
| Sprint6GraphAndGroupingTestSuite | policy graph hydration, evidence preflight, root-cause grouping | PASS |
| Sprint6LabAndPlanningTestSuite | policy-aware CROSS_TENANT / ROLE_COMPARISON planning recommendations | PASS |
| Sprint6LiveLabExperimentTestSuite | live secure/vulnerable localhost campaign | PASS, 16 assertions |
| EXP-S6-TENANT-RBAC-001 baseline | 10 labelled cases | TP=2 TN=1 FP=7 FN=0, precision=.222222 recall=1.0 F1=.363636 |
| EXP-S6-TENANT-RBAC-001 ACRA | 10 labelled cases | TP=2 TN=8 FP=0 FN=0, precision=1.0 recall=1.0 F1=1.0 |

The metrics above apply only to the controlled localhost dataset and are not real-world accuracy claims.

### Sprint 6 reporting/export

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-REPORT-001 | deterministic report model / content identity | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-002 | canonical JSON + SHA-256 | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-003 | Markdown review report | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-004 | raw-secret exclusion / redaction | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-005 | candidate != confirmed finding boundary | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-006 | Reporter plugin adapter | Sprint6ReportingExportTestSuite | PENDING CI |
| TEST-S6-REPORT-UI-001 | Report + JSON Export product views | Sprint6AuthorizationUiTestSuite | PENDING CI |

### Sprint 6 authorization UI

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-UI-001 | Authorization top-level product area | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-002 | Tenant Map / roles / hierarchy / permissions | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-003 | Effective permissions matrix / conflicts / coverage | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-004 | Policy and candidate-vs-finding evidence boundary | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-REG-001 | Retained Sprint 4 UI after S6 integration | Sprint4UiTestSuite | PASS |

### Sprint 6 planner/execution integration

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-AUTO-PLAN-001 | resolved S6 policy → generated CROSS_TENANT TestSeed | Sprint6PlannerExecutionIntegrationTestSuite | PASS |
| TEST-S6-AUTO-QUEUE-001 | generated seed → existing S4 TestPlanner / ExecutionQueue | same suite | PASS |
| TEST-S6-AUTO-EXEC-001 | queue → existing TestExecutor → secure localhost ACRA-Lab | run 35882729813 | PASS |
| TEST-S6-AUTO-MUT-001 | one-variable tenant-a → tenant-b path substitution | generated test S6-AUTO-TENANT-a915a78d9e3fd8c06e49c4e9 | PASS |
| TEST-S6-AUTO-SAFE-001 | credentials excluded from Mutation and serialized test | same suite | PASS |
| TEST-S6-ROLE-GEN-001 | safe read-only ROLE_COMPARISON active generation | authenticated contextRef substitution; raw credentials excluded from Mutation | PASS |
| TEST-S6-ROLE-EQUIV-001 | same endpoint/method/resource/body/non-auth headers | RequestEquivalenceGuard | PASS |
| TEST-S6-ROLE-SECRET-001 | viewer/admin raw tokens absent from serialized test and Mutation metadata | Sprint6PlannerExecutionIntegrationTestSuite | PASS |

## Sprint 5 final closure — 2026-09-23

Current authoritative S5 verification is GitHub Actions run `35872345270`: exact JDK 21 compilation and all selected core/S3/S4/S5 suites PASS. See `docs/sprints/sprint-05-final-completion.md` for the requirement matrix and residual validation boundaries.

## Current S5 defensive continuation — 2026-09-09

| Suite | Coverage | Evidence |
|---|---|---|
| Sprint5AssessmentGuardTestSuite | Missing/unknown/partial context, nonbinary decisions, conflicting ownership, incomplete evidence/provenance, missing endpoint | NEWLY EXECUTED, exact count/output in `artifacts/verification-s5-defensive.json` |
| Sprint5CorrelationSafetyTestSuite | Conflicting supplied records, duplicates, replay confidence, order invariance, missing/redacted references | NEWLY EXECUTED, same artifact |
| Sprint5SerializationSecurityTestSuite | Recognized credentials, generic sensitive fields, nested context projection, immutable lists, stable export, redacted-reference guards | NEWLY EXECUTED, same artifact |
| Core TestSuite | Core serialization/context/graph plus retained BOLA/BFLA tests | NEWLY EXECUTED, same artifact |
| Sprint3CoreTestSuite | Retained Sprint 3 core | NEWLY EXECUTED, same artifact |
| S4 core/security/graph/product suites | Affected source/serializer and prior safety contracts | NEWLY EXECUTED, same artifact |
| S5 tenant/workflow/property/finding/severity/orchestration/end-to-end | Missing implementation and test suites | MISSING / UNVERIFIED |
| Live ACRA-Lab / Burp / exact JDK 21 runtime / Maven | Not executed as part of offline verification | UNVERIFIED / BLOCKED as detailed in current S5 audit |

Runner: `scripts/verify-sprint5-defensive.ps1`. Legacy check counters are suite-reported totals, not necessarily raw assertion invocations. Repeated executions do not increase distinct coverage. Earlier matrices below retain their historical evidence dates.

## Sprint 1 executable core tests

| Test ID | Category | Requirement | Executable location | Result |
|---|---|---|---|---|
| TEST-HTTP-001 | HTTP model defensive copy | FR-016 | `HttpModelTests` | PASS |
| TEST-HTTP-002 | Path/query preservation | FR-016 | `HttpModelTests` | PASS |
| TEST-HTTP-NEG-001 | Missing host | NFR-003 | `HttpModelTests` | PASS |
| TEST-HTTP-NEG-002 | Invalid method | NFR-003 | `HttpModelTests` | PASS |
| TEST-SEC-004 | Oversized header rejected | SEC-008 | `HttpModelTests` | PASS |
| TEST-URI-001 | Encoded identifier raw/decoded/canonical | FR-003 | `UriTests` | PASS |
| TEST-URI-002 | Nested resources | FR-017 | `UriTests` | PASS |
| TEST-ID-001 | UUID detection | FR-004 | `UriTests` | PASS |
| TEST-URI-003 | Encoded slash representation divergence | FR-003 | `UriTests` | PASS |
| TEST-URI-NEG-001 | Malformed URI fail-safe | SEC-007 | `UriTests` | PASS |
| TEST-ID-NEG-001 | Unknown query value stays unknown | FR-004 | `UriTests` | PASS |
| AT-01 | Tenant/resource extraction | FR-019 | `ContextTests` | PASS |
| AT-03 | JWT sub principal with provenance | FR-018 | `ContextTests` | PASS |
| AT-07 | Conflicting tenant evidence | FR-019 | `ContextTests` | PASS |
| TEST-ACTION-001 | GET -> READ | FR-019 | `ContextTests` | PASS |
| TEST-ACTION-002 | Application approve action | FR-019 | `ContextTests` | PASS |
| AT-04 | Principal -> tenant evidence edge | FR-021 | `GraphTests` | PASS |
| AT-05 | Direct principal -> resource query | FR-006 | `GraphTests` | PASS |
| TEST-GRAPH-NEG-001 | Missing evidence edge rejected | SEC-006 | `GraphTests` | PASS |
| TEST-GRAPH-NEG-002 | Conflicting duplicate node rejected | NFR-003 | `GraphTests` | PASS |
| AT-06 | Bearer-token serialization redaction | SEC-005 | `SerializationSecurityTests` | PASS |
| TEST-SER-001 | Deterministic serialization | FR-024 | `SerializationSecurityTests` | PASS |
| TEST-SEC-002 | Control characters escaped | SEC-001 | `SerializationSecurityTests` | PASS |
| TEST-DOMAIN-NEG-001 | Invalid confidence rejected | NFR-003 | `SerializationSecurityTests` | PASS |
| TEST-ENTITY-001 | Exact entity equality | FR-022 | `ResolutionContractTests` | PASS |
| TEST-ENTITY-002 | Possible same is not auto-merged | FR-022 | `ResolutionContractTests` | PASS |
| TEST-DOMAIN-NEG-002 | Empty resource ID rejected | NFR-003 | `ResolutionContractTests` | PASS |
| TEST-GT-001 | Ground-truth context compatibility | FR-027 | `ResolutionContractTests` | PASS |
| TEST-PLUGIN-001 | Analyzer contract compiles/executes | FR-025 | `ResolutionContractTests` | PASS |
| TEST-SEC-003 | Malformed JWT does not invent identity | SEC-007 | `NegativeSecurityTests` | PASS |
| AT-08 | Unknown identifier does not invent resource | FR-004 | `NegativeSecurityTests` | PASS |
| TEST-SEC-001 | Serialization/log injection control chars escaped | SEC-001 | `NegativeSecurityTests` | PASS |

| TEST-GRAPH-NEG-003 | Duplicate evidence ID rejected | SEC-006 | `GraphTests` | PASS |
| TEST-SER-002 | Graph canonical serialization | FR-024 | `SerializationSecurityTests` | PASS |
| TEST-IDENTITY-NEG-001 | Conflicting identity evidence state | FR-018 | `ResolutionContractTests` | PASS |
| TEST-JSON-NEG-001 | Malformed JSON treated as opaque Sprint 1 body | SEC-007 | `NegativeSecurityTests` | PASS |
| TEST-SEC-005 | Oversized body rejected | SEC-008 | `NegativeSecurityTests` | PASS |

AT-02 is covered by `TEST-URI-001`: raw, decoded, normalized and canonical URI representations remain separate.

## Future research tests

| Test ID | Category | Requirement | Ground Truth | State |
|---|---|---|---|---|
| TM-A-001 | Target & Scope | SAFE-001 | N/A | PLANNED |
| TM-J-001 | BOLA | FR-010 | GT-BOLA-001 | PLANNED |
| TM-K-001 | BFLA | FR-010 | GT-BFLA-001 | PLANNED |
| TM-M-001 | Tenant Isolation | FR-005 | GT-TENANT-001 | PLANNED |
| TM-P-001 | Workflow Authorization | FR-005 | GT-WORKFLOW-001 | PLANNED |
| TM-Z-001 | Research Evaluation | RES-003 | Experiment registry | PLANNED |


## Sprint 2 passive integration tests

| Test ID | Category | Requirement | Current evidence | Result |
|---|---|---|---|---|
| TEST-MONTOYA-001 | Extension bootstrap | FR-028 | local Montoya-contract test doubles | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-002 | HTTP request received | FR-028/FR-029 | Sprint2TestSuite | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-003 | HTTP response received | FR-028/FR-031 | Sprint2TestSuite | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-004 | Transaction created | FR-031 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-005 | URI extracted | FR-032/FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-006 | Identity/session correlated | FR-033 / SEC-010 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-007 | Tenant correlated | FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-008 | Resource identified | FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-009 | Global graph updated | FR-035 | Sprint2TestSuite / pipeline graph | PASS |
| TEST-GRAPH-OWNER-S2-001 | Explicit resource owner provenance creates OWNS edge | FR-035 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-010 | Sensitive data redacted | SEC-009 | Sprint2TestSuite / security script | PASS |
| TEST-HTTP-COOKIE-001 | Cookie preservation + safe serialization | FR-032 / SEC-009 | Sprint2TestSuite | PASS |
| TEST-ENDPOINT-001 | Endpoint family aggregation | FR-034 | Sprint2TestSuite | PASS |
| TEST-CONTEXT-UI-001 | Context projection exposes role/session/owner/evidence/confidence | FR-038 | Sprint2TestSuite | PASS for model; real Burp UI runtime BLOCKED |
| TEST-DIFF-001 | Volatile field normalization | FR-036 | Sprint2TestSuite | PASS |
| TEST-ACTIVE-001 | Active execution disabled by default | SAFE-006 | Sprint2TestSuite | PASS |
| EXP-INTEGRATION-001 | Live local context reconstruction | RES-006 | `experiments/EXP-INTEGRATION-001.md` | PARTIAL: LOCAL PASS, BURP BLOCKED |

### Sprint 2 negative/security cases

| Case | Expected | Local result |
|---|---|---|
| Missing authentication | NONE/UNKNOWN, no invented principal | PASS |
| Unknown/custom authentication | classified conservatively | PASS |
| Malformed JWT | no invented identity | PASS via Sprint 1 regression |
| Duplicate endpoint observations | aggregate rather than fabricate endpoint | PASS |
| Conflicting tenant evidence | CONFLICTING_EVIDENCE | PASS |
| Oversized body | REJECTED | PASS |
| Raw bearer/API key serialization | redacted/fingerprinted | PASS |
| Unscoped active request | blocked | PASS through disabled executor/scope contract |
| No response | pending request, no fabricated completed transaction | PASS locally; real Burp runtime evidence BLOCKED |
| Binary/compressed response | preserved as bytes/opaque body; semantic decoding not claimed | PASS for opaque preservation; decoder deferred |
## Sprint 2 final promotion-gate matrix

The local columns describe executable RC1 evidence. Any row requiring the Burp desktop runtime remains explicitly unverified.

### Required positive matrix

| Test ID | Requirement | Local evidence | Promotion-gate state |
|---|---|---|---|
| TEST-MONTOYA-001 | Extension loads | Local bootstrap PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-002 | Montoya registration | Local contract PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-003 | HTTP request received | Local handler PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-004 | HTTP response received | Local handler PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-005 | Transaction generated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-006 | Raw request preserved | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-007 | Raw response preserved | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-008 | URI extracted | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-009 | Identity correlated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-010 | Tenant correlated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-011 | Resource identified | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-012 | Owner evidence attached | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-013 | Action classified | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-014 | Security context created | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-015 | Graph updated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-016 | Evidence generated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-017 | Sensitive data redacted | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-018 | Scope guard blocks out-of-scope target | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-019 | UI displays captured transaction | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-020 | UI displays security context | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-021 | UI displays endpoint | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-022 | Kill switch remains functional | PASS | PASS locally |
| TEST-MONTOYA-023 | Active execution disabled by default | PASS | PASS locally |
| TEST-MONTOYA-024 | Burp shutdown clean | Local unload contract PASS | REAL BURP BLOCKED / UNVERIFIED |

### Required negative matrix

| Test ID | Case | Expected | Current state |
|---|---|---|---|
| TEST-MONTOYA-NEG-001 | Malformed HTTP | REJECTED/ERROR without fabricated context | PASS local parser/model coverage; real Burp path UNVERIFIED |
| TEST-MONTOYA-NEG-002 | Missing response | Remain pending; no fabricated transaction | PASS locally |
| TEST-MONTOYA-NEG-003 | Malformed authentication | UNKNOWN; no invented principal | PASS locally |
| TEST-MONTOYA-NEG-004 | Conflicting identity | CONFLICTING_EVIDENCE | PASS via regression/local contracts |
| TEST-MONTOYA-NEG-005 | Conflicting tenant | CONFLICTING_EVIDENCE | PASS locally |
| TEST-MONTOYA-NEG-006 | Unknown resource | UNKNOWN; no fabricated resource | PASS via regression/local contracts |
| TEST-MONTOYA-NEG-007 | Binary response | Opaque preservation | PASS locally |
| TEST-MONTOYA-NEG-008 | Compressed response | Opaque preservation; decoding not claimed | PASS locally |
| TEST-MONTOYA-NEG-009 | Large body | REJECTED | PASS locally |
| TEST-MONTOYA-NEG-010 | Out-of-scope target | BLOCKED | PASS locally |
| TEST-MONTOYA-NEG-011 | Unsupported content type | UNKNOWN/PARTIAL without fabricated semantics | PASS for opaque handling |
| TEST-MONTOYA-NEG-012 | Burp shutdown/restart | Clean lifecycle required | BLOCKED / UNVERIFIED in real Burp |
| TEST-MONTOYA-NEG-013 | Extension reload | Clean lifecycle required | BLOCKED / UNVERIFIED in real Burp |
| TEST-MONTOYA-NEG-014 | Duplicate request | Single correlated completion | PASS locally |
| TEST-MONTOYA-NEG-015 | Credential-redaction verification | No raw credential persisted | PASS locally; real Burp path UNVERIFIED |

## Sprint 3 reconnaissance test matrix

### Deterministic core/adapter coverage

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S3-ID-001 | Tenant semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-ID-002 | User semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-ID-003 | Resource semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-001 | Tenant query classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-002 | Pagination classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-003 | Owner classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-001 | Tenant header classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-002 | Routing header classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-003 | Authentication header sensitivity | Sprint3CoreTestSuite | PASS |
| TEST-S3-VERSION-001 | Path version discovery | Sprint3CoreTestSuite | PASS |
| TEST-S3-VERSION-002 | Query/header version discovery | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-001 | Brace/colon/angle template grammar | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-002 | Wildcard classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-003 | Framework template equivalence | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-001 | OpenAPI 3 JSON import | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-002 | Common YAML subset import | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-003 | OpenAPI/traffic correlation | Sprint3CoreTestSuite | PASS |
| TEST-S3-DRIFT-001 | Undocumented parameter drift | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-001 | Resource/owner/tenant semantic extraction | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-002 | Dynamic field identification | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-003 | Soft-200 denial semantic classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-004 | Reordered/dynamic same-resource match | Sprint3CoreTestSuite | PASS |
| TEST-S3-COVERAGE-001 | Context coverage model | Sprint3CoreTestSuite | PASS |
| TEST-S3-PLAN-001 | Dry-run dispatch remains zero | Sprint3CoreTestSuite + AdapterSuite | PASS |
| TEST-S3-PLAN-002 | Candidate test-family recommendation | Sprint3CoreTestSuite | PASS |
| TEST-S3-MATRIX-001 | Authorization matrix domain | Sprint3CoreTestSuite | PASS |
| TEST-S3-FINGERPRINT-001 | Finding fingerprint deterministic | Sprint3CoreTestSuite | PASS |
| TEST-S3-DIFFMODEL-001 | DifferentialTest domain | Sprint3CoreTestSuite | PASS |
| TEST-S3-ADAPTER-001 | Recon stored from traffic pipeline | Sprint3AdapterTestSuite | PASS |
| TEST-S3-ADAPTER-002 | OpenAPI loaded into pipeline | Sprint3AdapterTestSuite | PASS |
| TEST-S3-ADAPTER-003 | Unknown parameter remains UNKNOWN | Sprint3AdapterTestSuite | PASS |
| EXP-RECON-001 | 10-operation local ground-truth reconnaissance | Sprint3LabExperiment | PASS local; Burp UNVERIFIED |
| EXP-ID-001 | Six labeled semantic identifiers | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-ROUTE-001 | Five route-equivalence labels | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-RESP-001 | FP semantic fixtures | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-CONTEXT-001 | Context coverage/evidence completeness | Sprint3MetricsExperiment | MEASURED |

### Sprint 3 live runtime requirements

| Test ID | Requirement | Current state |
|---|---|---|
| TEST-S3-BURP-001 | Real Burp traffic produces S3 recon record | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-002 | Real Burp UI shows Parameters/Identities/Tenants/Resources/Routes/Graph | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-003 | OpenAPI import/correlation exercised in Burp | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-004 | Dry-run plan visible with zero dispatch in Burp | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-005 | Context/graph reconstruction matches ACRA-Lab ground truth through Burp | BLOCKED / UNVERIFIED |

The runtime rows remain blocked because a Burp installation and official Maven dependency-resolution environment are unavailable. Local contract stubs are not substituted for Burp evidence.

## Sprint 4 controlled localhost execution matrix

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S4-LOCAL-001 | Concrete transport accepts existing request model and captures live localhost response/timing | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-002 | External/non-LAB target rejected by localhost transport | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-003 | Method/header/body preservation through ACRA-Lab echo | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-004 | Secure expected-ALLOW control (`User-A → Document-A`) | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-005 | Secure expected-DENY control (`User-A → Document-B`) | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-006 | One resource mutation produces semantic `EXPECTED_CHANGE` | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-007 | Vulnerable synthetic mismatch retained as observation, not finding | `EXP-EXEC-001` | PASS, differential `UNEXPECTED_CHANGE` |
| TEST-S4-LOCAL-008 | HTTP 200 application denial normalized semantically | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-009 | Dynamic timestamp/request ID + formatting/order variation not treated as semantic change | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-010 | Live response cookie and serialized request credentials remain redacted in evidence | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-011 | Timeout maps to operational `TIMEOUT` without observation | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-SAFE-012 | Pre-dispatch request equivalence: valid mutation dispatches once; invalid and type/location-contaminated mutations dispatch zero times | `Sprint4EngineSecurityTestSuite` | PASS, 11 focused assertions |
| TEST-S4-LOCAL-013 | Separate executor instances receive distinct process-local execution IDs | `Sprint4LocalhostIntegrationTestSuite` | PASS after verified repair |
| TEST-S4-ARCH-001 | Active network client isolated to loopback LAB transport | `scripts/architecture-sprint4.sh` | PASS |
| EXP-EXEC-001 | Live controlled localhost baseline/control/mutation/evidence pipeline | `research/EXP-EXEC-001.md` | PASS local |
| TEST-S4-GRAPH-001 | Completed observation hydrates the existing Security Context Graph through `TestExecutor` | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-002 | Relationship provenance traces execution, test, observation and originating evidence | `Sprint4GraphIntegrationTestSuite` + live acceptance | PASS |
| TEST-S4-GRAPH-003 | Unknown identity, tenant, resource, owner or policy prevents graph mutation | `Sprint4GraphIntegrationTestSuite` | PASS, `INCONCLUSIVE` |
| TEST-S4-GRAPH-004 | Configured/observed resource, owner and tenant conflicts remain distinct | `Sprint4GraphIntegrationTestSuite` | PASS, `CONFLICTING_CONTEXT` |
| TEST-S4-GRAPH-005 | Identical observation hydration is idempotent | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-006 | Replay creates fresh execution/observation/evidence identities and retains history | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-007 | Forged provenance and cross-project hydration are blocked | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-008 | Raw bearer/password/API-key/session secrets are absent from observations, evidence and graph serialization | `Sprint4GraphIntegrationTestSuite` + live acceptance | PASS |
| TEST-S4-GRAPH-009 | Conflicting node identity cannot partially mutate graph nodes, edges or evidence | `Sprint4GraphIntegrationTestSuite` | PASS, exact whole-graph state preserved |
| TEST-S4-GRAPH-010 | Live localhost observation hydrates graph through normal executor path | `Sprint4LocalhostIntegrationTestSuite` | PASS; execution `S4-EXEC-00000002` |

The verified graph/local results remain: graph integration **87 PASS**, core **54 PASS**, engine security **52 PASS**, localhost **60 PASS**, core regression **37 PASS**, Sprint 3 core **47 PASS** and Sprint 3 adapter **11 PASS**. Phase 3 adds complete mode/profile/selection, queue, safety/backoff, resolver, coverage/efficiency, active UI source/binding and research-software verification below. Broad FP/FN and performance metrics remain NOT MEASURED; real Burp and exact JDK 21 execution remain deferred/unverified.

## Sprint 4 Phase 3 product-completion matrix

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S4-PROFILE-001 | Five profiles alter contracts, mutations, comparison, budgets, evidence and safety | `Sprint4ProductCompletionTestSuite` | PASS |
| TEST-S4-SELECTION-001 | ALL/AUTOMATIC/USER_SELECTED/HYBRID exact planner behavior | product suite | PASS |
| TEST-S4-MODE-001 | Beginner/Professional/Researcher/Expert real configuration mapping | product suite | PASS |
| TEST-S4-QUEUE-001 | Eight states and start/pause/resume/stop/retry/skip/rerun | product suite | PASS |
| TEST-S4-QUEUE-002 | Stable ordering, deduplication and complete/failed/missing dependencies | product suite | PASS |
| TEST-S4-CONSENT-001 | Authorized local automatic policy; external/unauthorized/destructive disabled | product suite | PASS |
| TEST-S4-BACKOFF-001 | 429/503/Retry-After bounded | product suite | PASS |
| TEST-S4-OPFAIL-001 | Connection retry bounded and no Observation | product suite | PASS |
| TEST-S4-RESOLVER-001 | Precedence, equal-rank conflict and absent policy | product suite | PASS |
| TEST-S4-INCONCLUSIVE-001 | Unknown expected policy forces `INCONCLUSIVE` | product suite | PASS |
| TEST-S4-COVERAGE-001 | Endpoint/context/resource/mutation/test accounting | product suite | PASS |
| TEST-S4-EFFICIENCY-001 | Candidate/dedup/scope/budget/executed counts | product suite | PASS; optimization NOT MEASURED |
| TEST-S4-RESEARCH-SW-001 | TP/TN/FP/FN records and safe precision/recall/F1 denominators | product suite | PASS; campaign NOT RUN |
| TEST-S4-UI-001 | Eight active views installed in existing suite tab | `Sprint4UiTestSuite` | PASS |
| TEST-S4-UI-002 | Mode/profile/selection/limits/STOP ALL bind to backend workspace | UI suite | PASS |
| TEST-S4-UI-003 | Active execution unavailable without explicitly injected executor | UI suite | PASS |
| TEST-S4-WORKSPACE-001 | Explicitly configured workspace plan → queue → existing executor → result/coverage | `Sprint4ProductCompletionTestSuite` | PASS |
| TEST-S4-BURP-ACTIVE-001 | Passive context import and authorized local executor provisioning in real Burp | none | DEFERRED; default remains safely disabled |

NEWLY EXECUTED Phase 3 total: **558 represented assertions** across graph 87, S4 core 54, S4 security 52, S4 product 132, localhost 60, UI 26, core 37, S3 core 47, S3 adapter 11 and S2 52. All core and extension sources compile with `--release 21 -Xlint:all -Werror` using OpenJDK 26.0.1. Exact JDK 21 runtime is UNVERIFIED; Maven is BLOCKED; broad research/performance metrics are NOT MEASURED.
