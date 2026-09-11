# ADR-0036: Conservative S5 evidence and serialization boundaries

Date: 2026-09-09. Status: accepted for the defensive continuation.

## Context

The canonical S5-04 source accepted incomplete evidence into existing assessments, treated separate properties in one execution as duplicates, and could raise confidence across conflicting/repeated records without evidence of independence. Generic string fields could retain recognized credential text.

## Decision

Extend the existing models/evaluators/correlator/serializer. Preserve public record signatures and the existing graph, evidence and replay architecture. Reject incomplete/redacted references from assessment eligibility. Preserve contradictions; no vote, recency or maximum-confidence selection may override them. Existing aggregate lacks policy/project and execution-independence metadata, so correlation cannot establish strong independent corroboration.

Copy/sanitize recognized credential patterns at existing S5 record construction, and use sensitive field names at generic serialization boundaries. Reject redaction markers as evidence of complete identity/provenance. Add deterministic offline regression tests and keep historical runtime evidence separate.

## Consequences

Some previously accepted inputs now yield INCONCLUSIVE or INSUFFICIENT. Correlation confidence cannot increase merely through replay. Serialized sensitive fields may contain redaction markers; safe identifiers remain intact. Arbitrary opaque secrets cannot be recognized in an unrelated identifier field. Legacy 32-bit assessment IDs and absent authenticated provenance/policy binding remain visible debt. This ADR does not add an active discovery/reproduction pipeline or complete S5.
