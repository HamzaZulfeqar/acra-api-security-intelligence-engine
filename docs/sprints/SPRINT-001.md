# Sprint 1: Core Security Context & Domain Engine

**Version target:** v0.1.0  
**Type:** Foundation / Core Engine  
**Status:** VERIFIED

## Mission

Build the deterministic domain engine that converts normalized HTTP transactions into evidence-backed security context and graph relationships without vulnerability classification or active testing.

## Implemented feature set

S1-01 through S1-30 are represented by domain code, contracts, executable tests, or measured baselines as appropriate. Active testing and future protocol exploitation remain explicitly out of scope.

## Acceptance results

| Acceptance | Result |
|---|---|
| AT-01 tenant/resource extraction | PASS |
| AT-02 raw/decoded/canonical URI separation | PASS |
| AT-03 JWT `sub` principal with provenance | PASS |
| AT-04 principal -> tenant graph relation with evidence | PASS |
| AT-05 direct principal -> resource query | PASS |
| AT-06 bearer secret excluded from persisted serialization | PASS |
| AT-07 conflicting tenant evidence preserved | PASS |
| AT-08 unknown input remains UNKNOWN | PASS |

## Verification

- Build: PASS using Java 21 `javac -Werror`
- Unit/negative/contract tests: 37 PASS
- Security tests: PASS
- Regression test suite: PASS (same deterministic suite, no prior runtime regressions existed)
- Burp validation: NOT_APPLICABLE to Sprint 1
- Live lab validation: NOT_APPLICABLE to Sprint 1
- Performance baseline: EXECUTED, measurements recorded separately

## Out of scope maintained

No BOLA/BFLA exploitation, path attack engine, workflow bypass, OAuth exploitation, GraphQL/gRPC/WebSocket security engine, mass scanning, active network execution, or LLM-driven vulnerability classification was added.

## Next sprint

Sprint 2 may add the Montoya adapter after resolving the Burp/Montoya compatibility ADR and any build-tool decision needed for external dependencies.
