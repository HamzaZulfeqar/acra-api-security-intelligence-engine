# OpenAPI and Traffic Correlation

Sprint 3 introduces a dependency-free importer for:

- OpenAPI 3.x JSON
- Swagger 2.0 JSON
- a conservative common YAML subset sufficient for simple path/method/parameter fixtures

The importer extracts operations, path/query/header parameters, security schemes, operation IDs, tags and response-code declarations where represented by the supported parser path.

## Correlation states

- DOCUMENTED_OBSERVED
- DOCUMENTED_UNOBSERVED
- UNDOCUMENTED_OBSERVED
- CONFLICTING
- UNKNOWN

## Known limitations

This is not a complete standards implementation. Complex `$ref` resolution, external references, composition, callbacks, links, discriminators, full YAML semantics and framework-specific vendor extensions remain future work. Unsupported constructs must not be silently treated as fully parsed.
