# Sprint 13 Phase 7 — Real Burp Desktop / Montoya Runtime Validation Protocol

**Protocol ID:** ACRA-S13-BURP-v1  
**Branch:** `s13-burp-montoya-runtime`  
**Phase 6B completion base:** `d35e2a54891357e680253e2cfc766c3e4bb6f88d`  
**Burp target:** Burp Suite Desktop 2026.7.3 JAR  
**Official SHA-256:** `c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069`  
**Montoya compile contract:** `net.portswigger.burp.extensions:montoya-api:2026.7`

## Objective

Validate ACRA inside the real Burp Desktop / Montoya runtime rather than a mocked Montoya contract.

The minimum Phase 7 evidence chain is:

1. build the shaded ACRA extension JAR;
2. download the pinned Burp Desktop JAR;
3. verify the official Burp SHA-256;
4. start the controlled secure localhost ACRA-Lab;
5. start real Burp Desktop in headless mode;
6. preload the actual ACRA extension JAR through Burp user configuration;
7. observe ACRA Montoya `initialize()`;
8. route controlled HTTP requests through Burp Proxy;
9. observe real Montoya request/response callbacks;
10. observe ACRA passive pipeline processing;
11. verify the probe contains no authorization headers, bearer tokens, cookies or bodies;
12. retain active execution disabled.

## Phase 7 runtime evidence probe

Phase 7 adds an opt-in probe enabled only by:

`-Dacra.phase7.probeFile=<path>`

The probe records only:
- extension initialization/unloading;
- Burp message ID;
- request method/host/port/path;
- response status/tool/path;
- ACRA transaction ID;
- reconstructed principal/tenant/resource/action/context status.

It deliberately excludes:
- Authorization headers;
- tokens;
- cookies;
- request bodies;
- response bodies.

When the probe is disabled, normal extension behavior remains unchanged.

For the automated localhost runtime lane only, probe mode sets ACRA passive collection to `ALL_TRAFFIC`.
Active execution remains disabled. This avoids conflating Phase 7's Montoya runtime test with interactive Burp Target-scope
configuration.

## Development execution attempt 1

GitHub Actions run `36178508712`.

Verified before Burp startup:
- Phase 6B evidence lock: PASS;
- ACRA Maven/shaded-extension build: PASS;
- Burp 2026.7.3 download: PASS;
- Burp official SHA-256: PASS;
- controlled secure localhost lab health: PASS.

The execution then stopped at Burp's first-run interactive EULA prompt:

`Do you accept the license agreement? (y/n)`

No EULA response was automated because accepting a legal agreement requires explicit user authorization.

Therefore these Phase 7 items remain **NOT MEASURED**:
- Montoya initialization inside Burp;
- ACRA suite-tab/runtime load;
- real Burp Proxy callbacks;
- passive pipeline processing;
- GT-INTEGRATION-001 context reconstruction through real Burp.

## Completion rule

Phase 7 must not be marked complete until an explicitly authorized run records:
- `INITIALIZED`;
- at least two Burp `REQUEST` callbacks;
- corresponding `RESPONSE` callbacks;
- at least two `PROCESSED` pipeline events;
- localhost-only traffic;
- no secret-bearing fields in the probe;
- successful ACRA build and Burp checksum gate.

If the reconstructed GT-INTEGRATION-001 context is incomplete, retain that outcome as measured evidence rather than tuning
against the evaluation request.

## Claim boundary

Even a successful Phase 7 establishes only real Burp Desktop / Montoya runtime operation against controlled localhost
traffic. It does not establish:
- production scanner accuracy;
- arbitrary Burp-version compatibility;
- external-target safety or effectiveness;
- automatic issue publication correctness;
- independent replication.

Phase 8 remains the explicitly authorized external-target validation gate.


## Authorized execution and final measured result

The user explicitly authorized acceptance of the PortSwigger Burp Suite Community Edition EULA for the controlled
localhost Phase 7 CI validation.

The first authorized launcher attempt (`36179467800`) still failed because the shell emitted a literal escaped newline
instead of the required Enter key sequence. This was an orchestration defect, not a Burp/ACRA result.

After correcting only that launcher input, development workflow `36179678105` succeeded:
- Phase 6B evidence lock: PASS;
- ACRA extension Maven build: SUCCESS;
- Burp 2026.7.3 official SHA-256: PASS;
- secure localhost lab health: PASS;
- real Montoya initialization: PASS;
- REQUEST callbacks: 2;
- RESPONSE callbacks: 2;
- PROCESSED pipeline events: 2;
- GT-INTEGRATION-001: PASS;
- secret-exclusion: PASS.

Development was frozen at `4bb7b952fb5cd0692efc6153d51b6885a7d38f9d`.

A post-freeze evaluation fixture and label set were then created. Final strengthened workflow `36180569483` succeeded
at `e24a91297bf33bfe18a47453e8c191432b496b20`.

Each of two independent real-Burp evaluation passes measured:
- INITIALIZED=1;
- REQUEST=2;
- RESPONSE=2;
- PROCESSED=2;
- expected context reconstruction=2/2;
- response tool source=PROXY;
- loopback-only traffic;
- no secret-bearing probe content.

The normalized semantic evaluation outputs were identical.

The strengthened final gate additionally verified:
- the Findings & Reproduction review surface is packaged and installed by `AcraSuiteTab`;
- the UI explicitly preserves candidate-vs-confirmed distinction;
- `S11BurpIssueAdapter` refuses drafts that are not publication-eligible;
- the extension initialization path does not automatically invoke issue materialization/publication;
- active execution remains disabled.

### Final Phase 7 decision

**COMPLETE — CONTROLLED REAL BURP/MONTOYA RUNTIME VALIDATION PASSED.**

Phase 8 remains the explicitly authorized external-target validation gate.
