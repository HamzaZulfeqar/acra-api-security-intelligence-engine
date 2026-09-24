package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationDimension;
import io.acra.core.active.research.AblationDimensionEvidenceReference;
import io.acra.core.active.research.AblationEvidenceBundle;
import io.acra.core.active.research.AblationEvidenceDisposition;
import io.acra.core.active.research.AblationPredictionResult;
import io.acra.core.active.research.AblationPredictionState;
import io.acra.core.active.research.ResearchExecutionRecord;
import io.acra.core.active.research.ResearchMetrics;
import io.acra.core.active.research.ResearchPrediction;
import io.acra.core.active.research.S11AblationEvaluationReport;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.active.research.S11EvaluationDatasetManifest;
import io.acra.core.active.research.S11VariantEvaluation;
import io.acra.core.reporting.s11.S11ResearchReportExporter;
import io.acra.core.reporting.s11.S11ResearchReportGenerator;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Sprint11ResearchReportExportTestSuite {
    private Sprint11ResearchReportExportTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT11_RESEARCH_REPORT_EXPORT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        S11EvaluationDatasetManifest dataset = S11EvaluationDatasetManifest.canonical();
        S11AblationEvaluationReport evaluation = evaluationFixture(dataset);

        S11ResearchReportGenerator generator = new S11ResearchReportGenerator();
        var report = generator.generate(protocol, dataset, evaluation);
        S11ResearchReportExporter exporter = new S11ResearchReportExporter();
        var json = exporter.json(report);
        var markdown = exporter.markdown(report);

        int assertions = 0;
        TestSupport.assertEquals("s11-research-evaluation-report-v1", report.reportVersion(),
                "report version is explicit");
        assertions++;
        TestSupport.assertEquals(15, report.caseCount(), "report preserves dataset case count");
        assertions++;
        TestSupport.assertEquals(8, report.positiveCaseCount(), "report preserves positive count");
        assertions++;
        TestSupport.assertEquals(7, report.negativeCaseCount(), "report preserves negative count");
        assertions++;
        TestSupport.assertEquals(8, report.variants().size(), "report includes A0-A7");
        assertions++;
        TestSupport.assertEquals("0.533333", report.variants().getFirst().precision(),
                "report renders controlled A0 precision deterministically");
        assertions++;
        TestSupport.assertEquals("0.695652", report.variants().getFirst().f1(),
                "report renders controlled A0 F1 deterministically");
        assertions++;
        TestSupport.assertEquals("1.000000", report.variants().getLast().precision(),
                "report renders controlled A7 precision deterministically");
        assertions++;
        TestSupport.assertEquals("1.000000", report.variants().getLast().f1(),
                "report renders controlled A7 F1 deterministically");
        assertions++;

        TestSupport.assertContains(json.content(), "\"protocolFingerprint\"",
                "JSON includes protocol fingerprint");
        assertions++;
        TestSupport.assertContains(json.content(), "\"datasetFingerprint\"",
                "JSON includes dataset fingerprint");
        assertions++;
        TestSupport.assertContains(json.content(), "\"evaluationFingerprint\"",
                "JSON includes evaluation fingerprint");
        assertions++;
        TestSupport.assertContains(markdown.content(), "## Ablation metrics",
                "Markdown contains ablation metric table");
        assertions++;
        TestSupport.assertContains(markdown.content(), "controlled synthetic localhost dataset",
                "Markdown states controlled dataset limitation");
        assertions++;

        for (String unsafe : List.of(
                "Bearer ", "s11synthetic", "Authorization", "\"bodyText\"",
                "foreign-014", "Q7M2-X9P4-ZETA")) {
            TestSupport.assertNotContains(json.content(), unsafe,
                    "JSON report excludes raw/secret fixture material: " + unsafe);
            assertions++;
            TestSupport.assertNotContains(markdown.content(), unsafe,
                    "Markdown report excludes raw/secret fixture material: " + unsafe);
            assertions++;
        }

        var repeatedReport = generator.generate(protocol, dataset, evaluation);
        var repeatedJson = exporter.json(repeatedReport);
        var repeatedMarkdown = exporter.markdown(repeatedReport);
        TestSupport.assertEquals(report.reportId(), repeatedReport.reportId(),
                "report identity is deterministic");
        assertions++;
        TestSupport.assertEquals(json.sha256(), repeatedJson.sha256(),
                "canonical JSON digest is deterministic");
        assertions++;
        TestSupport.assertEquals(markdown.sha256(), repeatedMarkdown.sha256(),
                "Markdown digest is deterministic");
        assertions++;
        TestSupport.assertEquals(json.content(), repeatedJson.content(),
                "canonical JSON content is deterministic");
        assertions++;
        TestSupport.assertEquals(markdown.content(), repeatedMarkdown.content(),
                "Markdown content is deterministic");
        assertions++;

        Path output = outputDir();
        Files.createDirectories(output);
        Files.writeString(output.resolve("s11-research-evaluation.json"), json.content(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("s11-research-evaluation.json.sha256"),
                json.sha256() + "  s11-research-evaluation.json\n", StandardCharsets.UTF_8);
        Files.writeString(output.resolve("s11-research-evaluation.md"), markdown.content(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("s11-research-evaluation.md.sha256"),
                markdown.sha256() + "  s11-research-evaluation.md\n", StandardCharsets.UTF_8);

        System.out.println("SPRINT11_RESEARCH_REPORT_ARTIFACT " + output);
        System.out.println("SPRINT11_RESEARCH_REPORT_JSON_SHA256 " + json.sha256());
        System.out.println("SPRINT11_RESEARCH_REPORT_MARKDOWN_SHA256 " + markdown.sha256());

        return assertions;
    }

    private static S11AblationEvaluationReport evaluationFixture(
            S11EvaluationDatasetManifest dataset) {
        List<ResearchExecutionRecord> all = new ArrayList<>();
        List<S11VariantEvaluation> variants = new ArrayList<>();

        for (int variantIndex = 0; variantIndex <= 7; variantIndex++) {
            String variantId = "A" + variantIndex;
            List<ResearchExecutionRecord> records = new ArrayList<>();
            for (var item : dataset.cases()) {
                ResearchPrediction prediction = variantIndex <= 5
                        ? ResearchPrediction.POSITIVE
                        : (item.groundTruth() == io.acra.core.active.research.ResearchGroundTruth.POSITIVE
                                ? ResearchPrediction.POSITIVE : ResearchPrediction.NEGATIVE);
                String evidence = "s11-evidence-" + TokenFingerprint.sha256(
                        variantId + "|" + item.caseId()).substring(0, 32);
                ResearchExecutionRecord record = new ResearchExecutionRecord(
                        item.caseId(),
                        "s11-prediction-exec-report-fixture",
                        "S11-" + variantId + "-" + item.caseId(),
                        "s11-result-" + TokenFingerprint.sha256(variantId + "|" + item.caseId()).substring(0, 24),
                        List.of(evidence),
                        item.groundTruth(),
                        prediction);
                records.add(record);
                all.add(record);
            }
            variants.add(new S11VariantEvaluation(
                    variantId, records.size(), 0, ResearchMetrics.from(records), 1.0));
        }

        return new S11AblationEvaluationReport(
                "",
                dataset.datasetId(),
                "s11-prediction-exec-report-fixture",
                all,
                variants,
                "");
    }

    private static Path outputDir() {
        String configured = System.getenv("ACRA_S11_REPORT_DIR");
        return configured == null || configured.isBlank()
                ? Path.of("build", "s11-evaluation-report")
                : Path.of(configured);
    }
}
