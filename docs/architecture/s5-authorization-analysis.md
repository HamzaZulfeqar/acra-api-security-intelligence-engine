# S5 Authorization Analysis

## Current implemented boundary — 2026-09-09

`SecurityContextEngine` produces the existing AuthorizationContext from passive extraction, with expected/observed decisions UNKNOWN. The current source has no AuthorizationContextNormalizer mapping S4 Observation/Evidence into an S5 context. The historical flow below is an intended architecture, not implemented end-to-end behavior.

Existing BOLA/BFLA evaluators accept supplied contexts and produce typed assessments. Defensive guards reject incomplete/redacted references and nonbinary decisions; BFLA leaves the absent endpoint blank. Existing correlation aggregates supplied records conservatively and exposes conflicts; repeats do not establish independent corroboration. There is no final FindingCandidate/severity/orchestration implementation.

Record constructor copies and the existing DomainSerializer/UniversalRedactor protect recognized credential patterns. No second graph, evidence, context, replay or serialization architecture is introduced. No network or process dependency is added to assessment evaluation. Shared serialization hardening and conservative correlation are documented in ADR-0036.

The current software decision is S5 SOFTWARE PARTIAL. See `../sprints/sprint-05-final-software-closure.md` for source/test gaps and validation limits.

## Historical intended flow

S5 layers consume S4 execution evidence.

## Authorization context flow

Observation + Evidence + SecurityContextGraph + DifferentialResult -> AuthorizationContext

## S5-03

Function-level reasoning uses existing principal, role, action, endpoint and decision context. It produces BflaAssessment only, not confirmed findings.


S5-04: Evidence correlation aggregation foundation added.
