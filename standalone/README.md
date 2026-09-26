# ACRA Standalone Security Workbench

Sprint 10 introduces a standalone localhost product host over the existing ACRA Core.

## Requirements
- Java 21
- Maven 3.9+

Burp Suite is **not required** to launch the standalone workbench.

## Windows
From a cloned repository:

```bat
run-acra.bat
```

The launcher builds ACRA Core + standalone, starts the local service and opens the default browser when desktop browsing is available.

## Linux / macOS

```bash
bash run-acra.sh
```

## Direct JAR

```bash
mvn --batch-mode --no-transfer-progress -pl standalone -am package
java -jar standalone/target/acra-standalone.jar
```

Default URL:

```text
http://127.0.0.1:8787/
```

If port 8787 is unavailable and no explicit port was requested, ACRA selects an available loopback port and prints the actual URL.

Useful options:

```text
--port=8787
--home=/path/to/acra-workspace
--no-browser
```

Default local workspace:

```text
~/.acra/
```

## Current verified standalone capability

### Phase 1 — Local application + target onboarding
- standalone executable application;
- loopback-only management server;
- automatic browser opening where available;
- persistent local assessment projects;
- persistent authorized HTTP(S) targets;
- URL, hostname/IP and port through a normal HTTP(S) base URL;
- explicit environment and testing-mode declaration;
- mandatory authorization reference;
- rejection of embedded URL credentials;
- ACRA Core runtime linkage;
- project/target browser GUI;
- CSRF and Host-header protections;
- CSP and response hardening;
- no Burp dependency for startup or target onboarding.

### Phase 2 — Import + API Inventory
The **API Inventory** workspace is now functional.

Supported offline imports:
- OpenAPI 3.x JSON;
- Swagger 2.0 JSON;
- the existing conservative OpenAPI/Swagger YAML subset;
- HAR files;
- one raw HTTP request per raw-request import.

Workflow:

```text
Create/Open Project
        ↓
Register Authorized Target
        ↓
API Inventory
        ↓
Select Target
        ↓
Choose OpenAPI / HAR / Raw HTTP
        ↓
Upload local file OR paste content
        ↓
Import & Normalize
        ↓
Canonical Endpoint Inventory
```

The inventory records:
- HTTP method;
- scheme / host / port;
- raw path;
- canonical route;
- OPENAPI / HAR / RAW_HTTP provenance;
- response statuses observed from HAR;
- observation count;
- documented-vs-observed state;
- first/last seen timestamps.

Concrete resource observations are normalized using existing ACRA URI intelligence. For example, a traffic path such as:

```text
/api/v1/users/42
```

can correlate with a declared route family such as:

```text
/api/v1/users/{user_id}
```

Imports are bound to the selected registered target. ACRA rejects imported endpoints outside that target's:
- scheme;
- host;
- port;
- base path.

Current import safety limits:
- decoded import content: 2 MiB maximum;
- HAR: 10,000 entries maximum;
- raw HTTP Host header must match the resolved selected target/request URI.

Importing evidence is **offline** and does not start a scan or send requests to the target.

## Current boundary

Registering a target or importing evidence does **not** scan or attack it.

The following standalone workspaces are not yet claimed as fully wired:
- Authorization;
- Object Access;
- Function Access;
- Property Access;
- Workflow;
- Routing;
- Evidence;
- Candidates;
- Coverage;
- Reports.

They will reuse the existing ACRA Core implementations rather than introducing a second scanner.

Raw imported files are also not yet promoted into the formal ACRA Evidence graph; Phase 2 currently persists the normalized derived inventory.

Active testing, when wired later, must continue through ACRA's existing authorization, scope, consent, request-budget, rate, concurrency and safety gates.
