# EXP-BURP-INTEGRATION-001 — Burp to ACRA to ACRA-Lab Runtime Validation

**Sprint:** 2  
**Target release:** v0.2.0  
**Repository candidate:** v0.2.0-rc1  
**State:** BLOCKED / UNVERIFIED  
**Last attempted:** 2026-08-30T19:51:24Z

## Hypothesis

> ACRA can ingest a real HTTP transaction through Burp/Montoya and reconstruct the same security context previously reconstructed through direct local ingestion.

## Ground truth

- Principal: `user-a`
- Tenant: `tenant-a`
- Resource: `document:1001`
- Owner: `user-a`
- Action: `READ`
- Ground-truth record: `lab/ground-truth/GT-INTEGRATION-001.json`

## Required runtime path

```text
Client
  ↓
Burp Proxy
  ↓
Montoya HTTP handler
  ↓
ACRA TrafficCollector
  ↓
HttpTransaction
  ↓
Security Context
  ↓
Security Context Graph
  ↓
Evidence
```

## Environment discovered in this execution

- OS: Debian GNU/Linux 13 (trixie), x86_64
- Java: OpenJDK 21.0.11
- Maven executable: NOT AVAILABLE
- Outbound artifact/DNS resolution: BLOCKED in container
- Burp Suite installation: NOT AVAILABLE
- Git commit metadata: NOT AVAILABLE in supplied working copy
- Selected Montoya API: `2026.7`
- Selected stable Burp runtime gate: `2026.7.3`
- Selected Maven baseline: `3.9.16`

## Attempt result

| Step | Result | Evidence |
|---|---|---|
| Verify RC1 local repository | PASS | `docs/testing/artifacts/verification-s2.txt` |
| Resolve official Montoya dependency | BLOCKED / UNVERIFIED | Maven unavailable and container DNS/artifact access blocked |
| Package extension with official dependency | BLOCKED / UNVERIFIED | dependent on previous step |
| Start real Burp Suite | BLOCKED / UNVERIFIED | Burp is not installed in execution environment |
| Load ACRA extension | BLOCKED / UNVERIFIED | real Burp unavailable |
| Observe real Burp HTTP request/response | BLOCKED / UNVERIFIED | real Burp unavailable |
| Verify ACRA Burp UI | BLOCKED / UNVERIFIED | real Burp unavailable |
| Compare Burp-derived context with ground truth | BLOCKED / UNVERIFIED | no Burp-derived transaction exists |
| Verify graph provenance from real Burp traffic | BLOCKED / UNVERIFIED | no Burp-derived transaction exists |
| Verify synthetic credential redaction through real Burp | BLOCKED / UNVERIFIED | no Burp-derived transaction exists |

## Existing non-Burp baseline

`EXP-INTEGRATION-001` remains preserved and PASS for the direct local live-HTTP path. It reconstructed `user-a`, `tenant-a`, `document:1001`, owner `user-a`, action `READ`, and a canonical endpoint with redacted serialized credential evidence.

This non-Burp result is **not substituted** for the required Burp experiment.

## Result

**BLOCKED / UNVERIFIED**

The experiment cannot be promoted to PASS in this environment. Sprint 2 remains `v0.2.0-rc1`, research evidence level remains Level 2 for the Burp integration claim, and release decision remains HOLD.
