# RB-0 Requirements Register

RB-0 remains authoritative. Sprint 1 adds stable implementation requirements without changing the research boundary.

## Functional

- **FR-001** Target configuration shall support scope restrictions and scan mode.
- **FR-002** HTTP intelligence shall model complete requests and responses.
- **FR-003** URI intelligence shall preserve raw, parsed, decoded, normalized, and canonical representations independently.
- **FR-004** Identifier intelligence shall detect/classify candidate identifiers with confidence, reason, location, and source.
- **FR-005** Security Context shall model principal, role, tenant, resource, ownership, action, workflow, URI representation, expected/observed authorization, and evidence.
- **FR-006** A Security Context Graph shall represent contextual relationships with provenance.
- **FR-007** Test selection shall support automatic, user-selected, hybrid, and bounded exhaustive modes.
- **FR-008** Differential analysis shall compare more than HTTP status and body length.
- **FR-009** Semantic response analysis shall detect application-level allow/deny semantics.
- **FR-010** Evidence correlation shall combine multiple contextual signals before vulnerability classification.
- **FR-011** Findings shall maintain independent severity and confidence.
- **FR-012** Findings shall implement the defined lifecycle.
- **FR-013** Reproduction packages shall support JSON, Burp Issue, and SARIF export targets.
- **FR-014** ACRA-Lab shall provide vulnerable and secure ground-truth cases.
- **FR-015** Research evaluation shall support baseline and A0-A7 ablation comparisons.
- **FR-016** `HttpTransaction`, request, and response models shall be immutable/defensively copied where practical and retain raw and normalized information without analyzer-specific fields.
- **FR-017** URI path segments shall retain raw, decoded, normalized value, classification, and confidence.
- **FR-018** Identity/session/role models shall support UNKNOWN and provenance; possession of an Authorization header alone shall not identify a principal.
- **FR-019** Tenant, resource, and action models shall support confidence and evidence-backed extraction.
- **FR-020** `AuthorizationContext` shall combine principal, role, tenant, resource, owner, action, workflow state, expected/observed decision, and evidence references without declaring a vulnerability.
- **FR-021** Every inferred graph relationship shall reference valid evidence IDs.
- **FR-022** Entity resolution shall distinguish SAME, POSSIBLE_SAME, UNKNOWN, and CONFLICT without automatic merge of ambiguous entities.
- **FR-023** Endpoint and API inventory domain records shall preserve method, route template, version, environment, authentication method, observations, documentation state, and risk tier.
- **FR-024** Major domain objects shall support deterministic, versioned, secret-safe serialization.
- **FR-025** Core plugin contracts shall exist for Analyzer, MutationProvider, EvidenceProvider, ApiParser, WorkflowDetector, and Reporter without implementing active vulnerability plugins in Sprint 1.
- **FR-026** TestCase and TestState domain models shall represent preconditions, inputs, mutation design, expected outcome, evidence requirements, safety constraints, and lifecycle state.
- **FR-027** Core models shall accept ground-truth principal, role, tenant, resource, owner, action, decision, workflow state, and secure/vulnerable case labels.

## Non-functional

- **NFR-001** Core data structures shall use deterministic ordering/serialization where persisted output is produced.
- **NFR-002** Core shall compile and execute without a Burp/Montoya dependency.
- **NFR-003** Invalid domain values shall fail explicitly rather than silently propagating malformed state.
- **NFR-004** Performance baselines shall be measured and reported as environment-specific observations rather than unsupported performance claims.

## Safety

- **SAFE-001** Active tests shall not execute outside authorized target scope.
- **SAFE-002** Execution shall support rate, concurrency, mutation, and per-endpoint limits.
- **SAFE-003** Execution shall support pause, resume, stop, and dry-run controls.
- **SAFE-004** Target-health circuit-breaker behavior shall be supported.
- **SAFE-005** Sprint 1 core shall perform no active network probing, exploitation, bypass testing, or mass scanning.

## Security

- **SEC-001** Secrets and sensitive authorization material shall be protected in storage and exports.
- **SEC-002** Identity contexts shall remain isolated during differential testing.
- **SEC-003** Token renewal shall not silently alter principal, role, tenant, or scope.
- **SEC-004** Export sanitization shall redact, hash, tokenize, drop, or locally preserve sensitive fields according to policy.
- **SEC-005** Raw bearer/session/API-key credential values shall not appear in normal serialized evidence or exports by default.
- **SEC-006** Graph edges shall reject references to nonexistent nodes or evidence.
- **SEC-007** Malformed URI/JWT/input data shall fail safely to explicit UNKNOWN/error state where possible rather than fabricate identity/context.
- **SEC-008** Core HTTP models shall enforce bounded header/body guardrails to reduce accidental resource exhaustion from malformed fixtures/adapters.

## Research

- **RES-001** No research metric shall be reported before its experiment executes.
- **RES-002** A novelty ledger shall track candidate claims and overlap.
- **RES-003** A0-A7 ablation experiments shall measure the contribution of contextual dimensions.
- **RES-004** Ground-truth IDs shall trace predictions to known vulnerable and secure cases.
- **RES-005** Sprint 1 confidence scores and performance measurements are engineering baselines, not scientific validation of ACRA effectiveness.


## Sprint 2 integration requirements

- **FR-028** Burp adapter shall implement the current accepted Montoya bootstrap, HTTP handler and unload lifecycle without placing security logic in the entry point.
- **FR-029** Passive Burp observation and active request execution shall remain separate components.
- **FR-030** ScopeController shall support IN_SCOPE_ONLY, ALL_TRAFFIC, SELECTED_HOSTS, SELECTED_ENDPOINTS and MANUAL_ONLY decisions.
- **FR-031** TrafficCollector shall assign deterministic ACRA transaction IDs independent of Burp UI ordering and preserve timing/source metadata.
- **FR-032** Request/response mapping shall preserve raw information while keeping decoded/normalized/canonical representations separate.
- **FR-033** Live traffic shall produce passive authentication/session, tenant, resource, action, endpoint and Security Context observations with provenance.
- **FR-034** Endpoint inventory shall aggregate canonical endpoint families, observation counts and evidence-backed observed identifiers/contexts without assigning vulnerability risk.
- **FR-035** Every processed transaction shall be able to update the global evidence-backed Security Context Graph.
- **FR-036** Passive differential comparison shall support RAW, NORMALIZED, STRUCTURAL and SEMANTIC modes without treating a difference as a vulnerability.
- **FR-037** Evidence timeline records shall preserve chronological observation events for later reproduction.
- **FR-038** Burp UI shall expose Overview, Traffic, Contexts, Endpoints and Configuration views.
- **FR-039** ActiveRequestExecutor and scanner boundaries shall exist while unrestricted active execution remains disabled in Sprint 2.
- **FR-040** ACRA-Lab shall provide a deterministic basic secure/vulnerable integration fixture and ground truth for live context reconstruction.

- **NFR-005** Montoya-specific types shall remain outside `acra-core`.
- **NFR-006** The Burp observer shall not mutate observed traffic.
- **NFR-007** Passive observation stores shall be bounded by configuration.
- **NFR-008** Sprint 2 performance shall be measured at 100, 1,000 and 10,000 passive observations as environmental baselines only.

- **SAFE-006** Active execution shall be disabled by default in Sprint 2.
- **SAFE-007** Scope, timeout, rate, concurrency, maximum-body and kill/disable controls shall exist before later active testing is enabled.

- **SEC-009** Credential-bearing Burp headers/fields shall be redacted or fingerprinted before normal persistent/serializable evidence.
- **SEC-010** Passive token/session correlation shall not equate a token fingerprint with a verified principal without evidence.

- **RES-006** EXP-INTEGRATION-001 shall compare reconstructed context with known lab principal, tenant, resource and action ground truth.
- **RES-007** Real Burp exercise is required before Montoya integration is described as demonstrated at evidence Level 3.
- **RES-008** Failure to execute mandatory Burp/lab gates shall be recorded as PARTIAL/BLOCKED rather than inferred PASS.

## Sprint 3 reconnaissance requirements

- **FR-041** Sprint 3 shall audit and preserve existing Sprint 2 intelligence before adding overlapping functionality.
- **FR-042** Identifier intelligence shall assign semantic categories such as USER_ID, TENANT_ID, RESOURCE_ID, ORGANIZATION_ID, ROLE_ID, WORKFLOW_ID or UNKNOWN with confidence/reason/source.
- **FR-043** Parameter intelligence shall classify query/body parameters into authorization-relevant roles without assuming every parameter affects access control.
- **FR-044** Header intelligence shall distinguish authentication, identity, tenant, role, routing, proxy, origin, custom-security and unknown headers without treating presence as vulnerability.
- **FR-045** API version discovery shall support path, query, version-header and media-type hints.
- **FR-046** Resource reconnaissance shall represent ownership, tenant, parent and other resource relationships with evidence references.
- **FR-047** OpenAPI/Swagger ingestion shall create a declared API model that can be correlated with observed traffic. Unsupported specification constructs shall remain explicit limitations.
- **FR-048** Observed endpoints shall be classified against API specifications as documented/observed, documented/unobserved, undocumented/observed, conflicting or unknown.
- **FR-049** Schema drift shall model endpoint, method, parameter, authentication and response mismatches as observations rather than vulnerability findings.
- **FR-050** Route intelligence shall distinguish framework templates, literal wildcards, catch-alls and regex-like routes and provide deterministic canonical/equivalence classifications.
- **FR-051** Response semantic fingerprints shall extract stable field sets, resource IDs, owner IDs, tenant IDs, volatile fields and error-like semantics without relying on response length alone.
- **FR-052** Collection and pagination intelligence shall model collection membership and page/offset/limit/cursor signals for later authorization research.
- **FR-053** AuthorizationMatrix, SecurityContextFingerprint, DifferentialTest and FindingFingerprint foundations shall exist without active authorization verdicts.
- **FR-054** Hybrid identity confirmation shall permit user/lab confirmation to coexist with inferred identity state rather than overwriting provenance.
- **FR-055** Endpoint prioritization shall rank testing value from observed evidence and must not be represented as vulnerability severity.
- **FR-056** Reconnaissance-to-test handoff shall create candidate future test families and estimated request counts in dry-run mode with zero network dispatch.
- **FR-057** Context Coverage shall explicitly report which security-context dimensions are known/unknown for an endpoint.
- **FR-058** Sprint 3 Burp UI source shall expose reconnaissance views for parameters, identities, tenants, resources, routes and the context graph in addition to Sprint 2 views.

- **NFR-009** Sprint 3 reconnaissance algorithms shall remain deterministic in the core and require no Burp dependency.
- **NFR-010** OpenAPI parser support boundaries shall be documented rather than silently treating partial parsing as standards completeness.
- **NFR-011** Sprint 3 performance results shall remain observational engineering measurements with no release threshold unless separately accepted.

- **SAFE-008** Sprint 3 planning shall dispatch zero active requests.
- **SAFE-009** Existing Sprint 2 active execution defaults, zero budgets and kill-switch protections shall remain unchanged.

- **SEC-011** Security-relevant header/cookie intelligence shall not persist raw credential material through normal serializable evidence paths.
- **SEC-012** OpenAPI and manual context information shall be treated as evidence sources and shall not silently override conflicting observed identity/tenant state.

- **RES-009** Sprint 3 controlled experiments shall measure endpoint discovery, semantic identifier classification, tenant/owner extraction, route equivalence, context coverage and evidence completeness against explicit fixture ground truth.
- **RES-010** Sprint 3 fixture metrics may only be claimed for the measured controlled dataset and shall not be generalized to real API accuracy.
- **RES-011** Real Burp exercise remains required before Sprint 3 Burp reconnaissance is described as demonstrated at evidence Level 3 or higher.

## Sprint 11 research evaluation requirements

- **FR-059** The research layer shall represent the A0–A7 ablation sequence as deterministic, cumulative treatment definitions.
- **FR-060** A0 shall remain a naive differential baseline and A7 shall represent the full registered ACRA correlation stack.
- **FR-061** The protocol shall expose an explicit execution state so protocol definition cannot be confused with experiment completion.
- **FR-062** Controlled evaluation datasets shall be registered independently of treatment output before any A0–A7 metric is promoted.
- **FR-063** Dataset cases shall preserve deterministic ordering, stable IDs, provenance and independent binary ground truth.
- **NFR-012** The Sprint 11 protocol shall have deterministic versioning, identity and SHA-256 fingerprinting.
- **SAFE-010** Sprint 11 research execution shall remain limited to controlled registered ground truth until a later explicit safety gate authorizes anything broader.
- **SEC-013** Research protocol, dataset manifests and exported metrics shall not require raw credential material.
- **RES-012** A0–A7 shall use the registered cumulative methodology: identity, ownership, tenant, role, workflow, semantic evidence and evidence correlation.
- **RES-013** Required metrics are TP, TN, FP, FN, precision, recall, F1 and evidence completeness.
- **RES-014** No A0–A7 metric may be recorded as an actual result before corresponding controlled execution evidence exists.
- **RES-015** Controlled-dataset results shall not be generalized to real-world scanner accuracy or novelty without separate evidence.



## Sprint 12 reproduction and standards-export requirements

- **FR-064** A review-only reproduction package shall preserve candidate identity, endpoint/resource context, expected/observed decisions, dimensions, policy references and supporting evidence without persisting raw principal identifiers or candidate rationale.
- **FR-065** Reproduction packages shall provide deterministic JSON and SARIF 2.1.0 exports with stable SHA-256 content identities.
- **FR-066** SARIF projection shall preserve ACRA review semantics; unconfirmed candidates shall not be emitted as `kind=fail`.
- **FR-067** Burp Issue projection shall be isolated from core and shall not make a review candidate automatically publishable.
- **NFR-013** Reproduction export ordering, package identity, fingerprints and digests shall be deterministic.
- **SAFE-011** Reproduction export shall perform no active network replay by default.
- **SEC-014** Reproduction/SARIF/Burp projections shall exclude raw credentials, raw principal identifiers and candidate rationale unless a later explicitly authorized local-only artifact policy is defined.
- **RES-016** Standards-format interoperability evidence shall not be represented as vulnerability-detection accuracy evidence.

## Sprint 13 finding lifecycle governance requirements

- **FR-068** A governed finding shall enter lifecycle only from an evidence-backed `FindingCandidateState.CANDIDATE`.
- **FR-069** New governed findings shall begin in `REVIEW_REQUIRED`; severity, confidence and internal risk score shall not automatically confirm a vulnerability.
- **FR-070** Finding lifecycle transitions shall require explicit reviewer, decision and evidence references and shall be retained as append-only deterministic events.
- **FR-071** Supported lifecycle dispositions shall cover confirmation, false-positive review, accepted risk, remediation, retest, resolution and closure with fail-closed transitions.
- **FR-072** A failed retest shall return a previously confirmed finding to `CONFIRMED`; a passed retest may progress to `RESOLVED`.
- **NFR-014** Governed finding, lifecycle-event and transition identities/fingerprints shall be deterministic and history validation shall reject stale or contradictory state.
- **SAFE-012** Finding lifecycle transitions shall perform no network action, active replay or automatic Burp publication.
- **SEC-015** Reviewer references, decision references and lifecycle evidence references shall reject recognized secret-bearing material.
- **RES-017** Finding lifecycle state shall not be used as research ground truth unless separately registered by an independent research protocol.

