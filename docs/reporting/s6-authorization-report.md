# Sprint 6 Authorization Report Contract

Version: s6-authorization-report-v1

The report is generated from an explicit S6AuthorizationProductSnapshot. It includes policy metadata, effective authorization, policy conflicts, coverage, FindingCandidate review records, authorization risk assessments, evidence references and explicit limitations.

FindingCandidate remains a review state. The report keeps confirmedFindingCount=0 and does not create a confirmed-vulnerability lifecycle state.

Current export formats:
- canonical JSON through DomainSerializer / UniversalRedactor
- deterministic Markdown
- SHA-256 digest for the JSON artifact

Raw bearer/session/API-key/password material is not intended to appear in normal report exports. SARIF and Burp Issue outputs remain outside this S6 slice.
