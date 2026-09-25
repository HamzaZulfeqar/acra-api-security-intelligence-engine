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

## Phase 2 governance-workspace boundary

```text
FindingCandidate + AuthorizationRiskAssessment
                  |
                  v
      FindingGovernanceWorkspace
                  |
                  +--> REVIEW_REQUIRED queue
                  +--> CONFIRMED queue
                  +--> REMEDIATION queue
                  +--> RETEST queue
                  +--> TERMINAL queue
                  |
                  v
        immutable deterministic snapshot
```

Phase 2 invariants:

1. One governed finding per source candidate.
2. Identical open is idempotent.
3. Candidate/risk drift fails closed.
4. Transition requires current finding fingerprint.
5. Every state mutation delegates to `FindingLifecycleService`.
6. Queue/state accounting is derived from lifecycle state, never severity.
7. Confirmed history is preserved after resolution/closure.
8. False-positive history never becomes confirmed.
9. Workspace exposes no direct state or publication shortcut.

Phase 2 verification: GitHub Actions run `36077517602` — SUCCESS at
`0c9533c3a58aae4d5f3aa27cd2fb48e694616d6d`.

## Phase 3 dependency

A read-only product/UI projection should expose governance queues and append-only lifecycle history without
introducing any UI control that can confirm, remediate, retest, close or publish a finding.
