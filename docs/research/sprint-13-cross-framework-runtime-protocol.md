# Sprint 13 Phase 6B — Actual Local Framework Runtime Validation Protocol

**Protocol ID:** ACRA-S13-XRUNTIME-v1  
**Branch:** `s13-cross-framework-runtime`  
**Phase 6A completion base:** `84fff082a63c62d847d8b9df4bb17421d7d4bdb2`  
**Phase 6A normalization freeze:** `4011b9c05b99b14da66733aea47de43256060990`  
**Phase 6B development freeze:** `683933836d2c2fa5ec155bc8e224be87e6958e95`  
**Successful development workflow:** `36175284755`.

## Research question

Does the frozen Phase 6A normalization + Phase 5 governance stack preserve expected authorization dimensions and governed
dispositions when observations come from real localhost FastAPI, Flask, Express and Spring Boot runtime processes rather
than pre-authored JSON snapshots?

## Development runtime evidence

CI launched four real localhost applications:

- FastAPI / Uvicorn on 127.0.0.1:18201;
- Flask on 127.0.0.1:18202;
- Express / Node.js on 127.0.0.1:18203;
- Spring Boot / Java 21 on 127.0.0.1:18204.

Each service exposed eight controlled authorization surfaces:
- object;
- tenant;
- RBAC;
- workflow;
- routing;
- property;
- batch;
- indirect reference.

The driver issued 64 real HTTP requests: 16 per framework.

Initial development exposed a deliberate fixture ambiguity: `/routing/audit` produced strong RBAC evidence. Before
freeze, the runtime fixture was corrected to a routing-specific `/routing/equivalent` surface without changing the
frozen normalizer, dimension inference, policy engine or governance engine.

Frozen development result:
- live HTTP 200 responses: 64/64;
- inferred dimension: 64/64;
- governed disposition: 64/64;
- each framework: 16/16 HTTP, 16/16 dimension, 16/16 disposition;
- blind-label gate: PASS;
- repeated live execution: byte-identical prediction/evaluation artifacts;
- Maven ACRA product build: SUCCESS.

## Untouched runtime evaluation rule

After development freeze, evaluation must use separate runtime applications and different:
- route/resource vocabulary;
- actor names;
- tenant/organization names;
- role names;
- workflow action/state vocabulary;
- property names;
- policy IDs and versions;
- localhost ports.

The development runtime servers, development corpus, development labels, development policy registry, development
runner/verifier, Phase 6A normalizer and upstream reasoning/governance stack may not change after evaluation is observed.

## Metrics

Report:
- live HTTP success count;
- dimension accuracy;
- governed-disposition accuracy;
- per-framework live HTTP/dimension/disposition results;
- two-run repeatability;
- label isolation.

## Claim boundary

A successful Phase 6B establishes only controlled localhost runtime compatibility for the exact framework/runtime
versions exercised by CI. It does not establish:
- arbitrary framework/version compatibility;
- production deployment behavior;
- Burp desktop extension runtime behavior;
- internet/external-target safety or effectiveness;
- real-world vulnerability accuracy.

Real Burp desktop validation and authorized external-target validation remain separate later gates.
