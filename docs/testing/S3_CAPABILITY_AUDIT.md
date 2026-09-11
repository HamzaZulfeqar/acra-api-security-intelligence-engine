# Sprint 3 S2 Capability Audit

This audit was performed against the unpacked canonical `acra-sprint-02-v0.2.0-rc1.zip`, not from conversation summary alone.

| Capability | State before S3 | Evidence |
|---|---|---|
| raw HTTP request/response preservation | PASS | `HttpRequest`, `HttpResponse`, `MontoyaHttpMapper` |
| raw/decoded/normalized/canonical URI | PASS | `UriModel`, `DefaultUriExtractor` |
| identifier candidate detection | PASS | `IdentifierDetector` |
| identity/session correlation | PASS | `DefaultIdentityExtractor`, `SessionCorrelationStore` |
| tenant detection | PASS | `DefaultTenantExtractor` |
| resource and owner extraction | PASS | `DefaultResourceExtractor` |
| action classification | PASS | `DefaultActionExtractor` |
| endpoint-family fingerprinting | PASS | `Endpoint`, `EndpointInventory` |
| API inventory | PASS | `EndpointInventory`, `ApiInventoryService` |
| graph hydration | PASS | `SecurityContextEngine`, `TrafficIntelligencePipeline` |
| response normalization | PASS / INITIAL | `ResponseNormalizer` |
| OpenAPI import | MISSING | added in S3 |
| schema drift | MISSING | added in S3 |
| route grammar/equivalence | MISSING | added in S3 |
| semantic parameter/header classification | MISSING | added in S3 |
| context coverage | MISSING | added in S3 |
| dry-run test planning | MISSING | added in S3 |
