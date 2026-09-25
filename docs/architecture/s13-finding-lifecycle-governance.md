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

## Phase 3 read-only UI boundary

```text
FindingGovernanceWorkspace
          |
          v
FindingGovernanceSnapshot
          |
          v
S13FindingGovernancePanel
          |
          +--> Overview
          +--> Review Required
          +--> Confirmed
          +--> Remediation
          +--> Retest
          +--> Terminal
          +--> History
```

Phase 3 invariants:

1. UI state is derived from the same deterministic workspace snapshot.
2. Severity/confidence are displayed only as prioritization context.
3. CRITICAL severity can remain REVIEW_REQUIRED.
4. Append-only history exposes event order and opaque reviewer/decision/evidence references.
5. False-positive terminal history remains unconfirmed.
6. UI contains no lifecycle transition action control.
7. UI contains no reproduction/Burp publication action control.
8. Headless Swing verification is not real Burp desktop validation.

Phase 3 verification: GitHub Actions run `36079250303` — SUCCESS at
`b109b58c0a6ac7de032035e55ae57daffe6a84f6`.

## Phase 4 reporting boundary

```text
FindingGovernanceSnapshot
          |
          v
S13GovernanceReportGenerator
          |
          +--> minimized finding projections
          +--> append-only event projections
          +--> queue/history summary
          |
          v
S13GovernanceReport
          |
          +--> canonical JSON + SHA-256
          +--> deterministic Markdown + SHA-256
          +--> Reporter plugin
          +--> read-only Report / JSON UI
```

Phase 4 invariants:

1. Report identity is state-derived and independent of render timestamp.
2. Reporting consumes only immutable governance snapshots.
3. Current queue state and historical-confirmation state remain separate.
4. Event order and evidence references remain explicit.
5. Source candidate rationale/raw principal material is not part of the report schema.
6. Rendering/export cannot mutate governed-finding fingerprints or lifecycle history.
7. Reporting performs no network action, lifecycle transition or Burp publication.
8. UI previews use the same canonical exporter as archived artifacts.

Phase 4 verification: source run `36079738535` — SUCCESS at
`55d76ad0ff3f3536e5e76c9eefa8f3f6c1b00f04`.
Artifact retention: run `36079852878` — SUCCESS.

## Phase 5 dependency

Adversarial security/reproducibility tests and bounded engineering observations must precede Sprint 13 final closure.
