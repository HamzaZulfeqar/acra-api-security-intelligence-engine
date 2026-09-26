package io.acra.core.reporting.finding;

import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;

public final class FindingBurpIssueDraftGenerator {
    private final UniversalRedactor redactor = new UniversalRedactor();

    public FindingBurpIssueDraft generate(FindingReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction required");

        FindingBurpIssueSeverity severity = severity(reproduction.state(), reproduction.severity());
        FindingBurpIssueConfidence confidence = confidence(
                reproduction.confirmedFinding(), reproduction.confidence());
        String dimension = reproduction.dimensions().size() == 1
                ? reproduction.dimensions().get(0)
                : "CONTEXT";
        String name = "ACRA API Authorization Finding - " + dimension;
        String detail = "Lifecycle state: " + reproduction.state()
                + ". Expected authorization decision: " + reproduction.expectedDecision()
                + ". Observed authorization decision: " + reproduction.observedDecision()
                + ". Endpoint: " + reproduction.endpoint()
                + ". Resource: " + reproduction.resourceId()
                + ". ACRA finding fingerprint: " + reproduction.findingFingerprint()
                + ". Reproduction ID: " + reproduction.reproductionId() + ".";
        String remediation = "Review the affected API authorization policy and enforce the intended "
                + "principal, role, tenant, resource, ownership, and workflow constraints. "
                + "Retest the corrected control using the retained ACRA evidence references.";
        String background = "ACRA correlates authorization context and evidence before a finding "
                + "enters the human review lifecycle. This Burp issue draft is a reproduction "
                + "projection of that reviewed state.";
        String remediationBackground = "Authorization decisions should be enforced server-side "
                + "for every object, function, tenant boundary, indirect reference, and workflow "
                + "transition represented by the reviewed policy context.";

        String safeName = redactor.redactText(name);
        String safeDetail = redactor.redactText(detail);
        String safeRemediation = redactor.redactText(remediation);
        String safeBackground = redactor.redactText(background);
        String safeRemediationBackground = redactor.redactText(remediationBackground);

        String draftMaterial = String.join(
                "|",
                reproduction.reproductionId(),
                reproduction.findingId(),
                reproduction.state().name(),
                severity.name(),
                confidence.name());
        String draftId = "burp-issue-draft-"
                + TokenFingerprint.sha256(draftMaterial).substring(0, 24);

        return new FindingBurpIssueDraft(
                draftId,
                reproduction.reproductionId(),
                reproduction.findingId(),
                reproduction.state(),
                safeName,
                safeDetail,
                safeRemediation,
                redactor.redactText(reproduction.endpoint()),
                severity,
                confidence,
                safeBackground,
                safeRemediationBackground,
                severity == FindingBurpIssueSeverity.FALSE_POSITIVE
                        ? FindingBurpIssueSeverity.INFORMATION
                        : severity,
                reproduction.confirmedFinding(),
                reproduction.evidenceIds(),
                java.util.List.of(
                        "This draft does not publish an issue to Burp Suite.",
                        "A full base URL must be supplied at the Montoya adapter boundary.",
                        "Verified HttpRequestResponse objects are optional and are not fabricated.",
                        "Real Burp desktop publication remains a separate runtime validation gate."));
    }

    private static FindingBurpIssueSeverity severity(
            FindingLifecycleState state,
            FindingSeverity severity) {
        if (state == FindingLifecycleState.FALSE_POSITIVE) {
            return FindingBurpIssueSeverity.FALSE_POSITIVE;
        }
        return switch (severity) {
            case CRITICAL, HIGH -> FindingBurpIssueSeverity.HIGH;
            case MEDIUM -> FindingBurpIssueSeverity.MEDIUM;
            case LOW -> FindingBurpIssueSeverity.LOW;
            case INFO -> FindingBurpIssueSeverity.INFORMATION;
        };
    }

    private static FindingBurpIssueConfidence confidence(
            boolean confirmedFinding,
            FindingConfidence confidence) {
        if (!confirmedFinding) return FindingBurpIssueConfidence.TENTATIVE;
        return switch (confidence) {
            case HIGH -> FindingBurpIssueConfidence.CERTAIN;
            case MEDIUM -> FindingBurpIssueConfidence.FIRM;
            case LOW, INSUFFICIENT -> FindingBurpIssueConfidence.TENTATIVE;
        };
    }
}
