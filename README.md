# ACRA

**API Access Control & Routing Auditor**  
**Canonical baseline:** RB-0  
**Current candidate:** v0.3.0-rc1  
**Current sprint:** Sprint 3, PARTIAL

ACRA is a context-aware API authorization-auditing research system. It is not a generic IDOR/BOLA payload generator and does not treat HTTP status alone as an authorization verdict.

## Current architecture

```text
Burp / Lab / fixtures
        -> traffic transaction
        -> Security Context Engine
        -> Security Context Graph
        -> API Reconnaissance Engine
             -> semantic identifiers/parameters/headers
             -> route intelligence
             -> OpenAPI correlation/schema drift
             -> response semantics
             -> context coverage/prioritization
        -> dry-run controlled test plan
        -> active authorization testing [Sprint 4+]
```

## Sprint 3 status

Local verification currently passes 37 Sprint 1 regressions, 52 Sprint 2 regressions, 47 Sprint 3 core assertions and 11 Sprint 3 adapter assertions. The 10-operation local ACRA-Lab reconnaissance experiment passes and controlled fixture metrics are recorded.

Real Burp runtime validation remains BLOCKED / UNVERIFIED because this environment does not include Burp, Maven or external artifact resolution. The release therefore remains `v0.3.0-rc1` and must not be described as demonstrated in Burp.

See [`PROJECT_STATE.md`](PROJECT_STATE.md) and [`docs/sprints/SPRINT-003.md`](docs/sprints/SPRINT-003.md) for authoritative status.

## Safety boundary

Sprint 3 does not implement BOLA/BFLA exploitation, authorization bypass, active routing mutation, brute force, mass scanning or vulnerability findings. Dry-run plans dispatch zero network requests and existing active-execution budgets remain locked to zero.
