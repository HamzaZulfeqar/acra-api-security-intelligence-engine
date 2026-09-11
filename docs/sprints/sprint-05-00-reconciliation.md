# Sprint 5 source reconciliation — 2026-09-09

Canonical input: `acra-sprint-05-04-evidence-correlation-checkpoint-recreated.zip`.
SHA-256: `34b846321b1e2b4a6791bf8948c127cfba26697ec0425ec8dda8ea782e38df01`.

The supplied filename differs from the historical checkpoint name; its bytes match the user-provided canonical hash. The existing extracted working tree at `work/acra-sprint-05-final-work/acra` was retained on resumption. No original ZIP was extracted over that tree. No Git metadata is present in the extracted repository; changes are compared against the canonical archive bytes.

The source inspection covered root state/matrix/roadmap/changelog/known issues, core models and engines, tests, S4 evidence/replay/graph, extension UI, Maven declarations, scripts, configuration, lab fixtures, ground truth, research records and packaging manifest.

## Verified baseline findings

| Area | Actual source | Baseline status |
|---|---|---|
| S4 | `docs/sprints/sprint-04-final-software-audit.md`, existing active engine and graph | Historical S4 SOFTWARE COMPLETE; current offline regression separately verified |
| S5 context | `domain/authorization/AuthorizationContext.java` and `engine/SecurityContextEngine.java` | PARTIAL: context record exists; `AuthorizationContextNormalizer`, `AuthorizationContextCompleteness`, `AuthorizationFactState`, `AuthorizationContextAssessment` are absent |
| BOLA | `BolaAssessment`, `BolaAssessmentEvaluator`, `BolaAssessmentTests` | PARTIAL: three baseline reported checks; incomplete context/evidence could pass |
| BFLA | `BflaAssessment`, `BflaAssessmentEvaluator`, `BflaAssessmentTests` | PARTIAL: three baseline reported checks; action incorrectly copied into endpoint |
| Correlation | `AuthorizationAssessmentCorrelator`, `AuthorizationAssessmentAggregate`, `CorrelationState` | WEAK: no focused tests, empty conflicts, false duplicate/corroboration rules |
| Remaining S5 | Source search for tenant/workflow/property assessments, candidate, severity and final orchestration | MISSING |
| UI/configuration | Existing S4 Swing panels, `config/README.md`, `ui/dashboard/README.md` | No S5 result projection or S5 policy configuration |
| ACRA-Lab | `lab/common/basic_api.py`, existing S4 ground truth | S4 fixtures retained; no S5 runner/ground truth or independent property authorization fixture |
| Package metadata | `VERSION`, POMs, `REPOSITORY_MANIFEST.txt` | Version 0.3.0-rc1; manifest stale; do not manufacture a released version |

Baseline core compilation passed with `--release 21 -Xlint:all -Werror` using OpenJDK 26.0.1. The six baseline suites reported 43, 47, 54, 52, 87 and 132 checks (415 total) on 2026-09-05. Repeated executions are not additional distinct assertions. The final current-tree run is recorded separately in `docs/testing/artifacts/verification-s5-defensive.json`.

## Reuse and boundary

Existing URI, Endpoint, Principal, Role, Tenant, Resource, WorkflowState, Action, AuthorizationContext, BOLA/BFLA records, aggregate/correlator, DomainSerializer, UniversalRedactor, Observation, Evidence, replay and SecurityContextGraph remain the sole implementations. This continuation adds defensive guards, conflict preservation, credential redaction, tests and repeatable verification/package scripts. It does not add an active discovery or vulnerability-reproduction workflow.

The final source-backed requirement matrix is in `sprint-05-final-software-closure.md`. Earlier completion claims are retained as historical records and superseded for current S5 status by that matrix.

Packaging-time note: the original canonical ZIP later became unavailable at its supplied Downloads path. Earlier successful hash verification remains historical evidence; no new baseline byte diff is claimed. The current tree is exported with clean-unpack verification, and recorded edits are separately inventoried.
