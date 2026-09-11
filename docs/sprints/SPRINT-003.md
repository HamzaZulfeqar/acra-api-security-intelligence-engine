# Sprint 3: API Reconnaissance Expansion, Semantic Resource Intelligence & OpenAPI/Traffic Correlation

**Target:** v0.3.0  
**Current candidate:** v0.3.0-rc1  
**State:** PARTIAL  
**Type:** API reconnaissance / semantic intelligence / dry-run planning

## Objective

Extend the verified Sprint 2 passive intelligence pipeline into a schema-aware, security-relevant API reconnaissance layer without implementing authorization exploitation or vulnerability findings.

## Governance exception

Sprint 2 remains `v0.2.0-rc1` at evidence Level 2 because real Burp runtime and official Maven dependency gates are blocked in this environment. Sprint 3 development proceeded from the canonical RC1 repository under an explicit governance exception. This does not retroactively promote Sprint 2.

## S2 capability audit

| Capability | Repository evidence before S3 | Audit state | S3 action |
|---|---|---|---|
| HTTP raw request/response model | Core + Montoya mapper | PASS | Preserve |
| Raw/decoded/canonical URI | `UriModel`, `DefaultUriExtractor` | PASS | Extend semantically |
| Identifier detection | `IdentifierDetector` | PASS | Add semantic classification |
| Tenant extraction | `DefaultTenantExtractor` | PASS | Preserve, add recon classification/manual context contracts |
| Resource extraction | `DefaultResourceExtractor` | PASS | Preserve, add relationships/collection semantics |
| Owner evidence | Resource extractor + graph OWNS | PASS | Preserve and expose to recon |
| Action classification | `DefaultActionExtractor` | PASS | Preserve |
| Endpoint fingerprinting | `Endpoint` + inventory | PASS | Add route grammar/equivalence/lifecycle inputs |
| API inventory | `EndpointInventory` | PASS | Enrich with spec/recon evidence |
| Response normalization | `ResponseNormalizer` | PASS / INITIAL | Add semantic fingerprinting |
| OpenAPI parser | absent | MISSING | Implement |
| Schema-vs-traffic drift | absent | MISSING | Implement |
| Route grammar/equivalence | absent | MISSING | Implement |
| Security parameter classification | absent | MISSING | Implement |
| Context coverage | absent | MISSING | Implement |
| Recon risk prioritization | absent | MISSING | Implement |
| Dry-run test planner | absent | MISSING | Implement with zero dispatch |

## Implemented

- semantic identifier classification: USER_ID, TENANT_ID, RESOURCE_ID, ORGANIZATION_ID, ROLE_ID, WORKFLOW_ID, UNKNOWN.
- query/body parameter intelligence with security-relevant roles.
- security header classification.
- API version discovery from path, query, headers and Accept media-type hints.
- resource relationship records with evidence references.
- route grammar for observed, brace, colon, angle/framework, wildcard, catch-all and regex-like forms.
- route canonicalization and deterministic equivalence classification.
- dependency-free OpenAPI 3.x / Swagger 2.0 JSON importer plus a deliberately limited common YAML subset.
- OpenAPI-to-observed endpoint correlation.
- schema drift observations for endpoints, methods, parameters, auth schemes and response codes.
- response semantic fingerprints with resource, owner, tenant and volatile-field extraction.
- soft-denial/error-like semantic classification without authorization verdicts.
- semantic resource matching tolerant of reordered JSON and dynamic fields.
- collection and pagination intelligence.
- authorization matrix domain object.
- Security Context Fingerprint domain object.
- DifferentialTest domain object.
- deterministic finding-fingerprint foundation for future deduplication.
- hybrid identity confirmation registry and manual context mapping contract.
- endpoint risk prioritization as testing priority only, never vulnerability severity.
- zero-dispatch reconnaissance-to-test dry-run planner.
- target-profile domain contract.
- Sprint 3 passive pipeline integration and per-transaction reconnaissance store.
- Burp UI source extended with Parameters, Identities, Tenants, Resources, Routes and Context Graph views.
- expanded 10-operation secure/vulnerable ACRA-Lab reconnaissance fixture and OpenAPI 3.1 specification.

## Explicitly not implemented

- BOLA/BFLA exploitation or vulnerability conclusions.
- tenant or routing bypass attempts.
- active mutation dispatch.
- unrestricted network requests from the planner.
- authorization policy inference from HTTP status.
- full OpenAPI `$ref` resolution, callbacks/webhooks, discriminator semantics or complete YAML parser.
- GraphQL/gRPC/WebSocket protocol analyzers.
- real Burp runtime validation in this environment.

## Verification

| Gate | State |
|---|---|
| Sprint 1 regressions | PASS, 37 |
| Sprint 2 regressions | PASS, 52 |
| Sprint 3 core assertions | PASS, 47 |
| Sprint 3 adapter assertions | PASS, 11 |
| Total executable assertions | PASS, 147 |
| Security gate | PASS |
| Architecture boundary | PASS |
| Local S3 lab experiment | PASS |
| Controlled recon metrics experiment | PASS within fixture scope |
| S3 performance baseline | EXECUTED / OBSERVATIONAL |
| Official Maven/Montoya dependency build | BLOCKED / UNVERIFIED |
| Real Burp S2 runtime gate | BLOCKED / UNVERIFIED |
| Real Burp S3 UI/integration | BLOCKED / UNVERIFIED |

## Controlled metric result

These results apply only to the intentionally small Sprint 3 ground-truth fixtures and MUST NOT be generalized to real APIs:

- endpoint discovery precision: 1.0000
- endpoint discovery recall: 1.0000
- semantic identifier precision: 1.0000
- semantic identifier recall: 1.0000
- tenant detection accuracy: 1.0000
- owner extraction accuracy: 1.0000
- route-equivalence accuracy: 1.0000
- mean context coverage: 0.8111 across nine defined coverage fields
- evidence completeness: 1.0000 for the measured inferred fields
- soft-200 denial fixture handled: true
- reordered/dynamic same-resource fixture handled: true

## Definition-of-Done result

**PARTIAL.** Offline/core S3 scope is implemented and locally validated, but the sprint specification requires live Burp Level 3 and controlled Burp Level 4 evidence. Those remain blocked, therefore v0.3.0 cannot be promoted.

## Next release gate

Run the existing S2 Burp promotion experiment and the S3 Burp recon runbook on a machine with Burp, Maven and official Montoya dependency access. Preserve screenshots/logs and actual transaction evidence. Only after required live gates pass may `v0.3.0-rc1` be promoted.
