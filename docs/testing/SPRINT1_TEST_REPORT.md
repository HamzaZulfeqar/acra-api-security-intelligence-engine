# Sprint 1 Test Report

**Environment:** OpenJDK 21.0.11  
**Verification command:** `./scripts/verify-sprint1.sh`

## Automated suite

37 tests pass across:

- HTTP model and validation
- URI parsing/representation
- identifier detection
- context extraction
- action mapping
- JWT principal provenance
- tenant conflict handling
- graph provenance and invalid-edge rejection
- deterministic serialization
- universal redaction
- confidence validation
- entity resolution
- ground-truth compatibility
- plugin contract compilation
- malformed JWT handling
- unknown-state behavior
- control-character serialization safety

## Security assertions

The suite verifies that a runtime-generated bearer secret is absent from serialized output, cookie/header credentials are redacted, raw bytes persist only as digests, malformed identity material fails to UNKNOWN, and graph edges cannot reference missing evidence.

No external target is contacted by this test suite.
