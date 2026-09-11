# Sprint 4 Active Testing Engine

## Boundary

Sprint 4 is a controlled differential experiment engine for ACRA-Lab, synthetic fixtures and explicitly authorized loopback development targets. It is not a vulnerability scanner, does not discover external targets and does not promote an Observation to a finding.

The implementation remains inside `io.acra.core.active` and reuses the Sprint 1–3 endpoint, route, resource, AuthorizationMatrix, SecurityContextFingerprint, response-semantic and SecurityContextGraph models.

## Product flow

```text
PlanningInput
  -> TestPlanner / PlanningResult
  -> ExecutionQueue
  -> TestExecutor
  -> HttpTransport (LocalhostHttpTransport in S4)
  -> ResponseSnapshot / semantic normalization
  -> expected-decision resolution
  -> MultiWayDifferential
  -> Observation / EvidenceChain
  -> ObservationGraphIntegrator / existing SecurityContextGraph
```

`ActiveEngineWorkspace` is the product state shared by the Swing projection and these existing components. It does not replace the planner, queue or executor.

## Profiles and user modes

Five product profiles have executable definitions rather than labels:

| Profile | Primary families | Comparison | Evidence | Safety |
|---|---|---|---|---|
| Authorization Audit | user/resource/tenant/role/authentication differentials | semantic resource | detailed | safe read-only |
| Routing Audit | URI, normalization, route equivalence and method variation | structural | detailed | safe read-only |
| Context Audit | identity, role, tenant, resource and token context | status + semantic | detailed | safe read-only |
| Research Differential | all registered S4 contracts and mutations | research ablation | forensic | safe read-only |
| Expert Custom | all registered S4 contracts and mutations | status + semantic | forensic | safe read-only |

Each definition also supplies request/mutation budgets, concurrency, rate, evidence threshold and local-automatic policy. Beginner, Professional, Researcher and Expert modes map to concrete profile, selection, depth, limits, evidence and exposed-control configurations. Hidden controls cannot bypass a mode in `ActiveEngineWorkspace`.

## Selection and planning

`AUTOMATIC` admits planner-selected candidates, `USER_SELECTED` admits explicitly selected candidates, `HYBRID` admits either and `ALL` admits every otherwise eligible configured candidate. Profile families, inventory/scope eligibility, deterministic signature deduplication and request/mutation budgets are still enforced.

Planning returns exact candidate, eligible, deduplicated, scope-filtered, selection-filtered, budget-filtered, planned and estimated-request counts. Planning creates no transport and therefore remains zero-dispatch.

## Queue and execution

The queue supports `QUEUED`, `RUNNING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`, `SKIPPED` and `BLOCKED`, with start, pause, resume, stop, retry, skip, rerun and STOP ALL operations. Ordering is stable by deterministic priority and insertion sequence. Duplicate signatures are skipped. Dependencies must exist and complete; absent or terminal-failed dependencies block dependent work with a traceable reason.

The executor validates the full four-request set before transport. Scope, environment, consent, kill switch, request and mutation budgets, rate and concurrency controls remain mandatory. The local development policy automatically acknowledges only authorized, non-destructive LAB tests on loopback. Production, arbitrary external, unauthorized and destructive cases remain disabled.

429, 503, Retry-After, timeout and connection failure use bounded backoff/error paths. Operational failure creates no security Observation.

## Coverage and request efficiency

`ActiveCoverageTracker` records eligible/tested endpoints, contexts and resources; eligible/tested mutation categories; planned, deduplicated, filtered, executed, skipped, failed, blocked and cancelled tests. Test coverage is explicitly separate from vulnerability coverage.

The request-efficiency snapshot exposes candidate, deduplicated, scope-filtered, budget-filtered and executed test counts. It does not calculate or claim an optimization percentage.

## UI projection

The existing `AcraSuiteTab` now includes Test Plan, Test Queue, Test Detail, Execution, Differential Result, Evidence, Safety and Experiment views. Profile, Selection Mode, User Mode, budgets, concurrency, rate, coverage and STOP ALL controls update `ActiveEngineWorkspace` rather than a parallel UI model.

Active execution is unavailable in the default Burp construction because no authorized executor/target is injected there. This preserves the required fail-closed default. An explicitly configured local harness injects the existing workspace/executor, and the product suite verifies plan → queue → execution → result/coverage. Loading passive observations into an active plan from a real Burp session is a deferred adapter/runtime-validation workflow outside the local S4 software boundary.

## Research boundary

The research records support labelled TP/TN/FP/FN cases, independent ground truth, deterministic execution metadata and mathematically safe precision/recall/F1 calculation. Undefined metrics are represented as `N/A`. The broad `EXP-DIFF-001` and 100/1,000/10,000 performance campaigns remain deferred and are not claimed as results.
