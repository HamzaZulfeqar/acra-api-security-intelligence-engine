package io.acra.burp.tests.sprint12;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.sitemap.SiteMap;
import io.acra.burp.reporting.BurpIssuePublicationStatus;
import io.acra.burp.reporting.ReproductionIssuePublisher;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionPackage;
import io.acra.core.reproduction.ReproductionPackageBuilder;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Sprint12BurpIssuePublisherTestSuite {
    private Sprint12BurpIssuePublisherTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_PUBLISHER PASS assertions=" + assertions);
    }

    public static int run() {
        AtomicInteger adds = new AtomicInteger();
        AtomicBoolean fail = new AtomicBoolean(false);
        SiteMap siteMap = proxy(SiteMap.class, (method, args) -> {
            if (method.equals("add")) {
                adds.incrementAndGet();
                if (fail.get()) throw new IllegalStateException("synthetic SiteMap failure");
            }
            return null;
        });

        ReproductionIssuePublisher publisher = new ReproductionIssuePublisher(siteMap);
        ReproductionPackage candidate = packageFor(FindingCandidateState.CANDIDATE, "candidate-1");
        HttpRequestResponse exchange = exchange("https://api.example.test/api/v1/documents/42");
        int assertions = 0;

        check(publisher.publishedCount() == 0, "publisher starts empty"); assertions++;
        var first = publisher.publish(candidate, exchange);
        check(first.status() == BurpIssuePublicationStatus.PUBLISHED, "first publication succeeds"); assertions++;
        check(adds.get() == 1, "SiteMap.add invoked once"); assertions++;
        check(publisher.hasPublished(candidate.fingerprint()), "fingerprint marked published"); assertions++;
        check(first.issue().requestResponses().getFirst() == exchange, "original evidence retained"); assertions++;

        var duplicate = publisher.publish(candidate, exchange);
        check(duplicate.status() == BurpIssuePublicationStatus.DUPLICATE_SUPPRESSED,
                "duplicate publication suppressed"); assertions++;
        check(adds.get() == 1, "duplicate does not call SiteMap.add"); assertions++;
        check(publisher.publishedCount() == 1, "duplicate does not expand dedup set"); assertions++;

        ReproductionPackage second = packageFor(FindingCandidateState.CANDIDATE, "candidate-2");
        var secondResult = publisher.publish(
                second, exchange("https://api.example.test/api/v1/documents/43"));
        check(secondResult.status() == BurpIssuePublicationStatus.PUBLISHED,
                "distinct fingerprint publishes independently"); assertions++;
        check(adds.get() == 2, "distinct candidate calls SiteMap.add"); assertions++;

        ReproductionPackage retryCandidate = packageFor(FindingCandidateState.CANDIDATE, "candidate-retry");
        fail.set(true);
        expectThrows(
                () -> publisher.publish(
                        retryCandidate, exchange("https://api.example.test/api/v1/documents/44")),
                "SiteMap failure must propagate");
        assertions++;
        check(!publisher.hasPublished(retryCandidate.fingerprint()),
                "failed add rolls back dedup claim"); assertions++;
        fail.set(false);
        var retry = publisher.publish(
                retryCandidate, exchange("https://api.example.test/api/v1/documents/44"));
        check(retry.status() == BurpIssuePublicationStatus.PUBLISHED,
                "failed publication can be retried"); assertions++;
        check(publisher.hasPublished(retryCandidate.fingerprint()),
                "successful retry records fingerprint"); assertions++;
        check(adds.get() == 4, "add count includes failed attempt and retry"); assertions++;

        for (FindingCandidateState state :
                List.of(FindingCandidateState.REJECTED, FindingCandidateState.INCONCLUSIVE)) {
            int before = adds.get();
            expectThrows(
                    () -> publisher.publish(packageFor(state, state.name()), exchange),
                    state + " must fail closed");
            assertions++;
            check(adds.get() == before, state + " must not reach SiteMap.add");
            assertions++;
        }

        check(publisher.publishedCount() == 3, "only three candidate fingerprints published"); assertions++;
        return assertions;
    }

    private static ReproductionPackage packageFor(FindingCandidateState state, String id) {
        FindingCandidate candidate = new FindingCandidate(
                "s12-publish-" + id,
                state,
                "acra-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("BOLA"),
                "/api/v1/documents/{id}",
                id,
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                state == FindingCandidateState.REJECTED
                        ? AuthorizationDecision.DENY : AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-1"),
                "HIGH",
                "review-only",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}", id, "user-a",
                        "CROSS_TENANT", "BOLA", state.name()));

        AuthorizationReport report = new AuthorizationReport(
                "report-" + id,
                candidate.projectId(),
                "exec-1",
                "test-1",
                ContextStatus.RESOLVED,
                CorrelationState.CONSISTENT,
                state,
                state == FindingCandidateState.CANDIDATE
                        ? FindingSeverity.HIGH : FindingSeverity.INFO,
                state == FindingCandidateState.CANDIDATE
                        ? FindingConfidence.HIGH : FindingConfidence.INSUFFICIENT,
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                state == FindingCandidateState.INCONCLUSIVE
                        ? List.of("FINDING_CANDIDATE_INCONCLUSIVE") : List.of());

        return new ReproductionPackageBuilder().build(candidate, report);
    }

    private static HttpRequestResponse exchange(String url) {
        HttpService service = proxy(HttpService.class, (method, args) -> switch (method) {
            case "host" -> "api.example.test";
            case "port" -> 443;
            case "secure" -> true;
            default -> null;
        });
        HttpRequest request = proxy(HttpRequest.class, (method, args) -> switch (method) {
            case "url" -> url;
            case "method" -> "GET";
            case "path", "pathWithoutQuery" -> "/api/v1/documents/42";
            case "httpService" -> service;
            default -> null;
        });
        HttpResponse response = proxy(HttpResponse.class, (method, args) ->
                method.equals("statusCode") ? (short) 200 : null);
        return proxy(HttpRequestResponse.class, (method, args) -> switch (method) {
            case "request" -> request;
            case "response" -> response;
            case "hasResponse" -> true;
            case "httpService" -> service;
            default -> null;
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

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void expectThrows(ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
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
