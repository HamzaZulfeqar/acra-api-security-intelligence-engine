# ACRA Core Domain Model

**Version:** v0.1.0  
**Sprint:** 1

## Purpose

The domain engine answers one question: **what security entities and relationships does an observed HTTP transaction provide evidence for?** It does not classify vulnerabilities.

## Flow

```text
HttpTransaction
  -> UriModel
  -> Identity / Tenant / Resource extraction
  -> Action
  -> AuthorizationContext
  -> SecurityContextGraph
```

## Key invariants

- raw HTTP/URI information and normalized interpretations are distinct fields
- unknown values remain unknown
- inferred entities carry confidence and evidence
- conflict states remain explicit
- normal persistence never requires a raw bearer/session credential
- models are immutable or defensively copied where practical
- serialization is deterministic and versioned

## Implemented packages

- `domain/http`
- `domain/uri`
- `domain/identity`
- `domain/tenant`
- `domain/resource`
- `domain/authorization`
- `domain/workflow`
- `domain/evidence`
- `domain/endpoint`
- `domain/testing`
- `domain/groundtruth`
- `extraction`
- `graph`
- `serialization`
- `security`
- `resolution`
- `plugin`
- `engine`

## Deliberate non-features

No active requests, mutation execution, vulnerability classification, response-diff vulnerability logic, Burp API code, LLM security decision, OAuth exploitation, GraphQL exploitation, or workflow bypass logic exists in Sprint 1.
