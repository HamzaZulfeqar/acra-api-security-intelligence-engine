# ACRA Standalone Security Workbench

ACRA Sprint 10 provides a standalone localhost product host over the existing ACRA Core.

Burp Suite is **optional**. A fresh clone can run ACRA, create projects, register authorized targets, import API evidence, configure authorization context, review evidence/candidates/coverage/reports, and run the guarded loopback LAB validation path without loading the Burp extension.

## Requirements

Source-clone startup:
- Java 21+
- Maven 3.9+

Prebuilt distribution:
- Java 21+

## Source clone — Windows

```bat
run-acra.bat
```

After a successful local build, skip Maven on later starts:

```bat
run-acra.bat --no-build
```

## Source clone — Linux/macOS

```bash
bash run-acra.sh
```

After a successful local build:

```bash
bash run-acra.sh --no-build
```

Default management URL:

```text
http://127.0.0.1:8787/
```

Useful application arguments:

```text
--port=8787
--home=/path/to/acra-workspace
--no-browser
```

If the default port is occupied and no explicit port was requested, ACRA selects an available loopback port and prints the actual URL.

Default workspace:

```text
~/.acra/
```

## Verified standalone workflow

```text
Create Project
    ↓
Register Authorized Target
    ↓
Import OpenAPI / Swagger / HAR / Raw HTTP
    ↓
Canonical API Inventory
    ↓
Security Context
    ├── Principals
    ├── Roles
    ├── Tenants
    ├── Resources / Ownership
    └── Expected Authorization
    ↓
ACRA Core Authorization Projection
    ↓
Evidence + Differential Review
    ↓
Candidates + Coverage + Reports
    ↓
Optional Controlled Localhost Validation
```

### Phase 1 — Standalone foundation
Verified:
- executable standalone JAR;
- loopback-only management server;
- browser workbench;
- persistent projects and authorized targets;
- CSRF / Host-header / CSP hardening;
- Burp-independent startup.

### Phase 2 — Import + API Inventory
Verified offline imports:
- OpenAPI 3.x;
- Swagger 2.0;
- conservative existing YAML subset;
- HAR;
- raw HTTP request.

Imports are bound to the selected registered target. Out-of-scope scheme/host/port/base-path evidence is rejected.

Concrete traffic paths are canonicalized through ACRA URI intelligence so observed and documented routes can correlate.

### Phase 3 — Security Context
Verified persistent project context:
- principals;
- authentication type metadata without storing credentials;
- roles;
- tenants;
- resources and owners;
- expected authorization matrix.

Invalid cross-references and secret-bearing context metadata fail closed.

### Phase 4 — ACRA Core projection
Standalone context is projected into the existing Core authorization policy model.

The standalone host reuses:
- EffectiveAuthorizationResolver;
- BOLA/BFLA evaluators;
- S6 authorization workspace;
- S7 workflow workspace;
- S8 routing workspace;
- S9 property workspace.

Analyzer logic is not duplicated in browser JavaScript.

### Phase 5 — Evidence + Differential Review
Verified:
- project-isolated evidence archive;
- original input digest;
- persisted redacted representation;
- HTTP evidence samples;
- HTTP differential comparison;
- authorization-context differential comparison;
- secret-safe evidence viewer.

### Phase 6 — Candidates + Coverage + Reports
Verified:
- review-only candidates;
- separate analyst review state;
- tested / untested / partial / inconclusive coverage accounting;
- deterministic JSON and Markdown reports;
- report SHA-256;
- confirmed finding count remains zero unless separately established by product logic.

### Phase 7 — Controlled Active Validation
The Active Validation workspace is deliberately narrow.

Requirements:
- target host must be loopback;
- target environment must be `LAB`;
- testing mode must be `CONTROLLED_LAB`;
- expectation action must be `READ`;
- expected decision must be explicit ALLOW or DENY;
- inventory method must be GET, HEAD, or OPTIONS;
- operator confirmation is required.

Current active mutation:
- one trailing-slash route-equivalence representation.

The differential uses:
- tested authorization context;
- independent known-ALLOW positive-control context;
- anonymous negative control;
- mutated tested context.

Both authorization values are transient. They are not persisted or returned by the standalone API.

Execution continues through the existing ACRA Core:
- hard scope;
- environment guard;
- consent guard;
- request-equivalence guard;
- request budgets;
- concurrency controls;
- rate limits;
- kill switch;
- TestExecutor;
- Core evidence chain.

This is **not** an unrestricted network scanner.

### Phase 8 — Optional Burp Bridge
The prebuilt distribution may include the optional:

```text
acra-burp-extension.jar
```

In Burp's ACRA tab, use **Standalone Bridge** to:
1. keep the standalone URL on loopback;
2. enter the project ID and target ID shown on the standalone Targets page;
3. select a passively observed Burp transaction;
4. Probe Standalone;
5. explicitly Send Selected Transaction.

The bridge:
- never forwards automatically;
- refuses non-loopback standalone management URLs;
- performs the health/CSRF handshake;
- sends one RAW_HTTP import;
- leaves target-scope validation and evidence redaction to standalone ACRA.

Real Burp desktop load/visual validation remains a separate **UNVERIFIED / DEFERRED** lane. The bridge is verified by official-Montoya compilation and headless contract tests.

## Prebuilt distribution

Sprint 10 packaging produces:

```text
acra-sprint10-bundle.zip
└── acra-sprint10/
    ├── acra-standalone.jar
    ├── acra-burp-extension.jar
    ├── acra-standalone.bat
    ├── acra-standalone.sh
    ├── README.txt
    └── SHA256SUMS.txt
```

The closure workflow verifies the included JAR hashes after clean extraction and starts the extracted standalone JAR.

## Sprint 10 closure

Sprint 10 is **SOFTWARE COMPLETE** at the canonical source checkpoint:

```text
closure run: 36213547092
source commit: c94ddc6cf32175694bb0fbffe4c4fc7ce24050c2
source ZIP SHA-256:
296fcf420919c172c338988e320a06007aaa4640b90d2077a341759690fd5c0f

entries: 951
unsafe paths: 0
duplicate entries: 0
clean extraction: PASS
per-file SHA-256 equality: PASS
```

Canonical prebuilt distribution SHA-256:

```text
0c6929384eb3110f018a57b45fb09729957e6b1f90e4ac8d9b3352bd12920f15
```

Sprint 11 is **not started** by this closure.
