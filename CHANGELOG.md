## 2026-09-25 — Sprint 12 controlled A0-A7 research execution

- Created `s12-a0-a7-research-evaluation` from the verified Sprint 11 head.
- Added a controlled A0-A7 cumulative ablation runner over the frozen 16-case authorization dataset.
- Enforced prediction-label separation: ground-truth and expected-candidate fields are unavailable to the predictor.
- Executed only the intentionally vulnerable localhost fixture in the prediction path and joined labels afterward.
- Added deterministic JSON, CSV and JSONL research artifacts with configuration fingerprints and SHA-256 sidecars.
- Added independent metric recomputation, cardinality checks, secret scans and two-run byte-repeatability verification.
- First measured workflow `36065830981`: PASS; Maven product package: BUILD SUCCESS.
- Measured false positives progressed A0/A1=7, A2=4, A3=3, A4=2, A5=1, A6/A7=0; all variants had TP=8 and FN=0 on the fixed dataset.
- A6/A7 measured P=1/R=1/F1=1 only within the registered synthetic localhost fixture.
- Registered dimension discovery, external-target validation and real Burp runtime remain outside the measured claim.

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
