# Core API Contracts v0.1.0

Sprint 1 contracts are Java interfaces and immutable domain objects. They are not network APIs.

## Ingress extraction

- `UriExtractor.extract(HttpTransaction) -> UriModel`
- `IdentityExtractor.extract(HttpTransaction) -> IdentityExtraction`
- `TenantExtractor.extract(HttpTransaction, UriModel) -> ExtractionResult<Tenant>`
- `ResourceExtractor.extract(HttpTransaction, UriModel, tenantResult) -> ExtractionResult<Resource>`
- `ActionExtractor.extract(HttpTransaction, UriModel) -> Action`

## Core orchestration

`SecurityContextEngine.analyze(HttpTransaction) -> SecurityContextSnapshot`

The snapshot contains URI, identity, tenant, resource, action, endpoint, authorization context, evidence, and graph.

## Graph

- register evidence
- register nodes
- register evidence-bound edges
- direct outgoing-edge queries
- direct relationship target queries

## Plugins

- `Analyzer`
- `MutationProvider`
- `EvidenceProvider`
- `ApiParser`
- `WorkflowDetector`
- `Reporter`

These contracts do not authorize network execution. A future adapter/executor must enforce target safety separately.

## Serialization

`DomainSerializer.serialize(Object)` emits canonical JSON schema version `1.0` with redaction applied.

## Burp boundary

No Montoya type is accepted or returned by any v0.1.0 core interface.
