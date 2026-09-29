# ACRA standalone integration candidate — draft release notes

**Status:** draft notes for the integration branch, not a published versioned release. The published v0.3.0 line remains the existing release. Do not describe this candidate as a production-ready active scanner.

## What this candidate adds

- A Burp-optional browser workbench from a clean clone, launched by `run-acra.bat` on Windows or `bash run-acra.sh` on Linux/macOS (Java 21 and Maven 3.9+ for source builds).
- Persistent projects, registered authorized HTTP(S) targets, scoped offline OpenAPI/Swagger/HAR/raw-HTTP import, inventory, security context, authorization projection, review-only candidates, coverage and deterministic JSON/Markdown reports.
- Guarded active validation for controlled loopback LAB targets and a narrow, authorized development/staging IPv4 read-only slice with transient Authorization values and Core safety gates. Other external targets remain import/review-only.
- A combined candidate ZIP with standalone and optional Burp JARs, launchers and SHA-256 manifest. The workflow verifies extracted JAR hashes.

## Verification on integration candidate `3923980`

The combined Java 21 build, standalone suites, clean Windows launcher, Linux and Windows headless Chromium workflows, frozen research regressions, security checks, candidate packaging and all GitHub workflows passed. The browser check covers project creation, malformed-target rejection, target registration, offline import, coverage, report generation and reload. CI screenshots are retained as artifacts. The offline import observation from an earlier candidate measured 100 and 1,000 routes in 184.165 ms and 395.493 ms on one Linux runner; these figures are not active scanner throughput or a performance guarantee.

## Known limitations and remaining release gates

- No human visual/usability sign-off from a clean Windows machine yet.
- The non-loopback IPv4 slice has controlled CI evidence only. Independent safety review, broader authentication support, DNS/hostname handling, operator-visible exact run planning and authorized real API acceptance are still open in `docs/architecture/standalone-external-execution-proposal.md`.
- Frozen controlled fixtures and four-framework local regression do not establish production accuracy. An unseen, independently labelled authorized API study must report false positives, false negatives, inconclusive cases and end-to-end timings.
- Repository `main` protection and Dependency Graph require owner administration and verification in issue #6. No standalone versioned release has been published.

Review these gates before assigning a version, marking the draft PR ready, merging, or publishing a release. See `docs/release/STANDALONE_INTEGRATION.md` for candidate startup and provenance.
