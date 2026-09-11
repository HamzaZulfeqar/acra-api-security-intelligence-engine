# Live Traffic Redaction and Credential Boundary

## Invariant

Raw credentials may be needed transiently for Burp traffic handling, but normal domain serialization, observations, logs, experiment evidence, fixtures, and repository artifacts must not persist them.

## Protected classes

- `Authorization`
- `Cookie` / `Set-Cookie`
- `API-Key` / `X-API-Key`
- access and refresh tokens
- passwords and client secrets
- private-key material
- high-entropy credential-shaped values detected by the core redactor

## Correlation

Credential correlation uses SHA-256 fingerprints. Fingerprints are identifiers for correlation, not proof of principal identity.

## Ordering

```text
Burp traffic
  -> transient mapper/domain representation
  -> sensitive-data redaction/fingerprinting boundary
  -> persistent/serializable evidence
```

## Sprint 2 verification

Automated security tests verify that bearer/API-key literals do not appear in serialized traffic evidence and that production source does not contain test credential literals. The lab contains synthetic test-only tokens under `lab/test-data`.
