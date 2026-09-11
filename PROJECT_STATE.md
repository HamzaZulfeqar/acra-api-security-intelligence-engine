# ACRA Project State

## Current state — 2026-09-11

**Current sprint:** Sprint 5. **Decision:** S5 SOFTWARE PARTIAL. **S6:** NOT STARTED.  
**Current checkpoint:** `S5-BATCH3-FINAL-CONTINUATION-2026-09-11`.  
**Source baseline SHA-256:** `34b846321b1e2b4a6791bf8948c127cfba26697ec0425ec8dda8ea782e38df01`.  
**Working tree:** cumulative changes relative to the verified S5-04 archive; no embedded Git metadata.  
**Artifact version:** VERSION/POMs remain `0.3.0-rc1`; no release version is promoted by this continuation.

Current authoritative S5 audit: `docs/sprints/sprint-05-final-software-closure.md`. Source inspection found that the previous S5-00..04 completion claims exceeded what the canonical package contained. The context record, BOLA/BFLA and correlator exist; the named observation normalizer and the remaining tenant/workflow/property/candidate/severity/orchestration modules do not.

Defensive work preserved and completed: incomplete/redacted evidence guards, missing endpoint represented honestly, conservative conflict/duplicate correlation, recognized credential redaction in existing models/serializer, focused tests, reproducible verification and continuation packaging scripts. The 2026-09-09 integrated run passes 272 focused S5 assertions and 415 retained suite-reported checks. Exact commands/results are in `docs/testing/artifacts/verification-s5-defensive.json`. Recorded edits are in `docs/sprints/sprint-05-file-changes.json` after packaging; a new baseline byte diff is BLOCKED because the previously verified Downloads ZIP later became unavailable. The new archive is still compared byte-for-byte against the current working tree after clean unpack.

No new S5 live-lab, Burp, external, research or accuracy claim is made. Historical S4 SOFTWARE COMPLETE and S2/S3 Burp Level 3/4 BLOCKED / UNVERIFIED remain unchanged. Maven is BLOCKED; exact JDK 21 runtime is UNVERIFIED; Java 21 target compilation uses the available OpenJDK 26.0.1.

S5 Batch 1 source changes add `EvidenceReferenceValidator` and `EvidenceReferenceValidation` beside the existing `ExecutionEvidenceStore`. Store-backed BOLA/BFLA evaluation and validator-backed correlation now fail closed when evidence ownership, project scope, or replay lineage cannot be proven. `Sprint5EvidenceIntegrityTestSuite` adds focused coverage, but Java 21 compilation/test execution is blocked in this environment: only OpenJDK 17.0.8 was found, `javac` is absent from PATH, and Maven is unavailable. The Batch 1 checkpoint ZIP was independently verified with 604 files, safe paths, clean extraction equality and the required external SHA-256 sidecar. Full remaining S5 software gaps are itemized in the current audit. S6 remains gated on S5 completion.

S5 Batch 2 adds `PolicyValidationEvaluator`, `PolicyValidationResult`, and `PolicyValidationState` for explicit offline tenant, workflow, property, and policy-conflict review. `Sprint5PolicyValidationTestSuite` is present but Java 21 compilation and all new/affected test execution remain blocked because no Java 21 compiler is available; OpenJDK 17.0.8 rejects `--release 21` and Maven is unavailable. S5 remains SOFTWARE PARTIAL and S6 remains NOT STARTED.

The Batch 2 checkpoint archive was independently verified with 609 entries, safe paths, no build/cache entries, clean extraction equality, and an external SHA-256 sidecar. The exact hash is kept outside the archive to avoid recursive self-hashing.

S5 Batch 3 completed source-level integration review. Policy reviews now resolve their observation reference through the existing `EvidenceReferenceValidator`; unknown, forged, cross-project, replay-mismatched, or otherwise unverifiable observations remain inconclusive. The complete defensive chain and security properties were reviewed, but executable Java 21 compilation and tests remain blocked: PATH has no `javac`, the discovered compiler is OpenJDK 17.0.8, and Maven is unavailable. The final S5 decision remains SOFTWARE PARTIAL; S6 is NOT STARTED.

## Historical state before the S5 defensive continuation

The following earlier state and execution records are preserved verbatim as history. They are not the current S5 completion decision.

**Version:** 0.3.0-rc1  
**Target candidate:** 0.4.0-rc1  
**Canonical baseline:** RB-0  
**Current sprint:** Sprint 4  
**Sprint state:** S4 SOFTWARE COMPLETE / DEFERRED VALIDATION SEPARATE  
**Repository state:** S4_FINAL_SOFTWARE_CHECKPOINT_VERIFIED  
**Current checkpoint:** `S4-FINAL-SOFTWARE-CHECKPOINT-2026-09-05`  
**Implementation claim:** SPRINT 4 CONTROLLED-ACTIVE SOFTWARE IS COMPLETE FOR THE AUTHORIZED LOCAL/SYNTHETIC BOUNDARY: DISPATCH SAFETY, LOOPBACK TRANSPORT, ACRA-LAB EXECUTION, EXISTING-GRAPH HYDRATION, PROFILES/SELECTION/USER MODES, QUEUE, CONSENT, BACKOFF, EXPECTED DECISIONS, COVERAGE/EFFICIENCY, ACTIVE UI SOURCE PROJECTION, CONFIGURED WORKSPACE EXECUTION AND RESEARCH RECORDS ARE LOCALLY VERIFIED; MAVEN IS ENVIRONMENT-BLOCKED AND EXACT JDK 21, BROAD RESEARCH/PERFORMANCE, BURP AND EXTERNAL VALIDATION REMAIN SEPARATE DEFERRED/UNVERIFIED LANES

## Sprint 4 final software audit checkpoint

The canonical recovery input `acra-sprint-04-current-recovered.zip` was verified as SHA-256 `db303eeac4b6ffe44b6756b667b748414797069d235cc541ec0e3326e55266bf`, checked for unsafe paths and extracted cleanly. That recovery initiated a historical 50-row audit which concluded **S4 SOFTWARE PARTIAL**. The current source-driven 52-requirement audit in `docs/sprints/sprint-04-final-software-audit.md` supersedes that checkpoint and concludes **S4 SOFTWARE COMPLETE** for the authorized local/synthetic product boundary; deferred validation lanes remain separate.

The audit reproduced a dispatch-safety gap and made the one authorized engine repair: `RequestEquivalenceGuard` now independently verifies the declared mutation type/location, original value and all unchanged request dimensions before `MutationValidator` can allow dispatch. Focused executor tests prove valid mutation transport equals one, invalid mutation transport equals zero, and contaminated mutation transport equals zero.

The newly executed Java 21 verification passes 54 S4 core, 52 S4 engine-security, 51 S4 localhost, 37 core-regression and 47 Sprint 3 core assertions (241 newly executed). The retained Sprint 3 adapter result remains 11 PASS assertions, for 252 current unique local assertions. Evidence is in `docs/testing/artifacts/verification-s4-final-software-audit.txt`. The secure/vulnerable localhost milestone remains an observation/evidence result only and is not a vulnerability or accuracy claim.

## Sprint 4 pre-implementation gate

`docs/sprints/sprint-04-reconciliation.md` records the verified package hash, full repository inspection, reproduced Sprint 1–3 baseline, named Sprint 3 reuse map, requirement classifications, technical debt and preserved Burp blockers. Core Sprint 4 implementation must not begin before this state marker and that report exist.

The reconciliation authorizes only the verified Sprint 4 gaps. It does not promote Sprint 3, establish real Burp evidence, or claim an active authorization scanner exists yet.

## Sprint 4 core verification checkpoint

Checkpoint `S4-RESUME-2026-08-31-CORE-INTEGRATED` has now completed its focused core verification slice. Three Sprint 4 test files were added under `core/src/test/java/io/acra/core/tests/sprint4/`. The Java 21 main/test compile passes with `--release 21 -Xlint:all -Werror`; the focused Sprint 4 suites pass 54 core assertions and 41 engine-security assertions (95 total). Existing core and Sprint 3 core regressions were rerun and pass 37 and 47 assertions respectively.

No engine main-source defect was observed in this slice. One test-source lint failure (`AutoCloseable` lease not referenced) was fixed in the test only and the full focused slice was rerun successfully. Evidence is recorded in `docs/testing/artifacts/verification-s4-core.txt`.

This checkpoint does **not** establish concrete localhost transport, Sprint 4 ACRA-Lab active execution, FP/FN metrics, graph hydration, active UI behavior, research accuracy, Maven/Montoya packaging, or Burp runtime validation. Observations remain distinct from vulnerability findings.

## Sprint 4 localhost integration checkpoint

The next continuation slice is now executed and evidenced in `docs/testing/artifacts/verification-s4-localhost.txt` and `docs/testing/artifacts/exp-exec-001-localhost.txt`. A single concrete `LocalhostHttpTransport` was added behind the existing `HttpTransport` interface and is construction-bound to an authorized `LAB` target on loopback with redirects disabled. The existing executor, safety validator, request/response models, Sprint 3 semantic fingerprint/comparison code, observation model and evidence store were reused.

The existing ACRA-Lab was extended with isolated Sprint 4 fixtures and independent ground truth `lab/ground-truth/GT-EXEC-S4.json`. The secure live run established baseline/positive `ALLOW`, negative `DENY`, mutation `DENY`, and `EXPECTED_CHANGE`. The vulnerable fixture preserved a valid expected-DENY control but intentionally returned `ALLOW` for the single controlled resource mutation; ACRA stored that mismatch as an observation and classified the four-way comparison `UNEXPECTED_CHANGE`, without manufacturing a confirmed vulnerability finding. HTTP-200 application denial normalized to `DENY`, while dynamic timestamp/request-ID plus formatting/order-only public-response variation remained semantically equivalent. The live timeout remained an operational failure and produced no observation.

One live integration defect was verified and repaired: separate `TestExecutor` instances reused `S4-EXEC-00000001`, weakening evidence traceability. The executor sequence is now process-wide, and the final secure/vulnerable runs produced distinct IDs `S4-EXEC-00000002` and `S4-EXEC-00000003`. A legacy architecture script also required a narrow update so the passive core still forbids network clients while the isolated Sprint 4 active package permits only `LocalhostHttpTransport`.

This is controlled localhost research evidence only. It does not establish scanner accuracy, a vulnerability finding engine, real-world target safety, or Burp runtime behavior.

## Sprint 4 graph-integration checkpoint

Checkpoint `S4-PHASE2-01-GRAPH-INTEGRATION-VERIFIED-2026-09-04` closes the active Observation/Evidence → existing `SecurityContextGraph` software gap without adding a second graph. `ObservationGraphIntegrator` builds an isolated graph delta, verifies execution/test/observation/evidence provenance, refuses incomplete or cross-project context, preflights all identities and only then merges. A configured-versus-observed context disagreement is retained as distinct evidence and `CONFLICTS_WITH` relationships; it is not promoted to a vulnerability edge.

NEWLY EXECUTED verification passes 87 graph-integration, 54 S4 core, 52 S4 engine-security, 60 localhost-integration, 37 core-regression, 47 Sprint 3 core and 11 Sprint 3 adapter assertions: 348 current unique local assertions. The Java sources and tests compile warning-clean for the Java 21 target with `--release 21 -Xlint:all -Werror`; the available compiler/runtime is OpenJDK 26.0.1, so execution on an exact JDK 21 runtime remains UNVERIFIED. Maven remains unavailable.

The live loopback acceptance used the normal `SecurityTest → TestExecutor → LocalhostHttpTransport → ACRA-Lab → Response → Differential → Observation → Evidence → ObservationGraphIntegrator → SecurityContextGraph` path. It produced execution `S4-EXEC-00000002`, observation `S4-EXEC-00000002:observation`, chain evidence `S4-EVIDENCE-00000009`, graph evidence `ev-97dbe353e0decdbb45ee` and relationship `edge-837c5bd8d56408e3c53b`. Credential/session-secret exclusion, project isolation, duplicate hydration, fresh replay identity, historical evidence preservation, incomplete-context refusal, conflict preservation and whole-graph atomicity all pass. Evidence is in `docs/testing/artifacts/verification-s4-graph-integration.txt`.

## Sprint 4 Phase 3 product-completion checkpoint

The next closure batch extends existing components rather than recreating them. Five product profiles now define executable test families, mutation categories, comparison strategies, request/mutation budgets, evidence detail and safety defaults. `ALL`, `AUTOMATIC`, `USER_SELECTED` and `HYBRID` selection modes have exact planner proofs. Beginner, Professional, Researcher and Expert modes now change the real planning profile, depth, limits, evidence and available controls.

Queue lifecycle/operations, prerequisite handling (including absent/failed dependencies), authorized local automatic consent, 429/503/Retry-After/connection failure behavior, unknown-policy `INCONCLUSIVE` handling, active test coverage and request-efficiency accounting are directly verified. `ActiveEngineWorkspace` composes the existing planner, queue and executor; it does not replace them.

The existing Swing suite tab now exposes Test Plan, Test Queue, Test Detail, Execution, Differential Result, Evidence, Safety and Experiment views. Profile, selection, user mode, request/mutation budget, concurrency, rate, coverage and STOP ALL controls are bound to the workspace. Headless UI verification passes 26 assertions. The configured product workspace has a focused plan → queue → existing executor → result/coverage proof. The default Burp construction remains safely disabled until explicit configuration/consent, as required by the reconciliation; passive-to-active provisioning in a real Burp session is deferred adapter/runtime validation rather than a missing local S4 software requirement.

Research software records now cover labelled FP/FN fixtures, TP/TN/FP/FN outcomes, independent ground truth, experiment/run IDs, configuration, ordering and seed, with undefined precision/recall/F1 represented as `N/A`. `EXP-DIFF-001` remains research-deferred and every broad metric remains NOT MEASURED.

NEWLY EXECUTED verification passes 87 graph, 54 S4 core, 52 S4 security, 132 Phase 3 product, 60 localhost, 26 UI, 37 core regression, 47 S3 core, 11 S3 adapter and 52 S2 assertions: 558 represented assertions. All core and extension sources compile with `--release 21 -Xlint:all -Werror` using OpenJDK 26.0.1. Exact JDK 21 execution remains UNVERIFIED and Maven remains BLOCKED.

## Current gate status

| Gate | State |
|---|---|
| Sprint 1 regression | PASS, 37 |
| Sprint 2 regression | PASS, 52 |
| Sprint 3 core assertions | PASS, 47 |
| Sprint 3 adapter assertions | PASS, 11 |
| Sprint 1–3 baseline executable assertions | PASS, 147 |
| Security gate | PASS |
| Architecture boundary | PASS |
| JDK/local contract build | PASS |
| POM XML validation | PASS |
| Sprint 3 local 10-operation ACRA-Lab experiment | PASS |
| Sprint 3 controlled metric experiment | PASS within fixture scope |
| Sprint 3 performance baseline | EXECUTED / OBSERVATIONAL |
| Sprint 4 Java 21 target main/test compile | PASS, `--release 21 -Xlint:all -Werror` using OpenJDK 26.0.1; exact JDK 21 runtime UNVERIFIED |
| Sprint 4 focused core verification | PASS, 54 assertions |
| Sprint 4 engine-security verification | PASS, 52 assertions |
| Sprint 4 focused verification total | PASS, 106 assertions |
| Core regression rerun | PASS, 37 assertions |
| Sprint 3 core regression rerun | PASS, 47 assertions |
| Sprint 4 localhost transport | PASS, loopback-only `LocalhostHttpTransport` |
| Sprint 4 active ACRA-Lab execution | PASS, secure + vulnerable synthetic fixtures |
| Sprint 4 secure expected-ALLOW control | PASS, live localhost |
| Sprint 4 secure expected-DENY control | PASS, live localhost |
| Sprint 4 controlled resource mutation | PASS, DENY observed / EXPECTED_CHANGE |
| Sprint 4 vulnerable fixture mismatch | PASS as observation-only pipeline check; classification UNEXPECTED_CHANGE |
| Sprint 4 minimum FP-preparation semantics | PASS for HTTP-200 denial + dynamic/order fixture |
| Sprint 4 localhost integration suite | PASS, 60 assertions |
| Sprint 4 mutation dispatch safety | PASS: valid mutation dispatch=1; invalid=0; contaminated=0 |
| Sprint 4 Observation/Evidence → existing graph integration | PASS, 87 assertions plus live normal-pipeline hydration |
| Sprint 4 graph atomicity/conflict handling | PASS: inserted, equivalent reuse and blocked conflict; exact prior graph serialization retained on conflict |
| Sprint 4 Phase 3 product verification | PASS, 132 assertions including configured workspace execution |
| Sprint 4 active UI source/binding verification | PASS, 26 assertions; real Burp UI remains DEFERRED |
| Current represented verification assertions | PASS, 558 assertions (87 graph + 54 S4 core + 52 S4 security + 132 S4 product + 60 localhost + 26 UI + 37 core + 47 S3 core + 11 S3 adapter + 52 S2) |
| Sprint 4 final software audit | S4 SOFTWARE COMPLETE: 52 requirements classified; validation-only exclusions remain explicit |
| Sprint 4 FP/FN and research metrics | NOT MEASURED |
| Official Montoya Maven dependency build | BLOCKED / UNVERIFIED: Maven absent and outbound dependency resolution blocked |
| Historical Sprint 2 Burp runtime validation | BLOCKED / UNVERIFIED |
| Historical Sprint 3 Burp reconnaissance/UI validation | BLOCKED / UNVERIFIED |
| Research evidence level for Burp integration/recon | Level 2 |
| Release decision | HOLD |

The version **must remain `0.3.0-rc1`** until Sprint 3's mandatory live Burp validation gates have real evidence. Sprint 2 also remains unpromoted historically; Sprint 3 proceeded from its canonical RC1 under a recorded governance exception rather than pretending the S2 runtime gate passed.

## Implemented through Sprint 3

### Sprint 1 foundation

- standalone Java 21 deterministic domain engine.
- HTTP/URI, principal/session/role/tenant/resource/action/workflow/authorization models.
- evidence provenance, deterministic serialization, redaction/fingerprints.
- Security Context Graph, entity resolution, plugin/test/ground-truth contracts.

### Sprint 2 passive adapter

- Montoya adapter source targeting accepted API baseline.
- HTTP observation, scope control, transaction IDs and passive context extraction.
- endpoint-family inventory, observation/evidence timeline and graph hydration.
- passive response normalization/differential primitives.
- Swing Overview/Traffic/Contexts/Endpoints/Configuration source.
- active executor abstraction disabled by default with zero request/mutation budgets and kill switch.
- basic ACRA-Lab integration fixture.

### Sprint 3 reconnaissance expansion

- semantic identifier classification.
- security-relevant query/body parameter classification.
- security/proxy/routing header classification.
- API version signal discovery.
- resource relationship intelligence.
- deterministic route grammar, template model and equivalence engine.
- OpenAPI 3.x / Swagger 2.0 JSON ingestion and conservative common YAML subset.
- declared-vs-observed endpoint correlation and schema-drift observations.
- response semantic fingerprints for resource/owner/tenant IDs, fields, error-like semantics and volatile fields.
- collection and pagination intelligence.
- resource semantic matching tolerant of selected dynamic/reordered differences.
- AuthorizationMatrix, SecurityContextFingerprint, DifferentialTest and FindingFingerprint foundations.
- hybrid identity confirmation and manual-context registries.
- context coverage model.
- endpoint test-priority scoring explicitly separated from vulnerability severity.
- reconnaissance-to-test dry-run planning with zero dispatch.
- per-transaction reconnaissance storage integrated into passive traffic pipeline.
- Burp UI source extended with Parameters, Identities, Tenants, Resources, Routes and Context Graph views.
- expanded 10-operation lab and OpenAPI ground truth.

## Controlled Sprint 3 measurements

These are valid only for the deliberately small local fixture dataset:

| Metric | Measured result |
|---|---:|
| Endpoint discovery precision | 1.0000 |
| Endpoint discovery recall | 1.0000 |
| Semantic identifier precision | 1.0000 |
| Semantic identifier recall | 1.0000 |
| Tenant detection accuracy | 1.0000 |
| Owner extraction accuracy | 1.0000 |
| Route-equivalence accuracy | 1.0000 |
| Mean context coverage | 0.8111 |
| Evidence completeness for measured inferred fields | 1.0000 |

These values are not real-world scanner accuracy claims.

## Partial / deliberately limited

- OpenAPI importer is not a complete OpenAPI/YAML standards implementation; advanced `$ref`, external references, composition, callbacks/links and full YAML semantics are deferred.
- semantic identifier/resource inference remains deterministic and conservative.
- response semantics remain reconnaissance evidence, not an authorization verdict engine.
- route equivalence does not prove runtime proxy/router/authorization-boundary equivalence.
- context coverage intentionally leaves policy UNKNOWN unless explicit policy evidence exists.
- hybrid identity confirmation exists as a registry contract; full persistent/user-managed identity workflows remain future UI/workspace work.
- endpoint priority is a test-ordering signal only.
- dry-run plan remains zero-dispatch; the separate Sprint 4 executor can now dispatch only through the configured loopback LAB transport.
- persistence/database remains OPEN.
- the concrete Sprint 4 transport is HTTP/loopback-only and is not a general external-target transport.
- vulnerable-lab mismatch yields an observation with `UNEXPECTED_CHANGE` when the independent expected-DENY control remains valid but the controlled mutation is observed as `ALLOW`; no vulnerability finding is created.
- active observations with complete, project-matching context hydrate the existing in-memory Security Context Graph with verified provenance; incomplete context is `INCONCLUSIVE`, identity collisions are `BLOCKED`, and configured/observed context differences remain explicit conflicts.
- graph hydration is in-memory and process-local; persistent graph/evidence storage across restarts is not implemented.
- active UI source projection, backend control binding and explicitly configured workspace execution pass locally; real Burp behavior/provisioning remains deferred.

## Not implemented

- arbitrary/external active authorization testing or unrestricted request mutation dispatch.
- BOLA/BFLA/RBAC/tenant/routing/workflow vulnerability analyzers.
- policy-derived ALLOW/DENY verdict engine.
- confirmed vulnerability findings.
- full OpenAPI reference-resolution engine.
- GraphQL/gRPC/WebSocket analyzers.
- A0-A7 authorization-detection experiments.

## Accepted ADRs

ADR-0001, ADR-0002, ADR-0006, ADR-0007, ADR-0013, ADR-0015, ADR-0026, ADR-0028, ADR-0029, ADR-0031, ADR-0032, ADR-0033, ADR-0034, ADR-0035.

## Important open ADRs

ADR-0003, ADR-0004, ADR-0005, ADR-0008 through ADR-0012, ADR-0014, ADR-0016 through ADR-0025 where applicable, ADR-0027, ADR-0030.

## Known blockers

1. Maven executable is absent.
2. External DNS/artifact resolution is blocked.
3. Burp runtime was not exercised in this Phase 3 local product slice.
4. Historical S2/S3 Burp Level 3/4 evidence remains BLOCKED / UNVERIFIED; real Montoya packaging and current Burp runtime validation remain outside this slice.

## Novelty status

CANDIDATE / UNVERIFIED. API reconnaissance, OpenAPI correlation, route templating, identifier extraction and dry-run planning are engineering capabilities and are not independent novelty claims. The candidate research contribution remains context correlation and must be evaluated by later controlled authorization experiments.

## Next action

Sprint 4 implementation is closed at the verified source checkpoint. Real Burp provisioning/runtime, exact JDK 21, broad research/performance and authorized external validation are separate follow-up validation lanes. Do not begin Sprint 5 without an explicit new directive; no Sprint 5 work was performed here.

Historical S2/S3 Burp Level 3/4 debt remains unchanged and must be validated separately when deliberately scheduled.


## Sprint 5-02
Status: COMPLETE
Object-level authorization reasoning foundation added. No BOLA findings or exploit generation implemented.


S5-03 function-level authorization reasoning foundation added.


S5-04: Evidence correlation aggregation foundation added.
