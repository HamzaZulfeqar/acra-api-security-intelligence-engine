package io.acra.core.reporting.reproduction;

public final class BurpIssueDraftFactory {
    private static final String VERSION = "acra-burp-issue-draft-v1";

    public BurpIssueDraft from(ReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction package required");

        String detail = "ACRA authorization review artifact. "
                + "Expected decision: " + html(reproduction.expectedDecision().name())
                + ". Observed decision: " + html(reproduction.observedDecision().name())
                + ". Candidate state: " + html(reproduction.state().name())
                + ". Endpoint: " + html(reproduction.endpoint())
                + ". Resource: " + html(reproduction.resourceId())
                + ". Dimensions: " + html(String.join(", ", reproduction.dimensions()))
                + ". Reproduction package: " + html(reproduction.packageId())
                + ".";

        String remediation = "Review the authorization policy and supporting evidence. "
                + "Reproduce the behavior in an authorized environment before changing controls.";

        String background = "ACRA generated this item as a review artifact. "
                + "It is not a confirmed vulnerability and has not been submitted to Burp.";

        return new BurpIssueDraft(
                "",
                VERSION,
                reproduction.packageId(),
                reproduction.candidateId(),
                "ACRA authorization review artifact",
                detail,
                remediation,
                reproduction.endpoint(),
                BurpIssueDraftSeverity.INFORMATION,
                BurpIssueDraftConfidence.TENTATIVE,
                background,
                "",
                reproduction.evidenceIds(),
                false,
                BurpIssueSubmissionState.NOT_SUBMITTED);
    }

    private static String html(String value) {
        String input = value == null ? "" : value;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
