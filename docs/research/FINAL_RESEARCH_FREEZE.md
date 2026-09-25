# ACRA Final Research & Reproducibility Freeze

**Freeze ID:** `ACRA-S13-FINAL-FREEZE-2026-09-26`  
**Branch:** `s13-final-research-freeze`  
**Pre-freeze base:** `7786097a4ea94bba7f7285218ed1c91d5d4062cd`  
**Final state:** FROZEN COMPLETE.

## Decision

Sprint 13 research is being frozen after completion of Phases 1–7.

Phase 8 external-target validation is intentionally recorded as **NOT PERFORMED**. It is not counted as a pass and no
external-target claim is made.

This is a valid final research boundary: the project freezes exactly the evidence that exists rather than generating or
implying evidence that was not collected.

Any later external-target validation must be versioned as a new post-freeze experiment. It must not alter the canonical
Sprint 13 datasets, measured commits, workflow evidence, or historical metrics.

## What the freeze preserves

The freeze preserves:
- the original Sprint 13 held-out false-positive result rather than rewriting it;
- automatic dimension-discovery results;
- configurable-policy generalization evidence;
- negative-heavy/base-rate stress evidence;
- explicit uncertainty-governance evidence;
- framework-shaped normalization evidence;
- actual FastAPI, Flask, Express and Spring Boot localhost runtime evidence;
- real Burp Desktop / Montoya runtime evidence;
- all stated limitations and negative findings.

A later phase is not allowed to retroactively improve an earlier measured result.

## Canonical evidence chain

The machine-readable canonical index is:

`docs/research/FINAL_EVIDENCE_MANIFEST.json`

It contains the canonical workflow run and measured commit for each completed Sprint 13 research phase.

The repository verifier requires every canonical measured commit to remain present in Git history and to be an ancestor
of the final freeze branch.

## Reproducibility boundary

Reproduction instructions are centralized in:

`docs/research/FINAL_REPRODUCIBILITY.md`

The final verification workflow does not attempt to fabricate old measurements. It verifies:
- canonical commits still exist;
- canonical commits remain in ancestry;
- required protocols remain present;
- research JSON remains parseable;
- the experiment registry still contains the canonical experiment IDs;
- Phase 8 remains honestly marked NOT PERFORMED;
- Phase 7 frozen implementation remains unchanged after its development freeze;
- a deterministic SHA-256 inventory can be generated for tracked research evidence;
- the repository still compiles/packages with Maven.

## Freeze semantics

Once the final freeze is marked COMPLETE:
- Sprint 13 evidence is immutable;
- corrected wording may be added only as clearly versioned errata;
- new algorithmic work starts a new version/sprint;
- new external validation becomes a new experiment, not a retrofit;
- historical failed/partial results remain in the record;
- unsupported claims remain prohibited.

## External-validation decision

Phase 8 status:

**NOT PERFORMED BY FINAL FREEZE**

Reason:
no external target with explicit scope/authorization was registered for this research freeze.

This means the project may claim controlled local/runtime evidence, but may not claim production/external-target
effectiveness, production accuracy, or independent real-world validation.

## Final closure evidence

The final research/reproducibility gate passed.

Canonical closure validation:
- GitHub Actions run: `36183344395`;
- validated candidate commit: `3bc7df2f19d8b208f4586dfd4469d9b045e275f8`;
- canonical completed experiments: 8;
- Sprint 13 research/policy JSON parsed: 33 files;
- deterministic tracked evidence inventory: 158 files;
- history/ancestry integrity: PASS;
- Phase 7 frozen implementation lock: PASS;
- Phase 8 boundary: NOT PERFORMED / PASS;
- deterministic verifier repeatability: PASS;
- Maven package: BUILD SUCCESS;
- git diff check: PASS.

## Final decision

**SPRINT 13 RESEARCH & REPRODUCIBILITY FREEZE: COMPLETE.**

No Sprint 13 result may now be silently rewritten by later development.

Any future external-target validation, algorithm change, policy-learning work, framework adapter, active execution work, or
new Burp compatibility result must be recorded as a new post-freeze experiment/version.

Phase 8 remains historically recorded as **NOT PERFORMED BEFORE FREEZE** and therefore contributes no external-validity
claim to Sprint 13.
