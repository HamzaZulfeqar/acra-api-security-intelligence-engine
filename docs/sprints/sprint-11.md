# Sprint 11 — Finding Review Lifecycle, Reproduction & Research Ground-Truth Closure

Status: **IN PROGRESS — Phase 1 implementation candidate**

Branch: `s11-finding-lifecycle-reproduction`

Base: latest verified Sprint 10 software-complete line from `s10-batch-indirect-authorization`.

## Dependency decision

Sprint 10 is SOFTWARE COMPLETE. The next unresolved authoritative requirements are not another
authorization analyzer. The repository already contains:

- review-only `FindingCandidate` projection;
- independent deterministic `FindingSeverity` and `FindingConfidence`;
- controlled secure/vulnerable ACRA-Lab authorization fixtures;
- versioned deterministic report/export patterns for prior sprint product areas.

The canonical requirements still leave these product/research gaps open:

- FR-012: complete finding review lifecycle;
- FR-013: JSON, Burp Issue and SARIF reproduction targets;
- FR-015: baseline and A0-A7 ablation evaluation support.

`core/reporting/README.md` also explicitly records finding reporters and JSON/Burp Issue/SARIF
reproduction as future work.

## Sprint objective

Create the evidence-backed bridge from a review-only authorization candidate to a human-reviewed
finding record and reproducible export package, then close the labelled ACRA-Lab ground-truth
contract required by the later comparative research campaign.

Sprint 11 must not turn ACRA into an automatic vulnerability-confirmation engine.

## Planned dependency order

1. finding review lifecycle foundation;
2. reviewed-finding workspace/repository boundary;
3. reproduction package model with provenance minimization;
4. canonical JSON export;
5. SARIF export;
6. Burp Issue adapter contract without fabricating real Burp publication;
7. read-only finding/reproduction UI projection;
8. expanded labelled ACRA-Lab research ground truth and research-case registry;
9. security hardening, determinism and bounded performance observations;
10. traceability, regression, reproducible checkpoint and final software audit.

Sprint 12 comparative A0-A7 execution remains gated on Sprint 11 ground-truth closure.

## Phase 1 — finding review lifecycle foundation

Candidate implementation:

- `FindingLifecycleState`;
- `FindingReviewTransition`;
- `ReviewedFinding`;
- `FindingLifecycleService`;
- explicit candidate-to-review opening;
- explicit reviewer/evidence-backed transitions;
- stable finding identity derived from project/candidate/fingerprint;
- severity and confidence remain independent and immutable through lifecycle transitions;
- audit history is append-only and immutable to callers;
- secret-bearing review reason material is redacted at the domain boundary.

### Lifecycle contract

Existing `FindingCandidateState.CANDIDATE` is the pre-review state. A candidate is not itself a
confirmed finding.

The reviewed-finding lifecycle is:

```text
FindingCandidate(CANDIDATE)
        |
        v
NEEDS_REVIEW
   |          \
   v           v
VALIDATED   FALSE_POSITIVE
   |             [terminal]
   v
CONFIRMED
   |        \
   v         v
ACCEPTED_RISK FALSE_POSITIVE
  [terminal]     [terminal]
```

Rules:

- `REJECTED` and `INCONCLUSIVE` candidates cannot enter the lifecycle.
- `NEEDS_REVIEW -> CONFIRMED` is forbidden.
- `VALIDATED -> ACCEPTED_RISK` is forbidden.
- every state transition requires reviewer reference, decision reason and evidence;
- terminal states cannot be rewritten;
- lifecycle transitions never recalculate severity or confidence;
- no service method automatically confirms a finding from scanner output.

## Phase 1 acceptance boundary

Phase 1 must prove:

1. only a review candidate can open a finding;
2. candidate/risk identity must match;
3. supporting evidence is mandatory;
4. initial state is `NEEDS_REVIEW`;
5. severity and confidence remain independent;
6. finding identity is deterministic and render-time independent;
7. direct `NEEDS_REVIEW -> CONFIRMED` is rejected;
8. direct `VALIDATED -> ACCEPTED_RISK` is rejected;
9. validated and confirmed transitions are explicit and audited;
10. false-positive closure is explicit;
11. accepted-risk closure is only reachable after confirmation;
12. terminal states cannot transition;
13. timestamps cannot move backwards;
14. reviewer, reason and evidence are mandatory;
15. serialized review records remain secret-safe;
16. evidence/history collections are immutable to callers;
17. retained Sprint 10 reporting/security foundations remain green;
18. exact Java 21 compilation with warnings-as-errors passes;
19. Maven core test compilation passes.

## Explicit non-claims

Phase 1 does not yet claim:

- SARIF generation;
- Burp Issue publication;
- JSON reproduction-package closure;
- real Burp desktop validation;
- A0-A7 research execution;
- real-world scanner precision/recall/F1;
- automated confirmation of vulnerabilities.
