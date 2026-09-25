# Sprint 12 Requirements Traceability

Status: **CLOSURE CANDIDATE — Phases 1–5 verified; final closure pending**  
Branch: `s12-reproduction-standards-export`  
Immutable Sprint 11 base: `61441818179fed4aa1c1a143960bef53b6df9a11`

PASS means implementation plus executable evidence exists. Sprint 12 is not SOFTWARE COMPLETE until the dedicated
final closure workflow passes. Real Burp desktop SiteMap publication remains a separate runtime validation lane.

| ID | Requirement | Implementation evidence | Verification evidence | Status |
|---|---|---|---|---|
| FR-013 | Reproduction packages support JSON, Burp Issue and SARIF targets | S12 package/export/projection stack | Phases 1–4 | PASS |
| FR-064 | Review-only minimized reproduction package | `S12ReproductionPackage` / factory | Phase 1 + Phase 5 hardening | PASS |
| FR-065 | Deterministic JSON and SARIF 2.1.0 + SHA-256 | S12 JSON/SARIF exporters | Phase 1 + evidence artifact suite | PASS |
| FR-066 | Unconfirmed candidate not emitted as SARIF fail | `S12SarifExporter` | Phase 1 standards suite | PASS |
| FR-067 | Burp Issue projection isolated from core and non-publishable by default | core projection + extension adapter | Phases 1–3 | PASS |
| NFR-013 | Deterministic identities/fingerprints/digests | package/workspace/receipt/export contracts | Phases 1/3/5 | PASS |
| SAFE-011 | Export performs no active replay by default | reproduction/export architecture | foundation + UI gates | PASS |
| SEC-014 | Raw credentials/principal/rationale excluded | minimized package + hardening | Phases 1/5 | PASS |
| RES-016 | Interoperability evidence is not scanner-accuracy evidence | report/docs semantics | Phases 1–5 | PASS |
| S12-01 | Explicit approval required before Burp issue creation | publication approval + adapter | Phase 2 | PASS |
| S12-02 | Denied/mismatched approval causes zero publication side effects | publisher/factory/sink boundary | Phase 3 | PASS |
| S12-03 | Approved headless publication creates one review issue + receipt | publisher/receipt | Phase 3 | PASS |
| S12-04 | Receipt state cannot claim confirmed vulnerability | receipt contract | Phases 3/5 | PASS |
| S12-05 | Reproduction UI is read-only and contains no publish action | `S12ReproductionPanel` | Phase 4 UI suite | PASS |
| S12-06 | Candidate-package drift fails closed | `S12ReproductionWorkspace` | Phase 5 hardening | PASS |
| S12-07 | Publication URL excludes credentials/query/fragment | approval + receipt validation | Phase 5 hardening | PASS |
| S12-08 | Legacy S2/S3 stub contracts and real Montoya Maven compile both pass | Montoya stubs + Maven dependency | Phase 5 exact-head regressions | PASS |
| S12-09 | Canonical JSON/SARIF evidence files archived | evidence artifact suite | final closure workflow | PENDING |
| S12-10 | Official Maven package and retained release regressions | final verifier | final closure workflow | PENDING |
| S12-11 | Deterministic source ZIP + manifest + SHA-256 | `package-sprint12.sh` | final closure workflow | PENDING |
| S12-12 | Safe archive/no duplicates/clean extraction/per-file equality | package verifier | final closure workflow | PENDING |
| S12-13 | Real Burp desktop SiteMap publication | separate runtime gate | no current desktop publication evidence | UNVERIFIED / DEFERRED |

## Verified phase gates

- Phase 1: `36068186039` — SUCCESS
- Phase 2: `36068996138` — SUCCESS
- Phase 3: `36069442716` — SUCCESS
- Phase 4: `36070071498` — SUCCESS
- Phase 5: `36075554684` — SUCCESS
- Phase 5 retained Core/S2/S3: `36075554589` / `36075554624` / `36075554697` — SUCCESS

## Standards/runtime boundary

SARIF output targets SARIF 2.1.0. The extension is pinned to Montoya API 2026.7. Headless injected publication
evidence proves the software boundary only; it does not prove real Burp desktop SiteMap behavior.

## Final closure gate

Pending dedicated Sprint 12 Final Closure workflow verification.
