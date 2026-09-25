# ACRA Burp Extension Adapter

Current stable release: `v0.3.0`.

The extension remains a thin adapter over `acra-core`. It observes HTTP traffic, maps it to ACRA transactions, runs passive context/reconnaissance intelligence and presents observation views. Core authorization logic must not be moved into Burp-specific classes.

## Runtime status

Real Burp Desktop / Montoya runtime validation is verified under the controlled Sprint 13 Phase 7 boundary.

Canonical strengthened evidence:
- Burp Suite Community Edition 2026.7.3 JAR;
- Montoya API compile contract 2026.7;
- GitHub Actions run `36180569483`;
- measured head `e24a91297bf33bfe18a47453e8c191432b496b20`;
- real extension initialization: PASS;
- real Burp Proxy request/response callbacks: PASS;
- ACRA passive-pipeline processing: PASS;
- two post-freeze unseen localhost contexts: 2/2 on each of two independent Burp runs;
- semantic repeatability: PASS;
- Findings & Reproduction review boundary: PASS;
- no automatic issue-publication wiring: PASS.

This is controlled headless localhost evidence, not arbitrary Burp-version compatibility or production/external-target
validation.

The Phase 7 evidence probe is opt-in and disabled in ordinary use. Active request execution remains disabled by default.

License: Apache-2.0. See the repository `LICENSE` and `NOTICE`.
