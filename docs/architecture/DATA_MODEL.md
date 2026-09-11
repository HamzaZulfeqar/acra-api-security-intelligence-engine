# ACRA Core Data Model v0.1.0

Sprint 1 implements the technology-independent RB-0 concepts as Java 21 domain types. Persistence remains OPEN.

## HTTP

`HttpRequest`, `HttpResponse`, `HttpTransaction`, `HttpHeader`, `HttpMethod`, `HttpProtocol`.

Raw targets/bytes are retained in memory separately from parsed fields. Persistent serialization digests raw bytes and redacts credentials.

## URI

`UriModel`, `PathSegment`, `IdentifierCandidate` preserve raw, decoded, normalized and canonical route representations plus query/matrix parameters and normalization metadata.

## Security context

`Principal`, `SessionContext`, `Role`, `Tenant`, `Resource`, `Action`, `WorkflowState`, `AuthorizationContext`.

Missing values remain UNKNOWN/null rather than being invented.

## Evidence and graph

`Evidence`, `SecurityContextGraph`, `GraphNode`, `GraphEdge`, `GraphQuery`. Edges require existing evidence references.

## Extensibility and research compatibility

`Endpoint`, `ApiEndpointRecord`, `TestCase`, `TestState`, `GroundTruthContext`, entity-resolution records, and plugin contracts.

See `domain-model.md`, `security-context-graph.md`, `evidence-model.md`, and `plugin-contracts.md`.
