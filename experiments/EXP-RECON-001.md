# EXP-RECON-001 — Endpoint Reconnaissance

**State:** COMPLETED_LOCAL  
**Result:** PASS within controlled local ground truth  
**Burp evidence:** UNVERIFIED / BLOCKED

## Hypothesis

ACRA's Sprint 3 reconnaissance pipeline can reconstruct the known endpoint-operation inventory of the controlled ACRA-Lab fixture and correlate observed endpoint families with the supplied OpenAPI document.

## Ground truth

`GT-RECON-S3` defines 10 known operations.

## Procedure

1. Start the secure ACRA-Lab service directly with Python.
2. Import `lab/openapi/acra-lab-openapi.json`.
3. Send one benign authorized or public request for each of the 10 known operations.
4. Feed each resulting request/response through the same `TrafficCollector -> TrafficIntelligencePipeline` used by the adapter test path.
5. Compare inventory count and documentation correlation to ground truth.

## Observed

- known operations: 10
- observed endpoint operations: 10
- DOCUMENTED_OBSERVED: 10
- reconnaissance records: 10
- graph nodes: 22
- graph edges: 80
- dry-run dispatched requests: 0

## Scope limitation

This run does not contain a real Burp process. It is Level 2/local-lab evidence and must not be described as live Burp demonstration.
