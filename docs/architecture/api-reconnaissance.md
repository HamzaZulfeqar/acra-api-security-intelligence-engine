# API Reconnaissance Architecture

Sprint 3 adds a deterministic reconnaissance layer between passive Security Context reconstruction and future active authorization testing.

```text
HttpTransaction
  -> SecurityContextSnapshot
  -> ApiReconnaissanceEngine
       -> SemanticIdentifierClassifier
       -> ParameterClassifier
       -> HeaderClassifier
       -> ApiVersionDiscovery
       -> RouteTemplateEngine
       -> OpenAPI correlation
       -> ResponseSemanticAnalyzer
       -> ContextCoverage
       -> EndpointRiskPrioritizer
       -> ReconTestPlanner
  -> ApiReconnaissanceResult
```

`ApiReconnaissanceResult` is an observation/planning artifact. It does not contain a vulnerability verdict.

## Invariants

1. Raw URI/request data remains in Sprint 1/S2 models and is not replaced by reconnaissance normalization.
2. Unknown semantic classifications remain UNKNOWN.
3. Priority means testing value, not vulnerability severity.
4. Dry-run planning always dispatches zero network requests in Sprint 3.
5. OpenAPI declarations are evidence, not proof of runtime authorization behavior.
6. Burp-specific types remain outside `acra-core`.
