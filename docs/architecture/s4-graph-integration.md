# Sprint 4 Observation/Evidence Graph Integration

## Scope

Sprint 4 uses the existing `io.acra.core.graph.SecurityContextGraph`, `GraphNode`, `GraphEdge`, `NodeType` and `RelationType` types. `io.acra.core.active.graph.ObservationGraphIntegrator` is an adapter; it is not a second graph implementation and does not create vulnerability findings.

The integration is optional at `TestExecutor` construction. The legacy constructor remains compatible and performs no graph hydration. When a project-bound graph is supplied, a completed execution automatically invokes the adapter after the immutable Observation and its 11-stage evidence chain have been created.

## Source and mapping

The source is a completed `TestExecutionResult` whose Observation contains baseline, expected-allow control, expected-deny control, controlled mutation, semantic differential, immutable source/target context and an execution fingerprint.

| S4 fact | Existing graph representation |
|---|---|
| principal | `PRINCIPAL` node |
| role | `ROLE` node plus `HAS_ROLE` |
| tenant | `TENANT` node plus `BELONGS_TO` / `IN_TENANT` |
| resource | `RESOURCE` node |
| configured owner | `PRINCIPAL` node plus `OWNS` |
| action | `ACTION` node plus `PERFORMS` |
| endpoint/route | existing endpoint ID as an `ENDPOINT` node plus `REACHES` |
| observed access | `ACCESSES` with `EXACT_OBSERVED` confidence |
| configured metadata | existing relationship vocabulary with `EXPLICIT_METADATA` confidence |
| configured/observed disagreement | `CONFLICT` node and `CONFLICTS_WITH` edges |

No `vulnerable`, `confirmed finding` or equivalent graph relationship is produced.

## Evidence provenance

Before hydration, the adapter independently verifies project identity, completed execution state, test/observation/execution identity, configuration/environment/response fingerprints, ordered evidence-chain identity and every request/response/differential/observation object fingerprint.

Each graph evidence record contains only safe provenance metadata:

- execution ID in the existing evidence `requestId` field;
- test ID;
- observation ID;
- originating S4 evidence-chain ID;
- originating object ID and fingerprint;
- evidence stage in `location`.

Graph edges reference these graph evidence IDs. Raw requests, credentials, bearer tokens, cookies, passwords, API keys and session secrets are not copied into graph nodes or edges. Focused and live serialization tests enforce this boundary.

## Observed, configured and conflicting context

Observed request/response relationships use `EXACT_OBSERVED`. Relationships derived from the explicit `SecurityTest` resource/owner/tenant configuration use `EXPLICIT_METADATA`. If an allowed response identifies a different resource, owner or tenant, both configured and observed nodes/relationships remain present and a deterministic execution-scoped conflict node connects them. The hydration result is `CONFLICTING_CONTEXT`, not a finding.

Existing nodes with the same ID/type/label are reused so prior attributes are preserved. A conflicting identity is not overwritten and yields `BLOCKED`.

## Incomplete context

Hydration requires known identity, role, tenant, resource, owner, resource tenant, action, endpoint/route and non-ambiguous expected policy. Unknown identity, tenant, resource, owner or policy yields `INCONCLUSIVE`. No graph nodes, edges or graph evidence are added.

## Atomicity and duplication

The adapter builds a private `SecurityContextGraph` delta. It preflights every evidence, node and edge identity against the destination before calling the existing `mergeFrom`. A preflight conflict returns `BLOCKED`; the destination graph remains byte-for-byte equivalent under deterministic serialization. This is the smallest compatible atomicity boundary for the existing in-memory graph and does not introduce a transaction framework.

Graph evidence and edge identities are deterministic for an identical Observation, so duplicate hydration is idempotent. A replay produces new execution, observation and evidence identities, adds new provenance relationships and preserves historical evidence while reusing the stable context nodes.

## Isolation and limitations

The adapter is bound to one project ID and rejects a test for another project. It cannot use ordinary Observation contents to forge evidence because the complete evidence chain and object fingerprints are verified before any graph mutation.

The graph and active evidence store remain in-memory and process-local. Persistence and cross-process uniqueness are not claimed. The live acceptance is controlled localhost ACRA-Lab evidence only; Burp and authorized external validation remain separate and unverified.
