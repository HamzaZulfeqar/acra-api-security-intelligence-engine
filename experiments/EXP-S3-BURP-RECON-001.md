# EXP-S3-BURP-RECON-001 — Real Burp Reconnaissance Validation

**State:** BLOCKED  
**Result:** UNVERIFIED

## Hypothesis

ACRA can transform real Burp/Montoya traffic into the same Sprint 3 reconnaissance records measured in direct/local validation.

## Required environment

- Burp Suite compatible with accepted Sprint 2 runtime gate
- official Montoya dependency build
- Maven toolchain
- ACRA-Lab

## Blocker

The current execution environment has Java 21 but no Maven executable, no outbound artifact/DNS resolution, and no Burp installation.

No local contract stub, fixture, screenshot mock or direct HTTP ingestion is substituted for this experiment.
