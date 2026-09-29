# Proposed external-target execution boundary

**Status:** design for review. A first, narrow implementation supports approved development/staging IPv4 literals for read-only four-request route-equivalence validation. The complete authorization, authentication and hostname design below is not implemented or independently reviewed. Production targets and hostname registrations remain import/review-only.

## Authorization contract

An active run must name an operator-approved project, exact target origin (scheme, canonical hostname or IP, and port), permitted base path, permitted HTTP methods, time window, maximum request count and rate, and an authorization reference. The UI must show the expanded request plan and require confirmation of that exact plan immediately before execution. An empty or ambiguous field fails closed. A target record or imported specification alone is not consent to send traffic.

The operator supplies independent test identities and expected ALLOW/DENY decisions for each comparison. Credentials are supplied at execution time through a secret-safe input channel, bound to an identity reference, held only for the run, and never included in project JSON, reports, logs, screenshots or exception text. Authentication schemes outside explicitly supported adapters fail closed; automatic login, token refresh, cookie harvesting, or privilege escalation must not be inferred from an imported specification. Missing positive and negative controls produce an inconclusive result rather than a confirmed finding.

## Dispatch boundary

The outbound transport must accept a canonical plan, not an arbitrary URL from evidence or a response. Reject userinfo, unsupported schemes, DNS names that resolve outside an approved address set, mixed public/private results, redirects, proxy environment inheritance, and protocol downgrade. Resolve immediately before dispatch and pin the resolved destination for that request. Revalidate the scheme, host, port, and normalized base path for every request, including mutations and retries. Do not follow redirects. Explicitly handle IP literals, IPv4/IPv6, DNS changes, IDN normalization, percent encoding, dot segments, and host headers. External scope must never weaken the existing management server's loopback-only binding.

Reuse the Core consent, environment, hard-scope, equivalence, budget, concurrency, rate-limit and kill-switch gates. Start with read-only methods and a single conservative mutation class. Require an operator-visible dry run showing exact method, URL, identity reference, request count and estimated duration. A budget is shared across all identities, controls, retries and mutations; reaching any budget or time limit stops dispatch. Never automatically enumerate unknown routes or perform state-changing actions in an external environment.

## Evidence and failure handling

Keep raw secrets out of serialized evidence. Record the authorization reference, approved plan digest, request/evidence hashes, time, identity references, safety-gate decisions, cancellation and error cause. Redact request and response secrets before persistence. A timeout, rate limit, redirect, auth failure, scope drift, unexpected response or missing control is inconclusive; it must not become an automatically confirmed vulnerability. Stop on scope changes or a kill-switch event and preserve a redacted audit trail.

## Acceptance before implementation can be enabled

1. Threat-model and code review of destination pinning, redirect/proxy behavior, path normalization and secret lifecycle.
2. Local adversarial fixtures for DNS rebinding, mixed DNS answers, redirects, path escapes, credential leakage, budget exhaustion, cancellation and failures between identity comparisons; assert zero out-of-scope dispatch.
3. A clean Windows GUI run against an explicitly authorized test service with independent test identities and expected decisions. Verify redaction, audit trace and stop behavior.
4. Unseen secure/vulnerable API cases with independent labels and disclosed false positives, false negatives, inconclusive counts and elapsed end-to-end times.

Promotion requires all four gates and independent review of the narrow implementation. Until then, this branch remains a draft candidate; unsupported external targets remain import/review-only.
