package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationCampaignCellState;
import io.acra.core.active.research.AblationDimension;
import io.acra.core.active.research.AblationEvidenceBundle;
import io.acra.core.active.research.AblationEvidenceDisposition;
import io.acra.core.active.research.S11AblationCampaignPlan;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.active.research.S11CampaignEvidenceReadinessProjector;
import io.acra.core.active.research.S11TreatmentEvidenceCollector;
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
import java.util.List;
import java.util.Map;

public final class Sprint11TreatmentEvidenceCollectionTestSuite {
    private static final int SECURE = 18082;
    private static final int VULNERABLE = 18081;
    private static final String TOKEN = token("user-a", "tenant-a", "viewer");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint11TreatmentEvidenceCollectionTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT11_TREATMENT_EVIDENCE_COLLECTION PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        S11TreatmentEvidenceCollector collector = new S11TreatmentEvidenceCollector();
        List<AblationEvidenceBundle> bundles = collectAll(collector);
        int assertions = 0;

        TestSupport.assertEquals(15, bundles.size(),
                "all fifteen registered cases produce evidence bundles");
        assertions++;

        var protocol = S11AblationProtocol.canonical();
        for (int index = 0; index < bundles.size(); index++) {
            AblationEvidenceBundle bundle = bundles.get(index);
            TestSupport.assertEquals(String.format("S11-EVAL-%03d", index + 1), bundle.caseId(),
                    "evidence bundle ordering is deterministic");
            assertions++;
            TestSupport.assertTrue(!bundle.baselineEvidenceIds().isEmpty(),
                    "every case has baseline evidence");
            assertions++;
            TestSupport.assertEquals(7, bundle.dimensionEvidence().size(),
                    "every case accounts for all seven ablation dimensions");
            assertions++;
            TestSupport.assertTrue(bundle.completeFor(protocol.variants().getLast().enabledDimensions()),
                    "every fixture bundle is complete through A7");
            assertions++;
            TestSupport.assertTrue(bundle.missingDimensions(
                    protocol.variants().getLast().enabledDimensions()).isEmpty(),
                    "A7 completeness reports no missing dimensions");
            assertions++;

            for (String evidenceId : allEvidenceIds(bundle)) {
                TestSupport.assertTrue(evidenceId.matches("s11-evidence-[0-9a-f]{32}"),
                        "persisted evidence reference is hash-only");
                assertions++;
                TestSupport.assertNotContains(evidenceId, "user-a",
                        "evidence ID does not persist raw principal");
                assertions++;
                TestSupport.assertNotContains(evidenceId, "tenant-a",
                        "evidence ID does not persist raw tenant");
                assertions++;
                TestSupport.assertNotContains(evidenceId, "foreign",
                        "evidence ID does not persist raw response resource values");
                assertions++;
            }
        }

        AblationEvidenceBundle publicBundle = bundles.getFirst();
        TestSupport.assertEquals(
                AblationEvidenceDisposition.NOT_APPLICABLE,
                publicBundle.evidenceFor(AblationDimension.IDENTITY).disposition(),
                "public control records identity as explicitly not applicable");
        assertions++;
        TestSupport.assertEquals(
                AblationEvidenceDisposition.NOT_APPLICABLE,
                publicBundle.evidenceFor(AblationDimension.ROLE).disposition(),
                "public control records role as explicitly not applicable");
        assertions++;

        AblationEvidenceBundle authenticated = bundles.get(7);
        TestSupport.assertEquals(
                AblationEvidenceDisposition.OBSERVED,
                authenticated.evidenceFor(AblationDimension.IDENTITY).disposition(),
                "authenticated positive control records identity evidence");
        assertions++;
        TestSupport.assertEquals(
                AblationEvidenceDisposition.OBSERVED,
                authenticated.evidenceFor(AblationDimension.ROLE).disposition(),
                "authenticated positive control records role evidence");
        assertions++;

        for (AblationEvidenceBundle bundle : bundles) {
            TestSupport.assertEquals(
                    AblationEvidenceDisposition.NOT_APPLICABLE,
                    bundle.evidenceFor(AblationDimension.WORKFLOW).disposition(),
                    "non-workflow research fixtures account for workflow as not applicable");
            assertions++;
        }

        S11AblationCampaignPlan canonical = S11AblationCampaignPlan.canonical();
        var projected = new S11CampaignEvidenceReadinessProjector().project(canonical, bundles);
        TestSupport.assertEquals(120, projected.coverage().evidenceReadyCells(),
                "complete evidence bundles promote all 120 campaign cells to EVIDENCE_READY");
        assertions++;
        TestSupport.assertEquals(0, projected.coverage().plannedCells(),
                "no planned cells remain after complete evidence collection");
        assertions++;
        TestSupport.assertEquals(0, projected.coverage().executedCells(),
                "evidence collection executes no campaign cells");
        assertions++;

        var missingOne = new ArrayList<>(bundles);
        missingOne.removeLast();
        var partial = new S11CampaignEvidenceReadinessProjector().project(canonical, missingOne);
        TestSupport.assertEquals(112, partial.coverage().evidenceReadyCells(),
                "missing one case bundle leaves exactly eight cells unready");
        assertions++;
        TestSupport.assertEquals(8, partial.coverage().plannedCells(),
                "one missing case leaves its A0-A7 cells planned");
        assertions++;
        TestSupport.assertEquals(0, partial.coverage().executedCells(),
                "partial evidence projection still executes nothing");
        assertions++;

        var duplicate = new ArrayList<>(bundles);
        duplicate.add(bundles.getFirst());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11CampaignEvidenceReadinessProjector().project(canonical, duplicate),
                "duplicate evidence bundle fails closed");
        assertions++;

        for (RecordComponent component : AblationEvidenceBundle.class.getRecordComponents()) {
            String name = component.getName().toLowerCase();
            TestSupport.assertTrue(!name.contains("groundtruth"),
                    "evidence bundle schema contains no ground-truth field");
            assertions++;
            TestSupport.assertTrue(!name.contains("prediction"),
                    "evidence bundle schema contains no prediction field");
            assertions++;
        }

        S11TreatmentEvidenceCollector repeatedCollector = new S11TreatmentEvidenceCollector();
        List<AblationEvidenceBundle> repeated = collectAll(repeatedCollector);
        for (int index = 0; index < bundles.size(); index++) {
            TestSupport.assertEquals(bundles.get(index).fingerprint(), repeated.get(index).fingerprint(),
                    "evidence bundle fingerprint is deterministic for controlled inputs");
            assertions++;
        }

        return assertions;
    }

    private static List<AblationEvidenceBundle> collectAll(
            S11TreatmentEvidenceCollector collector) throws Exception {
        List<AblationEvidenceBundle> out = new ArrayList<>();

        out.add(collector.collect(
                "S11-EVAL-001", "/api/v1/s4/public",
                "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));

        out.add(collector.collect(
                "S11-EVAL-002", "/api/v1/s4/application-denial",
                "user-a", "tenant-a", "viewer",
                get(SECURE, "/api/v1/s4/application-denial", true, false),
                get(SECURE, "/api/v1/s4/application-denial", true, false)));

        for (int index = 3; index <= 6; index++) {
            String caseId = String.format("S11-EVAL-%03d", index);
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            out.add(collector.collect(
                    caseId, route, "", "", "",
                    get(SECURE, route + "?variant=a", false, false),
                    get(SECURE, route + "?variant=b", false, false)));
        }

        out.add(collector.collect(
                "S11-EVAL-007", "/api/v1/s4/public",
                "", "", "",
                get(SECURE, "/api/v1/s4/public?variant=a", false, false),
                get(SECURE, "/api/v1/s4/public?variant=b", false, false)));

        for (int index = 8; index <= 14; index++) {
            String caseId = String.format("S11-EVAL-%03d", index);
            String route = String.format("/api/v1/s11/research/case-%03d", index);
            String suffix = index == 10 ? "?variant=a&pad=17" : "?variant=a";
            out.add(collector.collect(
                    caseId, route, "user-a", "tenant-a", "viewer",
                    get(SECURE, route + suffix, true, false),
                    get(VULNERABLE, route + suffix, true, false)));
        }

        out.add(collector.collect(
                "S11-EVAL-015", "/api/v1/s11/research/case-015",
                "user-a", "", "",
                get(SECURE, "/api/v1/s11/research/case-015?variant=a", false, true),
                get(VULNERABLE, "/api/v1/s11/research/case-015?variant=a", false, true)));

        return List.copyOf(out);
    }

    private static List<String> allEvidenceIds(AblationEvidenceBundle bundle) {
        ArrayList<String> out = new ArrayList<>(bundle.baselineEvidenceIds());
        bundle.dimensionEvidence().forEach(value -> out.addAll(value.evidenceIds()));
        return out;
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
}
