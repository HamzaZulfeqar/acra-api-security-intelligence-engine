# Universal Redaction Boundary

## In-memory boundary

Authorized adapters may provide raw request material to the in-memory domain model. Raw credential material is not a normal persistent identity attribute.

## Persistent/serialization boundary

`DomainSerializer`:

- redacts Authorization, Cookie, Set-Cookie, API-key and auth-token headers
- redacts cookie values
- redacts bearer/JWT-like text patterns
- redacts known password/token/key body fields
- serializes raw byte arrays as length + SHA-256 digest + `rawPersisted=false`
- preserves stable structural metadata and content hashes

## Limitations

This is a deterministic Sprint 1 boundary, not a complete DLP/PII detection system. Persistence encryption, workspace archive format, and dynamic field masking remain separate ADRs.
