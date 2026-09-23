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
