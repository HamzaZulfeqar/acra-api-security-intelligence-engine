# ADR-0035: Reconnaissance Dry-Run Test Planning

**Status:** ACCEPTED  
**Decision:** Sprint 3 may recommend future authorization test families and estimate request counts, but must dispatch zero active requests.

## Decision

`ReconTestPlanner` may recommend future BOLA, BFLA, tenant, RBAC, property, workflow, routing, token-binding or version-drift families based on observed context. Endpoint priority is a test-ordering signal, not severity and not a vulnerability score.

Every Sprint 3 plan records `dispatchedRequests = 0` and safety markers declaring no network execution. Existing Sprint 2 active-executor zero budgets and kill-switch controls remain unchanged.
