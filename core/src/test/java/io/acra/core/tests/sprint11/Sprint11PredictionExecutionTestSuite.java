package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationCaseEvidence;
import io.acra.core.active.research.AblationEvidenceBundle;
import io.acra.core.active.research.AblationPredictionState;
import io.acra.core.active.research.ResearchPrediction;
import io.acra.core.active.research.S11AblationCampaignPlan;
import io.acra.core.active.research.S11AblationPredictionExecutor;
import io.acra.core.active.research.S11CampaignEvidenceReadinessProjector;
import io.acra.core.active.research.S11PredictionExecutionSnapshot;
import io.acra.core.active.research.S11TreatmentEvidenceCollector;
import io.acra.core.active.research.S11TreatmentPredictionSignalBuilder;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.tests.TestSupport;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

public final class Sprint11PredictionExecutionTestSuite {
    private static final int SECURE = 18082;
    private static final int VULNERABLE = 18081;
    private static final String TOKEN = token("user-a", "tenant-a", "viewer");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint11PredictionExecutionTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT11_PREDICTION_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        S11TreatmentEvidenceCollector evidenceCollector = new S11TreatmentEvidenceCollector();
        S11TreatmentPredictionSignalBuilder signalBuilder = new S11TreatmentPredictionSignalBuilder();
        List<CaseMaterial> materials = caseMaterials();

        List<AblationEvidenceBundle> bundles = new ArrayList<>();
        List<AblationCaseEvidence> predictionEvidence = new ArrayList<>();
        for (CaseMaterial material : materials) {
            AblationEvidenceBundle bundle = evidenceCollector.collect(
                    material.caseId(),
                    material.route(),
                    material.principal(),
                    material.tenant(),
                    material.role(),
                    material.left(),
                    material.right());
            bundles.add(bundle);
            predictionEvidence.add(signalBuilder.build(bundle, material.left(), material.right()));
        }

        int assertions = 0;
        S11AblationCampaignPlan canonical = S11AblationCampaignPlan.canonical();
        S11AblationCampaignPlan evidenceReady =
                new S11CampaignEvidenceReadinessProjector().project(canonical, bundles);
        TestSupport.assertEquals(120, evidenceReady.coverage().evidenceReadyCells(),
                "prediction execution begins only after all 120 cells are evidence-ready");
        assertions++;
        TestSupport.assertEquals(0, evidenceReady.coverage().executedCells(),
                "evidence-ready input has no executed cells");
        assertions++;

        S11PredictionExecutionSnapshot snapshot =
                new S11AblationPredictionExecutor().execute(evidenceReady, predictionEvidence);

        TestSupport.assertEquals(120, snapshot.results().size(),
                "prediction execution produces exactly one result per campaign cell");
        assertions++;
        TestSupport.assertEquals(120, snapshot.campaignPlan().coverage().executedCells(),
                "all evidence-ready cells become executed after explicit prediction result");
        assertions++;
        TestSupport.assertEquals(0, snapshot.campaignPlan().coverage().evidenceReadyCells(),
                "executed plan contains no residual evidence-ready cells");
        assertions++;
        TestSupport.assertEquals(0, snapshot.campaignPlan().coverage().plannedCells(),
                "executed plan contains no planned cells");
        assertions++;

        HashSet<String> resultIds = new HashSet<>();
        for (var result : snapshot.results()) {
            TestSupport.assertTrue(resultIds.add(result.resultId()),
                    "prediction result IDs are unique");
            assertions++;
            TestSupport.assertEquals(AblationPredictionState.RESOLVED, result.state(),
                    "complete evidence produces resolved prediction");
            assertions++;
            TestSupport.assertTrue(result.prediction() != null,
                    "resolved prediction contains binary prediction");
            assertions++;
        }

        var case3A0 = result(snapshot, "S11-EVAL-003", "A0");
        var case3A6 = result(snapshot, "S11-EVAL-003", "A6");
        TestSupport.assertEquals(ResearchPrediction.POSITIVE, case3A0.prediction(),
                "naive raw differential treats timestamp-only variation as positive");
        assertions++;
        TestSupport.assertEquals(ResearchPrediction.NEGATIVE, case3A6.prediction(),
                "semantic treatment resolves timestamp-only variation as negative");
        assertions++;

        var case14A3 = result(snapshot, "S11-EVAL-014", "A3");
        var case14A6 = result(snapshot, "S11-EVAL-014", "A6");
        TestSupport.assertEquals(ResearchPrediction.POSITIVE, case14A3.prediction(),
                "collection membership change is positive once ownership/tenant context is added");
        assertions++;
        TestSupport.assertEquals(ResearchPrediction.POSITIVE, case14A6.prediction(),
                "semantic equivalence cannot erase earlier ownership/tenant conflict");
        assertions++;

        var case8A0 = result(snapshot, "S11-EVAL-008", "A0");
        var case8A6 = result(snapshot, "S11-EVAL-008", "A6");
        TestSupport.assertEquals(ResearchPrediction.POSITIVE, case8A0.prediction(),
                "same-status secure/vulnerable raw responses differ");
        assertions++;
        TestSupport.assertEquals(ResearchPrediction.POSITIVE, case8A6.prediction(),
                "semantic treatment preserves authorization-relevant response difference");
        assertions++;

        S11PredictionExecutionSnapshot repeated =
                new S11AblationPredictionExecutor().execute(evidenceReady, predictionEvidence);
        TestSupport.assertEquals(snapshot.executionId(), repeated.executionId(),
                "prediction execution identity is deterministic");
        assertions++;
        TestSupport.assertEquals(snapshot.fingerprint(), repeated.fingerprint(),
                "prediction execution fingerprint is deterministic");
        assertions++;

        List<AblationCaseEvidence> missingCase = new ArrayList<>(predictionEvidence);
        missingCase.removeLast();
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationPredictionExecutor().execute(evidenceReady, missingCase),
                "missing case evidence fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationPredictionExecutor().execute(canonical, predictionEvidence),
                "prediction execution rejects non-evidence-ready plan");
        assertions++;

        for (RecordComponent component : S11PredictionExecutionSnapshot.class.getRecordComponents()) {
            String name = component.getName().toLowerCase();
            TestSupport.assertTrue(!name.contains("groundtruth"),
                    "prediction execution snapshot contains no ground-truth field");
            assertions++;
            TestSupport.assertTrue(!name.contains("metric"),
                    "prediction execution snapshot contains no metric field");
            assertions++;
        }

        return assertions;
    }

    private static io.acra.core.active.research.AblationPredictionResult result(
            S11PredictionExecutionSnapshot snapshot,
            String caseId,
            String variantId) {
        return snapshot.results().stream()
                .filter(value -> value.caseId().equals(caseId) && value.variantId().equals(variantId))
                .findFirst()
                .orElseThrow();
    }

    private static List<CaseMaterial> caseMaterials() throws Exception {
        List<CaseMaterial> out = new ArrayList<>();

        out.add(material(
                1, "/api/v1/s4/public", "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));
        out.add(material(
                2, "/api/v1/s4/application-denial", "user-a", "tenant-a", "viewer",
                get(SECURE, "/api/v1/s4/application-denial", true, false),
                get(SECURE, "/api/v1/s4/application-denial", true, false)));

        for (int index = 3; index <= 6; index++) {
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            out.add(material(
                    index, route, "", "", "",
                    get(SECURE, route + "?variant=a", false, false),
                    get(SECURE, route + "?variant=b", false, false)));
        }

        out.add(material(
                7, "/api/v1/s4/public", "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));

        for (int index = 8; index <= 14; index++) {
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            String suffix = index == 10 ? "?variant=a&pad=17" : "?variant=a";
            out.add(material(
                    index, route, "user-a", "tenant-a", "viewer",
                    get(SECURE, route + suffix, true, false),
                    get(VULNERABLE, route + suffix, true, false)));
        }

        out.add(material(
                15, "/api/v1/s11/research/case-015", "user-a", "", "",
                get(SECURE, "/api/v1/s11/research/case-015?variant=a", false, true),
                get(VULNERABLE, "/api/v1/s11/research/case-015?variant=a", false, true)));

        return List.copyOf(out);
    }

    private static CaseMaterial material(
            int index,
            String route,
            String principal,
            String tenant,
            String role,
            HttpResponse left,
            HttpResponse right) {
        return new CaseMaterial(
                String.format("S11-EVAL-%03d", index),
                route, principal, tenant, role, left, right);
    }

    private static HttpResponse get(
            int port,
            String target,
            boolean bearer,
            boolean customPrincipal) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + target))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .header("Accept", "application/json");
        if (bearer) request.header("Authorization", "Bearer " + TOKEN);
        if (customPrincipal) request.header("X-S11-Principal", "user-a");
        var response = CLIENT.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        return new HttpResponse(
                response.statusCode(),
                List.of(),
                response.body().getBytes(StandardCharsets.UTF_8),
                "application/json",
                HttpProtocol.HTTP_1_1,
                new byte[0]);
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
            String caseId,
            String route,
            String principal,
            String tenant,
            String role,
            HttpResponse left,
            HttpResponse right) { }
}
