# Sprint 12 — Reproduction Export & Interoperability

Status: **IN PROGRESS — Phase 1 VERIFIED**  
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

## Next dependency

Phase 2 implements deterministic generic reproduction JSON. JSON remains CONTRACT_DEFINED until that renderer is
verified.
