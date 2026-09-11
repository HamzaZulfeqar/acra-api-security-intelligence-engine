# Sprint 4 Test Planner

`TestPlanner` extends the Sprint 3 inventory, route, context-coverage, risk-priority, AuthorizationMatrix and OpenAPI inputs. It does not recreate them.

The planner applies, in order: configured profile families, inventory/scope eligibility, selection mode, deterministic canonical-signature deduplication, mutation budget and request budget. Stable priority and test ID ordering make equal inputs reproducible. The resulting `TestPlan` fingerprint covers the ordered canonical test signatures.

`PlanningResult` adds exact accounting for candidates, eligible tests, deduplicated tests, scope and selection filters, budget filters, planned tests and estimated requests. `ActiveCoverageTracker` consumes that result for product coverage and request-efficiency reporting. No planning path receives a transport; dry run dispatch is always zero.
