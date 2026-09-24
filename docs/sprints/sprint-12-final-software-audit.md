# Sprint 12 Final Software Audit

Audit state: **CLOSURE CANDIDATE — final workflow pending**  
Branch: `s12-reproduction-export-interoperability`

## Current decision

Sprint 12 Phases 1–5 are verified. SOFTWARE COMPLETE is not yet claimed until the dedicated final closure workflow
passes retained regressions, official Maven packaging, deterministic source packaging and archive-integrity checks.

## Implemented software boundary

Sprint 12 includes:

- versioned export-neutral reproduction packages;
- evidence-backed FindingCandidate projection;
- deterministic generic JSON reproduction export;
- deterministic SARIF 2.1.0 reproduction export;
- review-only Burp Issue projection;
- official Montoya 2026.7 compile adapter using AuditIssue/SiteMap integration APIs;
- exact capability-state separation between implemented formats and runtime-unverified Burp desktop behavior;
- cross-format lineage and redaction hardening.

## Verified pre-final evidence

- Phase 1 foundation: run `36071724705`, 24 assertions;
- Phase 2 JSON: run `36072071561`, 21 assertions after promotion;
- Phase 3 SARIF: run `36072500775`, 28 assertions after promotion + independent JSON-shape PASS;
- Phase 4 Burp Issue: run `36073018537`, 23 assertions after promotion + official Montoya compile PASS;
- Phase 5 interoperability: run `36073674429`, 39 assertions;
- retained Sprint 11 verification: PASS through the Sprint 12 verifier.

Current capability matrix:

| Target | Software state | Runtime interpretation |
|---|---|---|
| JSON | IMPLEMENTED | deterministic core export |
| SARIF | IMPLEMENTED | deterministic SARIF 2.1.0 export |
| BURP_ISSUE | IMPLEMENTED_RUNTIME_UNVERIFIED | core projection + official Montoya compile adapter; no real desktop insertion evidence |

## Final closure gates

Pending:

1. complete Sprint 12 verification stack;
2. retained Sprint 11 software verification;
3. official Maven package;
4. retained Sprint 2 local-contract regression;
5. retained Sprint 3 core + adapter regression;
6. retained Sprint 4 / 6 / 7 / 8 / 9 / 10 headless UI regressions;
7. official Montoya extension compilation;
8. deterministic Sprint 12 source checkpoint;
9. archive safe-path / duplicate validation;
10. clean extraction equality;
11. per-file SHA-256 equality.

## Explicit non-blocking validation debt

Real Burp desktop load, handler execution and SiteMap insertion remain **UNVERIFIED / DEFERRED**. Headless source,
unit tests and official dependency compilation do not substitute for that runtime evidence.
