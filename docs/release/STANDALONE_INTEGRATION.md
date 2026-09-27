# Standalone integration candidate

This branch combines the published `v0.3.0` source with the standalone Sprint 10–12 research line. It is a review candidate, not a new release or a claim of external-target validation.

## Provenance

- Base: published `main` at `0106a93` (v0.3.0 Burp release and onboarding).
- Standalone line: `s12-standalone-a0-a7-research-evaluation` at `a56de9e`.
- Histories diverge at `ce81220` after Sprint 9. The merge has two parents so both source histories remain inspectable.
- The conflicting sprint records remain separate under `docs/sprints/standalone-*`; older release evidence is preserved under its existing paths.

## Integration rules

- Keep `0.3.0` and Apache-2.0 metadata from the published release; register the standalone module in the root Maven reactor.
- Keep the existing Burp authorization panels and add the optional standalone bridge tab.
- Keep the published Core serializer and issue-draft contract where they are semantically equivalent.
- Do not replace the published Sprint 10–12 research records with the separate standalone Sprint 10–12 records.
- Require a combined Java 21 reactor build, standalone suites and extracted application startup before promotion.

## Acceptance still open

- Visual GUI walkthrough on Windows. CI verifies a clean checkout/launcher, localhost health, target page and persisted registered target.
- Any extension from guarded loopback LAB execution to authorized non-loopback targets requires a separately reviewed safety and authentication design.
- Independent unseen API accuracy and timed end-to-end measurements; the 16-case controlled A0–A7 fixture is insufficient.
- Public versioned standalone release and release notes; repository administration tracked in issue #6.

The integration workflow creates a candidate ZIP containing the standalone and optional Burp JARs, Windows/Linux launchers, this guide, and a SHA-256 manifest. CI verifies the extracted JAR hashes. This is a review artifact, not a published versioned release.

## Candidate first run

With Java 21 and Maven 3.9+ installed, check out this integration branch and run `run-acra.bat` on Windows or `bash run-acra.sh` on Linux/macOS. The launcher builds the standalone JAR and opens the loopback browser UI where supported. The management URL is printed if the browser does not open.

Create a project, then register an authorized HTTP(S) base URL, or enter a hostname/IP with protocol and optional base path. This step records the assessment boundary and does not make a network request. Import an OpenAPI specification, HAR, or raw HTTP evidence within that target scope. Add the relevant principals, roles, tenants and authorization expectations before reviewing candidates, coverage and reports.

Authentication values for the controlled loopback LAB validation are entered transiently at execution time and must be scoped to the lab. The application does not persist these values. A registered external host is currently limited to offline import and review; it cannot be actively probed by the standalone executor.

The integration CI repeats the frozen Sprint 13 synthetic holdout and four-framework localhost evaluation against the combined source. Passing those historical tests checks for integration regressions. It does not create a new, independent dataset or establish scanner accuracy against external APIs.

The CI artifact also records elapsed time for offline OpenAPI imports of 100 and 1,000 unique routes and reopening their persisted inventory. These are runner-specific engineering observations, with no network calls and no production latency or throughput promise.
