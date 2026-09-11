# S5-03 Function-Level Authorization Reasoning

S5-03 adds a deterministic BFLA assessment foundation. It consumes the existing AuthorizationContext and does not create a new authorization graph.

## Boundary

BFLA assessment evaluates whether an observed function/action authorization outcome is consistent with expected authorization decisions.

It does not create confirmed vulnerability findings.

## Rules

- ALLOW expected and ALLOW observed: NO_VIOLATION
- DENY expected and DENY observed: NO_VIOLATION
- DENY expected and ALLOW observed with complete context: BFLA_CANDIDATE
- Unknown or incomplete context: INCONCLUSIVE

BOLA and BFLA remain separate reasoning paths.
