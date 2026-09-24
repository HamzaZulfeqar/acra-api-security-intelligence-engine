## 2026-09-25 — Sprint 11 software completion

- Added evidence-backed finding lifecycle: NEEDS_REVIEW, VALIDATED, CONFIRMED, FALSE_POSITIVE and ACCEPTED_RISK.
- Added project-isolated finding review workspace and immutable audit history.
- Added minimized deterministic finding reproduction packages.
- Added canonical JSON + SHA-256 and SARIF 2.1.0 reproduction exports.
- Added Burp Issue draft generation and Montoya AuditIssue materialization adapter without automatic SiteMap publication.
- Added read-only Findings & Reproduction UI.
- Added GT-S11-AUTHORIZATION-RESEARCH with 16 controlled cases across 8 authorization dimensions.
- Added security hardening and bounded engineering observation suites.
- Added deterministic Sprint 11 checkpoint packaging and final closure workflow.
- Final executable closure run `36064082001`: PASS.
- EXP-A0 through EXP-A7 remain NOT_RUN; Sprint 11 research metrics remain NOT_MEASURED.
- Real Burp desktop publication/runtime remains UNVERIFIED / DEFERRED.

## 2026-09-23 — Sprint 5 software completion

- Added authoritative Authorization Context normalization/completeness/fact-state assessment.
- Bound S5 analysis to authenticated evidence, project, observation, execution, test, endpoint and supplied policy references.
- Hardened BOLA/BFLA identifiers with deterministic SHA-256 based IDs and added explicit BFLA endpoint binding for the final path.
- Promoted tenant, workflow and property policy review into typed authorization assessments while reusing the existing PolicyValidationEvaluator.
- Added conservative correlation metadata, FindingCandidate evaluation, explicit impact-driven severity/risk, final AuthorizationOrchestrator and secret-safe AuthorizationReport generation.
- Added Sprint5FinalClosureTestSuite and exact-JDK-21 GitHub Actions verification.
- Sprint 5 final verification PASS: 736 represented suite checks/assertions, including 321 S5-specific.
- S6 remains NOT STARTED.

﻿# Changelog

All notable changes to this project will be documented in this file.

The changelog follows a simple versioned approach to track project development, security-related improvements, research features, fixes, and documentation changes.

## [Unreleased]

### Added

- Professional repository security policy
- Contribution guidelines
- Changelog and project documentation structure

### Security

- Defined responsible vulnerability reporting practices
- Documented authorized security testing expectations

## Version History

### Initial Development

- Initial project development and research setup
- Security-focused architecture and tooling established
