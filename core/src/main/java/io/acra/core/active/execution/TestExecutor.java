package io.acra.core.active.execution;

import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.ExpectedDecisionResolver;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.analysis.MultiWayDifferential;
import io.acra.core.active.analysis.MultiWayDifferentialAnalyzer;
import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.ExecutionFingerprint;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.evidence.RequestSnapshot;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.evidence.SafetyEventType;
import io.acra.core.active.graph.GraphHydrationResult;
import io.acra.core.active.graph.ObservationGraphIntegrator;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.active.safety.BudgetKey;
import io.acra.core.active.safety.BudgetScope;
import io.acra.core.active.safety.ConcurrencyKey;
import io.acra.core.active.safety.ConcurrencyLease;
import io.acra.core.active.safety.ConcurrencyScope;
import io.acra.core.active.safety.HierarchicalBudgetManager;
import io.acra.core.active.safety.HierarchicalConcurrencyController;
import io.acra.core.active.safety.KillSwitch;
import io.acra.core.active.safety.MutationBudgetTracker;
import io.acra.core.active.safety.MutationValidator;
import io.acra.core.active.safety.RateLimitResult;
import io.acra.core.active.safety.ScopedRateLimiter;
import io.acra.core.active.safety.ValidationDecision;
import io.acra.core.active.safety.ValidationStatus;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.testing.TestState;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLException;

public final class TestExecutor {
    private record DispatchOutcome(ResponseSnapshot response, ExecutionFailure failure) {
        DispatchOutcome {
            if ((response == null) == (failure == null)) throw new IllegalArgumentException("dispatch must contain response or failure");
        }
    }

    private static final class DispatchCounter { private int requests; }

    private final Clock clock;
    private final Duration timeout;
    private final HttpTransport transport;
    private final DelayController delayController;
    private final RequestBuilder requestBuilder;
    private final MutationValidator validator;
    private final HierarchicalBudgetManager budgets;
    private final HierarchicalConcurrencyController concurrency;
    private final ScopedRateLimiter rateLimiter;
    private final BackoffPolicy backoffPolicy;
    private final KillSwitch killSwitch;
    private final MutationBudgetTracker mutationBudget;
    private final SafetyAuditLog audit;
    private final ExecutionEvidenceStore evidence;
    private final SecurityContextGraph observationGraph;
    private final ObservationGraphIntegrator graphIntegrator;
    private final MultiWayDifferentialAnalyzer differentialAnalyzer = new MultiWayDifferentialAnalyzer();
    private final ExpectedDecisionResolver expectedResolver = new ExpectedDecisionResolver();
    private static final AtomicLong EXECUTION_SEQUENCE = new AtomicLong();
    private final ConcurrentHashMap<String, CancellationToken> running = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GraphHydrationResult> graphHydrations = new ConcurrentHashMap<>();

    public TestExecutor(
            Clock clock,
            Duration timeout,
            HttpTransport transport,
            DelayController delayController,
            MutationValidator validator,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rateLimiter,
            BackoffPolicy backoffPolicy,
            KillSwitch killSwitch,
            MutationBudgetTracker mutationBudget,
            SafetyAuditLog audit,
            ExecutionEvidenceStore evidence) {
        this(clock, timeout, transport, delayController, validator, budgets, concurrency, rateLimiter,
                backoffPolicy, killSwitch, mutationBudget, audit, evidence, null, null);
    }

    public TestExecutor(
            Clock clock,
            Duration timeout,
            HttpTransport transport,
            DelayController delayController,
            MutationValidator validator,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rateLimiter,
            BackoffPolicy backoffPolicy,
            KillSwitch killSwitch,
            MutationBudgetTracker mutationBudget,
            SafetyAuditLog audit,
            ExecutionEvidenceStore evidence,
            SecurityContextGraph observationGraph,
            String graphProjectId) {
        if (clock == null || timeout == null || timeout.isNegative() || timeout.isZero() || transport == null
                || delayController == null || validator == null || budgets == null || concurrency == null
                || rateLimiter == null || backoffPolicy == null || killSwitch == null || mutationBudget == null
                || audit == null || evidence == null) {
            throw new IllegalArgumentException("executor dependencies required");
        }
        this.clock = clock;
        this.timeout = timeout;
        this.transport = transport;
        this.delayController = delayController;
        this.requestBuilder = new RequestBuilder();
        this.validator = validator;
        this.budgets = budgets;
        this.concurrency = concurrency;
        this.rateLimiter = rateLimiter;
        this.backoffPolicy = backoffPolicy;
        this.killSwitch = killSwitch;
        this.mutationBudget = mutationBudget;
        this.audit = audit;
        this.evidence = evidence;
        if ((observationGraph == null) != (graphProjectId == null || graphProjectId.isBlank())) {
            throw new IllegalArgumentException("graph and graph projectId must be configured together");
        }
        this.observationGraph = observationGraph;
        this.graphIntegrator = observationGraph == null ? null : new ObservationGraphIntegrator(graphProjectId);
    }

    public TestExecutionResult execute(SecurityTest test, ActiveConsent consent,
                                       List<ExpectedDecisionCandidate> expectedCandidates) {
        if (test == null) throw new IllegalArgumentException("test required");
        String executionId = String.format("S4-EXEC-%08d", EXECUTION_SEQUENCE.incrementAndGet());
        CancellationToken cancellation = new CancellationToken();
        running.put(executionId, cancellation);
        try {
            RequestSet requests;
            try {
                requests = requestBuilder.build(test);
            } catch (RuntimeException invalid) {
                ValidationDecision decision = ValidationDecision.invalid("request construction failed: " + invalid.getMessage());
                return failure(executionId, test, TestState.BLOCKED, decision,
                        new ExecutionFailure(ExecutionErrorType.INVALID_REQUEST, decision.reasons().getFirst(), false,
                                invalid.getClass().getName()));
            }
            List<BudgetKey> budgetKeys = budgetKeys(test);
            List<ConcurrencyKey> concurrencyKeys = concurrencyKeys(test);
            List<String> rateKeys = rateKeys(test);
            ValidationDecision validation = validator.validate(executionId, test, requests, consent,
                    budgetKeys, concurrencyKeys, rateKeys);
            if (validation.status() != ValidationStatus.ALLOWED) {
                ExecutionErrorType type = validation.status() == ValidationStatus.INVALID
                        ? ExecutionErrorType.INVALID_REQUEST : ExecutionErrorType.SCOPE_BLOCKED;
                return failure(executionId, test, TestState.BLOCKED, validation,
                        new ExecutionFailure(type, String.join("; ", validation.reasons()), false, ""));
            }
            if (!mutationBudget.recordGenerated()) {
                return failure(executionId, test, TestState.BLOCKED, validation,
                        new ExecutionFailure(ExecutionErrorType.BUDGET_EXCEEDED, "mutation generation budget exhausted", false, ""));
            }

            evidence.append(executionId, test.testId(), EvidenceStage.TEST, executionId + ":test", test);
            DispatchCounter counter = new DispatchCounter();
            DispatchOutcome baseline = dispatch(executionId, test, requests.baseline(), EvidenceStage.BASELINE_REQUEST,
                    EvidenceStage.BASELINE_RESPONSE, budgetKeys, concurrencyKeys, rateKeys, cancellation, counter);
            if (baseline.failure() != null) return dispatchFailure(executionId, test, validation, baseline.failure());
            DispatchOutcome positive = dispatch(executionId, test, requests.positiveControl(), EvidenceStage.POSITIVE_CONTROL_REQUEST,
                    EvidenceStage.POSITIVE_CONTROL_RESPONSE, budgetKeys, concurrencyKeys, rateKeys, cancellation, counter);
            if (positive.failure() != null) return dispatchFailure(executionId, test, validation, positive.failure());
            DispatchOutcome negative = dispatch(executionId, test, requests.negativeControl(), EvidenceStage.NEGATIVE_CONTROL_REQUEST,
                    EvidenceStage.NEGATIVE_CONTROL_RESPONSE, budgetKeys, concurrencyKeys, rateKeys, cancellation, counter);
            if (negative.failure() != null) return dispatchFailure(executionId, test, validation, negative.failure());
            DispatchOutcome mutation = dispatch(executionId, test, requests.mutation(), EvidenceStage.MUTATION_REQUEST,
                    EvidenceStage.MUTATION_RESPONSE, budgetKeys, concurrencyKeys, rateKeys, cancellation, counter);
            if (mutation.failure() != null) return dispatchFailure(executionId, test, validation, mutation.failure());

            ExpectedDecisionResolution expected = expectedResolver.resolve(expectedCandidates(test, expectedCandidates));
            MultiWayDifferential differential = differentialAnalyzer.compare(baseline.response(), positive.response(),
                    negative.response(), mutation.response(), expected);
            evidence.append(executionId, test.testId(), EvidenceStage.DIFFERENTIAL,
                    executionId + ":differential", differential);
            ExecutionFingerprint fingerprint = fingerprint(executionId, test, List.of(
                    baseline.response(), positive.response(), negative.response(), mutation.response()));
            List<String> evidenceIds = evidence.chain(executionId).stream().map(EvidenceChainEntry::evidenceId).toList();
            double confidence = confidence(expected, differential);
            Observation observation = new Observation(executionId + ":observation", test.testId(), baseline.response(),
                    positive.response(), negative.response(), mutation.response(), expected,
                    differential.mutationOutcome(), differential, test.sourceContext(), test.targetContext(), evidenceIds,
                    confidence, fingerprint, clock.instant());
            evidence.append(executionId, test.testId(), EvidenceStage.OBSERVATION, observation.observationId(), observation);
            mutationBudget.recordExecuted();
            audit.append(SafetyEventType.DISPATCH_COMPLETED, executionId, test.testId(),
                    Map.of("requests", Integer.toString(counter.requests), "classification", differential.classification().name()));
            TestExecutionResult result = new TestExecutionResult(executionId, test.testId(), TestState.COMPLETED,
                    validation, observation, null, evidence.chain(executionId));
            if (graphIntegrator != null) {
                graphHydrations.put(executionId, graphIntegrator.hydrate(observationGraph, test, result));
            }
            return result;
        } finally {
            running.remove(executionId);
        }
    }

    public Optional<TestExecutionResult> executeNext(ExecutionQueue queue, ActiveConsent consent,
                                                      List<ExpectedDecisionCandidate> expectedCandidates) {
        if (queue == null) throw new IllegalArgumentException("queue required");
        Optional<SecurityTest> next = queue.startNext();
        if (next.isEmpty()) return Optional.empty();
        SecurityTest test = next.get();
        TestExecutionResult result = execute(test, consent, expectedCandidates);
        switch (result.state()) {
            case COMPLETED -> queue.complete(test.testId());
            case PAUSED -> queue.pause(test.testId());
            case BLOCKED -> queue.block(test.testId(), result.failure() == null ? "blocked" : result.failure().message());
            case FAILED -> queue.fail(test.testId(), result.failure() == null ? "failed" : result.failure().message());
            case CANCELLED -> queue.cancel(test.testId(), result.failure() == null ? "cancelled" : result.failure().message());
            default -> throw new IllegalStateException("unexpected executor terminal state " + result.state());
        }
        return Optional.of(result);
    }

    public void cancel(String executionId) {
        CancellationToken token = running.get(executionId);
        if (token != null) token.cancel();
    }

    public void stopAll(String reason) {
        killSwitch.engage(reason);
        running.values().forEach(CancellationToken::cancel);
    }

    public Optional<GraphHydrationResult> graphHydration(String executionId) {
        if (executionId == null || executionId.isBlank()) throw new IllegalArgumentException("executionId required");
        return Optional.ofNullable(graphHydrations.get(executionId));
    }

    private DispatchOutcome dispatch(
            String executionId,
            SecurityTest test,
            BuiltRequest built,
            EvidenceStage requestStage,
            EvidenceStage responseStage,
            List<BudgetKey> budgetKeys,
            List<ConcurrencyKey> concurrencyKeys,
            List<String> rateKeys,
            CancellationToken cancellation,
            DispatchCounter counter) {
        int attempt = 0;
        while (true) {
            if (killSwitch.engaged() || cancellation.cancelled()) {
                return new DispatchOutcome(null, new ExecutionFailure(ExecutionErrorType.CANCELLED,
                        "execution cancelled by kill switch or cancellation token", false, ""));
            }
            if (counter.requests >= test.safetyPolicy().requestBudget()) {
                return new DispatchOutcome(null, new ExecutionFailure(ExecutionErrorType.BUDGET_EXCEEDED,
                        "test request budget exhausted", false, ""));
            }
            RateLimitResult rate = rateLimiter.tryAcquire(rateKeys, clock.instant());
            if (!rate.allowed()) {
                audit.append(SafetyEventType.RATE_LIMITED, executionId, test.testId(),
                        Map.of("retryAfterMillis", Long.toString(rate.retryAfterMillis()), "reason", rate.reason()));
                return new DispatchOutcome(null, new ExecutionFailure(ExecutionErrorType.RATE_LIMITED,
                        rate.reason(), true, ""));
            }
            try (var reservation = budgets.reserve(budgetKeys, 1, executionId, test.testId());
                 ConcurrencyLease lease = concurrency.acquire(concurrencyKeys)) {
                if (lease == null) throw new IllegalStateException("concurrency acquisition failed");
                counter.requests++;
                String attemptSuffix = ":a" + attempt;
                String requestId = executionId + ':' + built.kind().name().toLowerCase(java.util.Locale.ROOT) + ":request" + attemptSuffix;
                RequestSnapshot request = RequestSnapshot.capture(requestId, built, clock.instant());
                evidence.append(executionId, test.testId(), requestStage, requestId, request);
                audit.append(SafetyEventType.DISPATCH_STARTED, executionId, test.testId(),
                        Map.of("variant", built.kind().name(), "attempt", Integer.toString(attempt)));
                try {
                    cancellation.throwIfCancelled();
                    TransportResult result = transport.send(request, timeout, cancellation);
                    reservation.commit();
                    String responseId = executionId + ':' + built.kind().name().toLowerCase(java.util.Locale.ROOT) + ":response" + attemptSuffix;
                    ResponseSnapshot response = ResponseSnapshot.capture(responseId, requestId, result.response(),
                            result.cookies(), result.responseTiming(), clock.instant());
                    evidence.append(executionId, test.testId(), responseStage, responseId, response);
                    BackoffDecision backoff = backoffPolicy.forResponse(response.response().status(), response.response().headers(), attempt);
                    if (backoff.retryAllowed() && counter.requests < test.safetyPolicy().requestBudget()) {
                        try {
                            delayController.delay(backoff.delay());
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            return new DispatchOutcome(null, normalizeFailure(interrupted));
                        }
                        attempt++;
                        continue;
                    }
                    if (response.response().status() == 429 || response.response().status() == 503) {
                        return new DispatchOutcome(null, new ExecutionFailure(ExecutionErrorType.TARGET_UNSTABLE,
                                backoff.reason(), false, "HTTP " + response.response().status()));
                    }
                    return new DispatchOutcome(response, null);
                } catch (Exception exception) {
                    reservation.commit();
                    ExecutionFailure failure = normalizeFailure(exception);
                    BackoffDecision backoff = failure.retryable()
                            ? backoffPolicy.forTemporaryFailure(attempt, failure.message()) : BackoffDecision.none();
                    if (backoff.retryAllowed() && counter.requests < test.safetyPolicy().requestBudget()) {
                        try {
                            delayController.delay(backoff.delay());
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            return new DispatchOutcome(null, normalizeFailure(interrupted));
                        }
                        attempt++;
                        continue;
                    }
                    return new DispatchOutcome(null, failure);
                }
            } catch (RuntimeException budgetOrConcurrency) {
                ExecutionErrorType type = budgetOrConcurrency instanceof io.acra.core.active.safety.BudgetExceededException
                        ? ExecutionErrorType.BUDGET_EXCEEDED : ExecutionErrorType.INTERNAL_ENGINE_ERROR;
                return new DispatchOutcome(null, new ExecutionFailure(type, budgetOrConcurrency.getMessage(), false,
                        budgetOrConcurrency.getClass().getName()));
            }
        }
    }

    private TestExecutionResult dispatchFailure(String executionId, SecurityTest test, ValidationDecision validation,
                                                 ExecutionFailure failure) {
        mutationBudget.recordFailed();
        audit.append(SafetyEventType.DISPATCH_FAILED, executionId, test.testId(),
                Map.of("type", failure.type().name(), "message", failure.message()));
        TestState state = switch (failure.type()) {
            case CANCELLED -> TestState.CANCELLED;
            case RATE_LIMITED -> TestState.PAUSED;
            case SCOPE_BLOCKED, BUDGET_EXCEEDED -> TestState.BLOCKED;
            default -> TestState.FAILED;
        };
        return failure(executionId, test, state, validation, failure);
    }

    private TestExecutionResult failure(String executionId, SecurityTest test, TestState state,
                                        ValidationDecision validation, ExecutionFailure failure) {
        return new TestExecutionResult(executionId, test.testId(), state, validation, null, failure,
                evidence.chain(executionId));
    }

    private static ExecutionFailure normalizeFailure(Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new ExecutionFailure(ExecutionErrorType.CANCELLED, "execution interrupted", false, exception.getClass().getName());
        }
        if (exception instanceof CancellationException) {
            return new ExecutionFailure(ExecutionErrorType.CANCELLED, "execution cancelled", false, exception.getClass().getName());
        }
        if (exception instanceof HttpTimeoutException || exception instanceof TimeoutException) {
            return new ExecutionFailure(ExecutionErrorType.TIMEOUT, "request timed out", true, exception.getClass().getName());
        }
        if (exception instanceof SSLException) {
            return new ExecutionFailure(ExecutionErrorType.TLS_ERROR, "TLS failure", false, exception.getClass().getName());
        }
        if (exception instanceof ConnectException || exception instanceof UnknownHostException || exception instanceof IOException) {
            return new ExecutionFailure(ExecutionErrorType.CONNECTION_ERROR, "temporary connection failure", true,
                    exception.getClass().getName());
        }
        if (exception instanceof IllegalArgumentException) {
            return new ExecutionFailure(ExecutionErrorType.INVALID_REQUEST, exception.getMessage(), false, exception.getClass().getName());
        }
        return new ExecutionFailure(ExecutionErrorType.INTERNAL_ENGINE_ERROR,
                exception.getMessage() == null ? "internal execution failure" : exception.getMessage(), false,
                exception.getClass().getName());
    }

    private static List<ExpectedDecisionCandidate> expectedCandidates(
            SecurityTest test, List<ExpectedDecisionCandidate> candidates) {
        List<ExpectedDecisionCandidate> out = new ArrayList<>(candidates == null ? List.of() : candidates);
        if (test.expectedDecision() != AuthorizationDecision.UNKNOWN) {
            out.add(new ExpectedDecisionCandidate(test.expectedDecision(), ExpectedDecisionSource.EXPLICIT_TEST_DEFINITION,
                    test.testId(), test.expectedEvidence(), 0.80));
        }
        return List.copyOf(out);
    }

    private ExecutionFingerprint fingerprint(String executionId, SecurityTest test, List<ResponseSnapshot> responses) {
        String requestFingerprint = TokenFingerprint.sha256(evidence.chain(executionId).stream()
                .filter(entry -> entry.stage() == EvidenceStage.BASELINE_REQUEST
                        || entry.stage() == EvidenceStage.POSITIVE_CONTROL_REQUEST
                        || entry.stage() == EvidenceStage.NEGATIVE_CONTROL_REQUEST
                        || entry.stage() == EvidenceStage.MUTATION_REQUEST)
                .map(entry -> ((RequestSnapshot) evidence.object(entry.objectId())).fingerprint())
                .reduce("", (left, right) -> left + '\n' + right));
        String responseFingerprint = TokenFingerprint.sha256(responses.stream()
                .map(ResponseSnapshot::fingerprint).reduce("", (left, right) -> left + '\n' + right));
        String environmentFingerprint = TokenFingerprint.sha256(new DomainSerializer().serialize(test.target()));
        return new ExecutionFingerprint(executionId, test.testId(), requestFingerprint, responseFingerprint,
                test.configurationSnapshot().fingerprint(), environmentFingerprint);
    }

    private static double confidence(ExpectedDecisionResolution expected, MultiWayDifferential differential) {
        if (differential.classification() == DifferentialClassification.INCONCLUSIVE) return 0.25;
        double policy = expected.decision() == AuthorizationDecision.UNKNOWN ? 0.50 : expected.confidence();
        return Math.max(0.0, Math.min(1.0, Math.min(0.95, policy)));
    }

    private static List<BudgetKey> budgetKeys(SecurityTest test) {
        return List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, contextKey(test)));
    }

    private static List<ConcurrencyKey> concurrencyKeys(SecurityTest test) {
        return List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()));
    }

    private static List<String> rateKeys(SecurityTest test) {
        return List.of("host:" + test.target().host(), "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(), "test:" + test.testId());
    }

    private static String contextKey(SecurityTest test) {
        return test.targetContext().principal() + '|' + test.targetContext().tenant() + '|' + test.targetContext().role();
    }
}
