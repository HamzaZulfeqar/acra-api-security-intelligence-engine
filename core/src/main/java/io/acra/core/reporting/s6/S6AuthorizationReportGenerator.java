package io.acra.core.reporting.s6;
import io.acra.core.domain.authorization.*;
import io.acra.core.domain.finding.*;
import io.acra.core.product.authorization.S6AuthorizationProductSnapshot;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.*;

public final class S6AuthorizationReportGenerator {
    public static final String REPORT_VERSION="s6-authorization-report-v1";
    public S6AuthorizationReport generate(S6AuthorizationProductSnapshot snapshot,Instant generatedAt){
        if(snapshot==null||generatedAt==null)throw new IllegalArgumentException("snapshot/generatedAt required");
        AuthorizationPolicySnapshot policy=snapshot.policy(); List<S6AuthorizationAnalysisResult> analyses=snapshot.analyses();
        List<EffectiveAuthorizationMatrixEntry> matrix=analyses.stream().map(S6AuthorizationAnalysisResult::matrixEntry).filter(Objects::nonNull).sorted(Comparator.comparing(EffectiveAuthorizationMatrixEntry::resolutionId)).toList();
        List<PolicyConflictAssessment> conflicts=analyses.stream().map(S6AuthorizationAnalysisResult::policyConflict).filter(Objects::nonNull).sorted(Comparator.comparing(PolicyConflictAssessment::assessmentId)).toList();
        TreeMap<String,AuthorizationPolicyCoverage> coverage=new TreeMap<>();
        for(var a:analyses)if(a.effectiveResolution()!=null&&a.coverage()!=null)coverage.put(a.effectiveResolution().resolutionId(),a.coverage());
        List<FindingCandidate> candidates=analyses.stream().map(S6AuthorizationAnalysisResult::findingCandidate).filter(Objects::nonNull).sorted(Comparator.comparing(FindingCandidate::candidateId)).toList();
        List<AuthorizationRiskAssessment> risks=analyses.stream().map(S6AuthorizationAnalysisResult::riskAssessment).filter(Objects::nonNull).sorted(Comparator.comparing(AuthorizationRiskAssessment::riskId)).toList();
        int allow=(int)matrix.stream().filter(x->x.expectedDecision()==AuthorizationDecision.ALLOW).count();
        int deny=(int)matrix.stream().filter(x->x.expectedDecision()==AuthorizationDecision.DENY).count();
        int conflict=(int)conflicts.stream().filter(PolicyConflictAssessment::conflicting).count();
        int candidate=(int)candidates.stream().filter(x->x.state()==FindingCandidateState.CANDIDATE).count();
        int complete=(int)coverage.values().stream().filter(x->x.knownCount()==x.total()).count();
        double avg=coverage.isEmpty()?0.0:coverage.values().stream().mapToDouble(AuthorizationPolicyCoverage::ratio).average().orElse(0.0);
        var summary=new S6AuthorizationReportSummary(analyses.size(),allow,deny,conflict,candidate,risks.size(),0,complete,avg);
        TreeSet<String> evidence=new TreeSet<>();
        if(policy!=null){evidence.addAll(policy.evidenceIds());policy.memberships().forEach(x->evidence.addAll(x.evidenceIds()));policy.roleAssignments().forEach(x->evidence.addAll(x.evidenceIds()));policy.roleInheritances().forEach(x->evidence.addAll(x.evidenceIds()));policy.permissions().forEach(x->evidence.addAll(x.evidenceIds()));policy.rolePermissionAssignments().forEach(x->evidence.addAll(x.evidenceIds()));policy.rules().forEach(x->evidence.addAll(x.evidenceIds()));policy.delegations().forEach(x->evidence.addAll(x.evidenceIds()));}
        analyses.forEach(x->{if(x.effectiveResolution()!=null)evidence.addAll(x.effectiveResolution().evidenceIds());if(x.tenantIsolation()!=null)evidence.addAll(x.tenantIsolation().evidenceIds());if(x.rbac()!=null)evidence.addAll(x.rbac().evidenceIds());if(x.roleEscalation()!=null)evidence.addAll(x.roleEscalation().evidenceIds());});
        conflicts.forEach(x->evidence.addAll(x.evidenceIds())); candidates.forEach(x->evidence.addAll(x.supportingEvidenceIds()));
        String material=REPORT_VERSION+"|"+(policy==null?"NO_POLICY":policy.fingerprint())+"|"+matrix.stream().map(EffectiveAuthorizationMatrixEntry::resolutionId).toList()+"|"+candidates.stream().map(FindingCandidate::candidateId).toList()+"|"+conflicts.stream().map(PolicyConflictAssessment::assessmentId).toList();
        List<String> limitations=new ArrayList<>(List.of("FindingCandidate is a review candidate, not a confirmed vulnerability.","Report contains Sprint 6 authorization policy and analysis state only.","Normal export sanitization excludes raw credentials and authentication secrets.","Controlled-lab measurements do not establish real-world scanner accuracy."));
        if(policy==null)limitations.add("Policy is not loaded; authorization conclusions are incomplete.");
        return new S6AuthorizationReport("s6-report-"+TokenFingerprint.sha256(material).substring(0,24),REPORT_VERSION,policy==null?S6AuthorizationReportStatus.POLICY_NOT_LOADED:S6AuthorizationReportStatus.READY_FOR_REVIEW,generatedAt,policy,summary,matrix,conflicts,coverage,candidates,risks,List.copyOf(evidence),List.copyOf(limitations));
    }
}
