# Security Context Graph v0.1.0

## Nodes

`PRINCIPAL`, `ROLE`, `TENANT`, `SESSION`, `TOKEN`, `RESOURCE`, `ENDPOINT`, `ACTION`, `WORKFLOW`, `CONFLICT`.

## Relations

`AUTHENTICATES`, `HAS_ROLE`, `BELONGS_TO`, `OWNS`, `ACCESSES`, `PERFORMS`, `USES`, `REACHES`, `HAS_STATE`, `IN_TENANT`, `CONFLICTS_WITH`.

## Provenance invariant

A `GraphEdge` cannot be inserted unless:

1. source node exists
2. target node exists
3. every referenced evidence ID exists
4. at least one evidence ID is supplied
5. confidence is within 0..1

Direct adjacency queries are used for immediate relationships so a `User -> Resource` lookup need not scan unrelated nodes.

## Conflict handling

Conflicting extraction results are not merged. The engine may emit a `CONFLICT` graph node labelled `CONFLICTING_EVIDENCE`, while the authoritative extraction result remains `CONFLICTING_EVIDENCE` with all candidate values/provenance retained.
