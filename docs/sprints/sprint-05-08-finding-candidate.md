# S5-08 finding candidate

Final status: **COMPLETE** — 2026-09-23.

Implemented `FindingCandidate`, `FindingCandidateState` and `AuthorizationFindingEvaluator`. Candidate evaluation consumes normalized verified context, BOLA/BFLA, typed tenant/workflow/property assessments and conservative correlation. States are CANDIDATE, REJECTED and INCONCLUSIVE.

A FindingCandidate is explicitly not a confirmed real-world vulnerability. Conflicting/insufficient context or correlation remains INCONCLUSIVE.
