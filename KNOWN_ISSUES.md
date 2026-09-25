# Known Issues and Technical Debt

## Sprint 13 Phase 7 real Burp/Montoya limitations — 2026-09-25

- Real Burp Suite Community Edition 2026.7.3 / Montoya runtime validation is complete for controlled localhost traffic.
- The strengthened canonical evaluation workflow is `36180569483` at `e24a91297bf33bfe18a47453e8c191432b496b20`.
- Two independent post-freeze Burp runs each measured INIT=1, REQUEST=2, RESPONSE=2, PROCESSED=2 and expected context reconstruction=2/2.
- The result covers the pinned Burp 2026.7.3 JAR and Montoya 2026.7 compile contract only; arbitrary Burp versions remain unverified.
- The evaluation is headless CI runtime evidence. The ACRA Swing suite tab was constructed/registered successfully, but visual/manual desktop UX behavior was not independently inspected by a human in this experiment.
- The packaged Findings & Reproduction surface, publication-eligibility guard and absence of automatic issue-publication wiring were verified. Actual publication of a confirmed Burp issue was intentionally not exercised.
- Active ACRA request execution remained disabled. Phase 7 validates passive observation/integration, not active vulnerability execution.
- In probe mode only, passive collection is widened to ALL_TRAFFIC to avoid dependency on interactive Burp Target-scope configuration. Normal defaults remain IN_SCOPE_ONLY.
- The runtime evidence probe is opt-in and intentionally excludes headers, credentials, tokens, cookies, request bodies and response bodies.
- The evaluation used synthetic localhost identities/resources and does not establish production scanner accuracy or external-target effectiveness.
- Phase 8 must use only an explicitly authorized target with scope documented before any traffic is generated.
- Phase 7 source/runtime evidence is frozen and must not be reused as a tuning target.

## Sprint 13 Phase 6B actual local framework runtime limitations — 2026-09-25

- Phase 6B launched real localhost FastAPI/Uvicorn, Flask, Express/Node.js and Spring Boot/Java 21 applications in CI.
- Untouched runtime evaluation measured 64/64 successful HTTP responses, 64/64 dimensions and 64/64 governed dispositions.
- The perfect controlled result applies only to the exact runtime/dependency versions, endpoints and authorization semantics exercised by this CI experiment.
- The evaluation does not establish arbitrary framework-version compatibility, middleware compatibility, reverse-proxy behavior or production deployment compatibility.
- The first development runtime pass exposed a routing/RBAC fixture ambiguity; this was corrected before the development freeze and before untouched evaluation.
- The Phase 6A normalizer remains an explicit finite alias map; unseen serialization conventions remain a compatibility risk.
- Real Burp Suite desktop/Montoya initialization, proxy capture and extension UI/runtime behavior remain unverified.
- External authorized-target validation remains unperformed.
- Phase 6B development/evaluation servers, corpora, policies and runners are frozen evidence and must not be reused as later tuning targets.
- Phase 7 should validate Burp only against controlled localhost targets before any external-target work.

## Sprint 13 Phase 6A cross-framework-shaped limitations — 2026-09-25

- Phase 6A measured framework-shaped snapshots, not actual FastAPI, Flask, Express or Spring runtime traffic.
- Raw evaluation measured 60/64 correct dimensions and only 32/64 correct governed dispositions.
- FastAPI-shaped and Flask-shaped snake_case inputs matched the canonical schema without normalization: 16/16 dimension and 16/16 disposition each.
- Express-shaped and Spring-shaped camelCase inputs measured 14/16 raw dimensions and 0/16 raw dispositions each.
- The frozen framework-neutral normalizer restored 64/64 dimensions and 64/64 dispositions on the untouched snapshot evaluation.
- The perfect normalized snapshot result does not establish runtime router, middleware, serializer, dependency-version or HTTP-server behavior.
- Framework labels are not consumed by the normalizer or reasoning stack; the current alias map is explicit and finite.
- Unseen serialization aliases outside the frozen alias map remain an unmeasured compatibility risk.
- Phase 6A development/evaluation data is frozen evidence and must not become a Phase 6B tuning target.
- Phase 6B must launch actual local framework runtimes and capture real HTTP responses before a runtime cross-framework claim.
- Real Burp desktop runtime and authorized external-target validation remain separate later gates.

## Sprint 13 Phase 5 governance limitations — 2026-09-25

- Governed actionable findings measured TP=8/TN=48/FP=0/FN=8 on the 64-case untouched internal evaluation: precision=1.000000, recall=.500000, F1=.666667.
- The eight positive cases not promoted to actionable findings were policy-gap cases and were routed to review; escalation coverage was 1.000000 and silent positive count was 0.
- Review load remains substantial in the constructed uncertainty-heavy corpus: 40/64 cases (62.5%) were review-required.
- The 62.5% review rate is not a production analyst-workload estimate.
- Policy-health status is explicitly supplied by the registry; Phase 5 does not automatically determine whether policy is stale/current.
- POLICY_GAP and ambiguity classification depend on configured policy-registry coverage/matching quality.
- The governance layer was evaluated on synthetic snapshot observations rather than real framework/runtime traffic.
- Phase 5 exact disposition accuracy (64/64) does not establish production accuracy or independent replication.
- Cross-framework serialization, routing, middleware and request-body differences remain unmeasured.
- Real Burp desktop runtime and authorized external-target behavior remain unverified.
- The Phase 5 development and untouched evaluation corpora are frozen evidence and must not be used as tuning targets.
- Phase 6 must test the same governance contract across independently structured framework fixtures before any cross-framework claim.

## Sprint 13 Phase 4 adversarial / base-rate findings — 2026-09-25

- G1 measured TP=8/TN=49/FP=39/FN=0 on the 96-case negative-heavy stress corpus: precision=.170213, recall=1.000000, specificity=.556818, FPR=.443182, F1=.290909, MCC=.307860.
- Locked A7 was materially worse: TP=8/TN=20/FP=68/FN=0, precision=.105263, specificity=.227273, FPR=.772727.
- Explicit configured ALLOW controls remained clean: 0 false positives across 40 cases.
- Missing policy produced 12/16 false positives because UNKNOWN falls back to locked A7.
- Ambiguous policy produced 12/16 false positives for the same fallback reason.
- Stale policy produced 8/8 false positives because outdated configured policy returned decisive DENY on legitimate operations.
- Incomplete context produced 7/8 false positives; six cases became UNKNOWN and some dimensions produced decisive DENY with insufficient context.
- Policy coverage was 58 decisive / 38 UNKNOWN. Expected-authorization correctness was 48/96 overall and 48/58 among decisive policy decisions.
- Current G1 alert disposition therefore conflates policy uncertainty/configuration quality with vulnerability evidence.
- At 1% projected prevalence, measured G1 sensitivity/specificity imply PPV=.022284 and ~438.75 false alerts per 1,000 observations. This is a mathematical projection, not an observed production rate.
- Phase 5 must introduce explicit POLICY_GAP, AMBIGUOUS_POLICY, STALE_POLICY, INCOMPLETE_CONTEXT and INCONCLUSIVE states before further external-validity expansion.
- Phase 4 stress data is now frozen evidence and must not be reused as a tuning target.
- Real-world prevalence, automatic policy extraction, cross-framework behavior, Burp desktop runtime and authorized external-target performance remain unverified.

## Sprint 13 Phase 3 policy-generalization limitations — 2026-09-25

- G1 measured TP=8/TN=16/FP=0/FN=0 on the 24-case untouched internal evaluation after algorithm freeze.
- The perfect fixture metrics apply only to explicit configured-policy semantics; they do not establish automatic policy extraction or real-world scanner accuracy.
- The evaluation registry is supplied by configuration. Misconfigured, incomplete, stale, or conflicting policy registries remain a separate product risk.
- Policy-engine UNKNOWN currently falls back to locked A7; large-scale UNKNOWN-policy behavior has not yet been base-rate stressed.
- The Phase 3 evaluation is small, synthetic, internally authored and deliberately structured across eight dimensions.
- The 8-positive / 16-negative evaluation is still far more balanced than real vulnerability prevalence; precision under realistic low-prevalence conditions is not measured.
- Cross-framework behavior, gateway transformations, opaque route naming, real Burp desktop operation and external authorized targets remain unverified.
- The development and untouched evaluation corpora are now frozen evidence and must not be reused for later tuning.
- Phase 4 must stress larger negative populations, policy ambiguity, policy absence, overlapping policies, stale policy, malformed policy and realistic prevalence.

## Sprint 13 Phase 2 dimension-discovery findings — 2026-09-25

- Automatic dimension discovery measured 31/32 correct across the frozen S12+S13 synthetic corpus: accuracy=.968750, macro-F1=.968254.
- The retained mismatch is the canonical Sprint 12 `/api/v1/s8/admin` routing control, inferred as RBAC because its observable response contains an explicit `required_role` signal and the canonical request has no routing anomaly.
- The 16/16 result on the S13 holdout is still small synthetic evidence and must not be presented as real-world dimension-classification accuracy.
- The inference engine is deterministic project-authored evidence scoring, not an independently trained or externally replicated model.
- Path/name semantics contribute evidence and may fail on APIs using different naming conventions, gateways, frameworks or opaque resource paths.
- Removing the supplied dimension did not remove the five Phase 1 policy-generalization false positives; policy semantics remain the next research problem.
- Phase 1 and Phase 2 evaluation corpora are now frozen evidence and must not be reused as tuning targets.
- Cross-framework, real Burp desktop and authorized external-target validation remain unverified.

## Sprint 13 Phase 1 held-out findings — 2026-09-25

- Locked A7 generalization on the 16-case held-out corpus measured TP=8/TN=3/FP=5/FN=0, precision=.615385, recall=1.000000, F1=.761905.
- The five false positives are legitimate policy controls absent from Sprint 12 calibration: platform-wide tenant administration, a new security-admin role, a valid DRAFT→PENDING requester transition, security-admin use of an equivalent duplicate-separator route, and an allowed `nickname` profile property.
- These failures demonstrate fixture-specific policy assumptions in the current locked A0-A7 rules. The original held-out result is frozen and must not be retroactively tuned.
- Sprint 13 Phase 1 still supplies the registered authorization dimension; automatic dimension discovery remains NOT MEASURED.
- The held-out fixture is separately authored within this repository, not independent third-party replication.
- The dataset remains small and balanced; no prevalence-adjusted precision, confidence interval, or production base-rate claim is made.
- Cross-framework validation, real Burp desktop execution and explicitly authorized external-target validation remain future gates.
- Any policy-generalization redesign must use a separate development/tuning corpus and a new untouched evaluation corpus rather than reusing this held-out set as a tuning target.

## Sprint 12 controlled-research limitations — 2026-09-25

- A0-A7 measurements come from a balanced 16-case synthetic localhost ACRA-Lab dataset only.
- The authorization dimension is supplied to the research runner. Automatic dimension discovery/classification accuracy is **not measured** by this campaign.
- Later ablations use fixture-specific policy semantics for the registered dimensions; this is a controlled component-ablation study, not a blind production scanner evaluation.
- A6 and A7 both record TP=8/TN=8/FP=0/FN=0 on this dataset. Those 1.0 precision/recall/F1 values must not be generalized to unseen APIs or treated as a production-accuracy guarantee.
- A1 produces no measured change over A0 on this fixture set; the dataset does not isolate identity-quality errors strongly enough to estimate identity-context value independently.
- The dataset is small and deliberately balanced. No statistical-power, confidence-interval, prevalence-adjusted precision or deployment base-rate claim is made.
- A7's correlation capability produces no additional classification change over A6 on these cases, so this campaign does not independently quantify correlation's incremental value.
- ACRA-Lab uses unsigned synthetic tokens as execution scaffolding; this is not production authentication evidence.
- No real Burp desktop runtime, external authorized target, independent third-party dataset or cross-framework fixture was exercised.
- The next research-strengthening lane should add held-out/blinded cases, independently authored fixtures, harder negative controls, automatic dimension discovery and authorized external validation before broader claims.
- Real Burp issue publication remains UNVERIFIED / DEFERRED.

## Sprint 11 residual validation and research debt — 2026-09-25

- Sprint 11 software is complete under the verified local/software boundary; final executable closure run `36064082001` passed.
- Real Burp desktop loading and `SiteMap.add(AuditIssue)` publication remain UNVERIFIED / DEFERRED; Sprint 11 verifies only the Montoya materialization contract and explicitly prevents automatic publication.
- `GT-S11-AUTHORIZATION-RESEARCH` is a synthetic localhost dataset. Its 16 oracle cases establish controlled ground-truth readiness, not real-world scanner accuracy.
- At the Sprint 11 closure, EXP-A0 through EXP-A7 were NOT_RUN and their metrics were NOT_MEASURED. Sprint 12 subsequently executed the campaign; the Sprint 11 dataset remains a frozen registration artifact and is intentionally not backfilled.
- Finding review workspace state is in-memory; durable multi-user persistence and concurrency control remain future product hardening.
- The current redaction model protects recognized credential/token forms and structurally excludes reviewer/reason fields from reproduction exports; arbitrary opaque secrets that do not match protected structures remain a general sanitization limitation.
- Sprint 11 performance values are controlled CI observations only, not benchmarks, SLOs or capacity guarantees.
- Real external-target authorization validation remains outside the Sprint 11 boundary.
- Sprint 12 executed the registered A0-A7 research design without backfilling metrics into the Sprint 11 source artifact.


## Sprint 7 residual validation debt — 2026-09-24

- Sprint 7 software is complete under the defined software boundary; the closure candidate was proven by GitHub Actions run `35960826621`.
- Real Burp desktop runtime/load/handler/UI validation remains UNVERIFIED / DEFERRED.
- Controlled secure/vulnerable ACRA-Lab workflow cases are localhost research fixtures and do not establish real-world scanner accuracy.
- ACRA-Lab uses synthetic unsigned authentication material and is not production authentication evidence.
- Workflow/authorization graph, evidence and product state remain in-memory where inherited from earlier sprints; durable persistence is future product hardening.
- Sprint 7 100/1k/10k timing and memory values are observational CI engineering measurements, not benchmarks, SLOs or capacity guarantees.
- FindingCandidate remains review-only; differential changes, coverage and internal risk scores do not automatically create confirmed vulnerabilities.
- Automatic state-changing workflow testing remains restricted to explicitly authorized loopback LAB execution and conservative one-variable transition mutations.
- Property-level authorization, routing normalization, OAuth/OIDC/session-refresh analysis, GraphQL/gRPC/WebSocket active validation, SARIF/Burp Issue export and external-target validation remain outside Sprint 7.
- Sprint 8 is NOT STARTED.

## Historical issues

## Sprint 6 closure updates — 2026-09-23

- Legacy S2/S3 zero-length test sources were repaired from exact Git history; Sprint 2 and Sprint 3 CI are green again.
- Legacy local Montoya-stub suites are intentionally excluded from the current official-Montoya Maven test compile; they execute under their preserved stub-contract verification scripts.
- Real Burp desktop runtime/load/handler/UI validation remains separate and unverified.
- S6 performance values are observational CI measurements, not release thresholds or JMH benchmarks.

## Sprint 6 current limitations — 2026-09-23

- `EXP-S6-TENANT-RBAC-001` metrics are from ten controlled localhost cases only; they are not real-world accuracy claims.
- ACRA-Lab authentication uses unsigned synthetic tokens and must not be treated as production authentication evidence.
- The first live S6 campaign exposed a shared-scope isolation defect; it is fixed and covered by regression tests.
- S6 policy state and graph/evidence persistence remain in-memory.
- Full UI/reporting/performance/final package closure is still pending.
- ROLE_COMPARISON active generation is now implemented for safe read-only endpoints using explicit contextRef substitution. State-changing role comparisons remain intentionally outside this automatic path until separate confirmation/transaction-safety policy is defined.
- Historical real Burp runtime validation remains separate.

## Current residual issues after S5 completion — 2026-09-23

- Historical Sprint 2/3 real Burp runtime validation remains BLOCKED / UNVERIFIED and is independent of S5 software completion.
- FindingCandidate remains a candidate state, not an automatically confirmed real-world vulnerability.
- Independent corroboration is explicit; the final S5 orchestrator does not manufacture independence from repeated same-execution evidence.
- Internal authorization risk score is deterministic ACRA prioritization, not CVSS. Impact facts must be supplied rather than inferred from names.
- ExecutionEvidenceStore persistence remains in-memory architecture inherited from S4; durable persistence is future product hardening.
- The S5 final closure suite is offline/synthetic evidence. It does not establish real-world scanner accuracy or production target safety.
- S6 is intentionally NOT STARTED in the S5 checkpoint.
- setup-java@v4 deprecation warnings remain CI maintenance debt; they do not invalidate the Java 21 verification result.

## Current S5 continuation — 2026-09-09

S5 SOFTWARE PARTIAL. Current matrix: `docs/sprints/sprint-05-final-software-closure.md`.

- The canonical S5-04 ZIP does not contain the claimed AuthorizationContextNormalizer/completeness/fact/assessment types. Endpoint and policy binding remain absent from AuthorizationContext.
- Tenant/workflow/property assessment, FindingCandidate, finding evaluator, severity and final orchestration software/tests are missing.
- Batch 1 adds store-backed evidence-reference resolution and fail-closed execution/test/project/replay checks, but the strict validator path cannot prove project ownership unless the store is explicitly configured with a project ID. Legacy evaluator/correlator calls without a validator remain compatibility paths and do not authenticate supplied references. Arbitrary opaque credentials in identifier fields remain undetectable by patterns alone.
- Existing assessment identifiers use 32-bit Java hashCode. Existing aggregate does not carry testIds or explicit per-property associations, project IDs or policy versions. Conservative correlation cannot establish independent corroboration.
- S5 replay lineage, UI/configuration and S5 lab ground truth/runner remain absent or unverified. No live S5 cases or new research metrics were run.
- Maven is BLOCKED and exact JDK 21 runtime is UNVERIFIED. The current continuation preserves the source VERSION/POM value rather than claiming a release.
- Batch 1 Java 21 compilation and tests were not newly executed: the only discovered compiler is OpenJDK 17.0.8, `javac` is absent from PATH, and Maven is unavailable.
- Batch 1 checkpoint packaging is verified independently; the archive contains 604 source/repository files with no unsafe paths or build/cache artifacts and matches clean extraction byte-for-byte. The exact SHA-256 is recorded in the external `.sha256` sidecar.
- Batch 2 policy review is explicit-fixture based. Missing tenant, workflow, property, transition, approval, role-separation, policy, or provenance facts remain inconclusive; no policy is inferred from field or role names. Java 21 compilation and Batch 2/affected tests remain unexecuted because only OpenJDK 17.0.8 is available and Maven is unavailable.
- Batch 2 checkpoint packaging is independently verified for 609 entries, safe paths, no build/cache artifacts, clean extraction equality, and a matching external SHA-256 sidecar.
- Batch 3 source integration now validates policy observation IDs against the existing evidence store; executable Batch 3 and affected regression tests remain unverified because Java 21 and Maven are unavailable.
- The final S5 continuation checkpoint is current-tree-only. The prior canonical baseline archive is unavailable, so baseline byte comparison remains BLOCKED even though clean extraction/file equality will be checked for the new archive.
- The package script regenerates the repository manifest for this continuation; older manifest-debt statements below are historical.
- The original S5-04 Downloads ZIP became unavailable after successful hash verification. Final canonical-baseline delta audit is BLOCKED; current-tree ZIP/clean-unpack verification remains independent and executable. The file-change journal records edits, not an unavailable baseline byte diff.


## Sprint 4 localhost integration

The prior mutation-dispatch equivalence gap is closed at checkpoint `S4-FINAL-SOFTWARE-AUDIT-2026-09-01`: `RequestEquivalenceGuard` is enforced before dispatch, and focused executor tests prove valid=one mutation transport, invalid=zero transports and contaminated=zero transports.

1. **Transport scope is deliberately narrow.** `LocalhostHttpTransport` supports authorized `LAB` loopback HTTP only; external hosts, non-LAB targets and redirects are outside this slice.
2. **Active graph hydration is in-memory.** Observation/evidence hydration into the existing `SecurityContextGraph` is now verified, including provenance, incomplete-context refusal, conflict preservation, duplicate/replay behavior, project isolation and atomic preflight. Graph/evidence persistence across process restarts remains absent.
3. **Vulnerable fixture mismatch is observation-only.** The independent expected-DENY control remains `DENY`, while the deliberately vulnerable controlled mutation returns `ALLOW` and is classified `UNEXPECTED_CHANGE`; Sprint 4 still does not promote that Observation to a confirmed finding.
4. **Execution IDs are process-local.** The verified cross-executor collision was fixed with a process-wide sequence, but IDs are not globally persistent across process restarts because active evidence persistence/database is still absent.
5. **FP/FN metrics are not measured.** The labelled fixture registry and metric software contracts now exist, but the broad campaign remains research-deferred.
6. **Synthetic lab authentication is not production authentication.** ACRA-Lab decodes synthetic token claims and does not cryptographically validate them.
7. **Legacy architecture gate required S4 scoping.** Passive core still forbids network clients; `scripts/architecture-sprint4.sh` separately enforces that the active network client is isolated to the localhost transport.
8. **Maven/Montoya and real Burp remain blocked/unverified.** Localhost evidence does not replace historical S2/S3 Burp Level 3/4 debt.
9. **Burp-session active workspace provisioning is deferred.** The eight active S4 views, backend controls and explicitly configured workspace execution path pass local verification. The default Burp extension intentionally remains disabled until configuration/consent; passive-context import and live provisioning must be validated in the separately scheduled Burp lane.
10. **User-mode and profile contracts are locally complete.** Beginner, Professional, Researcher and Expert modes plus the five product profiles now alter real planner configuration. Real Burp UX remains deferred.
11. **Efficiency instrumentation is complete but large measurements are absent.** Candidate/deduplicated/scope-filtered/budget-filtered/executed counters pass exact tests. No 100/1,000/10,000 workload or optimization percentage is measured.
12. **Phase 3 branch verification is locally complete.** Selection modes, consent boundaries, backoff/retry, queue lifecycle/dependencies, expected-decision conflict/unknown policy, coverage and efficiency have focused tests. Cross-platform and Burp validation remain separate.
13. **Dedicated S4 architecture/security documents now exist.** Active engine, planner, executor, differential, graph integration and active-testing safety are documented; future runtime/research results must update them without rewriting historical evidence.
14. **The root repository manifest is historical.** `REPOSITORY_MANIFEST.txt` predates the Sprint 4 active source and must be regenerated only as part of a governed release/package closure.
15. **Exact JDK 21 execution is unverified.** The current sources/tests compile with `--release 21 -Xlint:all -Werror`, but the available compiler/runtime is OpenJDK 26.0.1. Maven is unavailable.

## Sprint 3

1. **Real Burp validation remains blocked historically.** Burp was unavailable in the recorded S2/S3 execution environment, so those runtime load, HTTP handling, UI and Burp-to-lab evidence gates remain UNVERIFIED.
2. **Official Maven dependency build remains blocked.** Maven is absent and external DNS/artifact resolution is unavailable. POMs are XML-valid; local compilation uses the narrow Montoya contract stubs preserved from Sprint 2.
3. **OpenAPI importer is intentionally incomplete.** OpenAPI 3.x and Swagger 2.0 JSON reconnaissance is implemented, with a conservative common YAML subset. Full `$ref`, external references, composition, callbacks/links and full YAML semantics are not claimed.
4. **Schema drift is observational.** A documented/observed mismatch is not itself a vulnerability.
5. **Route equivalence is syntactic/canonical.** It does not establish that proxies, gateways, frameworks and authorization middleware resolve routes identically.
6. **Response semantics are deterministic heuristics.** The selected soft-200/dynamic-field fixtures pass, but ADR-0004/0005/0025 remain open for later research-grade comparison rules.
7. **Hybrid identity confirmation is in-memory.** Persistent identity management and user workflow remain tied to future storage/UI decisions.
8. **Context policy coverage remains UNKNOWN.** Sprint 3 does not invent authorization policies from observed HTTP behavior.
9. **Endpoint priority is not severity.** It is only a dry-run testing-priority signal.
10. **Performance measurements are observational.** They are not JMH benchmarks and no release threshold is defined.
11. **10k recon retention is memory-heavy.** The latest recorded 10k S3 run shows a several-hundred-MB measured JVM memory delta; use `docs/testing/artifacts/performance-baseline-s3.txt` for the exact run because reconnaissance records are retained in-memory for the full run. Persistence/out-of-core work remains future scope.

## Sprint 2 retained blockers

- exact real Burp runtime behavior remains unverified.
- Docker Compose remains unverified in this environment; direct Python lab execution is used.
- active execution remains disabled by default.

## Sprint 1 retained limitations

- structural tenant/resource extraction is conservative.
- universal redaction is not a full DLP/PII classifier.
- raw authorized traffic can exist transiently in memory before safe serialization.
