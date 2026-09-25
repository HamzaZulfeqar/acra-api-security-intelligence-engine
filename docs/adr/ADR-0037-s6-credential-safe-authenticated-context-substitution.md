# ADR-0037 — Credential-Safe Authenticated-Context Substitution

Status: ACCEPTED FOR SPRINT 6

## Decision

ROLE_COMPARISON active tests use explicit in-memory request controls referenced by `contextRef`.
`Mutation` stores only non-secret role labels plus source/target context references. Raw bearer/session/API-key
material is not copied into mutation fields.

`AUTHENTICATED_CONTEXT_SUBSTITUTION` with `MutationLocation.CONTEXT` is resolved only from the current
`SecurityTest` baseline/positive/negative controls. No global secret store or second execution engine is added.

## Validation

Before execution, `RequestEquivalenceGuard` requires:
- same scheme/host/port/protocol
- same HTTP method
- same raw target
- same body
- same non-authentication headers
- same resource reference
- a real authentication-context difference
- source context reference equals the immutable baseline context
- target context reference resolves unambiguously to an explicit control

The normal environment, hard-scope, consent, kill-switch, budgets, concurrency and rate-limit guards still apply.

## Rationale

This allows authenticated role comparison without treating a role label as authorization proof and without
serializing raw credentials into Mutation metadata. Raw credentials may still exist transiently inside authorized
in-memory HttpRequest controls under the existing ADR-0026 boundary; normal serialization redacts them.
