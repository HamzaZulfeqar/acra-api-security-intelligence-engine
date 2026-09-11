# Sprint 4 Executor

`TestExecutor` orchestrates the existing immutable SecurityTest, four request variants, safety validator, hierarchical budgets, scoped rate limiting, concurrency leases, bounded backoff, transport, response capture, expected-decision resolver, semantic differential, Observation, evidence chain and optional existing-graph hydrator.

Every request set is fully constructed and checked by `RequestEquivalenceGuard` before any dispatch. Valid declared mutation dispatch is allowed; invalid or contaminated mutation dispatch is zero. `LocalhostHttpTransport` is the only concrete network client in the active core and accepts only its construction-bound, authorized LAB loopback authority with redirects disabled.

Timeout, connection, target and retry exhaustion failures are operational results and never create an Observation. STOP ALL engages the kill switch, cancels queued/running work and remains audited. Credentials and response cookies are redacted before persistable evidence is created.
