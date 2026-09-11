# Sprint 2: Burp Montoya Integration & Live Traffic Intelligence

**Target:** v0.2.0  
**Current candidate:** v0.2.0-rc1  
**State:** PARTIAL  
**Type:** Burp integration / passive intelligence

## Objective

Connect ACRA Core to Burp traffic, preserve the request/response evidence chain, perform passive security-context extraction, update the Security Context Graph, and expose the first Burp-local UI without implementing active vulnerability scanners.

## Implemented

- Montoya extension bootstrap, HTTP handler and unload lifecycle.
- Scope controller with all requested scope modes.
- Burp request/response correlation and ACRA transaction IDs.
- Request/response preservation and timing/source metadata.
- passive auth, identity/session, tenant, resource and action extraction.
- endpoint-family fingerprinting and API inventory service.
- global evidence-backed Security Context Graph updates.
- evidence timeline and observations distinct from findings.
- deterministic RAW/NORMALIZED/STRUCTURAL/SEMANTIC comparison primitives.
- volatile response masking baseline.
- Burp Swing tabs: Overview, Traffic, Contexts, Endpoints, Configuration, including a traffic-row detail panel for context/evidence timeline inspection.
- disabled-by-default active executor and scanner boundary.
- conservative scope/body/rate/concurrency/timeout configuration primitives.
- basic secure/vulnerable ACRA-Lab fixture and ground truth.
- EXP-INTEGRATION-001 local live-HTTP execution.
- Sprint 1 regressions and Sprint 2 local/security/architecture tests.
- 100/1,000/10,000 observation performance baseline.

## Explicitly not implemented

- BOLA/BFLA exploitation or findings.
- unrestricted active requests.
- aggressive mutations.
- scanner issue generation.
- policy-based authorization verdicts.
- OAuth/GraphQL/gRPC/WebSocket exploitation.

## Verification

| Gate | State |
|---|---|
| Sprint 1 regression | PASS, 37 tests |
| Sprint 2 automated suite | PASS, 52 assertions |
| Security checks | PASS |
| Architecture boundary | PASS |
| Local live ACRA-Lab integration | PASS |
| 100/1k/10k baseline | EXECUTED |
| Maven package with official dependencies | UNVERIFIED, Maven/network unavailable |
| Extension loaded in real Burp | BLOCKED / UNVERIFIED |
| Burp HTTP handler exercised | BLOCKED / UNVERIFIED |
| Burp UI exercised | BLOCKED / UNVERIFIED |
| Burp-to-lab Level 4 experiment | BLOCKED / UNVERIFIED |

## Definition-of-Done result

**PARTIAL.** The mandatory live Burp validation and controlled Burp-to-lab experiment have not executed. v0.2.0 therefore must not be tagged yet.

## Promotion gate

Run the real Burp validation procedure in `extension/burp-extension/README.md`, preserve evidence, update test/experiment records, and only then promote `VERSION` from `0.2.0-rc1` to `0.2.0`.
## Continuation execution result

**Recorded:** 2026-08-30T19:51:24Z  
**Version:** `v0.2.0-rc1`  
**Release decision:** HOLD  
**Research evidence level for Burp integration:** Level 2

The repository was re-verified without rewriting the existing Sprint 2 implementation. Local build/test/security/architecture/lab gates continue to pass. The official Montoya build could not execute because Maven is absent and outbound artifact resolution is blocked. A real Burp desktop runtime is not installed, so extension load, HTTP handling, UI behavior, scope/redaction through Burp, graph reconstruction from Burp traffic, and the Burp-to-lab experiment remain BLOCKED / UNVERIFIED.

`EXP-BURP-INTEGRATION-001` records the required runtime experiment and its blocker. No direct fixture or local HTTP result is substituted for Burp evidence.

A clean `v0.2.0-rc1` continuation package may be produced. A `v0.2.0` release package must not be produced until the promotion gate passes.
