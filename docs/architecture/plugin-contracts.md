# Plugin Contracts

Sprint 1 defines contracts only. It does not ship active vulnerability plugins.

Interfaces:

- `Analyzer`
- `MutationProvider`
- `EvidenceProvider`
- `ApiParser`
- `WorkflowDetector`
- `Reporter`

The core contract uses `AnalysisContext` containing the transaction, URI model, authorization context, and Security Context Graph. Future Burp and protocol modules remain adapters/plugins and must not force Montoya-specific types into the core domain.

The `Analyzer` contract returns observations/evidence references. A later finding engine is responsible for evidence-verified vulnerability lifecycle transitions.
