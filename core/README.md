# acra-core

Standalone deterministic ACRA domain engine for Sprint 1 / v0.1.0.

## Build locally

Requires Java 21.

```bash
./scripts/build-core.sh
./scripts/test-core.sh
./scripts/security-core.sh
./scripts/perf-baseline.sh
```

No third-party runtime or test dependency is required in Sprint 1. This is a temporary verification approach while ADR-0007 remains OPEN.

## Scope

The core models HTTP, URI representations, identity/session/role, tenant, resource, action, authorization context, evidence, endpoint inventory, test-case state, ground-truth compatibility, entity resolution, plugin contracts, deterministic serialization/redaction, and the in-memory Security Context Graph.

It does not send network traffic and does not report vulnerabilities.
