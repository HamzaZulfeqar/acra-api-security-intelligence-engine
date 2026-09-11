# Live Traffic Intelligence

## Transaction flow

```text
Montoya request event
       |
       v
ScopeController
       |
       v
pending request correlation by Burp message ID
       |
Montoya response event
       |
       v
TrafficCollector
       |
       +--> ACRA-TX-NNNNNN
       +--> timing/source metadata
       v
HttpTransaction
       |
       v
TrafficIntelligencePipeline
       |
       +--> URI / identifier extraction
       +--> authentication fingerprinting
       +--> identity / tenant / resource / action extraction
       +--> AuthorizationContext
       +--> evidence-bound graph update
       +--> endpoint-family inventory
       +--> evidence timeline
       v
TrafficObservation
```

## Preservation

Raw request target/path/body/header data is retained in the transient core HTTP model. Decoded/normalized/canonical URI representations remain separate. Persisted or exported representations must pass the redaction boundary.

## Endpoint fingerprinting

Observed identifier-bearing paths are canonicalized into endpoint families without overwriting raw paths. For example, repeated observations of `/api/v1/documents/1001` and `/api/v1/documents/1002` may aggregate as `GET /api/v1/documents/{document_id}`.

## Authentication fingerprinting

Passive classifications include `NONE`, `BEARER`, `JWT`, `SESSION_COOKIE`, `API_KEY`, `BASIC`, `CUSTOM`, and `UNKNOWN`. Classification is not authentication validation.

## Observation invariant

Sprint 2 produces observations and evidence. It cannot emit confirmed BOLA, BFLA, routing, tenant-isolation, or workflow vulnerabilities.


## Resource ownership evidence

Explicit `owner_id` / owner-principal metadata observed in request/response structures is retained as separate evidence. When present, the graph can add an evidence-bound `PRINCIPAL --OWNS--> RESOURCE` relationship. This does not mean the currently authenticated principal is automatically assumed to be the owner.
