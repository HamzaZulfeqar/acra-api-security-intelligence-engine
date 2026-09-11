# Sprint 4 Active Testing Safety

## Non-negotiable checks

Before dispatch, the executor enforces project/authority/path/method scope, authorized environment, active consent, kill-switch state, immutable configuration, declared mutation equivalence, request and mutation budgets, concurrency and scoped rate limits. All four variants must pass as a set.

Automatic local consent is narrowly limited to authorized `LAB` targets on `localhost`, `127.0.0.1`, `::1` or `[::1]`, with non-destructive test and mutation safety. Out-of-scope, external, unauthorized and destructive cases receive disabled consent and still encounter the underlying guards.

## Dispatch invariant

- valid declared mutation: mutation transport may dispatch;
- invalid original value/location/type: zero mutation transport dispatch;
- contaminated request with more than one changed dimension: zero mutation transport dispatch.

## Operational protection

Backoff is bounded for 429, 503, Retry-After and transport failure. Cancellation and STOP ALL preserve queue state. Timeouts, connection failures and invalid targets create operational failures, not security Observations.

## Evidence protection

Raw bearer tokens, passwords, API keys, session secrets and cookie values must not enter serialized evidence or the Security Context Graph. Graph provenance is built from executor/evidence identifiers, not ordinary observation payload fields, and cross-project hydration is blocked.

S4 authorizes no arbitrary external discovery or scanning. Burp and authorized external validation are separate deferred phases.
