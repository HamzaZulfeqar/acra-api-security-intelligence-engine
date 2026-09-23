package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.util.ArrayList;
import java.util.List;

public final class AuthorizationReportGenerator {
    private final DomainSerializer serializer = new DomainSerializer();

    public AuthorizationReport generate(AuthorizationAnalysisRequest request, AuthorizationAnalysisResult result) {
        if (request == null || result == null) throw new IllegalArgumentException("request and result required");
        List<String> limitations = new ArrayList<>();
        if (!result.contextAssessment().evidenceVerified()) limitations.add("EVIDENCE_NOT_VERIFIED");
        if (!result.correlation().independentlyCorroborated()) limitations.add("INDEPENDENT_CORROBORATION_UNVERIFIED");
        if (result.findingCandidate().state() == io.acra.core.domain.finding.FindingCandidateState.INCONCLUSIVE) {
            limitations.add("FINDING_CANDIDATE_INCONCLUSIVE");
        }
        String material = request.projectId() + "|" + request.executionId() + "|" + request.testId()
                + "|" + result.findingCandidate().candidateId();
        return new AuthorizationReport("report-" + TokenFingerprint.sha256(material).substring(0, 24),
                request.projectId(), request.executionId(), request.testId(),
                result.contextAssessment().resolutionStatus(), result.correlation().aggregate().state(),
                result.findingCandidate().state(), result.riskAssessment().severity(),
                result.riskAssessment().confidence(), result.findingCandidate().dimensions(),
                result.findingCandidate().assessmentIds(), result.findingCandidate().supportingEvidenceIds(),
                limitations.stream().distinct().sorted().toList());
    }

    public String renderJson(AuthorizationReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
        return serializer.serialize(report);
    }
}
