# ACRA Sprint 4 Localhost Integration Checkpoint Manifest

**Checkpoint:** `S4-RESUME-2026-08-31-LOCALHOST-INTEGRATED`  
**Previous checkpoint:** `S4-RESUME-2026-08-31-CORE-INTEGRATED` / `S4_ACTIVE_CORE_VERIFIED_LOCAL_TESTS`  
**Candidate:** `0.4.0-rc1`  
**State:** LOCALHOST INTEGRATION PASS / GRAPH + RESEARCH + UI PENDING  
**Execution boundary:** authorized ACRA-Lab loopback HTTP only

## Verified continuation slice

- Existing Sprint 4 active core reused; no duplicate test/planner/executor/differential/observation/evidence/replay architecture created.
- `LocalhostHttpTransport` added behind the existing `HttpTransport` boundary.
- Transport rejects non-loopback hosts, request-authority escape, and non-`LAB` environments; redirects remain disabled.
- Existing ACRA-Lab extended with canonical independent ground truth `lab/ground-truth/GT-EXEC-S4.json`.
- Secure live baseline/positive control: `User-A → Document-A → ALLOW`.
- Secure live expected-DENY control: `User-A → Document-B → DENY`.
- Single declared mutation: `Document-A → Document-B`, with User-A/method/authority/endpoint family held constant.
- Secure mutation: observed `DENY`, differential `EXPECTED_CHANGE`.
- Deliberately vulnerable local mutation: independent expected `DENY`, observed `ALLOW`, differential `UNEXPECTED_CHANGE`; retained as Observation only.
- Live timeout maps to operational `TIMEOUT` and creates no security Observation.
- HTTP-200 application denial, dynamic timestamp/request-ID, and formatting/order semantic-preparation cases pass.
- Synthetic bearer and response-cookie redaction verified at evidence boundaries.

## Verification

- Sprint 4 core: PASS, 54 assertions.
- Sprint 4 engine security: PASS, 41 assertions.
- Sprint 4 localhost integration: PASS, 51 assertions.
- Core regression: PASS, 37 assertions.
- Sprint 3 core regression: PASS, 47 assertions.
- Sprint 3 adapter regression: PASS, 11 assertions.
- Current verified assertion total: PASS, 241.
- Sprint 3 security/architecture: PASS.
- Sprint 4 architecture: PASS.
- Maven/Montoya package: BLOCKED / UNVERIFIED.
- Historical S2/S3 Burp Level 3/4: BLOCKED / UNVERIFIED, unchanged.

## Research scope

`research/EXP-EXEC-001.md` is Level 4 evidence only for the controlled localhost execution-substrate claim. Authorization-vulnerability accuracy, FP/FN rates, request-efficiency and performance are NOT MEASURED.

## Next exact Sprint 4 task

Integrate completed active Observation/Evidence records into the existing `SecurityContextGraph`, with provenance and incomplete-context blocking. Do not start Sprint 5.
