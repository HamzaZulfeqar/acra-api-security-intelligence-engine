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

- End-to-end clean Windows clone/start and GUI walkthrough.
- Any extension from guarded loopback LAB execution to authorized non-loopback targets requires a separately reviewed safety and authentication design.
- Independent unseen API accuracy and timed end-to-end measurements; the 16-case controlled A0–A7 fixture is insufficient.
- Versioned standalone distribution and release notes; repository administration tracked in issue #6.
