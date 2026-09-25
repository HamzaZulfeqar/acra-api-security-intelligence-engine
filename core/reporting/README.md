# Reporting and reproduction exports

Sprint 12 Phase 1 adds a generic, deterministic, minimized reproduction package for review-only finding
candidates.

Verified targets:

- JSON reproduction export;
- SARIF 2.1.0 review export.

Current boundary:

- candidates are not promoted to confirmed vulnerabilities;
- principal/tenant identifiers, free-form rationale, raw request/response bodies and credentials are excluded;
- SARIF uses review semantics and `confirmed=false`;
- Burp Issue draft + Montoya projection boundary is verified in Sprint 12 Phase 2;
- real Burp issue submission remains UNVERIFIED / DEFERRED until a separate desktop-runtime gate.
