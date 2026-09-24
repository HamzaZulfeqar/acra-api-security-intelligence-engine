package io.acra.core.reporting.s12;

public final class S12BurpIssueProjector {
    public static final String VERSION = "s12-burp-issue-projection-v1";

    public S12BurpIssueProjection project(S12ReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction package required");
        String detail = "ACRA review candidate. Expected authorization="
                + reproduction.expectedDecision()
                + "; observed authorization=" + reproduction.observedDecision()
                + "; state=" + reproduction.candidateState()
                + ". Human verification is required before treating this as a vulnerability.";
        return new S12BurpIssueProjection(
                VERSION,
                reproduction.sourceCandidateId(),
                "ACRA authorization candidate review",
                detail,
                "Review supporting evidence and policy context. Do not publish or remediate as a confirmed vulnerability without human verification.",
                reproduction.endpoint(),
                "INFORMATION",
                "TENTATIVE",
                false,
                reproduction.evidenceIds());
    }
}
