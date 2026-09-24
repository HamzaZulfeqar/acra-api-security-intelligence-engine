# Sprint 12 — Reproduction Export & Interoperability

Status: **IN PROGRESS — Phases 1–4 VERIFIED**  
Branch: `s12-reproduction-export-interoperability`  
Immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`

## Motivation

FR-013 requires JSON, Burp Issue and SARIF reproduction-package targets. Sprint 11 closed successfully, but the
repository still had no generic reproduction-package model, SARIF exporter or Burp Issue projection.

## Phase 1 — export-neutral reproduction package foundation

Implemented:

- `ReproductionExportTarget`;
- `ReproductionExportCapabilityState`;
- `ReproductionExportCapability`;
- `ReproductionPackage`;
- `ReproductionPackageProjector`;
- `ReproductionExportRegistry`.

Invariants:

1. Schema: `acra-reproduction-package-v1`.
2. Package ID and fingerprint are deterministic.
3. Supporting evidence is mandatory.
4. Candidate lifecycle is preserved.
5. Every package is review-only.
6. Expected/observed authorization remains explicit.
7. Export-facing strings are redacted.
8. Exact targets: JSON, SARIF, BURP_ISSUE.
9. Phase 1 claims target contracts only, not renderer implementation.

### Verification

Run `36071724705`: **SUCCESS** at `3c61578f5fa8b0635313ae6c0cbffe7c870aef40`.

- Sprint 12 foundation: PASS, 24 assertions;
- retained Sprint 11 verification: PASS;
- Java 21 warnings-as-errors compilation: PASS;
- Maven core test compilation: PASS.

## Phase 2 — deterministic generic reproduction JSON

Implemented:

- `ReproductionExportArtifact`;
- `ReproductionJsonExporter`;
- `ReproductionJsonReporter`;
- canonical deterministic JSON content;
- stable artifact SHA-256 and filename;
- review-only state / expected-observed authorization / evidence lineage preservation;
- universal redaction on exported content.

Verification:
- run `36072071561`: SUCCESS at `5722352c2c35e3f3c045705c6b85e8b9bbb335bf`;
- JSON suite: PASS, 21 assertions after promotion;
- capability promotion run `36072225976`: SUCCESS at `a6d437d412f61a329dc24afacaa20bc6ace900cd`;
- capability state: **IMPLEMENTED**.

## Phase 3 — SARIF 2.1.0 interoperability

Implemented:

- public canonical JSON serializer for standard-shaped documents;
- `ReproductionSarifExporter`;
- `ReproductionSarifReporter`;
- SARIF 2.1.0 version/schema;
- ACRA tool driver and stable rule;
- one review-only SARIF result per reproduction package;
- severity mapping to note / warning / error;
- evidence and ACRA candidate metadata in SARIF properties;
- deterministic content and SHA-256.

Verification:
- run `36072500775`: SUCCESS at `1b328b058f92c02500b044eb7de607c5e25de810`;
- SARIF suite: PASS, 28 assertions after promotion;
- independent Python JSON-shape validation: PASS;
- capability promotion run `36072674240`: SUCCESS at `d3bf2cc83be052fb5883b3187ff083a37ebaae53`;
- capability state: **IMPLEMENTED**.

## Phase 4 — Burp Issue projection + official Montoya adapter

Implemented:

- `BurpIssueSeverity`;
- `BurpIssueConfidence`;
- `BurpIssueProjection`;
- `BurpIssueProjector`;
- extension `BurpAuditIssueAdapter` compiled against Montoya 2026.7;
- official `AuditIssue.auditIssue(...)` construction path;
- explicit `api.siteMap().add(...)` adapter method;
- no automatic bootstrap wiring.

Safety/semantic boundaries:

- only CANDIDATE packages project;
- REJECTED and INCONCLUSIVE fail closed;
- all projected issues remain review-only;
- ACRA HIGH confidence maps to FIRM, never CERTAIN;
- query/fragment secret material is excluded from issue URL;
- real desktop insertion is not claimed by compile/headless evidence.

Verification:
- run `36073018537`: SUCCESS at `0d13916d40f17a52958f2c682798c76d3773ef73`;
- Burp issue projection: PASS, 23 assertions after promotion;
- adapter source contract: PASS;
- official Montoya adapter compilation: PASS;
- promotion run `36073182973`: SUCCESS at `2f893b6b4a8fb10ea752dd7308f74a8404b644bb`;
- capability state: **IMPLEMENTED_RUNTIME_UNVERIFIED**.

## Next dependency

Phase 5 must harden cross-format interoperability: one candidate must project consistently across JSON, SARIF and
Burp Issue; secrets must remain absent across all surfaces; capability states must remain exact; and invalid
candidate/evidence/origin inputs must fail closed. After Phase 5, Sprint 12 can enter final closure.
