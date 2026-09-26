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

## Current verified Phase 1 capability
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

## Important boundary
Registering a target does **not** scan or attack it.

The deeper API Inventory, Authorization, Object Access, Function Access, Property Access, Workflow, Routing, Evidence, Candidates, Coverage and Reports areas are represented in the standalone product shell but are not yet claimed as wired into the standalone host.

Those areas will reuse the existing ACRA Core implementations rather than introducing a second scanner.

Active testing, when wired later, must continue through ACRA's existing authorization, scope, consent, request-budget, rate, concurrency and safety gates.
