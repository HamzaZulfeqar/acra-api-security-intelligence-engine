package io.acra.core.reporting.s6;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.security.*;
import java.util.Locale;

public final class S6AuthorizationReportExporter {
    private final DomainSerializer serializer=new DomainSerializer();
    private final UniversalRedactor redactor=new UniversalRedactor();
    public S6AuthorizationExportArtifact json(S6AuthorizationReport report){require(report);String c=serializer.serialize(report);return artifact("JSON","application/json",report.reportId()+".json",c);}
    public S6AuthorizationExportArtifact markdown(S6AuthorizationReport report){require(report);StringBuilder b=new StringBuilder("# ACRA Sprint 6 Authorization Report\n\n");b.append("Report ID: ").append(s(report.reportId())).append("\nVersion: ").append(s(report.reportVersion())).append("\nStatus: ").append(report.status()).append("\nGenerated: ").append(report.generatedAt()).append("\n");if(report.policy()!=null)b.append("Policy: ").append(s(report.policy().policyId())).append(" / ").append(s(report.policy().version())).append("\n");else b.append("Policy: NOT LOADED\n");var x=report.summary();b.append("\n## Summary\n\nAnalyses: ").append(x.analysisCount()).append("\nExpected ALLOW: ").append(x.expectedAllowCount()).append("\nExpected DENY: ").append(x.expectedDenyCount()).append("\nConflicts: ").append(x.conflictCount()).append("\nFinding candidates: ").append(x.findingCandidateCount()).append("\nRisk assessments: ").append(x.riskAssessmentCount()).append("\nConfirmed findings: ").append(x.confirmedFindingCount()).append("\nAverage coverage: ").append(String.format(Locale.ROOT,"%.4f",x.averageCoverage())).append("\n\n## Finding Candidates - Review Only\n");report.findingCandidates().forEach(c->b.append("- ").append(s(c.candidateId())).append(" state=").append(c.state()).append(" expected=").append(c.expectedDecision()).append(" observed=").append(c.observedDecision()).append("\n"));b.append("\n## Limitations\n");report.limitations().forEach(l->b.append("- ").append(s(l)).append("\n"));String c=b.toString();return artifact("MARKDOWN","text/markdown",report.reportId()+".md",c);}
    private S6AuthorizationExportArtifact artifact(String f,String m,String n,String c){return new S6AuthorizationExportArtifact(f,m,n,TokenFingerprint.sha256(c),c);}
    private String s(String v){return redactor.redactText(v==null?"":v);}
    private static void require(S6AuthorizationReport r){if(r==null)throw new IllegalArgumentException("report required");}
}
