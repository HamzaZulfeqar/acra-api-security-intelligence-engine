# ADR-0034: Deterministic Route Grammar and Equivalence

**Status:** ACCEPTED  
**Decision:** Represent route syntax and canonical family equivalence separately from runtime routing/authorization equivalence.

## Decision

Sprint 3 recognizes observed, brace-template, colon-template, angle/framework-template, literal wildcard, catch-all and regex-like route forms. Deterministic normalization may classify routes as syntactically equal, canonically equivalent, same family, different or unknown.

A canonical/family match is reconnaissance evidence only. It does not prove that reverse proxies, gateways, middleware and applications reach the same authorization boundary. Runtime route-boundary validation remains a later controlled experiment.
