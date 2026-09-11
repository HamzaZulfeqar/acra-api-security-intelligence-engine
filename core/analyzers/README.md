# Future analyzers

Vulnerability analyzers are intentionally **not implemented** through Sprint 2. The stable analyzer/plugin contracts live under `core/src/main/java/io/acra/core/plugin`.

Future BOLA, BFLA, tenant, routing and workflow analyzers must consume the core Security Context/evidence contracts rather than embedding Burp-specific logic.
