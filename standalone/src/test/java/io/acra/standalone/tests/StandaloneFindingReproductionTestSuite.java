package io.acra.standalone.tests;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.testing.TestState;
import io.acra.core.security.TokenFingerprint;
import io.acra.standalone.model.ControlledExecutionRecord;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneFindingLifecycleService;
import io.acra.standalone.service.StandaloneFindingReproductionService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.ControlledExecutionStore;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class StandaloneFindingReproductionTestSuite {
    private static int assertions;

    private StandaloneFindingReproductionTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s11-reproduction-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Sprint 11 Reproduction", "Deterministic reproduction");
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:18911/api/v1",
                    "LAB",
                    "AUTH-S11-REPRO",
                    "CONTROLLED_LAB");

            String spec = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Controlled Lab","version":"1"},
                      "paths":{
                        "/demo":{
                          "get":{"responses":{"200":{"description":"ok"},"403":{"description":"denied"}}}
                        }
                      }
                    }
                    """;
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "lab.json", spec);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "viewer-a", "Viewer A", "BEARER");
            contexts.addRole(project.id(), "viewer", "Viewer");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "demo-resource", "demo", "viewer-a", "tenant-a", "ACTIVE");
            var expectation = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/demo", "READ",
                    "viewer-a", "viewer", "tenant-a", "demo-resource",
                    "DENY", "Equivalent route must remain denied");

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var activeEvidence = evidence.captureExecutionSummary(
                    project.id(),
                    target.id(),
                    "controlled-route-equivalence:fixture",
                    "testId=S11-REPRO-TEST\nexpected=DENY\nobserved=ALLOW\ndifferential=UNEXPECTED_CHANGE");

            UUID runId = UUID.randomUUID();
            Instant executedAt = Instant.parse("2026-09-26T03:40:00Z");
            ControlledExecutionRecord execution = new ControlledExecutionRecord(
                    runId,
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "S11-REPRO-TEST",
                    "S11-REPRO-EXEC",
                    "S11-REPRO-OBS",
                    "/api/v1/demo",
                    "/api/v1/demo",
                    "/api/v1/demo/",
                    AuthorizationDecision.DENY,
                    AuthorizationOutcome.ALLOW,
                    DifferentialClassification.UNEXPECTED_CHANGE,
                    TestState.COMPLETED,
                    activeEvidence.evidenceId(),
                    14,
                    executedAt);
            new ControlledExecutionStore(workspace).save(execution);

            StandaloneFindingLifecycleService lifecycle = new StandaloneFindingLifecycleService(workspace);
            var opened = lifecycle.openFromExecution(project.id(), runId);
            check(opened.finding().state() == FindingLifecycleState.NEEDS_REVIEW,
                    "source execution opens reviewed finding");

            StandaloneFindingReproductionService reproduction =
                    new StandaloneFindingReproductionService(workspace);
            var initialPackage = reproduction.reproduction(project.id(), opened.finding().findingId());
            var initialJson = reproduction.json(project.id(), opened.finding().findingId());

            check(initialPackage.generatedAt().equals(opened.finding().updatedAt()),
                    "package generation timestamp is bound to finding state timestamp");
            check(!initialPackage.confirmedFinding(),
                    "needs-review reproduction cannot claim confirmation");
            check(initialPackage.assessmentIds().contains("controlled-route-equivalence:" + runId),
                    "package retains source run reference");
            check(initialPackage.executionIds().contains(execution.executionId()),
                    "package retains Core execution reference");
            check(initialPackage.observationIds().contains(execution.observationId()),
                    "package retains observation reference");
            check(initialPackage.evidenceIds().contains(activeEvidence.evidenceId().toString()),
                    "package retains active evidence reference");
            check(initialPackage.reviewTrail().isEmpty(),
                    "package does not invent review history");

            var initialSarif = reproduction.sarif(project.id(), opened.finding().findingId());
            var initialDraft = reproduction.burpDraft(project.id(), opened.finding().findingId());
            check(initialSarif.content().contains("\"version\":\"2.1.0\""),
                    "SARIF 2.1.0 version is emitted");
            check(initialSarif.content().contains("\"acraLifecycleState\":\"NEEDS_REVIEW\""),
                    "SARIF preserves needs-review lifecycle state");
            check(initialSarif.content().contains("\"kind\":\"review\""),
                    "needs-review SARIF result is a review result");
            check(!initialDraft.publicationEligible(),
                    "needs-review Burp draft is not publication eligible");
            check(initialDraft.confidence().name().equals("TENTATIVE"),
                    "unconfirmed Burp draft remains tentative");

            var validationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-validation-repro", "Validation artifact");
            var validated = lifecycle.transition(
                    project.id(),
                    opened.finding().findingId(),
                    "VALIDATED",
                    "reviewer-alpha-hidden",
                    "Unique validation reason that must not be exported",
                    List.of(validationEvidence.evidenceId()));

            var validatedPackage = reproduction.reproduction(project.id(), validated.finding().findingId());
            var validatedJsonA = reproduction.json(project.id(), validated.finding().findingId());
            var validatedJsonB = reproduction.json(project.id(), validated.finding().findingId());

            check(validatedPackage.state() == FindingLifecycleState.VALIDATED,
                    "package reflects validated lifecycle state");
            check(validatedPackage.reviewTrail().size() == 1,
                    "package retains minimized transition trail");
            check(validatedPackage.reviewTrail().getFirst().evidenceIds().contains(
                            validationEvidence.evidenceId().toString()),
                    "review trail retains review evidence ID");
            check(!validatedJsonA.content().contains("reviewer-alpha-hidden"),
                    "JSON structurally excludes reviewer identity");
            check(!validatedJsonA.content().contains("Unique validation reason that must not be exported"),
                    "JSON structurally excludes review reason");
            check(!validatedJsonA.content().contains("\"reviewerReference\"")
                            && !validatedJsonA.content().contains("\"reason\""),
                    "JSON schema contains no reviewer/reason fields");
            check(!validatedJsonA.content().contains("Authorization:")
                            && !validatedJsonA.content().contains("Bearer "),
                    "JSON contains no fabricated credential-bearing HTTP material");
            check(validatedJsonA.content().equals(validatedJsonB.content()),
                    "unchanged finding state yields deterministic JSON");
            check(validatedJsonA.sha256().equals(validatedJsonB.sha256()),
                    "unchanged finding state yields stable SHA-256");
            check(validatedJsonA.sha256().equals(TokenFingerprint.sha256(validatedJsonA.content())),
                    "export SHA-256 matches canonical JSON bytes");
            check(!initialJson.sha256().equals(validatedJsonA.sha256()),
                    "review state/evidence change changes export digest");

            var confirmationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-confirmation-repro", "Confirmation artifact");
            var confirmed = lifecycle.transition(
                    project.id(),
                    validated.finding().findingId(),
                    "CONFIRMED",
                    "reviewer-beta-hidden",
                    "Independent confirmation reason that must not be exported",
                    List.of(confirmationEvidence.evidenceId()));

            var confirmedPackage = reproduction.reproduction(project.id(), confirmed.finding().findingId());
            var confirmedJson = reproduction.json(project.id(), confirmed.finding().findingId());
            check(confirmedPackage.state() == FindingLifecycleState.CONFIRMED,
                    "package reflects explicit confirmed state");
            check(confirmedPackage.confirmedFinding(),
                    "confirmedFinding only follows explicit lifecycle confirmation");
            check(confirmedPackage.reviewTrail().size() == 2,
                    "confirmed package preserves both minimized transitions");
            check(!confirmedJson.content().contains("reviewer-beta-hidden")
                            && !confirmedJson.content().contains("Independent confirmation reason"),
                    "confirmed JSON still excludes reviewer identity/reason");
            check(confirmedJson.content().contains("\"confirmedFinding\":true"),
                    "canonical JSON explicitly records confirmed state");
            check(confirmedJson.content().contains("\"expectedDecision\":\"DENY\"")
                            && confirmedJson.content().contains("\"observedDecision\":\"ALLOW\""),
                    "canonical JSON preserves authorization mismatch");
            check(confirmedJson.content().contains(runId.toString()),
                    "canonical JSON retains source-run lineage through assessment reference");
            check(confirmedJson.content().contains(activeEvidence.evidenceId().toString()),
                    "canonical JSON retains active evidence lineage");
            check(!confirmedJson.content().contains("viewer-s11")
                            && !confirmedJson.content().contains("admin-s11"),
                    "canonical JSON contains no transient fixture credential material");

            var confirmedSarifA = reproduction.sarif(project.id(), confirmed.finding().findingId());
            var confirmedSarifB = reproduction.sarif(project.id(), confirmed.finding().findingId());
            var confirmedDraftA = reproduction.burpDraft(project.id(), confirmed.finding().findingId());
            var confirmedDraftB = reproduction.burpDraft(project.id(), confirmed.finding().findingId());

            check(confirmedSarifA.content().contains("\"acraLifecycleState\":\"CONFIRMED\""),
                    "confirmed SARIF preserves lifecycle state");
            check(confirmedSarifA.content().contains("\"confirmedFinding\":true"),
                    "confirmed SARIF preserves explicit confirmation");
            check(confirmedSarifA.content().contains("\"kind\":\"fail\""),
                    "confirmed SARIF result is a fail result");
            check(confirmedSarifA.content().equals(confirmedSarifB.content())
                            && confirmedSarifA.sha256().equals(confirmedSarifB.sha256()),
                    "unchanged confirmed state yields deterministic SARIF");
            check(!confirmedSarifA.content().contains("reviewer-alpha-hidden")
                            && !confirmedSarifA.content().contains("reviewer-beta-hidden")
                            && !confirmedSarifA.content().contains("Unique validation reason")
                            && !confirmedSarifA.content().contains("Independent confirmation reason"),
                    "SARIF excludes reviewer identity and review reasons");

            check(confirmedDraftA.publicationEligible(),
                    "explicit confirmed state makes Burp draft publication eligible");
            check(confirmedDraftA.severity().name().equals("LOW"),
                    "standalone LOW severity maps to Burp LOW draft severity");
            check(confirmedDraftA.confidence().name().equals("CERTAIN"),
                    "confirmed HIGH confidence maps to CERTAIN draft confidence");
            check(confirmedDraftA.draftId().equals(confirmedDraftB.draftId()),
                    "Burp draft identity is deterministic");
            check(confirmedDraftA.limitations().stream()
                            .anyMatch(value -> value.contains("does not publish")),
                    "Burp draft explicitly states non-publication boundary");
            String draftMaterial = new io.acra.core.serialization.DomainSerializer().serialize(confirmedDraftA);
            check(!draftMaterial.contains("reviewer-alpha-hidden")
                            && !draftMaterial.contains("reviewer-beta-hidden")
                            && !draftMaterial.contains("Authorization:")
                            && !draftMaterial.contains("Bearer "),
                    "Burp draft excludes reviewer and credential material");

            var reopened = new StandaloneFindingReproductionService(new LocalWorkspaceStore(temp))
                    .json(project.id(), confirmed.finding().findingId());
            check(reopened.content().equals(confirmedJson.content())
                            && reopened.sha256().equals(confirmedJson.sha256()),
                    "restart produces identical deterministic reproduction JSON");
            var reopenedSarif = new StandaloneFindingReproductionService(new LocalWorkspaceStore(temp))
                    .sarif(project.id(), confirmed.finding().findingId());
            check(reopenedSarif.content().equals(confirmedSarifA.content())
                            && reopenedSarif.sha256().equals(confirmedSarifA.sha256()),
                    "restart produces identical deterministic SARIF");

            System.out.println("SPRINT11_STANDALONE_FINDING_REPRODUCTION PASS assertions=" + assertions);
        } finally {
            if (Files.exists(temp)) {
                try (var walk = Files.walk(temp)) {
                    for (Path pathToDelete : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(pathToDelete);
                    }
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
