# Sprint 7 Workflow Authorization Architecture

## Dependency rule

Sprint 7 starts from the frozen Sprint 6 checkpoint and reuses:
- `AuthorizationPolicySnapshot`
- `Delegation`
- `AuthorizationDecision`
- `PolicyResolutionState`
- `Workflow` / `WorkflowState`
- `SecurityContextFingerprint`
- existing redaction/serialization boundaries.

## Core flow

```
WorkflowPolicySnapshot
        +
AuthorizationPolicySnapshot (S6 delegations)
        +
WorkflowAuthorizationRequest
        ↓
WorkflowAuthorizationResolver
        ↓
transition match
        ↓
role / tenant / approval / separation-of-duties checks
        ↓
delegation validation
        ↓
token-context fingerprint binding
        ↓
policy precedence/default decision
        ↓
WorkflowAuthorizationResolution
```

## Credential boundary

Sprint 7 token binding stores only SHA-256 fingerprints. Constructors reject non-SHA-256 token-context values.
Raw bearer tokens, cookies, API keys, passwords and session secrets are not valid workflow-binding material.
The fingerprint is retained only in explicit workflow binding/request state; `WorkflowAuthorizationResolution`
records the matched binding ID rather than copying the token-context fingerprint into downstream findings/reporting state.

## Fail-closed behavior

Missing required roles, approval, role separation, token binding or valid delegation prevents an ALLOW rule from
becoming eligible. Explicit default DENY then resolves the transition to DENY. Conflicting eligible allow/deny
rules remain CONFLICTING unless explicit evidence-backed precedence is supplied.


## Phase 2 assessment/finding path

```
WorkflowAuthorizationResolution
        ↓
S7WorkflowAssessmentEvaluator
        ↓
WorkflowTransitionAssessment
        ↓
EvidenceReferenceValidator
        ↓
S7WorkflowFindingCandidateEvaluator
        ↓
FindingCandidate
        ↓
AuthorizationSeverityEvaluator
        ↓
AuthorizationRiskAssessment
```

A candidate is emitted only for a verified expected-DENY / observed-ALLOW mismatch with valid project,
execution, test and observation ownership. Policy conflicts and evidence failures remain INCONCLUSIVE.

The downstream S7 result stores binding IDs and delegation IDs, but not raw authentication material or the
token-context fingerprint itself.

## Phase 3 active-validation path

```
WorkflowAuthorizationResolution (baseline ALLOW)
        +
WorkflowAuthorizationResolution (target state DENY)
        ↓
S7WorkflowPlanningCandidate
        ↓
S7WorkflowTestSeedFactory
        ↓
WORKFLOW_TRANSITION / BODY / STATE_CHANGING
        ↓
S7WorkflowPlanningBridge
        ↓
existing S4 TestPlanner → ExecutionQueue → MutationValidator → TestExecutor
        ↓
secure/vulnerable localhost ACRA-Lab
        ↓
Observation + MultiWayDifferential
```

The generated transition test is deliberately constrained to one mutable workflow dimension: the target state.
Workflow ID, principal, tenant, resource, action and source state must remain equivalent. A conflicting or
incomplete policy cannot produce an active seed. DELETE is not auto-generated, and the existing local execution
policy still requires an explicitly authorized loopback LAB target.

The controlled secure fixture denies `DRAFT → APPROVED` when the declared action is `SUBMIT`; the deliberately
vulnerable fixture models a target-state validation defect and permits the same request. This yields a reproducible
expected-vs-unexpected differential without claiming real-world scanner accuracy.

## Phase 4 coverage/product projection

Coverage is modeled as a provenance lifecycle, not a security verdict:

```
WorkflowAuthorizationResolution
        ↓
POLICY_ONLY
        ↓ generated matching WORKFLOW_TRANSITION seed
PLANNED
        ↓ execution attempt
ATTEMPTED
        ↓ completed Observation with evidence
OBSERVED
```

The coverage identity binds policy fingerprint, workflow, principal, tenant, resource, action, source state and
target state. The tracker rejects resolution drift for an existing identity and rejects execution credit when the
test was not first registered as planned. Resolved, planned and observed ratios therefore describe evidence
coverage only; they do not imply vulnerability likelihood, finding severity or real-world scanner accuracy.

The Burp product projection uses `S7WorkflowWorkspace` as its source of truth. UI tables render policy rules,
resolved transitions, explicit conflicts and coverage entries without reconstructing or guessing authorization
state in Swing code.

## Phase 5 reporting/export boundary

```
S7WorkflowProductSnapshot
        ↓
S7WorkflowReportGenerator
        ↓
S7WorkflowReport
   ├── workflow policy
   ├── deterministic resolutions
   ├── transition assessments
   ├── coverage lifecycle
   ├── FindingCandidate (review only)
   ├── risk assessments
   ├── evidence IDs
   └── explicit limitations
        ↓
S7WorkflowReportExporter
   ├── canonical JSON + SHA-256
   └── deterministic Markdown
        ↓
S7WorkflowJsonReporter / Workflow UI
```

Report identity is derived from stable policy/resolution/coverage/candidate identities rather than wall-clock
time. The generated timestamp is report metadata, not part of the report identity. Coverage lifecycle state
remains distinct from finding state and severity. The report explicitly records zero confirmed findings because
Sprint 7 produces review candidates, not automatic vulnerability confirmation.
