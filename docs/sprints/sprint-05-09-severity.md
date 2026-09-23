# S5-09 deterministic severity and risk

Final status: **COMPLETE** — 2026-09-23.

Implemented `AuthorizationImpactProfile`, `AuthorizationRiskAssessment`, `FindingSeverity`, `FindingConfidence` and `AuthorizationSeverityEvaluator`.

Severity is computed only from explicitly supplied impact facts. Confidence is independent. The internal score is ACRA prioritization and is explicitly not CVSS.
