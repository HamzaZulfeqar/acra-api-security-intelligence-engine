package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationCaseEvidence;
import io.acra.core.active.research.AblationEvidenceBundle;
import io.acra.core.active.research.S11AblationCampaignPlan;
import io.acra.core.active.research.S11AblationEvaluator;
import io.acra.core.active.research.S11AblationPredictionExecutor;
import io.acra.core.active.research.S11AblationEvaluationReport;
import io.acra.core.active.research.S11CampaignEvidenceReadinessProjector;
import io.acra.core.active.research.S11EvaluationDatasetManifest;
import io.acra.core.active.research.S11PredictionExecutionSnapshot;
import io.acra.core.active.research.S11TreatmentEvidenceCollector;
import io.acra.core.active.research.S11TreatmentPredictionSignalBuilder;
import io.acra.core.active.research.S11VariantEvaluation;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.tests.TestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.OptionalDouble;

public final class Sprint11AblationEvaluationTestSuite {
    private static final int SECURE = 18082;
    private static final int VULNERABLE = 18081;
    private static final String TOKEN = token("user-a", "tenant-a", "viewer");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint11AblationEvaluationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT11_ABLATION_EVALUATION PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        List<CaseMaterial> materials = caseMaterials();
        S11TreatmentEvidenceCollector evidenceCollector = new S11TreatmentEvidenceCollector();
        S11TreatmentPredictionSignalBuilder signalBuilder = new S11TreatmentPredictionSignalBuilder();

        List<AblationEvidenceBundle> bundles = new ArrayList<>();
        List<AblationCaseEvidence> predictionEvidence = new ArrayList<>();
        for (CaseMaterial material : materials) {
            AblationEvidenceBundle bundle = evidenceCollector.collect(
                    material.caseId(), material.route(), material.principal(), material.tenant(), material.role(),
                    material.left(), material.right());
            bundles.add(bundle);
            predictionEvidence.add(signalBuilder.build(bundle, material.left(), material.right()));
        }

        S11AblationCampaignPlan evidenceReady =
                new S11CampaignEvidenceReadinessProjector().project(
                        S11AblationCampaignPlan.canonical(), bundles);
        S11PredictionExecutionSnapshot predictions =
                new S11AblationPredictionExecutor().execute(evidenceReady, predictionEvidence);

        String predictionFingerprintBefore = predictions.fingerprint();
        String executionIdBefore = predictions.executionId();
        List<String> resultFingerprintsBefore = predictions.results().stream()
                .map(value -> value.fingerprint())
                .toList();

        S11EvaluationDatasetManifest dataset = S11EvaluationDatasetManifest.canonical();
        S11AblationEvaluationReport report =
                new S11AblationEvaluator().evaluate(predictions, dataset, bundles);

        int assertions = 0;
        TestSupport.assertEquals(120, report.records().size(),
                "evaluation creates one labelled record per executed prediction");
        assertions++;
        TestSupport.assertEquals(8, report.variants().size(),
                "evaluation creates exactly A0-A7 variant summaries");
        assertions++;
        TestSupport.assertEquals(dataset.datasetId(), report.datasetId(),
                "evaluation binds immutable registered dataset");
        assertions++;
        TestSupport.assertEquals(predictions.executionId(), report.predictionExecutionId(),
                "evaluation binds immutable prediction execution");
        assertions++;

        for (S11VariantEvaluation variant : report.variants()) {
            TestSupport.assertEquals(15, variant.evaluatedCases(),
                    "each variant evaluates all fifteen controlled cases");
            assertions++;
            TestSupport.assertEquals(0, variant.inconclusiveCases(),
                    "controlled evidence yields no inconclusive evaluation cells");
            assertions++;
            TestSupport.assertTrue(Math.abs(variant.evidenceCompleteness() - 1.0) < 0.0000001,
                    "evidence completeness is independently 1.0 for each variant");
            assertions++;
            TestSupport.assertEquals(15,
                    variant.metrics().truePositive() + variant.metrics().trueNegative()
                            + variant.metrics().falsePositive() + variant.metrics().falseNegative(),
                    "confusion matrix denominator remains fifteen cases");
            assertions++;
        }

        for (int index = 0; index <= 5; index++) {
            S11VariantEvaluation variant = report.variants().get(index);
            TestSupport.assertEquals(8, variant.metrics().truePositive(),
                    "A0-A5 retain all eight controlled positive predictions");
            assertions++;
            TestSupport.assertEquals(0, variant.metrics().trueNegative(),
                    "A0-A5 naive/context-only layers do not yet resolve negative differential controls");
            assertions++;
            TestSupport.assertEquals(7, variant.metrics().falsePositive(),
                    "A0-A5 retain seven controlled false positives");
            assertions++;
            TestSupport.assertEquals(0, variant.metrics().falseNegative(),
                    "A0-A5 miss no controlled positive case");
            assertions++;
            assertApprox(8.0 / 15.0, variant.metrics().precision(),
                    "A0-A5 precision matches controlled dataset");
            assertions++;
            assertApprox(1.0, variant.metrics().recall(),
                    "A0-A5 recall matches controlled dataset");
            assertions++;
            assertApprox(16.0 / 23.0, variant.metrics().f1(),
                    "A0-A5 F1 matches controlled dataset");
            assertions++;
        }

        for (int index = 6; index <= 7; index++) {
            S11VariantEvaluation variant = report.variants().get(index);
            TestSupport.assertEquals(8, variant.metrics().truePositive(),
                    "A6-A7 retain all eight controlled positives");
            assertions++;
            TestSupport.assertEquals(7, variant.metrics().trueNegative(),
                    "A6-A7 resolve all seven negative controls");
            assertions++;
            TestSupport.assertEquals(0, variant.metrics().falsePositive(),
                    "A6-A7 produce no false positives on the controlled dataset");
            assertions++;
            TestSupport.assertEquals(0, variant.metrics().falseNegative(),
                    "A6-A7 produce no false negatives on the controlled dataset");
            assertions++;
            assertApprox(1.0, variant.metrics().precision(), "A6-A7 controlled precision is 1.0");
            assertions++;
            assertApprox(1.0, variant.metrics().recall(), "A6-A7 controlled recall is 1.0");
            assertions++;
            assertApprox(1.0, variant.metrics().f1(), "A6-A7 controlled F1 is 1.0");
            assertions++;
        }

        TestSupport.assertEquals(predictionFingerprintBefore, predictions.fingerprint(),
                "evaluation cannot mutate prediction snapshot fingerprint");
        assertions++;
        TestSupport.assertEquals(executionIdBefore, predictions.executionId(),
                "evaluation cannot mutate prediction execution identity");
        assertions++;
        TestSupport.assertEquals(resultFingerprintsBefore,
                predictions.results().stream().map(value -> value.fingerprint()).toList(),
                "evaluation cannot mutate prediction result fingerprints");
        assertions++;

        S11AblationEvaluationReport repeated =
                new S11AblationEvaluator().evaluate(predictions, dataset, bundles);
        TestSupport.assertEquals(report.evaluationId(), repeated.evaluationId(),
                "evaluation identity is deterministic");
        assertions++;
        TestSupport.assertEquals(report.fingerprint(), repeated.fingerprint(),
                "evaluation fingerprint is deterministic");
        assertions++;

        List<AblationEvidenceBundle> incompleteBundles = new ArrayList<>(bundles);
        incompleteBundles.removeLast();
        S11AblationEvaluationReport incomplete =
                new S11AblationEvaluator().evaluate(predictions, dataset, incompleteBundles);
        TestSupport.assertTrue(incomplete.variants().stream()
                        .allMatch(value -> value.evidenceCompleteness() < 1.0),
                "evidence completeness drops independently when one evidence bundle is missing");
        assertions++;
        TestSupport.assertEquals(report.variants().stream().map(S11VariantEvaluation::metrics).toList(),
                incomplete.variants().stream().map(S11VariantEvaluation::metrics).toList(),
                "classification metrics do not silently change when evidence completeness changes");
        assertions++;

        return assertions;
    }

    private static void assertApprox(double expected, OptionalDouble actual, String message) {
        TestSupport.assertTrue(actual.isPresent() && Math.abs(expected - actual.getAsDouble()) < 0.0000001, message);
    }

    private static List<CaseMaterial> caseMaterials() throws Exception {
        List<CaseMaterial> out = new ArrayList<>();

        out.add(material(1, "/api/v1/s4/public", "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));
        out.add(material(2, "/api/v1/s4/application-denial", "user-a", "tenant-a", "viewer",
                get(SECURE, "/api/v1/s4/application-denial", true, false),
                get(SECURE, "/api/v1/s4/application-denial", true, false)));

        for (int index = 3; index <= 6; index++) {
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            out.add(material(index, route, "", "", "",
                    get(SECURE, route + "?variant=a", false, false),
                    get(SECURE, route + "?variant=b", false, false)));
        }

        out.add(material(7, "/api/v1/s4/public", "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));

        for (int index = 8; index <= 14; index++) {
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            String suffix = index == 10 ? "?variant=a&pad=17" : "?variant=a";
            out.add(material(index, route, "user-a", "tenant-a", "viewer",
                    get(SECURE, route + suffix, true, false),
                    get(VULNERABLE, route + suffix, true, false)));
        }

        out.add(material(15, "/api/v1/s11/research/case-015", "user-a", "", "",
                get(SECURE, "/api/v1/s11/research/case-015?variant=a", false, true),
                get(VULNERABLE, "/api/v1/s11/research/case-015?variant=a", false, true)));

        return List.copyOf(out);
    }

    private static CaseMaterial material(
            int index, String route, String principal, String tenant, String role,
            HttpResponse left, HttpResponse right) {
        return new CaseMaterial(
                String.format("S11-EVAL-%03d", index),
                route, principal, tenant, role, left, right);
    }

    private static HttpResponse get(
            int port, String target, boolean bearer, boolean customPrincipal) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + target))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .header("Accept", "application/json");
        if (bearer) request.header("Authorization", "Bearer " + TOKEN);
        if (customPrincipal) request.header("X-S11-Principal", "user-a");
        var response = CLIENT.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        return new HttpResponse(
                response.statusCode(), List.of(),
                response.body().getBytes(StandardCharsets.UTF_8),
                "application/json", HttpProtocol.HTTP_1_1, new byte[0]);
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s11synthetic";
    }

    private record CaseMaterial(
            String caseId, String route, String principal, String tenant, String role,
            HttpResponse left, HttpResponse right) { }
}
