package io.acra.core.product.finding;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.domain.finding.GovernedFinding;
import io.acra.core.engine.FindingLifecycleService;
import io.acra.core.reporting.s13.S13GovernanceExportArtifact;
import io.acra.core.reporting.s13.S13GovernanceReport;
import io.acra.core.reporting.s13.S13GovernanceReportExporter;
import io.acra.core.reporting.s13.S13GovernanceReportGenerator;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

public final class FindingGovernanceWorkspace {
    private final TreeMap<String, GovernedFinding> findings = new TreeMap<>();
    private final TreeMap<String, String> candidateFindings = new TreeMap<>();
    private final FindingLifecycleService lifecycle = new FindingLifecycleService();
    private final S13GovernanceReportGenerator reportGenerator = new S13GovernanceReportGenerator();
    private final S13GovernanceReportExporter reportExporter = new S13GovernanceReportExporter();

    public synchronized GovernedFinding open(
            FindingCandidate candidate,
            AuthorizationRiskAssessment riskAssessment) {
        GovernedFinding proposed = lifecycle.create(candidate, riskAssessment);
        String existingFindingId = candidateFindings.get(proposed.sourceCandidateId());

        if (existingFindingId != null) {
            GovernedFinding existing = findings.get(existingFindingId);
            if (existing == null) {
                throw new IllegalStateException("candidate finding index corruption");
            }
            if (!existing.sourceCandidateFingerprint().equals(proposed.sourceCandidateFingerprint())
                    || !existing.riskAssessmentId().equals(proposed.riskAssessmentId())
                    || existing.severity() != proposed.severity()
                    || existing.confidence() != proposed.confidence()) {
                throw new IllegalArgumentException("candidate finding governance drift");
            }
            return existing;
        }

        GovernedFinding collision = findings.get(proposed.findingId());
        if (collision != null && !collision.fingerprint().equals(proposed.fingerprint())) {
            throw new IllegalArgumentException("governed finding identity collision");
        }

        findings.put(proposed.findingId(), proposed);
        candidateFindings.put(proposed.sourceCandidateId(), proposed.findingId());
        return proposed;
    }

    public synchronized GovernedFinding transition(
            String findingId,
            String expectedFingerprint,
            FindingLifecycleTransitionRequest request) {
        if (findingId == null || findingId.isBlank()) {
            throw new IllegalArgumentException("findingId required");
        }
        if (expectedFingerprint == null || expectedFingerprint.isBlank()) {
            throw new IllegalArgumentException("expectedFingerprint required");
        }

        GovernedFinding current = findings.get(findingId.strip());
        if (current == null) throw new IllegalArgumentException("unknown governed finding");
        if (!current.fingerprint().equals(expectedFingerprint.strip())) {
            throw new IllegalArgumentException("stale governed finding snapshot");
        }

        GovernedFinding updated = lifecycle.transition(current, request);
        findings.put(updated.findingId(), updated);
        return updated;
    }

    public synchronized GovernedFinding find(String findingId) {
        if (findingId == null || findingId.isBlank()) {
            throw new IllegalArgumentException("findingId required");
        }
        return findings.get(findingId.strip());
    }

    public synchronized S13GovernanceReport report(Instant at) {
        return reportGenerator.generate(snapshot(), at);
    }

    public synchronized S13GovernanceExportArtifact exportJson(Instant at) {
        return reportExporter.json(report(at));
    }

    public synchronized S13GovernanceExportArtifact exportMarkdown(Instant at) {
        return reportExporter.markdown(report(at));
    }

    public synchronized FindingGovernanceSnapshot snapshot() {
        return new FindingGovernanceSnapshot(List.copyOf(findings.values()));
    }

    public synchronized void clear() {
        findings.clear();
        candidateFindings.clear();
    }
}
