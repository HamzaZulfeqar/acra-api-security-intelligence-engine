package io.acra.standalone.service;

import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.LocalhostHttpTransport;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.ReproducibilityMetadata;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.active.safety.BudgetKey;
import io.acra.core.active.safety.BudgetScope;
import io.acra.core.active.safety.ConcurrencyKey;
import io.acra.core.active.safety.ConcurrencyScope;
import io.acra.core.active.safety.HierarchicalBudgetManager;
import io.acra.core.active.safety.HierarchicalConcurrencyController;
import io.acra.core.active.safety.KillSwitch;
import io.acra.core.active.safety.LocalDevelopmentExecutionPolicy;
import io.acra.core.active.safety.MutationBudgetTracker;
import io.acra.core.active.safety.MutationValidator;
import io.acra.core.active.safety.RateLimitPolicy;
import io.acra.core.active.safety.ScopedRateLimiter;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.testing.TestState;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.ControlledExecutionRecord;
import io.acra.standalone.model.InventoryRecord;
import io.acra.standalone.model.ResourceContextRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.store.ControlledExecutionStore;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StandaloneControlledExecutionService {
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    private final LocalWorkspaceStore workspace;
    private final SecurityContextService contextService;
    private final ControlledExecutionStore executionStore;
    private final StandaloneEvidenceService evidenceService;
    private final Clock clock;
    private final SafetyAuditLog safetyAudit;
    private final KillSwitch killSwitch;

    public StandaloneControlledExecutionService(LocalWorkspaceStore workspace) {
        this(workspace, Clock.systemUTC());
    }

    StandaloneControlledExecutionService(LocalWorkspaceStore workspace, Clock clock) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.contextService = new SecurityContextService(workspace);
        this.executionStore = new ControlledExecutionStore(workspace);
        this.evidenceService = new StandaloneEvidenceService(workspace);
        this.clock = java.util.Objects.requireNonNull(clock);
        this.safetyAudit = new SafetyAuditLog(clock);
        this.killSwitch = new KillSwitch(safetyAudit);
        this.killSwitch.reset(true, "standalone controlled execution service initialized");
    }

    public ControlledExecutionRecord executeRouteEquivalence(
            UUID projectId,
            UUID targetId,
            UUID expectationId,
            String concretePath,
            String authorizationValue,
            boolean userConfirmed
    ) throws IOException {
        if (!userConfirmed) throw new IllegalArgumentException("explicit execution confirmation is required");

        TargetRecord target = workspace.findTarget(projectId, targetId);
        requireControlledLoopbackTarget(target);

        AuthorizationExpectationRecord expectation = expectation(projectId, expectationId);
        if (!expectation.targetId().equals(targetId)) {
            throw new IllegalArgumentException("expectation does not belong to selected target");
        }
        if (expectation.action() != ActionType.READ) {
            throw new IllegalArgumentException("Phase 7 route-equivalence execution supports READ expectations only");
        }
        if (!binary(expectation.expectedDecision())) {
            throw new IllegalArgumentException("active execution requires explicit ALLOW or DENY expectation");
        }

        InventoryRecord inventory = inventory(projectId, targetId, expectation.endpoint());
        if (!SAFE_METHODS.contains(inventory.method())) {
            throw new IllegalArgumentException("Phase 7 route-equivalence execution permits GET, HEAD or OPTIONS only");
        }

        String path = validateConcretePath(target, concretePath);
        String canonical = canonicalPath(target, inventory.method(), path);
        if (!canonical.equals(expectation.endpoint())) {
            throw new IllegalArgumentException("concrete path does not resolve to the configured expectation endpoint");
        }

        String mutatedPath = equivalentTrailingSlash(path);
        validateConcretePath(target, mutatedPath);

        String auth = validateEphemeralAuthorization(authorizationValue);
        SecurityTest test = securityTest(projectId, target, inventory, expectation, path, mutatedPath, auth);
        Harness harness = harness(projectId.toString(), test.target());

        configureGuards(harness, test);
        ActiveConsent consent = new LocalDevelopmentExecutionPolicy().consentFor(test);
        if (!consent.activeTestingEnabled() || !consent.targetAuthorized() || !consent.testApproved()) {
            throw new IllegalArgumentException("local development execution policy did not authorize this test");
        }

        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                expectation.expectedDecision(),
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "standalone-expectation:" + expectation.id(),
                List.of(),
                1.0);

        var result = harness.executor().execute(test, consent, List.of(expected));
        if (result.state() != TestState.COMPLETED || result.observation() == null) {
            String reason = result.failure() == null ? "execution did not complete"
                    : result.failure().message();
            throw new IllegalStateException("controlled execution blocked/failed: " + reason);
        }

        String summary = "testId=" + test.testId()
                + "\nexecutionId=" + result.executionId()
                + "\nendpoint=" + expectation.endpoint()
                + "\nrequestPath=" + path
                + "\nmutatedPath=" + mutatedPath
                + "\nexpected=" + expectation.expectedDecision()
                + "\nobserved=" + result.observation().observedDecision()
                + "\ndifferential=" + result.observation().differences().classification()
                + "\nstate=" + result.state();

        var evidenceArtifact = evidenceService.captureExecutionSummary(
                projectId,
                targetId,
                "controlled-active:" + result.executionId(),
                summary);

        ControlledExecutionRecord record = new ControlledExecutionRecord(
                UUID.randomUUID(),
                projectId,
                targetId,
                expectation.id(),
                test.testId(),
                result.executionId(),
                result.observation().observationId(),
                expectation.endpoint(),
                path,
                mutatedPath,
                expectation.expectedDecision(),
                result.observation().observedDecision(),
                result.observation().differences().classification(),
                result.state(),
                evidenceArtifact.evidenceId(),
                result.evidenceChain().size(),
                clock.instant());
        return executionStore.save(record);
    }

    public List<ControlledExecutionRecord> executions(UUID projectId) throws IOException {
        return executionStore.list(projectId);
    }

    public boolean killSwitchEngaged() {
        return killSwitch.engaged();
    }

    public void engageKillSwitch(String reason) {
        killSwitch.engage(reason == null || reason.isBlank() ? "standalone operator stop" : reason.strip());
    }

    public void resetKillSwitch(boolean confirmed, String reason) {
        if (!confirmed) throw new IllegalArgumentException("kill-switch reset requires explicit confirmation");
        killSwitch.reset(true, reason == null || reason.isBlank() ? "standalone operator reset" : reason.strip());
    }

    private SecurityTest securityTest(
            UUID projectId,
            TargetRecord target,
            InventoryRecord inventory,
            AuthorizationExpectationRecord expectation,
            String path,
            String mutatedPath,
            String authorizationValue
    ) throws IOException {
        URI base = target.baseUri();
        int port = effectivePort(base);
        String host = normalizeHost(base.getHost());
        String allowedPrefix = basePath(base);

        TargetDescriptor descriptor = new TargetDescriptor(
                projectId.toString(),
                target.id().toString(),
                base.getScheme(),
                host,
                port,
                ExecutionEnvironment.LAB,
                true,
                List.of(allowedPrefix),
                Set.of(inventory.method()));

        Endpoint endpoint = new Endpoint(
                "S10-ENDPOINT-" + inventory.id(),
                inventory.method(),
                path,
                expectation.endpoint(),
                expectation.endpoint(),
                host,
                "",
                List.of("standalone controlled route-equivalence execution"));

        List<HttpHeader> authenticatedHeaders = authorizationValue.isBlank()
                ? List.of(new HttpHeader("Accept", "application/json"))
                : List.of(
                        new HttpHeader("Authorization", authorizationValue),
                        new HttpHeader("Accept", "application/json"));

        HttpRequest authenticated = HttpRequest.of(
                inventory.method(),
                base.getScheme(),
                host,
                port,
                path,
                authenticatedHeaders,
                new byte[0],
                HttpProtocol.HTTP_1_1);

        HttpRequest anonymous = HttpRequest.of(
                inventory.method(),
                base.getScheme(),
                host,
                port,
                path,
                List.of(new HttpHeader("Accept", "application/json")),
                new byte[0],
                HttpProtocol.HTTP_1_1);

        String contextRef = "standalone-context-" + expectation.id();
        String resourceRef = expectation.resourceId().isBlank()
                ? "endpoint:" + expectation.endpoint()
                : "resource:" + expectation.resourceId();

        RequestDefinition baseline = new RequestDefinition(
                "S10-REQ-BASE-" + expectation.id(), authenticated, contextRef, resourceRef);
        RequestDefinition positive = new RequestDefinition(
                "S10-REQ-POS-" + expectation.id(), authenticated, contextRef, resourceRef);
        RequestDefinition negative = new RequestDefinition(
                "S10-REQ-NEG-" + expectation.id(), anonymous, "anonymous-context", resourceRef);

        Mutation mutation = new Mutation(
                "S10-MUT-ROUTE-" + expectation.id(),
                MutationType.EQUIVALENT_ROUTE_REPRESENTATION,
                MutationLocation.URI_REPRESENTATION,
                path,
                mutatedPath,
                contextRef,
                contextRef,
                "generated trailing-slash equivalent route representation",
                "authorization decision should remain " + expectation.expectedDecision(),
                SafetyClass.SAFE_READ_ONLY,
                "standalone-route:" + TokenFingerprint.sha256(
                        target.id() + "|" + expectation.id() + "|" + path + "|" + mutatedPath).substring(0, 24));

        SecurityContextFingerprint context = context(expectation);
        Resource resource = resource(projectId, expectation);
        Instant now = clock.instant();

        return new SecurityTest(
                "S10-ACTIVE-ROUTE-" + TokenFingerprint.sha256(
                        projectId + "|" + target.id() + "|" + expectation.id() + "|" + path).substring(0, 24),
                "1",
                TestContract.ROUTE_EQUIVALENCE,
                HttpProtocol.HTTP_1_1,
                descriptor,
                endpoint,
                inventory.method(),
                baseline,
                positive,
                negative,
                mutation,
                context,
                context,
                resource,
                resource,
                expectation.expectedDecision(),
                List.of(),
                SafetyPolicy.safeLabReadOnly(8, 2),
                80,
                "explicit standalone controlled route-equivalence validation",
                ConfigurationSnapshot.of(java.util.Map.of(
                        "mode", "standalone-controlled-lab",
                        "targetId", target.id().toString(),
                        "expectationId", expectation.id().toString())),
                List.of(),
                new ReproducibilityMetadata(
                        "s10-phase7",
                        deterministicSeed(projectId, target.id(), expectation.id(), path),
                        now,
                        List.of("standalone-security-context", "standalone-api-inventory")),
                4,
                List.of(
                        "method remains read-only",
                        "scheme/host/port remain unchanged",
                        "authorization context remains unchanged for mutation",
                        "only trailing-slash URI representation changes"));
    }

    private SecurityContextFingerprint context(AuthorizationExpectationRecord expectation) {
        return new SecurityContextFingerprint(
                expectation.principalId(),
                expectation.roleId(),
                expectation.tenantId(),
                expectation.resourceId(),
                owner(expectation),
                expectation.action().name(),
                "UNKNOWN",
                expectation.action() + " " + expectation.endpoint(),
                "RAW",
                "standalone-context-" + expectation.id(),
                expectation.expectedDecision(),
                AuthorizationDecision.UNKNOWN,
                List.of());
    }

    private Resource resource(UUID projectId, AuthorizationExpectationRecord expectation) throws IOException {
        if (expectation.resourceId().isBlank()) return null;
        ResourceContextRecord record = contextService.resources(projectId).stream()
                .filter(value -> value.resourceId().equals(expectation.resourceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("expectation resource is unavailable"));
        return new Resource(
                record.resourceId(),
                record.resourceType(),
                "",
                record.ownerPrincipalId(),
                record.tenantId(),
                record.state(),
                Confidence.unknown());
    }

    private String owner(AuthorizationExpectationRecord expectation) {
        if (expectation.resourceId().isBlank()) return "";
        try {
            return contextService.resources(expectation.projectId()).stream()
                    .filter(value -> value.resourceId().equals(expectation.resourceId()))
                    .map(ResourceContextRecord::ownerPrincipalId)
                    .findFirst().orElse("");
        } catch (IOException ex) {
            throw new IllegalStateException("unable to resolve resource owner", ex);
        }
    }

    private Harness harness(String projectId, TargetDescriptor target) {
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(safetyAudit);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        ScopedRateLimiter rate = new ScopedRateLimiter();
        MutationValidator validator = new MutationValidator(
                projectId,
                Set.of(ExecutionEnvironment.LAB),
                killSwitch,
                budgets,
                concurrency,
                rate,
                safetyAudit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock, projectId);
        DelayController noDelay = duration -> { };
        TestExecutor executor = new TestExecutor(
                clock,
                Duration.ofSeconds(4),
                new LocalhostHttpTransport(target),
                noDelay,
                validator,
                budgets,
                concurrency,
                rate,
                new BackoffPolicy(0, Duration.ofMillis(50)),
                killSwitch,
                new MutationBudgetTracker(4),
                safetyAudit,
                evidence);
        return new Harness(executor, budgets, concurrency, rate);
    }

    private static void configureGuards(Harness harness, SecurityTest test) {
        for (BudgetKey key : List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT,
                        test.targetContext().principal() + "|" + test.targetContext().tenant() + "|"
                                + test.targetContext().role()))) {
            harness.budgets().configure(key, 8);
        }

        for (ConcurrencyKey key : List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()))) {
            harness.concurrency().configure(key, 1);
        }

        RateLimitPolicy policy = new RateLimitPolicy(8, 60, 2, 0);
        for (String key : List.of(
                "host:" + test.target().host(),
                "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(),
                "test:" + test.testId())) {
            harness.rate().configure(key, policy);
        }
    }

    private AuthorizationExpectationRecord expectation(UUID projectId, UUID expectationId) throws IOException {
        return contextService.expectations(projectId).stream()
                .filter(value -> value.id().equals(expectationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown authorization expectation"));
    }

    private InventoryRecord inventory(UUID projectId, UUID targetId, String endpoint) throws IOException {
        return workspace.listInventory(projectId).stream()
                .filter(value -> value.targetId().equals(targetId)
                        && value.canonicalPath().equals(endpoint)
                        && SAFE_METHODS.contains(value.method()))
                .sorted(Comparator.comparingInt(value -> methodRank(value.method())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "selected expectation has no read-only inventory endpoint for active validation"));
    }

    private static int methodRank(HttpMethod method) {
        return switch (method) {
            case GET -> 0;
            case HEAD -> 1;
            case OPTIONS -> 2;
            default -> 99;
        };
    }

    private static void requireControlledLoopbackTarget(TargetRecord target) {
        URI uri = target.baseUri();
        String host = normalizeHost(uri.getHost());
        if (!"LAB".equals(target.environment())) {
            throw new IllegalArgumentException("controlled active execution requires LAB environment");
        }
        if (!"CONTROLLED_LAB".equals(target.testingMode())) {
            throw new IllegalArgumentException("controlled active execution requires CONTROLLED_LAB testing mode");
        }
        if (!LOOPBACK.contains(host)) {
            throw new IllegalArgumentException("Phase 7 controlled execution is restricted to loopback targets");
        }
        if (!Set.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("controlled execution requires HTTP(S)");
        }
    }

    private static String validateConcretePath(TargetRecord target, String value) {
        String path = value == null ? "" : value.strip();
        if (path.isBlank() || !path.startsWith("/") || path.contains("://") || path.indexOf('#') >= 0
                || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("concretePath must be a safe origin-form path");
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")) {
            throw new IllegalArgumentException("encoded slash/dot/backslash forms are not accepted in this Phase 7 slice");
        }
        String base = basePath(target.baseUri());
        if (!"/".equals(base) && !boundaryMatch(path, base)) {
            throw new IllegalArgumentException("concretePath is outside the registered target base path");
        }
        return path;
    }

    private static String canonicalPath(TargetRecord target, HttpMethod method, String path) {
        io.acra.core.extraction.defaults.DefaultUriExtractor extractor =
                new io.acra.core.extraction.defaults.DefaultUriExtractor();
        HttpRequest request = HttpRequest.of(
                method,
                target.baseUri().getScheme(),
                normalizeHost(target.baseUri().getHost()),
                effectivePort(target.baseUri()),
                path,
                List.of(),
                new byte[0],
                HttpProtocol.HTTP_1_1);
        io.acra.core.domain.http.HttpTransaction tx = new io.acra.core.domain.http.HttpTransaction(
                request,
                null,
                Instant.now(),
                "standalone-active-validation",
                "CONTROLLED_LAB",
                java.util.Map.of());
        return extractor.extract(tx).canonicalPath();
    }

    private static String equivalentTrailingSlash(String path) {
        int query = path.indexOf('?');
        String route = query >= 0 ? path.substring(0, query) : path;
        String suffix = query >= 0 ? path.substring(query) : "";
        String changed;
        if (route.length() > 1 && route.endsWith("/")) changed = route.substring(0, route.length() - 1);
        else changed = route + "/";
        if (changed.equals(route)) throw new IllegalArgumentException("unable to generate route-equivalent representation");
        return changed + suffix;
    }

    private static String validateEphemeralAuthorization(String value) {
        String auth = value == null ? "" : value.strip();
        if (auth.length() > 4096 || auth.indexOf('\r') >= 0 || auth.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("authorization value is invalid");
        }
        return auth;
    }

    private static boolean binary(AuthorizationDecision decision) {
        return decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY;
    }

    private static String basePath(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) return "/";
        String normalized = path.startsWith("/") ? path : "/" + path;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean boundaryMatch(String path, String prefix) {
        if ("/".equals(prefix)) return true;
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() > 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String normalizeHost(String host) {
        if (host == null) return "";
        String normalized = host.toLowerCase(Locale.ROOT);
        if ("[::1]".equals(normalized)) return "::1";
        return normalized;
    }

    private static long deterministicSeed(UUID projectId, UUID targetId, UUID expectationId, String path) {
        String hash = TokenFingerprint.sha256(projectId + "|" + targetId + "|" + expectationId + "|" + path);
        return Long.parseUnsignedLong(hash.substring(0, 15), 16);
    }

    private record Harness(
            TestExecutor executor,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rate) {
    }
}
