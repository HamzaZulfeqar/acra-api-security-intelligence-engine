# Reconnaissance-to-Test Dry-Run Planning

`ReconTestPlanner` converts observed endpoint/context evidence into candidate future test families.

Potential families include BOLA, BFLA, tenant isolation, RBAC, property authorization, workflow, routing, token binding and version drift.

Sprint 3 invariant:

```text
estimatedRequests >= 0
dispatchedRequests = 0
NO_NETWORK_REQUESTS
ACTIVE_EXECUTION_REMAINS_DISABLED
```

The planner is advisory. It cannot emit a finding and cannot invoke Montoya request execution.
