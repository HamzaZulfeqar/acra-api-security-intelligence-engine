package io.acra.burp.tests.sprint12;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.burp.reporting.ReproductionAuditIssueAdapter;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionPackageBuilder;
import java.lang.reflect.Proxy;
import java.util.List;

public final class Sprint12BurpIssueAdapterTestSuite {
    private Sprint12BurpIssueAdapterTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_ADAPTER PASS assertions=" + assertions);
    }

    public static int run() {
        var adapter = new ReproductionAuditIssueAdapter();
        var candidate = candidate(
                FindingCandidateState.CANDIDATE,
                FindingSeverity.CRITICAL,
                FindingConfidence.HIGH,
                "/api/v1/documents/<script>alert(1)</script>");
        var value = new ReproductionPackageBuilder().build(
                candidate,
                report(candidate, FindingSeverity.CRITICAL, FindingConfidence.HIGH));
        HttpRequestResponse exchange = exchange("https://api.example.test/api/v1/documents/42", true);
        var issue = adapter.create(value, exchange);
        int assertions = 0;

        check("ACRA Authorization Review Candidate".equals(issue.name()), "issue name");
        assertions++;
        check(issue.baseUrl().equals("https://api.example.test/api/v1/documents/42"), "issue base URL");
        assertions++;
        check(issue.severity() == AuditIssueSeverity.HIGH, "critical ACRA severity maps to Burp HIGH");
        assertions++;
        check(issue.confidence() == AuditIssueConfidence.FIRM, "HIGH ACRA confidence maps conservatively to FIRM");
        assertions++;
        check(issue.definition().typicalSeverity() == AuditIssueSeverity.HIGH, "typical severity");
        assertions++;
        check(issue.requestResponses().size() == 1, "exactly one evidence request/response");
        assertions++;
        check(issue.requestResponses().getFirst() == exchange, "original evidence object retained");
        assertions++;
        check(issue.collaboratorInteractions().isEmpty(), "no fabricated collaborator interaction");
        assertions++;
        check(issue.detail().contains("Review-only authorization candidate"), "review-only detail");
        assertions++;
        check(issue.detail().contains("not confirmed exploitation"), "non-confirmation boundary");
        assertions++;
        check(issue.detail().contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "dynamic endpoint HTML escaped");
        assertions++;
        check(!issue.detail().contains("<script>"), "raw dynamic HTML absent");
        assertions++;
        check(!issue.detail().contains("super-secret-token"), "bearer secret absent");
        assertions++;
        check(!issue.detail().contains("DoNotExport"), "candidate rationale secret absent");
        assertions++;

        for (FindingSeverity severity : FindingSeverity.values()) {
            var mappedCandidate = candidate(
                    FindingCandidateState.CANDIDATE,
                    severity,
                    FindingConfidence.MEDIUM,
                    "/api/v1/documents/{id}");
            var mapped = adapter.create(
                    new ReproductionPackageBuilder().build(
                            mappedCandidate,
                            report(mappedCandidate, severity, FindingConfidence.MEDIUM)),
                    exchange);
            AuditIssueSeverity expected = switch (severity) {
                case CRITICAL, HIGH -> AuditIssueSeverity.HIGH;
                case MEDIUM -> AuditIssueSeverity.MEDIUM;
                case LOW -> AuditIssueSeverity.LOW;
                case INFO -> AuditIssueSeverity.INFORMATION;
            };
            check(mapped.severity() == expected, "severity mapping " + severity);
            assertions++;
        }

        for (FindingConfidence confidence : FindingConfidence.values()) {
            var mappedCandidate = candidate(
                    FindingCandidateState.CANDIDATE,
                    FindingSeverity.MEDIUM,
                    confidence,
                    "/api/v1/documents/{id}");
            var mapped = adapter.create(
                    new ReproductionPackageBuilder().build(
                            mappedCandidate,
                            report(mappedCandidate, FindingSeverity.MEDIUM, confidence)),
                    exchange);
            AuditIssueConfidence expected = confidence == FindingConfidence.HIGH
                    ? AuditIssueConfidence.FIRM : AuditIssueConfidence.TENTATIVE;
            check(mapped.confidence() == expected, "confidence mapping " + confidence);
            assertions++;
            check(mapped.confidence() != AuditIssueConfidence.CERTAIN,
                    "review-only candidate never maps to CERTAIN");
            assertions++;
        }

        for (FindingCandidateState state :
                List.of(FindingCandidateState.REJECTED, FindingCandidateState.INCONCLUSIVE)) {
            var nonIssue = candidate(
                    state,
                    FindingSeverity.INFO,
                    FindingConfidence.INSUFFICIENT,
                    "/api/v1/documents/{id}");
            var packageValue = new ReproductionPackageBuilder().build(
                    nonIssue,
                    report(nonIssue, FindingSeverity.INFO, FindingConfidence.INSUFFICIENT));
            expectThrows(() -> adapter.create(packageValue, exchange),
                    state + " package must fail closed");
            assertions++;
        }

        expectThrows(() -> adapter.create(value, null), "null request/response must fail closed");
        assertions++;
        expectThrows(() -> adapter.create(value, exchange("", true)), "blank URL must fail closed");
        assertions++;
        expectThrows(() -> adapter.create(value, exchange(
                "https://api.example.test/api/v1/documents/42", false)),
                "request without response must fail closed");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(
            FindingCandidateState state,
            FindingSeverity severity,
            FindingConfidence confidence,
            String endpoint) {
        return new FindingCandidate(
                "s12-burp-" + state.name().toLowerCase() + "-" + severity.name().toLowerCase()
                        + "-" + confidence.name().toLowerCase(),
                state,
                "acra-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("BOLA", "TENANT"),
                endpoint,
                "document-42",
                "Authorization: Bearer super-secret-token",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                state == FindingCandidateState.REJECTED
                        ? AuthorizationDecision.DENY : AuthorizationDecision.ALLOW,
                List.of("evidence-1", "evidence-2"),
                List.of("password=DoNotExport"),
                List.of("policy-1"),
                confidence.name(),
                "password=DoNotExport",
                FindingFingerprint.of(
                        endpoint,
                        "document-42",
                        "Authorization: Bearer super-secret-token",
                        "CROSS_TENANT",
                        "BOLA+TENANT",
                        state.name()));
    }

    private static AuthorizationReport report(
            FindingCandidate candidate,
            FindingSeverity severity,
            FindingConfidence confidence) {
        return new AuthorizationReport(
                "report-" + candidate.candidateId(),
                candidate.projectId(),
                "exec-1",
                "test-1",
                ContextStatus.RESOLVED,
                CorrelationState.CONSISTENT,
                candidate.state(),
                severity,
                confidence,
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                candidate.state() == FindingCandidateState.INCONCLUSIVE
                        ? List.of("FINDING_CANDIDATE_INCONCLUSIVE") : List.of());
    }

    private static HttpRequestResponse exchange(String url, boolean withResponse) {
        HttpService service = proxy(HttpService.class, (method, args) -> switch (method) {
            case "host" -> "api.example.test";
            case "port" -> 443;
            case "secure" -> true;
            default -> defaultValue(method, args);
        });
        HttpRequest request = proxy(HttpRequest.class, (method, args) -> switch (method) {
            case "url" -> url;
            case "method" -> "GET";
            case "path", "pathWithoutQuery" -> "/api/v1/documents/42";
            case "httpService" -> service;
            default -> defaultValue(method, args);
        });
        HttpResponse response = withResponse
                ? proxy(HttpResponse.class, (method, args) -> switch (method) {
                    case "statusCode" -> (short) 200;
                    default -> defaultValue(method, args);
                })
                : null;

        return proxy(HttpRequestResponse.class, (method, args) -> switch (method) {
            case "request" -> request;
            case "response" -> response;
            case "hasResponse" -> withResponse;
            case "httpService" -> service;
            default -> defaultValue(method, args);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (instance, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "Sprint12Proxy(" + type.getSimpleName() + ")";
                            case "hashCode" -> System.identityHashCode(instance);
                            case "equals" -> instance == (args == null ? null : args[0]);
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(), args);
                });
    }

    private static Object defaultValue(String method, Object[] args) {
        if (method.equals("headers")
                || method.equals("markers")
                || method.equals("requestMarkers")
                || method.equals("responseMarkers")) return List.of();
        if (method.equals("hasResponse")) return false;
        if (method.startsWith("has") || method.startsWith("contains")) return false;
        return null;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void expectThrows(ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        } catch (Exception other) {
            throw new AssertionError(message + ": " + other, other);
        }
    }

    @FunctionalInterface
    private interface Handler {
        Object invoke(String method, Object[] args) throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
