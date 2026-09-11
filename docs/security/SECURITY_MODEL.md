# Security Model

## Authorized-use boundary

ACRA active testing remains restricted to explicitly authorized targets. Sprint 1 contains no network executor.

## Sprint 1 data boundary

Raw authorized traffic may exist transiently in the in-memory `HttpRequest`/`HttpResponse`. Persistent/domain export paths use `DomainSerializer` and `UniversalRedactor`:

- sensitive auth/cookie/API-key headers -> redacted
- cookie values -> redacted
- bearer/JWT-like text -> redacted
- known secret-bearing body fields -> redacted
- raw byte arrays -> length + SHA-256, raw bytes not persisted
- token/session correlation -> SHA-256 fingerprint

## Provenance integrity

Graph edges cannot be inserted without valid nodes and valid evidence IDs. Invalid confidence values and malformed required identifiers fail explicitly.

## Session continuity

The `SessionContext` model exists, but renewal/refresh behavior remains future work under ADR-0024.

## Dynamic data

Dynamic response masking remains future work under ADR-0025.

## AI boundary

ADR-0031 prohibits LLM-only vulnerability decisions. Sprint 1 contains no LLM decision path.
