# GitHub Workflows

- `sprint2-ci.yml` preserves the Sprint 2 official Montoya/Maven integration gate.
- `sprint3-ci.yml` pins Java 21 and Maven 3.9.16, resolves the official Montoya API dependency, packages the multi-module project, runs S1/S2/S3 regressions, security/architecture gates, local Sprint 3 lab/metric/performance checks, and uploads evidence.

A CI Maven package is build evidence. It is still not a substitute for loading the extension into a real Burp desktop runtime.
