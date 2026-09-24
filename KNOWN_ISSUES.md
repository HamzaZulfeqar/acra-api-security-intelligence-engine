# Known Issues and Technical Debt

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
