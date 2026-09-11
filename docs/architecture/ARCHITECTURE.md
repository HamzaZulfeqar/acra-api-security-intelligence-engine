# ACRA Architecture

## Four-layer research architecture

1. API Understanding
2. Security Context
3. Controlled Differential Testing
4. Evidence Correlation

## v0.1.0 implemented slice

```text
Authorized HTTP Transaction
        |
        v
  URI / Request Model
        |
  +-----+------+------+
  |            |      |
Identity     Tenant  Resource
  |            |      |
  +------------+------+
               |
             Action
               |
               v
   Authorization Context
               |
               v
  Security Context Graph
               |
       Evidence provenance
```

Controlled differential execution, response-semantic vulnerability analysis, evidence-correlation findings, Burp reporting, lab execution, and research experiments remain later layers.

## Constraints

- `acra-core` has no Burp/Montoya dependency.
- UNKNOWN remains UNKNOWN without evidence.
- conflicting evidence is preserved.
- graph relationships require provenance.
- status codes are not vulnerability conclusions.
- Sprint 1 sends no active network traffic.
- no LLM output is authoritative for vulnerability classification.


## v0.2.0-rc1 implemented slice

```text
Burp Montoya event
        |
        v
ScopeController / TrafficCollector
        |
        v
HttpTransaction
        |
        v
TrafficIntelligencePipeline
   +----+----+----+----+
   |         |         |
Context   Inventory  Timeline
   |         |         |
   +---------+---------+
             |
             v
Security Context Graph
             |
             v
Observation / Evidence
```

Sprint 2 maintains the invariant that observation is not finding. Active execution and scanner registration are boundaries only and remain disabled. The full controlled differential testing and evidence-correlation finding layers remain future sprints.
