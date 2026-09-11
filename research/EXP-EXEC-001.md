# EXP-EXEC-001 — Controlled Localhost Execution Pipeline

**Sprint:** 4  
**Candidate:** `0.4.0-rc1`  
**State:** COMPLETED_LOCAL  
**Execution boundary:** ACRA-Lab only, loopback HTTP, synthetic identities/resources  
**Ground truth:** `lab/ground-truth/GT-EXEC-S4.json`  
**Execution evidence:** `docs/testing/artifacts/exp-exec-001-localhost.txt`  
**Verification evidence:** `docs/testing/artifacts/verification-s4-localhost.txt`

## Hypothesis

A loopback-only concrete transport can drive the existing Sprint 4 baseline/control/mutation executor through ACRA-Lab and produce semantic differential observations with traceable, redacted evidence, while operational failures remain separate from security observations.

## Lab configuration

- Secure service: `ACRA_LAB_MODE=secure`, `127.0.0.1:18082`.
- Vulnerable service: `ACRA_LAB_MODE=vulnerable`, `127.0.0.1:18081`.
- Transport: `LocalhostHttpTransport`, redirects disabled, one configured authorized `LAB` target only.
- Authentication: synthetic unsigned JWT-shaped fixture tokens consumed only by the ACRA-Lab claim decoder.
- Baseline: `User-A → Document-A → READ`.
- Expected-ALLOW control: `User-A → Document-A → READ`.
- Secure expected-DENY control: `User-A → Document-B → READ`.
- Controlled experiment: `User-A → Document-B → READ`, constructed by exactly one path resource-reference mutation from the baseline.
- Vulnerable-fixture negative control: `User-B → Document-A → READ`, retained independently so the deliberately vulnerable `Document-B` mutation can be evaluated without invalidating the four-way control set.

The policy outcomes come from `GT-EXEC-S4`; they are not calculated from ACRA's differential result.

## Independent ground truth

`GT-EXEC-S4` defines the identities, tenants, resources, owners and expected decisions independently of ACRA output. The core policy cases used here are:

- `GT-S4-ALLOW-A-A`: User-A / Tenant-A / Document-A / owner User-A / expected `ALLOW`.
- `GT-S4-DENY-A-B`: User-A / Tenant-A / Document-B / owner User-B / expected `DENY`.
- `GT-S4-DENY-B-A`: User-B / Tenant-B / Document-A / owner User-A / expected `DENY`.
- `GT-S4-VULN-A-B`: secure policy expects `DENY`, deliberately vulnerable fixture is expected to expose `ALLOW` for the controlled Document-B case.

## Controlled difference

Exactly one declared request dimension changes in the experiment:

```text
Document-A
    ↓
Document-B
```

The principal (`User-A`), authentication context, HTTP method (`GET`), target authority and endpoint family remain unchanged. Independent ground truth expects `DENY` for `User-A → Document-B`.

## Secure execution

**Execution ID:** `S4-EXEC-00000002`  
**Request fingerprint:** `93ee763813edb461888011e89caf8bbb836cc2e644edd3ed74ea124426642005`  
**Response fingerprint:** `f6e299b4fad2cd8f584c88aded4a8b28ee746decdbe0506474ac90b46eb4d486`

Observed semantic sequence:

```text
Baseline                  ALLOW
Expected-ALLOW control    ALLOW
Expected-DENY control     DENY
Controlled mutation       DENY
```

**Expected:** `DENY`  
**Differential classification:** `EXPECTED_CHANGE`  
**Evidence-chain entries:** 11  
**Result:** PASS

The secure negative control uses the same User-A authentication context and targets Document-B, satisfying the explicit expected-DENY control contract. The live response carried a synthetic session cookie; `ResponseSnapshot` stored its value as `<redacted>`. Persistable evidence and safety-audit serialization were checked for the raw synthetic bearer token and it was absent.

## Vulnerable synthetic control

**Execution ID:** `S4-EXEC-00000003`  
**Request fingerprint:** `9973a381c4a8a1a423ee0cd7ca79d78abf99202da099fee5257d7eb655d30b0c`  
**Response fingerprint:** `7787fca70dcda3d37f7ab0f8961942aae5971983717a6c2b2e21daa28c90d39a`

The deliberately vulnerable fixture keeps an independent negative control (`User-B → Document-A → DENY`) valid while intentionally omitting the direct-resource authorization check for the controlled `User-A → Document-B` mutation.

Observed semantic sequence:

```text
Baseline                  ALLOW
Expected-ALLOW control    ALLOW
Independent deny control  DENY
Controlled mutation       ALLOW
```

**Independent expected policy for mutation:** `DENY`  
**Observed mutation:** `ALLOW`  
**Differential classification:** `UNEXPECTED_CHANGE`  
**Evidence-chain entries:** 11  
**Security finding:** not produced

This verifies that Sprint 4 can preserve an expected-vs-observed mismatch as an Observation without promoting it to a confirmed vulnerability.

## Minimum false-positive preparation

The requested minimum semantic cases were executed locally:

- HTTP `200` application-level denial → normalized `DENY`.
- Dynamic timestamp and request-ID variance → raw responses changed but semantic comparison remained equivalent.
- Public response with reordered fields and formatting variation → semantic comparison remained equivalent.

These are preparation cases only. No FP rate, precision, recall or F1 metric is calculated from them.

## Operational-failure separation

A live slow ACRA-Lab request with a 30 ms executor timeout produced `TIMEOUT`. The execution created no security Observation. A live connection-error case was not executed in this slice.

## Verified implementation changes

- Added `LocalhostHttpTransport` behind the existing `HttpTransport` interface.
- Extended the existing ACRA-Lab with isolated Sprint 4 fixtures; no second lab was created.
- Preserved existing request/response, semantic fingerprint, differential, Observation and evidence models.
- Preserved the verified process-wide execution-ID repair so separate executor instances do not collide within one process.
- Kept the active network client isolated to the Sprint 4 active transport package through the Sprint 4 architecture gate.
- Canonicalized the Sprint 4 ground-truth record to `GT-EXEC-S4.json`; no duplicate S4 ground-truth file remains.

## Test evidence

Final isolated-workspace verification for this slice:

- Sprint 4 core verification: 54 PASS.
- Sprint 4 engine-security verification: 41 PASS.
- Sprint 4 localhost integration: 51 PASS.
- Core regression: 37 PASS.
- Sprint 3 core regression: 47 PASS.
- Sprint 3 adapter regression: 11 PASS.
- Sprint 3 security/architecture: PASS.
- Sprint 4 architecture: PASS.
- Current verified assertion total represented by these suites: 241 PASS.
- Full Maven/Montoya package: BLOCKED / UNVERIFIED because Maven/dependency resolution remain unavailable.

## Result

**Local execution-substrate claim:** PASS.  
**Research evidence level for this claim:** Level 4, controlled live local experiment.  
**Authorization-vulnerability accuracy:** NOT MEASURED.  
**FP/FN campaign:** NOT MEASURED.  
**Burp runtime:** UNVERIFIED for Sprint 4; historical S2/S3 Level 3/4 status remains BLOCKED / UNVERIFIED.

## Limitations

- The concrete transport is deliberately restricted to an authorized loopback `LAB` target.
- ACRA-Lab is a synthetic research fixture, not a production authorization system.
- Lab tokens are synthetic and are not cryptographically validated.
- `UNEXPECTED_CHANGE` is an Observation, not a vulnerability finding.
- Active observations are not yet hydrated into the existing `SecurityContextGraph`.
- No large FP/FN, request-efficiency, performance, active UI or Burp execution was performed in this slice.
