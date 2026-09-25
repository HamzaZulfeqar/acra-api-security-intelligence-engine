# Test Matrix

## Sprint 13 Phase 5 policy reliability & uncertainty governance — 2026-09-25

Canonical untouched evaluation: GitHub Actions run `36170353179` — **PASS** at
`46f77a8d76b160cec70f7aef3f612e0d63af56da`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Phase 4 evidence lock | Phase 4 corpus/runner/verifier unchanged | PASS |
| Governance freeze | governance engine + frozen upstream algorithms unchanged from `11afbb80...` | PASS |
| Development corpus | 64 cases | PASS |
| Development dimension inference | 64/64 | PASS |
| Development expected disposition | 64/64 | PASS |
| Development actionable precision | FP=0, P=1.0 | PASS |
| Development escalation coverage | candidate or review positive coverage | 1.0 |
| Untouched evaluation corpus | created after governance freeze | PASS |
| Evaluation label absence | labels removed during both prediction passes | PASS |
| Evaluation dimension inference | 64/64 | PASS |
| Evaluation expected disposition | 64/64 | PASS |
| Frozen A7 | TP=16/TN=21/FP=27/FN=0 | P=.372093 / R=1 / F1=.542373 |
| Legacy G1 | TP=16/TN=25/FP=23/FN=0 | P=.410256 / R=1 / F1=.581818 |
| Governed actionable | TP=8/TN=48/FP=0/FN=8 | P=1 / R=.5 / F1=.666667 |
| Positive review routing | 8 positive cases | PASS |
| Silent positive check | 0 | PASS |
| Escalation coverage | actionable candidate or review | 1.0 |
| Review rate | 40/64 | .625 |
| Actionable candidate rate | 8/64 | .125 |
| Prediction repeatability | two blind runs byte-identical | PASS |
| Evaluation repeatability | two sealed-label joins byte-identical | PASS |
| SHA-256 sidecars | prediction/evaluation JSON + JSONL | PASS |
| Secret-material scan | bearer/Authorization/cookie material excluded | PASS |
| Maven package | product modules compile/package | BUILD SUCCESS |
| Automatic policy-health determination | explicit metadata only | NOT MEASURED |
| Cross-framework generalization | next independent lane | NOT MEASURED |
| Real Burp desktop runtime | separate lane | UNVERIFIED / DEFERRED |
| External authorized target | outside Phase 5 | NOT PERFORMED |

### Untouched disposition distribution

| Disposition | Count | Actionable | Review required |
|---|---:|---|---|
| VULNERABILITY_CANDIDATE | 8 | yes | no |
| AUTHORIZED_CONTROL | 8 | no | no |
| CONTROL_ENFORCED | 8 | no | no |
| POLICY_GAP | 16 | no | yes |
| AMBIGUOUS_POLICY | 8 | no | yes |
| STALE_POLICY | 8 | no | yes |
| INCOMPLETE_CONTEXT | 8 | no | yes |

The governed actionable recall of .5 must be interpreted together with escalation coverage=1.0. Eight positive cases
were intentionally withheld from actionable status because no authoritative matching policy existed; all eight remained
visible through review-required POLICY_GAP disposition.

## Sprint 13 Phase 4 adversarial / base-rate stress — 2026-09-25

Canonical workflow: GitHub Actions run `36168752869` — **PASS** at
`ed1601542739cce20be48c001ae4e663bfdcf890`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Phase 3 evidence lock | Phase 3 dev/eval corpora + runners unchanged | PASS |
| Frozen algorithm gate | locked A7 + dimension + policy engines unchanged | PASS |
| Stress corpus | 96 cases: 8 positive / 88 negative | PASS |
| Measured prevalence | 8.333333% | PASS |
| Dimension inference | 96/96 | PASS |
| Bad schema registry | fail closed | PASS |
| Empty/non-list policies | fail closed | PASS |
| Duplicate policy ID | fail closed | PASS |
| Invalid dimension / missing regex / invalid regex | fail closed | PASS |
| Equal-priority ambiguity | UNKNOWN | PASS |
| Missing policy | UNKNOWN | PASS |
| Priority resolution | deterministic higher priority | PASS |
| UNKNOWN fallback invariant | preserves locked A7 | PASS |
| Explicit ALLOW | suppresses candidate | PASS |
| Explicit DENY + observed ALLOW | promotes mismatch | PASS |
| Blind label boundary | label file removed during both prediction passes | PASS |
| Prediction repeatability | byte-identical | PASS |
| Evaluation repeatability | byte-identical | PASS |
| SHA-256 sidecars | prediction/evaluation JSON + JSONL | PASS |
| Secret-material scan | bearer/Authorization/cookie material excluded | PASS |
| Maven package | product modules compile/package | BUILD SUCCESS |

### Measured classifier result

| Variant | TP | TN | FP | FN | Precision | Recall | Specificity | FPR | NPV | Balanced Acc. | F1 | MCC |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Locked A7 | 8 | 20 | 68 | 0 | .105263 | 1.000000 | .227273 | .772727 | 1.000000 | .613636 | .190476 | .154672 |
| G1 | 8 | 49 | 39 | 0 | .170213 | 1.000000 | .556818 | .443182 | 1.000000 | .778409 | .290909 | .307860 |

G1 Wilson 95% intervals:
- sensitivity [.675592, 1.000000];
- specificity [.452818, .656065];
- precision [.088864, .301398];
- NPV [.927302, 1.000000].

### G1 false positives by policy condition

| Condition | Count | FP | FN | UNKNOWN policy |
|---|---:|---:|---:|---:|
| EXPLICIT_ALLOW | 40 | 0 | 0 | 0 |
| NO_POLICY | 16 | 12 | 0 | 16 |
| AMBIGUOUS_POLICY | 16 | 12 | 0 | 16 |
| STALE_POLICY | 8 | 8 | 0 | 0 |
| INCOMPLETE_CONTEXT | 8 | 7 | 0 | 6 |
| POSITIVE | 8 | 0 | 0 | 0 |

### G1 prevalence projection

| Assumed prevalence | Projected PPV | Projected NPV | Alerts / 1,000 | False alerts / 1,000 |
|---:|---:|---:|---:|---:|
| 1% | .022284 | 1.000000 | 448.750000 | 438.750000 |
| 5% | .106152 | 1.000000 | 471.022727 | 421.022727 |
| 8.333333% | .170213 | 1.000000 | 489.583333 | 406.250000 |
| 10% | .200456 | 1.000000 | 498.863636 | 398.863636 |

The projections derive mathematically from measured sensitivity/specificity and are not additional observed datasets.

## Sprint 13 Phase 3 configurable policy generalization — 2026-09-25

Canonical untouched evaluation: GitHub Actions run `36149071484` — **PASS** at
`82ae165b702d18b0eae872d51f8f695f723fec13`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Phase 2 immutable base | dimension engine/history preserved | PASS |
| Phase 3 algorithm freeze | policy engine + dimension engine + locked A7 unchanged from `c7c66336...` | PASS |
| Development corpus | 24 cases, 8 positive / 16 legitimate controls | PASS |
| Development repeatability | two prediction/evaluation passes | PASS |
| Development label absence | labels removed during prediction | PASS |
| Untouched evaluation corpus | created only after algorithm freeze | PASS |
| Evaluation label absence | labels physically removed during both prediction passes | PASS |
| Automatic dimension inference | 24/24 evaluation cases | PASS |
| Explicit configured-policy decisions | 24/24 decisive, UNKNOWN=0 | PASS |
| Locked A7 evaluation | TP=8/TN=4/FP=12/FN=0 | P=.4 / R=1 / F1=.571429 |
| Policy-generalized G1 evaluation | TP=8/TN=16/FP=0/FN=0 | P=1 / R=1 / F1=1 |
| Evaluation repeatability | byte-identical prediction and evaluation artifacts | PASS |
| Artifact SHA-256 | prediction/evaluation JSON + JSONL | PASS |
| Secret-material scan | bearer/Authorization/cookie material excluded | PASS |
| Maven package | product modules compile/package after evaluation | BUILD SUCCESS |
| Automatic policy extraction | not part of configured-policy experiment | NOT MEASURED |
| Real Burp desktop runtime | separate validation lane | UNVERIFIED / DEFERRED |
| External authorized target | outside Phase 3 | NOT PERFORMED |

The G1 1.0 values are controlled internal fixture results and must not be generalized to production accuracy.
Phase 4 must test larger, less-balanced and adversarial negative populations.

## Sprint 13 Phase 2 automatic authorization-dimension discovery — 2026-09-25

Measured successful workflow: GitHub Actions run `36145805521` — **PASS** at
`3f4b4238efe514b7ef1fe65b2cd477233c54fa73`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Dimension-free Phase 2 feature corpus | 32 cases, no registered dimension/vulnerability label | PASS |
| Sealed dimension-label corpus | 32 cases | PASS |
| Label absence during inference | label file physically removed for both prediction passes | PASS |
| S12 immutable rule gate | S12 runner/verifier unchanged | PASS |
| Phase 1 immutable-history gate | heldout fixture/data/runner/verifier unchanged | PASS |
| Automatic dimension inference | 32 cases / 8 classes | PASS |
| Downstream A0-A7 with inferred dimension | 32 × 8 | PASS, 256 rows |
| Evaluation rows | 32 dimension + 256 downstream | PASS, 288 |
| Prediction repeatability | two blind passes byte-identical | PASS |
| Evaluation repeatability | two sealed-label joins byte-identical | PASS |
| Secret-material scan | Authorization/bearer/cookie material excluded | PASS |
| Maven package | product modules compile/package after Phase 2 harness | BUILD SUCCESS |
| Real Burp desktop runtime | separate lane | UNVERIFIED / DEFERRED |
| External authorized target | outside Phase 2 | NOT PERFORMED |

### Dimension discovery metrics

| Dataset | Correct | Accuracy | Macro-F1 |
|---|---:|---:|---:|
| S12 calibration | 15/16 | .937500 | .933333 |
| S13 holdout | 16/16 | 1.000000 | 1.000000 |
| Combined | 31/32 | .968750 | .968254 |

Confidence distribution: 30 HIGH / 2 MEDIUM.

### Downstream A7 using inferred dimension only

| Dataset | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| S12 calibration | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |
| S13 holdout | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 |

The single dimension mismatch and the five holdout binary false positives are retained as research evidence; no score
threshold or perfect result is required for experiment completion.

## Sprint 13 Phase 1 held-out external-validity evaluation — 2026-09-25

Measured successful workflow: GitHub Actions run `36143281129` — **PASS** at
`e515a31d91d775d5f0f35c32a48507455f84992d`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Locked Sprint 12 rule gate | S12 runner/verifier unchanged from `bd944e83...` | PASS |
| Frozen held-out feature corpus | 16 cases / 8 dimensions | PASS |
| Sealed label corpus | separate file, 8 positive / 8 negative | PASS |
| Label absence during prediction | label file physically removed for both prediction passes | PASS |
| Held-out oracle validation | secure/vulnerable expected decisions for all 16 cases | PASS |
| A0-A7 held-out prediction | 16 × 8 variants | PASS, 128 rows |
| Prediction repeatability | two blind passes byte-identical | PASS |
| Evaluation repeatability | two post-label-join passes byte-identical | PASS |
| Artifact SHA-256 | prediction + evaluation JSON/JSONL | PASS |
| Research artifact secret scan | bearer/Authorization material excluded | PASS |
| Maven package | product modules compile/package after harness | BUILD SUCCESS |
| Dimension discovery | registered dimension supplied | NOT MEASURED |
| Real Burp desktop runtime | separate validation lane | UNVERIFIED / DEFERRED |
| External authorized target | outside Phase 1 | NOT PERFORMED |

### Measured held-out results

| Variant | TP | TN | FP | FN | Precision | Recall | F1 | Hard-negative FP |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 0 | 8 | 0 | .500000 | 1.000000 | .666667 | 8 |
| A1 | 8 | 0 | 8 | 0 | .500000 | 1.000000 | .666667 | 8 |
| A2 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A3 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A4 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A5 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A6 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |
| A7 | 8 | 3 | 5 | 0 | .615385 | 1.000000 | .761905 | 5 |

The lower held-out precision/F1 relative to Sprint 12 is retained as evidence of limited policy generalization rather
than treated as a failed test. Completion depends on experiment integrity and reproducibility, not on achieving a target
accuracy score.

## Sprint 12 A0-A7 controlled research evaluation — 2026-09-25

First measured successful workflow: GitHub Actions run `36065830981` — **PASS**.

| Test / Gate | Coverage | Result |
|---|---|---|
| Sprint 11 frozen ground-truth revalidation | 16 cases / 8 dimensions / balanced labels | PASS |
| Sprint 12 label-isolation boundary | predictor excludes groundTruth / secureExpected / vulnerableExpected / expectedCandidate | PASS |
| Vulnerable-only prediction execution | predictor observes localhost vulnerable fixture only | PASS |
| A0-A7 campaign | 16 cases × 8 variants | PASS, 128 rows |
| Metric recomputation | independent TP/TN/FP/FN + precision/recall/F1 verification | PASS |
| Configuration fingerprints | one deterministic fingerprint per variant | PASS |
| Artifact SHA-256 sidecars | JSON / CSV / JSONL | PASS |
| Repeatability gate | second execution byte-identical to first | PASS |
| Research artifact secret scan | bearer/Authorization/synthetic-cookie material excluded | PASS |
| Maven package | product modules compile/package after research harness | BUILD SUCCESS |
| Dimension discovery | registered dimension is supplied | NOT MEASURED |
| Real Burp desktop runtime | separate validation lane | UNVERIFIED / DEFERRED |
| External-target / real-world accuracy | outside registered campaign | NOT PERFORMED |

### Measured synthetic/local results

| Variant | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| A0 | 8 | 1 | 7 | 0 | .533333 | 1.000000 | .695652 |
| A1 | 8 | 1 | 7 | 0 | .533333 | 1.000000 | .695652 |
| A2 | 8 | 4 | 4 | 0 | .666667 | 1.000000 | .800000 |
| A3 | 8 | 5 | 3 | 0 | .727273 | 1.000000 | .842105 |
| A4 | 8 | 6 | 2 | 0 | .800000 | 1.000000 | .888889 |
| A5 | 8 | 7 | 1 | 0 | .888889 | 1.000000 | .941176 |
| A6 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |
| A7 | 8 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

These measurements apply only to the frozen 16-case synthetic localhost fixture. A6/A7's 1.0 fixture
metrics do not establish production accuracy, external validity or superiority on unseen APIs. A7 adds
correlation but produces no additional metric change over A6 on this dataset.

## Sprint 11 finding lifecycle, reproduction and ground-truth closure — 2026-09-25

Final executable closure: GitHub Actions run `36064082001` — **PASS**.

| Test / Gate | Coverage | Result |
|---|---|---|
| Sprint11FindingLifecycleFoundationTestSuite | lifecycle transition safety and human-review boundary | PASS, 40 assertions |
| Sprint11FindingReviewWorkspaceTestSuite | project isolation, idempotent intake, state accounting | PASS, 29 assertions |
| Sprint11FindingReproductionPackageTestSuite | minimized deterministic reproduction package | PASS, 38 assertions |
| Sprint11FindingReproductionJsonExportTestSuite | canonical JSON, SHA-256, Reporter adapter | PASS, 34 assertions |
| Sprint11FindingReproductionSarifExportTestSuite | SARIF 2.1.0 mapping and deterministic export | PASS, 41 assertions |
| Sprint11FindingBurpIssueDraftTestSuite | Burp issue draft eligibility/severity/confidence boundary | PASS, 39 assertions |
| Sprint11FindingSecurityHardeningTestSuite | secret exclusion, identity validation, immutability, determinism | PASS, 37 assertions |
| Sprint11FindingPerformanceObservationTestSuite | 1,000 intake / 2,000 JSON+SARIF repetitions | PASS, observational |
| Sprint11FindingReviewUiTestSuite | read-only Findings / History / JSON / SARIF / Burp Draft views | PASS, 47 assertions |
| Sprint 11 ground-truth verifier | 16 cases, balanced positive/negative, 8 authorization dimensions | PASS |
| Official Maven package | Montoya 2026.7 extension compile/package | PASS |
| Legacy Sprint 2 contract CI | local stubs + official Maven package | PASS |
| Legacy Sprint 3 contract CI | local stubs + official Maven package | PASS |
| Deterministic Sprint 11 checkpoint | ZIP integrity / unsafe paths / duplicate paths / clean extraction / SHA-256 equality | PASS |
| Real Burp desktop issue publication | separate runtime lane | UNVERIFIED / DEFERRED |
| EXP-A0…EXP-A7 measured campaign | later research lane | NOT_RUN / NOT_MEASURED at Sprint 11 closure; executed separately in Sprint 12 |

Final-run bounded observation: intake 246 ms for 1,000 records; 1,597 ms for 2,000 JSON+SARIF export repetitions;
JSON 1,256 bytes; SARIF 2,196 bytes. These are controlled CI engineering observations, not benchmarks or SLOs.

Executable closure package at commit `a1f22eb438f5517731cf0f6d16dc87bfd3e2f8c2`:
SHA-256 `3ca60a82888a0b7649da8433ad670df4dc62f0f168bd88dc814199731ff4fcfd`, 974 entries,
0 unsafe paths, 0 duplicates, clean extraction equality PASS, per-file SHA-256 equality PASS.


## Sprint 10 batch & indirect authorization — 2026-09-25

Authoritative pre-final verification through Phase 8 is GitHub Actions run `36055037223`.

| Test / Gate | Coverage | Result |
|---|---|---|
| Sprint10BatchIndirectFoundationTestSuite | per-item batch reasoning, indirect fingerprint/resolution, provenance, determinism | PASS |
| Sprint10ControlledBatchIndirectLabTestSuite | fixed secure/vulnerable localhost ground truth | PASS |
| Sprint10ControlledBatchExecutionTestSuite | safe S4 batch active execution | PASS, 25 assertions |
| Sprint10ControlledIndirectExecutionTestSuite | safe S4 indirect active execution | PASS, 23 assertions |
| Sprint10FindingCandidateProjectionTestSuite | provenance-gated review-only finding projection | PASS, 22 assertions |
| Sprint10CoverageAccountingTestSuite | combined batch/indirect policy coverage | PASS, 22 assertions |
| Sprint10BatchIndirectReportingExportTestSuite | minimized deterministic report / JSON / SHA-256 / Markdown / Reporter | PASS, 43 assertions |
| Sprint10BatchIndirectSecurityHardeningTestSuite | malformed input, evidence, fingerprint, coverage drift, workspace/report boundaries | PASS, 20 assertions |
| Sprint10BatchIndirectPerformanceObservationTestSuite | 100 / 1,000 / 10,000 bounded engineering observations | PASS, 22 assertions |
| Sprint10BatchIndirectUiTestSuite | Overview / Policies / Observations / Assessments / Candidates / Coverage / Report / JSON Export | PASS, 230 assertions |
| Real Burp desktop load/handler/UI | separate runtime lane | UNVERIFIED / DEFERRED |

The performance values are engineering observations from one CI environment, not benchmarks, SLOs, capacity or
scanner-accuracy claims. Sprint 10 does not permit identifier guessing, alias enumeration or external-target probing.

### Sprint 10 final closure

GitHub Actions run `36055604554`: **PASS** at closure-candidate source commit `a0c6ae56db84fdd9f79c56aaf8770261ac4fd529`.

| Final gate | Result |
|---|---|
| Sprint 10 full verification | PASS |
| Retained Sprint 9 / 8 / 7 / 6 foundations | PASS |
| Official Maven package | PASS |
| Sprint 2 local-contract regression | PASS — 52 tests |
| Sprint 3 core / adapter regression | PASS — 47 / 11 |
| Sprint 4 UI | PASS — 26 tests |
| Sprint 6 / 7 / 8 / 9 / 10 product UI | PASS — 27 / 20 / 23 / 59 / 230 |
| Deterministic source ZIP | PASS — 931 entries |
| Unsafe / duplicate paths | PASS — 0 / 0 |
| Clean extraction / per-file SHA-256 equality | PASS / PASS |
| Real Burp desktop runtime | UNVERIFIED / DEFERRED |

Closure-candidate source ZIP SHA-256:
`dbd67307bf56d3333f77d44ca72bb1b1062322cbff9b3f1c0bde58d5b01931e0`.

The closure-candidate hash identifies the successful pre-final-status source snapshot. Post-documentation packaging
is represented by the external `.sha256` sidecar to avoid a recursive embedded-hash dependency.

## Sprint 6 controlled tenant/RBAC validation — 2026-09-23

GitHub Actions run `35878508170`: **PASS** on exact Temurin JDK 21.0.12.1.

| Test / Experiment | Coverage | Result |
|---|---|---|
| Sprint6PolicyFoundationTestSuite | scope, membership, multi-role, hierarchy, policy snapshot, secret safety | PASS |
| Sprint6PolicyResolutionTestSuite | effective permissions, global/delegated/shared scope, precedence/conflict, shared-scope isolation | PASS |
| Sprint6OrchestrationTestSuite | S5→S6 orchestration, tenant/RBAC/finding/risk composition | PASS |
| Sprint6GraphAndGroupingTestSuite | policy graph hydration, evidence preflight, root-cause grouping | PASS |
| Sprint6LabAndPlanningTestSuite | policy-aware CROSS_TENANT / ROLE_COMPARISON planning recommendations | PASS |
| Sprint6LiveLabExperimentTestSuite | live secure/vulnerable localhost campaign | PASS, 16 assertions |
| EXP-S6-TENANT-RBAC-001 baseline | 10 labelled cases | TP=2 TN=1 FP=7 FN=0, precision=.222222 recall=1.0 F1=.363636 |
| EXP-S6-TENANT-RBAC-001 ACRA | 10 labelled cases | TP=2 TN=8 FP=0 FN=0, precision=1.0 recall=1.0 F1=1.0 |

The metrics above apply only to the controlled localhost dataset and are not real-world accuracy claims.

### Sprint 6 performance/security closure

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-PERF-001 | 100 / 1,000 / 10,000 policy resolution | Sprint6PerformanceObservationTestSuite | PASS |
| TEST-S6-PERF-002 | policy-aware planning recommendations | Sprint6PerformanceObservationTestSuite | PASS |
| TEST-S6-PERF-003 | S6 report generation at 100 / 1,000 / 10,000 analyses | Sprint6PerformanceObservationTestSuite | PASS |
| TEST-S6-SEC-CTX-001 | missing / ambiguous / spoofed authenticated context refs | Sprint6SecurityHardeningTestSuite | PASS |
| TEST-S6-SEC-EQUIV-001 | path / non-auth header / resource drift rejection | Sprint6SecurityHardeningTestSuite | PASS |
| TEST-S6-SEC-AUTH-001 | unchanged auth context rejected | Sprint6SecurityHardeningTestSuite | PASS |
| TEST-S6-SEC-SERIAL-001 | viewer/admin raw token exclusion | Sprint6SecurityHardeningTestSuite | PASS |
| TEST-S6-SEC-POLICY-001 | conflicting policy produces no executable seed | Sprint6SecurityHardeningTestSuite | PASS |

### Sprint 6 reporting/export

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-REPORT-001 | deterministic report model / content identity | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-002 | canonical JSON + SHA-256 | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-003 | Markdown review report | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-004 | raw-secret exclusion / redaction | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-005 | candidate != confirmed finding boundary | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-006 | Reporter plugin adapter | Sprint6ReportingExportTestSuite | PASS |
| TEST-S6-REPORT-UI-001 | Report + JSON Export product views | Sprint6AuthorizationUiTestSuite | PASS |

### Sprint 6 authorization UI

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-UI-001 | Authorization top-level product area | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-002 | Tenant Map / roles / hierarchy / permissions | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-003 | Effective permissions matrix / conflicts / coverage | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-004 | Policy and candidate-vs-finding evidence boundary | Sprint6AuthorizationUiTestSuite | PASS |
| TEST-S6-UI-REG-001 | Retained Sprint 4 UI after S6 integration | Sprint4UiTestSuite | PASS |

### Sprint 6 planner/execution integration

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S6-AUTO-PLAN-001 | resolved S6 policy → generated CROSS_TENANT TestSeed | Sprint6PlannerExecutionIntegrationTestSuite | PASS |
| TEST-S6-AUTO-QUEUE-001 | generated seed → existing S4 TestPlanner / ExecutionQueue | same suite | PASS |
| TEST-S6-AUTO-EXEC-001 | queue → existing TestExecutor → secure localhost ACRA-Lab | run 35882729813 | PASS |
| TEST-S6-AUTO-MUT-001 | one-variable tenant-a → tenant-b path substitution | generated test S6-AUTO-TENANT-a915a78d9e3fd8c06e49c4e9 | PASS |
| TEST-S6-AUTO-SAFE-001 | credentials excluded from Mutation and serialized test | same suite | PASS |
| TEST-S6-ROLE-GEN-001 | safe read-only ROLE_COMPARISON active generation | authenticated contextRef substitution; raw credentials excluded from Mutation | PASS |
| TEST-S6-ROLE-EQUIV-001 | same endpoint/method/resource/body/non-auth headers | RequestEquivalenceGuard | PASS |
| TEST-S6-ROLE-SECRET-001 | viewer/admin raw tokens absent from serialized test and Mutation metadata | Sprint6PlannerExecutionIntegrationTestSuite | PASS |

## Sprint 5 final closure — 2026-09-23

Current authoritative S5 verification is GitHub Actions run `35872345270`: exact JDK 21 compilation and all selected core/S3/S4/S5 suites PASS. See `docs/sprints/sprint-05-final-completion.md` for the requirement matrix and residual validation boundaries.

## Current S5 defensive continuation — 2026-09-09

| Suite | Coverage | Evidence |
|---|---|---|
| Sprint5AssessmentGuardTestSuite | Missing/unknown/partial context, nonbinary decisions, conflicting ownership, incomplete evidence/provenance, missing endpoint | NEWLY EXECUTED, exact count/output in `artifacts/verification-s5-defensive.json` |
| Sprint5CorrelationSafetyTestSuite | Conflicting supplied records, duplicates, replay confidence, order invariance, missing/redacted references | NEWLY EXECUTED, same artifact |
| Sprint5SerializationSecurityTestSuite | Recognized credentials, generic sensitive fields, nested context projection, immutable lists, stable export, redacted-reference guards | NEWLY EXECUTED, same artifact |
| Core TestSuite | Core serialization/context/graph plus retained BOLA/BFLA tests | NEWLY EXECUTED, same artifact |
| Sprint3CoreTestSuite | Retained Sprint 3 core | NEWLY EXECUTED, same artifact |
| S4 core/security/graph/product suites | Affected source/serializer and prior safety contracts | NEWLY EXECUTED, same artifact |
| S5 tenant/workflow/property/finding/severity/orchestration/end-to-end | Missing implementation and test suites | MISSING / UNVERIFIED |
| Live ACRA-Lab / Burp / exact JDK 21 runtime / Maven | Not executed as part of offline verification | UNVERIFIED / BLOCKED as detailed in current S5 audit |

Runner: `scripts/verify-sprint5-defensive.ps1`. Legacy check counters are suite-reported totals, not necessarily raw assertion invocations. Repeated executions do not increase distinct coverage. Earlier matrices below retain their historical evidence dates.

## Sprint 1 executable core tests

| Test ID | Category | Requirement | Executable location | Result |
|---|---|---|---|---|
| TEST-HTTP-001 | HTTP model defensive copy | FR-016 | `HttpModelTests` | PASS |
| TEST-HTTP-002 | Path/query preservation | FR-016 | `HttpModelTests` | PASS |
| TEST-HTTP-NEG-001 | Missing host | NFR-003 | `HttpModelTests` | PASS |
| TEST-HTTP-NEG-002 | Invalid method | NFR-003 | `HttpModelTests` | PASS |
| TEST-SEC-004 | Oversized header rejected | SEC-008 | `HttpModelTests` | PASS |
| TEST-URI-001 | Encoded identifier raw/decoded/canonical | FR-003 | `UriTests` | PASS |
| TEST-URI-002 | Nested resources | FR-017 | `UriTests` | PASS |
| TEST-ID-001 | UUID detection | FR-004 | `UriTests` | PASS |
| TEST-URI-003 | Encoded slash representation divergence | FR-003 | `UriTests` | PASS |
| TEST-URI-NEG-001 | Malformed URI fail-safe | SEC-007 | `UriTests` | PASS |
| TEST-ID-NEG-001 | Unknown query value stays unknown | FR-004 | `UriTests` | PASS |
| AT-01 | Tenant/resource extraction | FR-019 | `ContextTests` | PASS |
| AT-03 | JWT sub principal with provenance | FR-018 | `ContextTests` | PASS |
| AT-07 | Conflicting tenant evidence | FR-019 | `ContextTests` | PASS |
| TEST-ACTION-001 | GET -> READ | FR-019 | `ContextTests` | PASS |
| TEST-ACTION-002 | Application approve action | FR-019 | `ContextTests` | PASS |
| AT-04 | Principal -> tenant evidence edge | FR-021 | `GraphTests` | PASS |
| AT-05 | Direct principal -> resource query | FR-006 | `GraphTests` | PASS |
| TEST-GRAPH-NEG-001 | Missing evidence edge rejected | SEC-006 | `GraphTests` | PASS |
| TEST-GRAPH-NEG-002 | Conflicting duplicate node rejected | NFR-003 | `GraphTests` | PASS |
| AT-06 | Bearer-token serialization redaction | SEC-005 | `SerializationSecurityTests` | PASS |
| TEST-SER-001 | Deterministic serialization | FR-024 | `SerializationSecurityTests` | PASS |
| TEST-SEC-002 | Control characters escaped | SEC-001 | `SerializationSecurityTests` | PASS |
| TEST-DOMAIN-NEG-001 | Invalid confidence rejected | NFR-003 | `SerializationSecurityTests` | PASS |
| TEST-ENTITY-001 | Exact entity equality | FR-022 | `ResolutionContractTests` | PASS |
| TEST-ENTITY-002 | Possible same is not auto-merged | FR-022 | `ResolutionContractTests` | PASS |
| TEST-DOMAIN-NEG-002 | Empty resource ID rejected | NFR-003 | `ResolutionContractTests` | PASS |
| TEST-GT-001 | Ground-truth context compatibility | FR-027 | `ResolutionContractTests` | PASS |
| TEST-PLUGIN-001 | Analyzer contract compiles/executes | FR-025 | `ResolutionContractTests` | PASS |
| TEST-SEC-003 | Malformed JWT does not invent identity | SEC-007 | `NegativeSecurityTests` | PASS |
| AT-08 | Unknown identifier does not invent resource | FR-004 | `NegativeSecurityTests` | PASS |
| TEST-SEC-001 | Serialization/log injection control chars escaped | SEC-001 | `NegativeSecurityTests` | PASS |

| TEST-GRAPH-NEG-003 | Duplicate evidence ID rejected | SEC-006 | `GraphTests` | PASS |
| TEST-SER-002 | Graph canonical serialization | FR-024 | `SerializationSecurityTests` | PASS |
| TEST-IDENTITY-NEG-001 | Conflicting identity evidence state | FR-018 | `ResolutionContractTests` | PASS |
| TEST-JSON-NEG-001 | Malformed JSON treated as opaque Sprint 1 body | SEC-007 | `NegativeSecurityTests` | PASS |
| TEST-SEC-005 | Oversized body rejected | SEC-008 | `NegativeSecurityTests` | PASS |

AT-02 is covered by `TEST-URI-001`: raw, decoded, normalized and canonical URI representations remain separate.

## Future research tests

| Test ID | Category | Requirement | Ground Truth | State |
|---|---|---|---|---|
| TM-A-001 | Target & Scope | SAFE-001 | N/A | PLANNED |
| TM-J-001 | BOLA | FR-010 | GT-BOLA-001 | PLANNED |
| TM-K-001 | BFLA | FR-010 | GT-BFLA-001 | PLANNED |
| TM-M-001 | Tenant Isolation | FR-005 | GT-TENANT-001 | PLANNED |
| TM-P-001 | Workflow Authorization | FR-005 | GT-WORKFLOW-001 | PLANNED |
| TM-Z-001 | Research Evaluation | RES-003 | Experiment registry | PLANNED |


## Sprint 2 passive integration tests

| Test ID | Category | Requirement | Current evidence | Result |
|---|---|---|---|---|
| TEST-MONTOYA-001 | Extension bootstrap | FR-028 | local Montoya-contract test doubles | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-002 | HTTP request received | FR-028/FR-029 | Sprint2TestSuite | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-003 | HTTP response received | FR-028/FR-031 | Sprint2TestSuite | LOCAL PASS; REAL BURP BLOCKED |
| TEST-MONTOYA-004 | Transaction created | FR-031 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-005 | URI extracted | FR-032/FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-006 | Identity/session correlated | FR-033 / SEC-010 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-007 | Tenant correlated | FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-008 | Resource identified | FR-033 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-009 | Global graph updated | FR-035 | Sprint2TestSuite / pipeline graph | PASS |
| TEST-GRAPH-OWNER-S2-001 | Explicit resource owner provenance creates OWNS edge | FR-035 | Sprint2TestSuite | PASS |
| TEST-MONTOYA-010 | Sensitive data redacted | SEC-009 | Sprint2TestSuite / security script | PASS |
| TEST-HTTP-COOKIE-001 | Cookie preservation + safe serialization | FR-032 / SEC-009 | Sprint2TestSuite | PASS |
| TEST-ENDPOINT-001 | Endpoint family aggregation | FR-034 | Sprint2TestSuite | PASS |
| TEST-CONTEXT-UI-001 | Context projection exposes role/session/owner/evidence/confidence | FR-038 | Sprint2TestSuite | PASS for model; real Burp UI runtime BLOCKED |
| TEST-DIFF-001 | Volatile field normalization | FR-036 | Sprint2TestSuite | PASS |
| TEST-ACTIVE-001 | Active execution disabled by default | SAFE-006 | Sprint2TestSuite | PASS |
| EXP-INTEGRATION-001 | Live local context reconstruction | RES-006 | `experiments/EXP-INTEGRATION-001.md` | PARTIAL: LOCAL PASS, BURP BLOCKED |

### Sprint 2 negative/security cases

| Case | Expected | Local result |
|---|---|---|
| Missing authentication | NONE/UNKNOWN, no invented principal | PASS |
| Unknown/custom authentication | classified conservatively | PASS |
| Malformed JWT | no invented identity | PASS via Sprint 1 regression |
| Duplicate endpoint observations | aggregate rather than fabricate endpoint | PASS |
| Conflicting tenant evidence | CONFLICTING_EVIDENCE | PASS |
| Oversized body | REJECTED | PASS |
| Raw bearer/API key serialization | redacted/fingerprinted | PASS |
| Unscoped active request | blocked | PASS through disabled executor/scope contract |
| No response | pending request, no fabricated completed transaction | PASS locally; real Burp runtime evidence BLOCKED |
| Binary/compressed response | preserved as bytes/opaque body; semantic decoding not claimed | PASS for opaque preservation; decoder deferred |
## Sprint 2 final promotion-gate matrix

The local columns describe executable RC1 evidence. Any row requiring the Burp desktop runtime remains explicitly unverified.

### Required positive matrix

| Test ID | Requirement | Local evidence | Promotion-gate state |
|---|---|---|---|
| TEST-MONTOYA-001 | Extension loads | Local bootstrap PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-002 | Montoya registration | Local contract PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-003 | HTTP request received | Local handler PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-004 | HTTP response received | Local handler PASS | REAL BURP BLOCKED / UNVERIFIED |
| TEST-MONTOYA-005 | Transaction generated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-006 | Raw request preserved | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-007 | Raw response preserved | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-008 | URI extracted | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-009 | Identity correlated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-010 | Tenant correlated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-011 | Resource identified | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-012 | Owner evidence attached | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-013 | Action classified | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-014 | Security context created | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-015 | Graph updated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-016 | Evidence generated | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-017 | Sensitive data redacted | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-018 | Scope guard blocks out-of-scope target | PASS | PASS locally; real Burp path UNVERIFIED |
| TEST-MONTOYA-019 | UI displays captured transaction | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-020 | UI displays security context | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-021 | UI displays endpoint | Model/UI source PASS | REAL BURP UI BLOCKED / UNVERIFIED |
| TEST-MONTOYA-022 | Kill switch remains functional | PASS | PASS locally |
| TEST-MONTOYA-023 | Active execution disabled by default | PASS | PASS locally |
| TEST-MONTOYA-024 | Burp shutdown clean | Local unload contract PASS | REAL BURP BLOCKED / UNVERIFIED |

### Required negative matrix

| Test ID | Case | Expected | Current state |
|---|---|---|---|
| TEST-MONTOYA-NEG-001 | Malformed HTTP | REJECTED/ERROR without fabricated context | PASS local parser/model coverage; real Burp path UNVERIFIED |
| TEST-MONTOYA-NEG-002 | Missing response | Remain pending; no fabricated transaction | PASS locally |
| TEST-MONTOYA-NEG-003 | Malformed authentication | UNKNOWN; no invented principal | PASS locally |
| TEST-MONTOYA-NEG-004 | Conflicting identity | CONFLICTING_EVIDENCE | PASS via regression/local contracts |
| TEST-MONTOYA-NEG-005 | Conflicting tenant | CONFLICTING_EVIDENCE | PASS locally |
| TEST-MONTOYA-NEG-006 | Unknown resource | UNKNOWN; no fabricated resource | PASS via regression/local contracts |
| TEST-MONTOYA-NEG-007 | Binary response | Opaque preservation | PASS locally |
| TEST-MONTOYA-NEG-008 | Compressed response | Opaque preservation; decoding not claimed | PASS locally |
| TEST-MONTOYA-NEG-009 | Large body | REJECTED | PASS locally |
| TEST-MONTOYA-NEG-010 | Out-of-scope target | BLOCKED | PASS locally |
| TEST-MONTOYA-NEG-011 | Unsupported content type | UNKNOWN/PARTIAL without fabricated semantics | PASS for opaque handling |
| TEST-MONTOYA-NEG-012 | Burp shutdown/restart | Clean lifecycle required | BLOCKED / UNVERIFIED in real Burp |
| TEST-MONTOYA-NEG-013 | Extension reload | Clean lifecycle required | BLOCKED / UNVERIFIED in real Burp |
| TEST-MONTOYA-NEG-014 | Duplicate request | Single correlated completion | PASS locally |
| TEST-MONTOYA-NEG-015 | Credential-redaction verification | No raw credential persisted | PASS locally; real Burp path UNVERIFIED |

## Sprint 3 reconnaissance test matrix

### Deterministic core/adapter coverage

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S3-ID-001 | Tenant semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-ID-002 | User semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-ID-003 | Resource semantic identifier | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-001 | Tenant query classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-002 | Pagination classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-PARAM-003 | Owner classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-001 | Tenant header classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-002 | Routing header classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-HEADER-003 | Authentication header sensitivity | Sprint3CoreTestSuite | PASS |
| TEST-S3-VERSION-001 | Path version discovery | Sprint3CoreTestSuite | PASS |
| TEST-S3-VERSION-002 | Query/header version discovery | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-001 | Brace/colon/angle template grammar | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-002 | Wildcard classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-ROUTE-003 | Framework template equivalence | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-001 | OpenAPI 3 JSON import | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-002 | Common YAML subset import | Sprint3CoreTestSuite | PASS |
| TEST-S3-OPENAPI-003 | OpenAPI/traffic correlation | Sprint3CoreTestSuite | PASS |
| TEST-S3-DRIFT-001 | Undocumented parameter drift | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-001 | Resource/owner/tenant semantic extraction | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-002 | Dynamic field identification | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-003 | Soft-200 denial semantic classification | Sprint3CoreTestSuite | PASS |
| TEST-S3-RESP-004 | Reordered/dynamic same-resource match | Sprint3CoreTestSuite | PASS |
| TEST-S3-COVERAGE-001 | Context coverage model | Sprint3CoreTestSuite | PASS |
| TEST-S3-PLAN-001 | Dry-run dispatch remains zero | Sprint3CoreTestSuite + AdapterSuite | PASS |
| TEST-S3-PLAN-002 | Candidate test-family recommendation | Sprint3CoreTestSuite | PASS |
| TEST-S3-MATRIX-001 | Authorization matrix domain | Sprint3CoreTestSuite | PASS |
| TEST-S3-FINGERPRINT-001 | Finding fingerprint deterministic | Sprint3CoreTestSuite | PASS |
| TEST-S3-DIFFMODEL-001 | DifferentialTest domain | Sprint3CoreTestSuite | PASS |
| TEST-S3-ADAPTER-001 | Recon stored from traffic pipeline | Sprint3AdapterTestSuite | PASS |
| TEST-S3-ADAPTER-002 | OpenAPI loaded into pipeline | Sprint3AdapterTestSuite | PASS |
| TEST-S3-ADAPTER-003 | Unknown parameter remains UNKNOWN | Sprint3AdapterTestSuite | PASS |
| EXP-RECON-001 | 10-operation local ground-truth reconnaissance | Sprint3LabExperiment | PASS local; Burp UNVERIFIED |
| EXP-ID-001 | Six labeled semantic identifiers | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-ROUTE-001 | Five route-equivalence labels | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-RESP-001 | FP semantic fixtures | Sprint3MetricsExperiment | PASS fixture scope |
| EXP-CONTEXT-001 | Context coverage/evidence completeness | Sprint3MetricsExperiment | MEASURED |

### Sprint 3 live runtime requirements

| Test ID | Requirement | Current state |
|---|---|---|
| TEST-S3-BURP-001 | Real Burp traffic produces S3 recon record | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-002 | Real Burp UI shows Parameters/Identities/Tenants/Resources/Routes/Graph | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-003 | OpenAPI import/correlation exercised in Burp | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-004 | Dry-run plan visible with zero dispatch in Burp | BLOCKED / UNVERIFIED |
| TEST-S3-BURP-005 | Context/graph reconstruction matches ACRA-Lab ground truth through Burp | BLOCKED / UNVERIFIED |

The runtime rows remain blocked because a Burp installation and official Maven dependency-resolution environment are unavailable. Local contract stubs are not substituted for Burp evidence.

## Sprint 4 controlled localhost execution matrix

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S4-LOCAL-001 | Concrete transport accepts existing request model and captures live localhost response/timing | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-002 | External/non-LAB target rejected by localhost transport | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-003 | Method/header/body preservation through ACRA-Lab echo | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-004 | Secure expected-ALLOW control (`User-A → Document-A`) | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-005 | Secure expected-DENY control (`User-A → Document-B`) | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-006 | One resource mutation produces semantic `EXPECTED_CHANGE` | `EXP-EXEC-001` | PASS |
| TEST-S4-LOCAL-007 | Vulnerable synthetic mismatch retained as observation, not finding | `EXP-EXEC-001` | PASS, differential `UNEXPECTED_CHANGE` |
| TEST-S4-LOCAL-008 | HTTP 200 application denial normalized semantically | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-009 | Dynamic timestamp/request ID + formatting/order variation not treated as semantic change | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-010 | Live response cookie and serialized request credentials remain redacted in evidence | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-LOCAL-011 | Timeout maps to operational `TIMEOUT` without observation | `Sprint4LocalhostIntegrationTestSuite` | PASS |
| TEST-S4-SAFE-012 | Pre-dispatch request equivalence: valid mutation dispatches once; invalid and type/location-contaminated mutations dispatch zero times | `Sprint4EngineSecurityTestSuite` | PASS, 11 focused assertions |
| TEST-S4-LOCAL-013 | Separate executor instances receive distinct process-local execution IDs | `Sprint4LocalhostIntegrationTestSuite` | PASS after verified repair |
| TEST-S4-ARCH-001 | Active network client isolated to loopback LAB transport | `scripts/architecture-sprint4.sh` | PASS |
| EXP-EXEC-001 | Live controlled localhost baseline/control/mutation/evidence pipeline | `research/EXP-EXEC-001.md` | PASS local |
| TEST-S4-GRAPH-001 | Completed observation hydrates the existing Security Context Graph through `TestExecutor` | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-002 | Relationship provenance traces execution, test, observation and originating evidence | `Sprint4GraphIntegrationTestSuite` + live acceptance | PASS |
| TEST-S4-GRAPH-003 | Unknown identity, tenant, resource, owner or policy prevents graph mutation | `Sprint4GraphIntegrationTestSuite` | PASS, `INCONCLUSIVE` |
| TEST-S4-GRAPH-004 | Configured/observed resource, owner and tenant conflicts remain distinct | `Sprint4GraphIntegrationTestSuite` | PASS, `CONFLICTING_CONTEXT` |
| TEST-S4-GRAPH-005 | Identical observation hydration is idempotent | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-006 | Replay creates fresh execution/observation/evidence identities and retains history | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-007 | Forged provenance and cross-project hydration are blocked | `Sprint4GraphIntegrationTestSuite` | PASS |
| TEST-S4-GRAPH-008 | Raw bearer/password/API-key/session secrets are absent from observations, evidence and graph serialization | `Sprint4GraphIntegrationTestSuite` + live acceptance | PASS |
| TEST-S4-GRAPH-009 | Conflicting node identity cannot partially mutate graph nodes, edges or evidence | `Sprint4GraphIntegrationTestSuite` | PASS, exact whole-graph state preserved |
| TEST-S4-GRAPH-010 | Live localhost observation hydrates graph through normal executor path | `Sprint4LocalhostIntegrationTestSuite` | PASS; execution `S4-EXEC-00000002` |

The verified graph/local results remain: graph integration **87 PASS**, core **54 PASS**, engine security **52 PASS**, localhost **60 PASS**, core regression **37 PASS**, Sprint 3 core **47 PASS** and Sprint 3 adapter **11 PASS**. Phase 3 adds complete mode/profile/selection, queue, safety/backoff, resolver, coverage/efficiency, active UI source/binding and research-software verification below. Broad FP/FN and performance metrics remain NOT MEASURED; real Burp and exact JDK 21 execution remain deferred/unverified.

## Sprint 4 Phase 3 product-completion matrix

| Test ID | Capability | Evidence | Result |
|---|---|---|---|
| TEST-S4-PROFILE-001 | Five profiles alter contracts, mutations, comparison, budgets, evidence and safety | `Sprint4ProductCompletionTestSuite` | PASS |
| TEST-S4-SELECTION-001 | ALL/AUTOMATIC/USER_SELECTED/HYBRID exact planner behavior | product suite | PASS |
| TEST-S4-MODE-001 | Beginner/Professional/Researcher/Expert real configuration mapping | product suite | PASS |
| TEST-S4-QUEUE-001 | Eight states and start/pause/resume/stop/retry/skip/rerun | product suite | PASS |
| TEST-S4-QUEUE-002 | Stable ordering, deduplication and complete/failed/missing dependencies | product suite | PASS |
| TEST-S4-CONSENT-001 | Authorized local automatic policy; external/unauthorized/destructive disabled | product suite | PASS |
| TEST-S4-BACKOFF-001 | 429/503/Retry-After bounded | product suite | PASS |
| TEST-S4-OPFAIL-001 | Connection retry bounded and no Observation | product suite | PASS |
| TEST-S4-RESOLVER-001 | Precedence, equal-rank conflict and absent policy | product suite | PASS |
| TEST-S4-INCONCLUSIVE-001 | Unknown expected policy forces `INCONCLUSIVE` | product suite | PASS |
| TEST-S4-COVERAGE-001 | Endpoint/context/resource/mutation/test accounting | product suite | PASS |
| TEST-S4-EFFICIENCY-001 | Candidate/dedup/scope/budget/executed counts | product suite | PASS; optimization NOT MEASURED |
| TEST-S4-RESEARCH-SW-001 | TP/TN/FP/FN records and safe precision/recall/F1 denominators | product suite | PASS; campaign NOT RUN |
| TEST-S4-UI-001 | Eight active views installed in existing suite tab | `Sprint4UiTestSuite` | PASS |
| TEST-S4-UI-002 | Mode/profile/selection/limits/STOP ALL bind to backend workspace | UI suite | PASS |
| TEST-S4-UI-003 | Active execution unavailable without explicitly injected executor | UI suite | PASS |
| TEST-S4-WORKSPACE-001 | Explicitly configured workspace plan → queue → existing executor → result/coverage | `Sprint4ProductCompletionTestSuite` | PASS |
| TEST-S4-BURP-ACTIVE-001 | Passive context import and authorized local executor provisioning in real Burp | none | DEFERRED; default remains safely disabled |

NEWLY EXECUTED Phase 3 total: **558 represented assertions** across graph 87, S4 core 54, S4 security 52, S4 product 132, localhost 60, UI 26, core 37, S3 core 47, S3 adapter 11 and S2 52. All core and extension sources compile with `--release 21 -Xlint:all -Werror` using OpenJDK 26.0.1. Exact JDK 21 runtime is UNVERIFIED; Maven is BLOCKED; broad research/performance metrics are NOT MEASURED.


### Sprint 7 workflow authorization

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-WORKFLOW-FOUNDATION-001 | transition policy / role / approval / separation / terminal handling | Sprint7WorkflowAuthorizationFoundationTestSuite | PASS |
| TEST-S7-WORKFLOW-BINDING-001 | SHA-256-only token-context binding and raw-token rejection | Sprint7WorkflowAuthorizationFoundationTestSuite | PASS |
| TEST-S7-WORKFLOW-DELEGATION-001 | active vs expired S6 delegation reuse | Sprint7WorkflowAuthorizationFoundationTestSuite | PASS |
| TEST-S7-WORKFLOW-CONFLICT-001 | allow/deny conflict and explicit precedence | Sprint7WorkflowAuthorizationFoundationTestSuite | PASS |
| TEST-S7-ASSESS-001 | WorkflowAuthorizationResolution → WorkflowTransitionAssessment | Sprint7WorkflowAssessmentIntegrationTestSuite | PASS |
| TEST-S7-FINDING-001 | evidence/provenance-gated FindingCandidate projection | Sprint7WorkflowAssessmentIntegrationTestSuite | PASS |
| TEST-S7-RISK-001 | deterministic workflow risk prioritization | Sprint7WorkflowAssessmentIntegrationTestSuite | PASS |
| TEST-S7-PROVENANCE-001 | cross-project ownership mismatch stays INCONCLUSIVE | Sprint7WorkflowAssessmentIntegrationTestSuite | PASS |
| TEST-S7-DATA-MIN-001 | raw token and token fingerprint minimized from downstream result | Sprint7WorkflowAssessmentIntegrationTestSuite | PASS |
| TEST-S7-GT-001 | ten-case controlled localhost workflow ground-truth fixture | GT-S7-WORKFLOW-AUTHORIZATION.json + integration suite | PASS |

### Sprint 7 Phase 3 — controlled active workflow execution

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-PLAN-001 | explicit workflow policy generates one automatic `TRANSITION` seed | `Sprint7WorkflowPlannerExecutionIntegrationTestSuite` | PASS |
| TEST-S7-MUTATION-001 | single-variable target-state body mutation uses `WORKFLOW_TRANSITION` | same suite | PASS |
| TEST-S7-SAFETY-001 | mutation remains `STATE_CHANGING`; DELETE is not auto-generated; execution remains authorized localhost LAB only | planner factory + existing S4 safety path | PASS |
| TEST-S7-LAB-SECURE-001 | secure fixture denies direct `DRAFT → APPROVED` SUBMIT bypass | live controlled execution | PASS — DENY / `EXPECTED_CHANGE` |
| TEST-S7-LAB-VULN-001 | deliberately vulnerable fixture permits same bypass | live controlled execution | PASS — ALLOW / `UNEXPECTED_CHANGE` |
| TEST-S7-CONFLICT-002 | conflicting workflow resolution creates no executable seed | integration suite | PASS |
| TEST-S7-SECRET-002 | raw synthetic bearer token absent from serialized generated test and mutation context | integration suite | PASS |
| TEST-S7-REGRESSION-001 | retained Sprint 6 foundation after S7 active-validation changes | GitHub Actions run `35958482546` | PASS |

Phase 3 focused suite: **17 assertions PASS**. GitHub Actions run `35958482546` also passed the Sprint 7
foundation, lab contract and retained Sprint 6 regression. These are controlled localhost fixture results only.

### Sprint 7 Phase 4 — workflow coverage and product UI

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-COVERAGE-001 | deterministic context identity and matrix ordering | `Sprint7WorkflowCoverageTestSuite` | PASS |
| TEST-S7-COVERAGE-002 | resolved vs unresolved context accounting | same suite | PASS |
| TEST-S7-COVERAGE-003 | planning/observation ratios do not infer unobserved coverage | same suite | PASS |
| TEST-S7-COVERAGE-004 | generated transition advances POLICY_ONLY → PLANNED | live planner/execution suite | PASS |
| TEST-S7-COVERAGE-005 | completed secure execution advances to OBSERVED with evidence | live planner/execution suite | PASS |
| TEST-S7-COVERAGE-006 | secure/vulnerable runs retain distinct execution/observation references | live planner/execution suite | PASS |
| TEST-S7-COVERAGE-007 | policy-resolution drift for same coverage identity rejected | coverage entry/tracker contract | PASS |
| TEST-S7-UI-001 | Workflow top-level product area installed | `Sprint7WorkflowUiTestSuite` | PASS |
| TEST-S7-UI-002 | Workflow Map / Transition Matrix / Policy Conflicts / Coverage views | same suite | PASS |
| TEST-S7-UI-003 | UI preserves candidate and coverage-vs-severity boundaries | same suite | PASS |
| TEST-S7-UI-REG-001 | retained Sprint 4 UI after S7 product integration | run `35959256852` | PASS |
| TEST-S7-UI-REG-002 | retained Sprint 6 Authorization UI after S7 product integration | run `35959256852` | PASS |
| TEST-S7-MAVEN-001 | real extension module test compilation on Java 21 | run `35959256852` | PASS |

Authoritative Phase 4 UI gate: GitHub Actions run `35959256852`.

### Sprint 7 Phase 5 — workflow reporting/export

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-REPORT-001 | deterministic workflow report identity/model | `Sprint7WorkflowReportingExportTestSuite` | PASS |
| TEST-S7-REPORT-002 | policy/resolution/assessment/coverage/candidate/risk aggregation | same suite | PASS |
| TEST-S7-REPORT-003 | canonical JSON + stable SHA-256 | same suite | PASS |
| TEST-S7-REPORT-004 | deterministic Markdown review report | same suite | PASS |
| TEST-S7-REPORT-005 | raw-secret exclusion / redaction | same suite | PASS |
| TEST-S7-REPORT-006 | `confirmedFindingCount = 0` review-only boundary | same suite | PASS |
| TEST-S7-REPORT-007 | coverage lifecycle exported separately from severity/finding state | same suite | PASS |
| TEST-S7-REPORT-008 | Reporter plugin adapter | `S7WorkflowJsonReporter` | PASS |
| TEST-S7-REPORT-UI-001 | Workflow Report + JSON Export product views | `Sprint7WorkflowUiTestSuite` | PASS |
| TEST-S7-REPORT-CI-001 | report JSON / digest / Markdown artifact bundle | GitHub Actions run `35960316132` | PASS |

Authoritative Phase 5 gate: GitHub Actions run `35960316132`.

### Sprint 7 Phase 6 — security hardening and performance

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-SEC-TOKEN-001 | reject raw token as workflow token-binding material | `Sprint7WorkflowSecurityHardeningTestSuite` | PASS |
| TEST-S7-SEC-TOKEN-002 | reject raw token as request token-context fingerprint | same suite | PASS |
| TEST-S7-SEC-DRIFT-001 | principal/context drift blocks target-state-only mutation | same suite | PASS |
| TEST-S7-SEC-BODY-001 | repeated source-state value blocks ambiguous auto-mutation | same suite | PASS |
| TEST-S7-SEC-DESTRUCTIVE-001 | DELETE workflow transitions are not auto-generated | same suite | PASS |
| TEST-S7-SEC-CONFLICT-001 | conflicting policy generates no active workflow seed | same suite | PASS |
| TEST-S7-SEC-COVERAGE-001 | policy-resolution drift for same coverage identity fails closed | same suite | PASS |
| TEST-S7-SEC-COVERAGE-002 | unplanned execution cannot receive workflow coverage credit | same suite | PASS |
| TEST-S7-SEC-SERIAL-001 | raw token/fingerprint absent from generated workflow test serialization | same suite | PASS |
| TEST-S7-PERF-001 | 100 workflow contexts resolve/cover/report | `Sprint7WorkflowPerformanceObservationTestSuite` | PASS |
| TEST-S7-PERF-002 | 1,000 workflow contexts resolve/cover/report | same suite | PASS |
| TEST-S7-PERF-003 | 10,000 workflow contexts resolve/cover/report | same suite | PASS |

Authoritative Phase 6 gate: GitHub Actions run `35960583660`. Timing and memory values are observational only.


### Sprint 7 final closure

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S7-FINAL-001 | exact Java 21 S7/core retained regression | `scripts/verify-sprint7-final.sh` / run `35960826621` | PASS |
| TEST-S7-FINAL-002 | official Maven package | run `35960826621` | PASS |
| TEST-S7-FINAL-003 | retained Sprint 2 local-contract regression | run `35960826621` | PASS |
| TEST-S7-FINAL-004 | retained Sprint 3 local-contract regression | run `35960826621` | PASS |
| TEST-S7-FINAL-005 | retained S4/S6/S7 UI regressions | run `35960826621` | PASS |
| TEST-S7-PACKAGE-001 | deterministic source ZIP + manifest + SHA-256 | `package-sprint7.sh` | PASS |
| TEST-S7-PACKAGE-002 | safe paths / clean extraction / per-file equality | run `35960826621` | PASS — 791 entries, 0 unsafe paths |
| TEST-S7-BURP-001 | real Burp desktop runtime | separate runtime lane | UNVERIFIED / DEFERRED |

Closure candidate checkpoint SHA-256 from run `35960826621`: `0f69ce2e770dc5010f17fe00474455d18f34de11e02460889d428a87a7af9b4d`. The canonical final-status digest is emitted by the external sidecar after the final-status documentation commit is verified.


### Sprint 8 — routing normalization and authorization-path intelligence

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-NORM-001 | ordered RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION trace | `Sprint8RoutingNormalizationFoundationTestSuite` | PASS |
| TEST-S8-NORM-002 | canonical-equivalent representation change | same suite | PASS |
| TEST-S8-NORM-003 | directly observed canonical route divergence | same suite | PASS |
| TEST-S8-NORM-004 | missing-stage attribution remains INCONCLUSIVE | same suite | PASS |
| TEST-S8-NORM-005 | duplicate stage / missing provenance / query leakage fail closed | same suite | PASS |
| TEST-S8-BOUNDARY-001 | stable adjacent route + authorization context | `Sprint8AuthorizationPathDifferentialTestSuite` | PASS |
| TEST-S8-BOUNDARY-002 | route-only normalization differential | same suite | PASS |
| TEST-S8-BOUNDARY-003 | HTTP method change | same suite | PASS |
| TEST-S8-BOUNDARY-004 | host and API-version changes | same suite | PASS |
| TEST-S8-BOUNDARY-005 | authorization-only boundary change | same suite | PASS |
| TEST-S8-BOUNDARY-006 | combined route + authorization differential | same suite | PASS |
| TEST-S8-BOUNDARY-007 | stage gaps remain INCONCLUSIVE | same suite | PASS |
| TEST-S8-BOUNDARY-008 | unknown authorization evidence remains INCONCLUSIVE | same suite | PASS |
| TEST-S8-BOUNDARY-009 | host/path input hardening | same suite | PASS |
| TEST-S8-REG-001 | retained Sprint 3 route/core behavior | run `35965612498` | PASS |
| TEST-S8-REG-002 | retained Sprint 7 foundation | run `35965612498` | PASS |

Authoritative Phase 2 gate: GitHub Actions run `35965612498`.


### Sprint 8 Phase 3 — controlled route-equivalence execution

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-LAB-001 | controlled routing ground truth | `GT-S8-ROUTING-NORMALIZATION.json` | PASS — 3 cases |
| TEST-S8-ACTIVE-001 | canonical → duplicate-separator URI mutation | `Sprint8ControlledRoutingDifferentialTestSuite` | PASS |
| TEST-S8-ACTIVE-002 | equivalent-route mutation uses existing `UriMutationAdapter` | same suite | PASS |
| TEST-S8-ACTIVE-003 | explicit localhost scope includes both route representations | existing `HardScopeGuard` + controlled target | PASS |
| TEST-S8-ACTIVE-004 | secure fixture preserves viewer DENY | run `35966112820` | PASS — DENY / `NO_CHANGE` |
| TEST-S8-ACTIVE-005 | deliberately vulnerable fixture exposes normalization authorization mismatch | run `35966112820` | PASS — ALLOW / `UNEXPECTED_CHANGE` |
| TEST-S8-ACTIVE-006 | raw bearer material excluded from serialized routing test | focused suite | PASS |
| TEST-S8-REG-003 | retained Sprint 3 core | run `35966112820` | PASS — 47 tests |
| TEST-S8-REG-004 | retained Sprint 7 foundation through performance suites | run `35966112820` | PASS |

Phase 3 focused active suite: **10 assertions PASS**. Authoritative gate: GitHub Actions run `35966112820`.

### Sprint 8 Phase 4 — routing assessment and provenance

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-ASSESS-001 | routing change + expected DENY + observed ALLOW → assessment candidate | controlled live suite | PASS |
| TEST-S8-ASSESS-002 | secure route-equivalence control → NO_VIOLATION | controlled live suite | PASS |
| TEST-S8-FINDING-001 | provenance-verified vulnerable routing case → FindingCandidate CANDIDATE | `Sprint8ControlledRoutingDifferentialTestSuite` | PASS |
| TEST-S8-FINDING-002 | secure routing case → FindingCandidate REJECTED | same suite | PASS |
| TEST-S8-PROVENANCE-001 | execution/test/observation/project ownership validation | existing `EvidenceReferenceValidator` + live store | PASS |
| TEST-S8-PROVENANCE-002 | cross-project provenance mismatch → INCONCLUSIVE | same suite | PASS |
| TEST-S8-REVIEW-001 | candidate rationale preserves non-confirmed review boundary | same suite | PASS |
| TEST-S8-REG-005 | retained Sprint 3 core | run `35969974964` | PASS — 47 tests |
| TEST-S8-REG-006 | retained Sprint 7 foundation through performance suites | run `35969974964` | PASS |

Phase 4 controlled suite: **22 assertions PASS**. Authoritative gate: GitHub Actions run `35969974964`.

### Sprint 8 Phase 5 — product workspace and Burp UI

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-UI-001 | Routing top-level product area installed | `Sprint8RoutingUiTestSuite` | PASS |
| TEST-S8-UI-002 | Stage Traces / Boundary Matrix / Assessments / Candidates views | same suite | PASS |
| TEST-S8-UI-003 | routing workspace exposes deterministic read-only snapshot | same suite | PASS |
| TEST-S8-UI-004 | candidate != confirmed vulnerability language retained | same suite | PASS |
| TEST-S8-UI-005 | routing divergence != vulnerability severity language retained | same suite | PASS |
| TEST-S8-UI-006 | unknown processing stages remain unknown | same suite | PASS |
| TEST-S8-MAVEN-001 | extension Maven test compilation | run `35970306700` | PASS |
| TEST-S8-UI-REG-001 | retained Sprint 4 UI | run `35970306700` | PASS — 26 |
| TEST-S8-UI-REG-002 | retained Sprint 6 Authorization UI | run `35970306700` | PASS — 27 |
| TEST-S8-UI-REG-003 | retained Sprint 7 Workflow UI | run `35970306700` | PASS — 20 |

Sprint 8 Routing UI: **17 assertions PASS**. Authoritative gate: GitHub Actions run `35970306700`.

### Sprint 8 Phase 6 — deterministic reporting/export

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-REPORT-001 | deterministic routing report identity/model | `Sprint8RoutingReportingExportTestSuite` | PASS |
| TEST-S8-REPORT-002 | routing/boundary/assessment/candidate aggregation | same suite | PASS |
| TEST-S8-REPORT-003 | canonical JSON + stable SHA-256 | same suite | PASS |
| TEST-S8-REPORT-004 | deterministic Markdown review report | same suite | PASS |
| TEST-S8-REPORT-005 | raw-secret exclusion / redaction | same suite | PASS |
| TEST-S8-REPORT-006 | `confirmedFindingCount = 0` review-only boundary | same suite | PASS |
| TEST-S8-REPORT-007 | Reporter plugin adapter | `S8RoutingJsonReporter` | PASS |
| TEST-S8-REPORT-UI-001 | Routing Report + JSON Export product views | `Sprint8RoutingUiTestSuite` | PASS |
| TEST-S8-REDACT-001 | embedded Authorization text does not truncate later JSON fields | shared `UniversalRedactor` regression + report suite | PASS |
| TEST-S8-UI-REG-004 | retained S4/S6/S7 UI after report integration | run `35970917885` | PASS |

Routing reporting/export: **19 assertions PASS**. Routing UI: **23 assertions PASS**.
Authoritative Phase 6 gate: GitHub Actions run `35970917885`.

### Sprint 8 Phase 7 — security hardening and performance

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-SEC-PATH-001 | query/fragment leakage rejected at route-stage boundary | `Sprint8RoutingSecurityHardeningTestSuite` | PASS |
| TEST-S8-SEC-PROVENANCE-001 | provenance-free route stage rejected | same suite | PASS |
| TEST-S8-SEC-HOST-001 | scheme/path/user-info host values rejected | same suite | PASS |
| TEST-S8-SEC-STAGE-001 | duplicate processing stage rejected | same suite | PASS |
| TEST-S8-SEC-INCONCLUSIVE-001 | unknown auth / stage gaps fail closed | same suite | PASS |
| TEST-S8-SEC-CANDIDATE-001 | route-stable auth change not promoted as routing candidate | same suite | PASS |
| TEST-S8-SEC-MUTATION-001 | non-equivalent/authority/fragment URI mutations rejected | same suite | PASS |
| TEST-S8-SEC-REDACT-001 | embedded bearer redaction preserves subsequent report fields | same suite | PASS |
| TEST-S8-SEC-REPORT-001 | report confirmation count remains zero | same suite | PASS |
| TEST-S8-PERF-001 | 100 routing contexts analyze/report | `Sprint8RoutingPerformanceObservationTestSuite` | PASS |
| TEST-S8-PERF-002 | 1,000 routing contexts analyze/report | same suite | PASS |
| TEST-S8-PERF-003 | 10,000 routing contexts analyze/report | same suite | PASS |

Authoritative Phase 7 gate: GitHub Actions run `35971212640`. Timing and memory values are observational only.


### Sprint 8 final closure

| Test ID | Coverage | Evidence | Result |
|---|---|---|---|
| TEST-S8-FINAL-001 | exact Java 21 Sprint 8 + retained S6/S7 verification | `scripts/verify-sprint8-final.sh` / run `35971753523` | PASS |
| TEST-S8-FINAL-002 | official Maven package | run `35971753523` | PASS |
| TEST-S8-FINAL-003 | retained Sprint 2 local-contract regression | run `35971753523` | PASS — 52 tests |
| TEST-S8-FINAL-004 | retained Sprint 3 core/adapter regression | run `35971753523` | PASS — 47 + 11 |
| TEST-S8-FINAL-005 | retained S4/S6/S7/S8 UI regressions | run `35971753523` | PASS — 26 / 27 / 20 / 23 |
| TEST-S8-PACKAGE-001 | deterministic source ZIP + manifest + SHA-256 | `package-sprint8.sh` | PASS |
| TEST-S8-PACKAGE-002 | safe paths / duplicate entries / clean extraction / per-file equality | run `35971753523` | PASS — 836 entries, 0 unsafe, 0 duplicates |
| TEST-S8-BURP-001 | real Burp desktop runtime | separate runtime lane | UNVERIFIED / DEFERRED |

Closure-candidate source ZIP SHA-256:
`e063217d3c8795a59ce1cd7e052a2c9b0475a86836239973ef1d470a6c627af7`.
