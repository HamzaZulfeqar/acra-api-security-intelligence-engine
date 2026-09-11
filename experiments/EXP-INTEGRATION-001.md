# EXP-INTEGRATION-001: Live Traffic to Security Context

**State:** PARTIAL  
**Sprint:** 2  
**Target release:** v0.2.0  
**Candidate:** v0.2.0-rc1

## Research question

Can ACRA reliably transform live Burp traffic into a structured security context without losing information required for later authorization and routing analysis?

## Hypothesis

An authorized HTTP transaction observed through the Burp adapter can be converted into ACRA's transaction, URI, identity, tenant, resource, action, evidence, endpoint, and graph representations while persistent evidence remains free of raw credentials.

## Environment used for current execution

- Java: OpenJDK 21.0.11
- Local lab runtime: Python 3.13 standard library
- Docker: unavailable
- Maven: unavailable
- Burp Suite: unavailable
- Montoya compile target: 2026.7

## Ground truth

`GT-INTEGRATION-001`

- Principal: `user-a`
- Tenant: `tenant-a`
- Resource: `document:1001`
- Action: `READ`
- Endpoint family: `GET /api/v1/tenants/{tenant}/documents/{document_id}`

## Procedure executed

1. Start the secure local ACRA-Lab HTTP service.
2. Send a benign authenticated GET request using a synthetic lab token.
3. Capture the actual HTTP response.
4. Feed the observed request/response through the real Sprint 2 `TrafficCollector` and `TrafficIntelligencePipeline` using Montoya-shaped test doubles.
5. Compare ACRA context with known lab ground truth.
6. Verify serialized transaction/evidence contains no raw bearer token.

## Observed behavior

```text
HTTP status: 200
Transaction: ACRA-TX-000001
Principal: user-a
Tenant: tenant-a
Resource: document:1001
Action: READ
Endpoint: GET /api/v1/tenants/{tenant}/documents/{document_id}
Evidence count: 9
Local lab stage: PASS
Burp stage: UNVERIFIED_NOT_AVAILABLE_IN_EXECUTION_ENVIRONMENT
```

The raw execution artifact is stored at `docs/testing/artifacts/exp-integration-001-local-lab.txt`.

## Result

**PARTIAL**

The local live-HTTP portion demonstrates the core/collector/context path, but the hypothesis explicitly requires the real Burp adapter runtime path. Burp Suite was unavailable, so the mandatory Level 3 Burp and Level 4 Burp-to-lab evidence gates have not executed.

No precision/recall/F1 or authorization-detection claim is derived from this experiment.
