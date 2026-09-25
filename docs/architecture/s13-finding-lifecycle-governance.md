# Sprint 13 Finding Lifecycle Governance Architecture

## Boundary

Sprint 13 adds the governed layer between review candidates and downstream remediation/reproduction workflows.

```text
FindingCandidate + AuthorizationRiskAssessment
                  |
                  v
          FindingLifecycleService
                  |
                  v
          REVIEW_REQUIRED
                  |
          explicit human review
                  |
       +----------+----------+
       |                     |
       v                     v
   CONFIRMED            FALSE_POSITIVE
       |                     |
       |                     +--> CLOSED
       |
       +--> ACCEPTED_RISK --> CLOSED
       |
       +--> REMEDIATION_IN_PROGRESS
                    |
                    v
              RETEST_REQUIRED
                 /       \
          PASS /           \ FAIL
              v             v
          RESOLVED      CONFIRMED
              |
              v
            CLOSED
```

## Trust boundary

Lifecycle governance consumes already evidence-backed candidates. It does not infer a vulnerability from HTTP
traffic, severity, confidence, risk score, SARIF output or Burp publication state.

The only entry state is `REVIEW_REQUIRED`. Confirmation is represented by an explicit lifecycle event containing:

- opaque reviewer reference;
- opaque decision reference;
- supporting evidence IDs;
- deterministic event identity/fingerprint.

Raw reviewer notes are deliberately absent from the Phase 1 record.

## Phase 1 invariants

- candidate state remains distinct from governed finding state;
- risk severity/confidence are copied as prioritization context only;
- transition policy is explicit and fail-closed;
- all human transitions require evidence;
- history cannot skip sequence numbers or contradict previous state;
- no lifecycle constructor can claim a non-review initial state without event history;
- deterministic identity allows stale/collision detection in later workspace phases;
- secret-bearing references fail closed;
- no Montoya/network dependency exists in the lifecycle core.

Phase 1 verification: GitHub Actions run `36077162629` — SUCCESS.

## Phase 2 dependency

A synchronized governance workspace must provide deterministic indexing, stale-snapshot protection, state
accounting and review queues while delegating every transition to the verified lifecycle service.
