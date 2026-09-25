# Reporting and reproduction exports

Sprint 11 implements the finding reproduction/reporting layer that earlier sprints left open.

Implemented in core:

- minimized `FindingReproductionPackage`;
- canonical JSON export with SHA-256;
- SARIF 2.1.0 export;
- Burp Issue draft projection;
- Reporter adapters for JSON and SARIF;
- secret-minimized lifecycle/review projection.

The Burp-specific Montoya `AuditIssue` materialization adapter remains in
`extension/burp-extension`, preserving the core/Burp dependency boundary.

Important boundaries:

- a `FindingCandidate` is not automatically confirmed;
- Burp Issue draft/materialization does not automatically publish to Burp Site Map;
- real Burp desktop publication remains UNVERIFIED / DEFERRED;
- EXP-A0 through EXP-A7 remain NOT_RUN and their metrics are NOT_MEASURED.
