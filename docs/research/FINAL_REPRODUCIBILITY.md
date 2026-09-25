# ACRA Final Reproducibility Guide

**Freeze:** `ACRA-S13-FINAL-FREEZE-2026-09-26`

## Purpose

This guide describes how to verify the frozen repository and how to locate the canonical Sprint 13 evidence. It does not
promise that third parties can reproduce identical wall-clock timing, runtime-generated Burp message IDs, or timestamps.

## Minimum environment

Expected CI/runtime toolchain:
- Git with full history;
- Python 3.12;
- Java 21;
- Maven;
- Node.js 20 for the Express runtime experiment;
- Linux/GitHub Actions-compatible shell environment;
- network access only where a workflow explicitly downloads a pinned external runtime such as Burp Desktop.

Burp Phase 7 additionally pins:
- Burp Suite Community Edition 2026.7.3;
- SHA-256 `c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069`;
- Montoya API compile contract 2026.7.

## Canonical source of truth

Read:
1. `docs/research/FINAL_EVIDENCE_MANIFEST.json`;
2. `docs/research/EXPERIMENT_REGISTRY.md`;
3. the phase-specific protocol referenced by the manifest;
4. `docs/testing/TEST_MATRIX.md`;
5. `KNOWN_ISSUES.md`;
6. `docs/research/FINAL_CLAIM_BOUNDARY.md`.

The manifest records the measured commit and workflow run for every completed Sprint 13 research phase.

## Final repository verification

Run:

```bash
python3 scripts/verify-sprint13-final-research-freeze.py
mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package
```

The verifier:
- checks schema/state consistency;
- proves the canonical measured commits remain in Git history;
- proves they remain ancestors of the freeze branch;
- checks required protocols/files;
- parses Sprint 13 ground-truth/policy JSON;
- checks the experiment registry contains canonical experiment IDs;
- enforces the Phase 8 NOT_PERFORMED claim boundary;
- verifies the frozen Phase 7 extension implementation has not changed;
- creates deterministic SHA-256 inventories under `build/s13-final-freeze/`.

## Re-running individual experiments

Use the exact phase workflow and protocol rather than inventing a new command line.

Key workflows include:
- `.github/workflows/sprint13-heldout.yml`;
- `.github/workflows/sprint13-dimension-discovery.yml`;
- `.github/workflows/sprint13-policy-generalization.yml`;
- `.github/workflows/sprint13-baserate.yml`;
- `.github/workflows/sprint13-governance-eval.yml`;
- `.github/workflows/sprint13-cross-framework-eval.yml`;
- `.github/workflows/sprint13-cross-framework-runtime-eval.yml`;
- `.github/workflows/sprint13-burp-runtime-eval.yml`.

Historical workflows may depend on exact pinned corpus/files and full Git history.

## Determinism

Where experiments claim byte repeatability, their phase-specific verifier is authoritative.

For real Burp runtime evaluation, raw probe files contain runtime-generated values. Phase 7 therefore requires semantic
repeatability of normalized evaluation output rather than byte equality of raw runtime traces.

## Data integrity

Ground-truth labels are intentionally stored separately from feature inputs for blind-evaluation phases.

Do not merge feature and label files for convenience. Doing so destroys the original experimental boundary.

## Future work after freeze

Any new:
- detector tuning;
- policy-learning logic;
- framework adapter;
- Burp-version compatibility work;
- active scanning;
- external-target validation

must start as a new post-freeze experiment/version.

Do not modify frozen metrics to incorporate later results.
