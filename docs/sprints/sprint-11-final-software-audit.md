# Sprint 11 Final Software Audit

Date: 2026-09-25  
Branch: `s11-finding-lifecycle-reproduction`  
Final executable closure run: `36064082001`

## Decision

**S11 SOFTWARE COMPLETE**

This decision applies to the implemented and verified local/software product boundary. Real Burp desktop
publication and A0-A7 comparative research execution remain separate deferred validation/research lanes.

## Audit matrix

| Area | Evidence | Result | Residual boundary |
|---|---|---|---|
| Finding lifecycle | 40 lifecycle assertions | COMPLETE | human review remains required |
| Review workspace | 29 workspace assertions | COMPLETE | in-memory workspace |
| Reproduction package | 38 assertions | COMPLETE | references retained instead of fabricated raw HTTP |
| JSON export | 34 assertions + SHA-256 | COMPLETE | none for canonical software export |
| SARIF 2.1.0 export | 41 assertions | COMPLETE | downstream consumer interoperability beyond schema/content checks not claimed |
| Burp Issue draft | 39 assertions | COMPLETE | draft is not live publication |
| Montoya adapter | official Maven package PASS | COMPLETE | real Burp desktop execution deferred |
| Findings UI | 47 headless assertions | COMPLETE | headless source/UI verification only |
| Security hardening | 37 assertions | COMPLETE | opaque secrets not recognizable by any redactor remain a general limitation |
| ACRA-Lab ground truth | 16 cases, balanced 8/8, 8 dimensions | COMPLETE | synthetic/local dataset only |
| A0-A7 experiment registry | EXP-A0…EXP-A7 registered | SOFTWARE READY | NOT_RUN / NOT_MEASURED |
| Bounded performance observation | 1,000 cases; 2,000 export iterations | COMPLETE / OBSERVATIONAL | not a benchmark or SLO |
| Sprint 10 regression | reporting 43, security 20, UI 230 plus full S10 foundation | PASS | none for retained software boundary |
| Older sprint regression | S6-S9 foundations and S2/S3 local contracts | PASS | historical Burp runtime debt remains |
| Official Maven package | Montoya 2026.7 dependency | PASS | live Burp application not executed |
| Deterministic package | 974 entries; clean extraction/hash equality | PASS | digest changes when later documentation is added |

## Sprint 11 measured closure evidence

Final run `36064082001` recorded:

```text
SPRINT11_FINDING_LIFECYCLE_FOUNDATION PASS assertions=40
SPRINT11_FINDING_REVIEW_WORKSPACE PASS assertions=29
SPRINT11_FINDING_REPRODUCTION_PACKAGE PASS assertions=38
SPRINT11_FINDING_REPRODUCTION_JSON_EXPORT PASS assertions=34
SPRINT11_FINDING_REPRODUCTION_SARIF_EXPORT PASS assertions=41
SPRINT11_FINDING_BURP_ISSUE_DRAFT PASS assertions=39
SPRINT11_FINDING_SECURITY_HARDENING PASS assertions=37
SPRINT11_FINDING_REVIEW_UI PASS assertions=47
SPRINT11_GROUND_TRUTH PASS cases=16 positive=8 negative=8 dimensions=8 a0_a7=NOT_RUN metrics=NOT_MEASURED
SPRINT11_FINAL_VERIFICATION PASS
```

Bounded CI engineering observation:

```text
cases=1000
exportIterations=2000
intakeMillis=246
exportMillis=1597
jsonBytes=1256
sarifBytes=2196
```

These values are environment-specific observations only.

## Packaging evidence

Executable closure checkpoint:

```text
archive=acra-sprint-11-final.zip
archiveSha256=3ca60a82888a0b7649da8433ad670df4dc62f0f168bd88dc814199731ff4fcfd
entryCount=974
unsafePaths=0
duplicateEntries=0
cleanExtractionEquality=PASS
perFileSha256Equality=PASS
```

That SHA-256 identifies the executable closure head `a1f22eb438f5517731cf0f6d16dc87bfd3e2f8c2`
before final documentation synchronization. The documentation-correct head is re-run through the same deterministic
package gate.

## Deferred / not claimed

- Real Burp desktop issue publication: **UNVERIFIED / DEFERRED**.
- Real Burp desktop UI/runtime validation for Sprint 11: **UNVERIFIED / DEFERRED**.
- External target validation: **NOT PERFORMED**.
- A0-A7 research execution: **NOT_RUN**.
- Sprint 11 TP/TN/FP/FN: **NOT_MEASURED**.
- Sprint 11 precision/recall/F1: **NOT_MEASURED**.
- Real-world scanner accuracy/effectiveness: **NOT CLAIMED**.
- Production performance/SLO/capacity: **NOT CLAIMED**.

## Next gate

Sprint 12 may execute the registered A0-A7 comparative experiment only from the verified
`GT-S11-AUTHORIZATION-RESEARCH` ground truth and must record measured results separately from Sprint 11 software closure.
