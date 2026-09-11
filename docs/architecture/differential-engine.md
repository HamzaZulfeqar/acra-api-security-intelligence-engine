# Sprint 4 Differential Engine

The differential engine compares immutable baseline, expected-allow control, expected-deny control and one controlled experiment. It reuses Sprint 3 response semantic fingerprints and comparison modes.

Observed outcomes normalize to `ALLOW`, `DENY`, `AUTHENTICATION_REQUIRED`, `NOT_FOUND`, `PARTIAL`, `ERROR` or `UNKNOWN`. The expected-decision resolver keeps ACRA-Lab ground truth, configured policy, test definition, AuthorizationMatrix, validated metadata and inference sources distinct, with deterministic precedence and explicit same-rank conflict.

An absent or conflicting expected policy resolves to `UNKNOWN`; `MultiWayDifferentialAnalyzer` then returns `INCONCLUSIVE` regardless of raw response change. Supported final comparison classes are `NO_CHANGE`, `EXPECTED_CHANGE`, `UNEXPECTED_CHANGE`, `CONTEXTUAL_CHANGE` and `INCONCLUSIVE`. None is a vulnerability finding.
