# ADR-0033: Sprint 3 OpenAPI Ingestion Baseline

**Status:** ACCEPTED  
**Decision:** Implement dependency-free OpenAPI 3.x and Swagger 2.0 JSON ingestion plus a conservative common YAML subset for Sprint 3 reconnaissance.

## Context

Sprint 3 requires declared-vs-observed API correlation, while the current execution environment cannot resolve new Maven dependencies. Treating a partial parser as standards-complete would violate ACRA's evidence discipline.

## Decision

- `acra-core` remains dependency-free for this Sprint 3 parser path.
- JSON import supports OpenAPI 3.x and Swagger 2.0 root documents, paths, HTTP operations, parameters, operation IDs, tags, security-scheme declarations and response-code keys used by the reconnaissance model.
- A deliberately limited YAML subset supports common path/method/parameter fixtures.
- Unsupported `$ref`, external references, composition and full YAML semantics are documented as limitations.
- Parsed specification data is evidence and does not override conflicting runtime observations.

## Consequences

Sprint 3 can perform deterministic specification correlation offline. Full standards compliance may later replace this importer behind the existing API contract without changing the reconnaissance domain model.
