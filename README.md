# ACRA — API Access Control & Routing Auditor

**Published stable release:** `v0.3.0`  
**Current development direction:** standalone localhost GUI  
**License:** Apache-2.0  
**Runtime:** Java 21  
**Burp integration:** optional adapter

ACRA is being developed as a standalone local API-security analysis platform. The primary product path is:

```text
git clone
    ↓
one start command
    ↓
http://127.0.0.1:8765
    ↓
authorized target / imported API data
    ↓
ACRA Core analysis
    ↓
contexts + endpoints + evidence + findings/review
```

**Burp is not required to start or use the standalone ACRA GUI.**

The published `v0.3.0` tag predates this standalone work and remains the frozen stable Burp-centric release. The
standalone localhost product path is post-`v0.3.0` development and will require a later release promotion.

## Standalone Quick Start — current development branch

### 1. Clone

```bash
git clone https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine.git
cd acra-api-security-intelligence-engine
```

Until Sprint 17 merges to `main`, use:

```bash
git checkout s17-standalone-localhost-gui
```

### 2. Start ACRA

Windows PowerShell:

```powershell
.\acra.ps1
```

Linux/macOS:

```bash
./acra.sh
```

The launcher builds the standalone module, starts the local server, and opens the browser when desktop browser launching
is available.

Default GUI:

```text
http://127.0.0.1:8765
```

For headless/manual-browser use:

```bash
./acra.sh --no-browser
```

Custom port:

```bash
./acra.sh --port=9000
```

### 3. Analyze without Burp

The current Sprint 17 Phase 1 GUI accepts an exact HTTP(S) URL for a system you own or are explicitly authorized to test.

It performs one bounded read-only GET, converts the real request/response exchange into ACRA Core's `HttpTransaction`,
and automatically runs `SecurityContextEngine`.

The GUI returns structured:
- endpoint;
- response status;
- principal when evidence resolves it;
- role when evidence resolves it;
- tenant when evidence resolves it;
- resource/owner when evidence resolves it;
- action;
- context status;
- evidence count;
- analysis history.

No Burp runtime, Montoya configuration, or Burp target-scope configuration is involved in this standalone path.

### Current Sprint 17 safety boundary

Phase 1 deliberately does **not** crawl, brute-force, enumerate credentials, mutate state, or automatically confirm
vulnerabilities.

It performs one explicitly authorized read-only GET per submitted URL.

The next standalone phases add:
1. OpenAPI/Swagger import;
2. HAR import;
3. multi-request/session correlation;
4. policy/governance/finding workspaces;
5. evidence and report downloads;
6. local persistence;
7. optional Burp connector feeding the same engine/UI;
8. packaged no-Maven distribution.

## Requirements

- Java 21
- Maven
- Git

Build only the standalone application:

```bash
mvn --batch-mode --no-transfer-progress clean package -pl app/standalone -am
```

Standalone JAR:

```text
app/standalone/target/acra-standalone-0.3.0.jar
```

Run directly:

```bash
java -jar app/standalone/target/acra-standalone-0.3.0.jar
```

## What ACRA Core analyzes

ACRA's framework-neutral core includes security-context and authorization-oriented machinery for:

- identity and role evidence;
- tenant boundaries;
- resource and ownership context;
- action semantics;
- endpoint modeling;
- object-level authorization;
- role/function authorization;
- workflow/state authorization;
- routing equivalence;
- property-level authorization;
- batch authorization;
- indirect-reference authorization;
- evidence correlation;
- governed uncertainty;
- review-oriented candidate findings and reproduction artifacts.

The standalone application is progressively exposing these capabilities through one local GUI.

## Optional Burp integration

The Burp extension remains available as an **optional traffic/analyst adapter**.

Build it with:

```bash
mvn --batch-mode --no-transfer-progress clean verify -pl extension/burp-extension -am
```

JAR:

```text
extension/burp-extension/target/acra-burp-extension-0.3.0.jar
```

Burp is useful when you intentionally want Proxy/Montoya traffic ingestion, but it is no longer the target primary
product interface.

## Repository layout

```text
core/                     Framework-neutral ACRA analysis engine
app/standalone/           Standalone localhost web GUI/runtime
extension/burp-extension/ Optional Burp/Montoya adapter
lab/                      Controlled research fixtures
scripts/                  Verification and automation
docs/                      Architecture, research and sprint records
.github/workflows/         CI/security/research gates
```

## Sprint 17 verification

Dedicated workflow:

`Sprint 17 Standalone Localhost GUI`

Canonical Phase 1 run:

`36204523161 — SUCCESS`

It verifies:
- standalone shaded JAR builds;
- ACRA Core is packaged into the standalone runtime;
- localhost GUI status endpoint starts;
- `burpRequired=false`;
- a synthetic localhost API is queried directly;
- the response is analyzed by ACRA Core;
- action/context/evidence output is returned;
- history is populated;
- authorization acknowledgement is enforced;
- Burp independence is explicit.

Run locally:

```bash
bash scripts/verify-sprint17-standalone.sh
```

## Published v0.3.0

The existing stable release remains available at:

https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0

That tag is immutable historical release evidence and does not yet include the Sprint 17 standalone GUI.

## Research / claim boundary

Sprint 13 research remains frozen.

External-target validation was **NOT PERFORMED** before that freeze. Therefore neither `v0.3.0` nor this standalone
development branch claims:
- production scanner accuracy;
- production safety;
- arbitrary-target effectiveness;
- independent real-world validation.

Candidate evidence remains distinct from a confirmed vulnerability.

## License

Apache License, Version 2.0.

SPDX: `Apache-2.0`

See `LICENSE` and `NOTICE`.

## Maintainer

Hamza Zulfiqar  
GitHub: `HamzaZulfeqar`
