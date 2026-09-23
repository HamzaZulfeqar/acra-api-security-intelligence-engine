package io.acra.core.tests.sprint6;

import io.acra.core.active.research.ResearchExecutionRecord;
import io.acra.core.active.research.ResearchGroundTruth;
import io.acra.core.active.research.ResearchMetrics;
import io.acra.core.active.research.ResearchPrediction;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.tests.TestSupport;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

public final class Sprint6LiveLabExperimentTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T15:00:00Z");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private Sprint6LiveLabExperimentTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT6_LIVE_LAB_EXPERIMENT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        AuthorizationPolicySnapshot policy = policy();
        EffectiveAuthorizationResolver resolver = new EffectiveAuthorizationResolver();
        List<LiveCase> cases = cases();
        List<ResearchExecutionRecord> baselineRecords = new ArrayList<>();
        List<ResearchExecutionRecord> acraRecords = new ArrayList<>();
        List<CaseResult> results = new ArrayList<>();
        int assertions = 0;

        for (LiveCase item : cases) {
            LiveObservation observation = execute(item);
            TestSupport.assertEquals(item.expectedHttpStatus(), observation.status(),
                    item.id() + " live HTTP status must match independently declared fixture expectation");
            assertions++;

            ResearchPrediction baselinePrediction = naiveBaseline(item);
            EffectiveAuthorizationResolution resolution = resolver.resolve(policy,
                    new EffectiveAuthorizationRequest(item.principalId(), item.subjectTenantId(),
                            item.targetTenantId(), item.resourceId(), item.resourceType(), item.path(), "",
                            item.action(), observation.authorizationDecision(), item.sharedResource(), NOW));
            ResearchPrediction acraPrediction = resolution.mismatchCandidate()
                    ? ResearchPrediction.POSITIVE : ResearchPrediction.NEGATIVE;

            ResearchGroundTruth truth = item.groundTruthPositive()
                    ? ResearchGroundTruth.POSITIVE : ResearchGroundTruth.NEGATIVE;
            baselineRecords.add(record(item, truth, baselinePrediction, "baseline"));
            acraRecords.add(record(item, truth, acraPrediction, "acra"));
            results.add(new CaseResult(item.id(), item.service(), item.path(), observation.status(),
                    item.expectedHttpStatus(), truth, baselinePrediction, acraPrediction,
                    resolution.expectedDecision(), observation.authorizationDecision(),
                    resolution.tenantRelationship().name(), resolution.state().name(),
                    resolution.effectiveRoleIds(), resolution.permissionIds()));
        }

        ResearchMetrics baseline = ResearchMetrics.from(baselineRecords);
        ResearchMetrics acra = ResearchMetrics.from(acraRecords);

        TestSupport.assertEquals(2, acra.truePositive(),
                "ACRA treatment should detect both deliberately vulnerable labelled cases");
        assertions++;
        TestSupport.assertEquals(8, acra.trueNegative(),
                "ACRA treatment should retain all secure/legitimate controls as negatives");
        assertions++;
        TestSupport.assertEquals(0, acra.falsePositive(),
                "ACRA treatment should not misclassify controlled global/delegated/shared/secure cases");
        assertions++;
        TestSupport.assertEquals(0, acra.falseNegative(),
                "ACRA treatment should not miss controlled vulnerable cases");
        assertions++;
        TestSupport.assertTrue(baseline.falsePositive() > acra.falsePositive(),
                "policy-aware treatment should reduce false positives versus naive tenant/role comparison");
        assertions++;
        TestSupport.assertTrue(metric(acra.precision()) >= metric(baseline.precision()),
                "ACRA precision should not be lower than naive baseline on this controlled dataset");
        assertions++;

        Path output = experimentOutput();
        Files.createDirectories(output.getParent());
        Files.writeString(output, json(results, baseline, acra), StandardCharsets.UTF_8);
        System.out.println("SPRINT6_EXPERIMENT_RESULT " + summary("baseline", baseline));
        System.out.println("SPRINT6_EXPERIMENT_RESULT " + summary("acra", acra));
        System.out.println("SPRINT6_EXPERIMENT_ARTIFACT " + output);
        return assertions;
    }

    private static List<LiveCase> cases() {
        return List.of(
                new LiveCase("S6-LIVE-001", "secure", 18082, "GET",
                        "/api/v1/s6/tenants/tenant-a/reports/report-a",
                        "user-a", "tenant-a", "viewer", List.of(), "tenant-a", "report-a",
                        "report", "READ_REPORT", false, false, 200),
                new LiveCase("S6-LIVE-002", "secure", 18082, "GET",
                        "/api/v1/s6/tenants/tenant-b/reports/report-b",
                        "user-a", "tenant-a", "viewer", List.of(), "tenant-b", "report-b",
                        "report", "READ_REPORT", false, false, 403),
                new LiveCase("S6-LIVE-003", "vulnerable", 18081, "GET",
                        "/api/v1/s6/tenants/tenant-b/reports/report-b",
                        "user-a", "tenant-a", "viewer", List.of(), "tenant-b", "report-b",
                        "report", "READ_REPORT", false, true, 200),
                new LiveCase("S6-LIVE-004", "secure", 18082, "GET",
                        "/api/v1/s6/tenants/tenant-b/reports/report-b",
                        "global-admin", "global", "global-admin", List.of(), "tenant-b", "report-b",
                        "report", "READ_REPORT", false, false, 200),
                new LiveCase("S6-LIVE-005", "secure", 18082, "GET",
                        "/api/v1/s6/tenants/tenant-b/reports/report-b",
                        "delegate-a", "tenant-a", "delegated-admin", List.of("tenant-b"), "tenant-b", "report-b",
                        "report", "READ_REPORT", false, false, 200),
                new LiveCase("S6-LIVE-006", "secure", 18082, "GET",
                        "/api/v1/s6/tenants/shared/reports/shared-report",
                        "user-a", "tenant-a", "viewer", List.of(), "shared", "shared-report",
                        "report", "READ_REPORT", true, false, 200),
                new LiveCase("S6-LIVE-007", "secure", 18082, "POST",
                        "/api/v1/s6/tenants/tenant-a/admin/export",
                        "user-a", "tenant-a", "viewer", List.of(), "tenant-a", "tenant-a-export",
                        "tenant-export", "ADMIN_EXPORT", false, false, 403),
                new LiveCase("S6-LIVE-008", "vulnerable", 18081, "POST",
                        "/api/v1/s6/tenants/tenant-a/admin/export",
                        "user-a", "tenant-a", "viewer", List.of(), "tenant-a", "tenant-a-export",
                        "tenant-export", "ADMIN_EXPORT", false, true, 200),
                new LiveCase("S6-LIVE-009", "secure", 18082, "POST",
                        "/api/v1/s6/tenants/tenant-b/admin/export",
                        "global-admin", "global", "global-admin", List.of(), "tenant-b", "tenant-b-export",
                        "tenant-export", "ADMIN_EXPORT", false, false, 200),
                new LiveCase("S6-LIVE-010", "secure", 18082, "POST",
                        "/api/v1/s6/tenants/tenant-b/admin/export",
                        "delegate-a", "tenant-a", "delegated-admin", List.of("tenant-b"), "tenant-b",
                        "tenant-b-export", "tenant-export", "ADMIN_EXPORT", false, false, 200));
    }

    private static AuthorizationPolicySnapshot policy() {
        return AuthorizationPolicySnapshot.create("s6-live-policy", "1", "controlled-local-lab",
                List.of(),
                List.of(
                        new RoleAssignment("ra-viewer", "user-a", "viewer", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e-viewer")),
                        new RoleAssignment("ra-shared", "user-a", "shared-reader", "",
                                AuthorizationScope.shared(), true, List.of("e-shared")),
                        new RoleAssignment("ra-global", "global-admin", "global-admin", "",
                                AuthorizationScope.global(), true, List.of("e-global"))),
                List.of(),
                List.of(
                        new Permission("p-read-a", "READ_REPORT", "report", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e-read-a")),
                        new Permission("p-read-shared", "READ_REPORT", "report", "", "",
                                AuthorizationScope.shared(), List.of("e-read-shared")),
                        new Permission("p-read-global", "READ_REPORT", "report", "", "",
                                AuthorizationScope.global(), List.of("e-read-global")),
                        new Permission("p-export-global", "ADMIN_EXPORT", "tenant-export", "", "",
                                AuthorizationScope.global(), List.of("e-export-global")),
                        new Permission("p-read-b-delegated", "READ_REPORT", "report", "", "",
                                AuthorizationScope.tenant("tenant-b"), List.of("e-read-b")),
                        new Permission("p-export-b-delegated", "ADMIN_EXPORT", "tenant-export", "", "",
                                AuthorizationScope.tenant("tenant-b"), List.of("e-export-b"))),
                List.of(
                        new RolePermissionAssignment("rp-viewer-read", "viewer", "p-read-a", "tenant-a",
                                List.of("e-rp1")),
                        new RolePermissionAssignment("rp-shared-read", "shared-reader", "p-read-shared", "",
                                List.of("e-rp2")),
                        new RolePermissionAssignment("rp-global-read", "global-admin", "p-read-global", "",
                                List.of("e-rp3")),
                        new RolePermissionAssignment("rp-global-export", "global-admin", "p-export-global", "",
                                List.of("e-rp4")),
                        new RolePermissionAssignment("rp-delegate-read", "delegated-admin", "p-read-b-delegated",
                                "tenant-b", List.of("e-rp5")),
                        new RolePermissionAssignment("rp-delegate-export", "delegated-admin", "p-export-b-delegated",
                                "tenant-b", List.of("e-rp6"))),
                List.of(),
                List.of(new Delegation("delegation-a-b", "admin-b", "delegate-a", "tenant-b", "tenant-b",
                        "delegated-admin", List.of("READ_REPORT", "ADMIN_EXPORT"),
                        AuthorizationScope.tenant("tenant-b"), NOW.minusSeconds(60), NOW.plusSeconds(3600),
                        List.of("e-delegation"))),
                List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static LiveObservation execute(LiveCase item) throws IOException, InterruptedException {
        String token = token(item.principalId(), item.subjectTenantId(), item.role(), item.delegatedTenants());
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + item.port() + item.path()))
                .timeout(Duration.ofSeconds(4))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
        HttpRequest request = "POST".equals(item.method())
                ? builder.POST(HttpRequest.BodyPublishers.ofString("{}")).build()
                : builder.GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        AuthorizationDecision decision = response.statusCode() >= 200 && response.statusCode() < 300
                ? AuthorizationDecision.ALLOW : AuthorizationDecision.DENY;
        return new LiveObservation(response.statusCode(), decision);
    }

    private static ResearchPrediction naiveBaseline(LiveCase item) {
        boolean tenantMismatch = !item.subjectTenantId().equals(item.targetTenantId());
        boolean privilegedRoleMismatch = "ADMIN_EXPORT".equals(item.action())
                && !List.of("admin", "tenant-admin").contains(item.role().toLowerCase(Locale.ROOT));
        return tenantMismatch || privilegedRoleMismatch
                ? ResearchPrediction.POSITIVE : ResearchPrediction.NEGATIVE;
    }

    private static ResearchExecutionRecord record(LiveCase item, ResearchGroundTruth truth,
                                                   ResearchPrediction prediction, String treatment) {
        return new ResearchExecutionRecord(item.id(), "exec-" + treatment + "-" + item.id(),
                "test-" + item.id(), "obs-" + item.id(), List.of("live-http-" + item.id()),
                truth, prediction);
    }

    private static String token(String sub, String tenant, String role, List<String> delegatedTenants) {
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String delegated = delegatedTenants.stream().map(v -> "\"" + v + "\"")
                .reduce((a,b) -> a + "," + b).orElse("");
        String payload = "{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                + "\",\"role\":\"" + role + "\",\"delegated_tenants\":[" + delegated + "]}";
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    private static Path experimentOutput() {
        String configured = System.getenv("ACRA_S6_EXPERIMENT_OUTPUT");
        return configured == null || configured.isBlank()
                ? Path.of("build", "s6-experiment", "EXP-S6-TENANT-RBAC-001.json")
                : Path.of(configured);
    }

    private static String json(List<CaseResult> results, ResearchMetrics baseline, ResearchMetrics acra) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"experiment_id\": \"EXP-S6-TENANT-RBAC-001\",\n");
        out.append("  \"dataset_id\": \"GT-S6-TENANT-RBAC\",\n");
        out.append("  \"scope\": \"controlled localhost ACRA-Lab only\",\n");
        out.append("  \"measured_at\": \"2026-09-23T15:00:00Z\",\n");
        out.append("  \"baseline\": ").append(metricsJson(baseline)).append(",\n");
        out.append("  \"acra\": ").append(metricsJson(acra)).append(",\n");
        out.append("  \"cases\": [\n");
        for (int i = 0; i < results.size(); i++) {
            CaseResult r = results.get(i);
            out.append("    {")
                    .append("\"id\":\"").append(r.id()).append("\",")
                    .append("\"service\":\"").append(r.service()).append("\",")
                    .append("\"status\":").append(r.status()).append(",")
                    .append("\"expected_status\":").append(r.expectedStatus()).append(",")
                    .append("\"ground_truth\":\"").append(r.truth()).append("\",")
                    .append("\"baseline_prediction\":\"").append(r.baseline()).append("\",")
                    .append("\"acra_prediction\":\"").append(r.acra()).append("\",")
                    .append("\"policy_expected\":\"").append(r.policyExpected()).append("\",")
                    .append("\"observed\":\"").append(r.observed()).append("\",")
                    .append("\"tenant_relationship\":\"").append(r.tenantRelationship()).append("\",")
                    .append("\"resolution_state\":\"").append(r.resolutionState()).append("\"")
                    .append("}");
            if (i + 1 < results.size()) out.append(",");
            out.append("\n");
        }
        out.append("  ],\n");
        out.append("  \"limitations\": [")
                .append("\"synthetic localhost fixtures\",")
                .append("\"unsigned synthetic lab tokens\",")
                .append("\"metrics do not establish real-world scanner accuracy\",")
                .append("\"Burp runtime path not part of this experiment\"")
                .append("]\n");
        out.append("}\n");
        return out.toString();
    }

    private static String metricsJson(ResearchMetrics metrics) {
        return "{\"tp\":" + metrics.truePositive()
                + ",\"tn\":" + metrics.trueNegative()
                + ",\"fp\":" + metrics.falsePositive()
                + ",\"fn\":" + metrics.falseNegative()
                + ",\"precision\":" + decimal(metrics.precision())
                + ",\"recall\":" + decimal(metrics.recall())
                + ",\"f1\":" + decimal(metrics.f1()) + "}";
    }

    private static String summary(String name, ResearchMetrics m) {
        return name + " TP=" + m.truePositive() + " TN=" + m.trueNegative()
                + " FP=" + m.falsePositive() + " FN=" + m.falseNegative()
                + " precision=" + decimal(m.precision())
                + " recall=" + decimal(m.recall()) + " f1=" + decimal(m.f1());
    }

    private static String decimal(OptionalDouble value) {
        return value.isPresent() ? String.format(Locale.ROOT, "%.6f", value.getAsDouble()) : "null";
    }

    private static double metric(OptionalDouble value) {
        return value.orElse(0.0);
    }

    private record LiveCase(String id, String service, int port, String method, String path,
                            String principalId, String subjectTenantId, String role, List<String> delegatedTenants,
                            String targetTenantId, String resourceId, String resourceType, String action,
                            boolean sharedResource, boolean groundTruthPositive, int expectedHttpStatus) { }

    private record LiveObservation(int status, AuthorizationDecision authorizationDecision) { }

    private record CaseResult(String id, String service, String path, int status, int expectedStatus,
                              ResearchGroundTruth truth, ResearchPrediction baseline, ResearchPrediction acra,
                              AuthorizationDecision policyExpected, AuthorizationDecision observed,
                              String tenantRelationship, String resolutionState,
                              List<String> effectiveRoles, List<String> permissions) { }
}
