package io.acra.core.tests.sprint6;
import io.acra.core.domain.authorization.*;
import io.acra.core.domain.finding.*;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.reporting.s6.*;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class Sprint6ReportingExportTestSuite {
 private static final Instant NOW=Instant.parse("2026-09-23T16:20:00Z");
 private Sprint6ReportingExportTestSuite(){}
 public static void main(String[] args)throws Exception{int a=run();System.out.println("SPRINT6_REPORTING_EXPORT PASS assertions="+a);}
 public static int run()throws Exception{
  int a=0;S6AuthorizationWorkspace w=fixture();var r=w.report(NOW);
  TestSupport.assertEquals(S6AuthorizationReportStatus.READY_FOR_REVIEW,r.status(),"report ready");a++;
  TestSupport.assertEquals(1,r.summary().analysisCount(),"analysis count");a++;
  TestSupport.assertEquals(1,r.summary().findingCandidateCount(),"candidate count");a++;
  TestSupport.assertEquals(0,r.summary().confirmedFindingCount(),"no confirmed finding promotion");a++;
  TestSupport.assertEquals(1,r.summary().conflictCount(),"conflict count");a++;
  TestSupport.assertTrue(r.evidenceIds().containsAll(List.of("e-policy","e-role","e-perm","e-rp","e-resolution","e-conflict","e-candidate")),"evidence aggregation");a++;
  var j1=w.exportJson(NOW);var j2=w.exportJson(NOW);
  TestSupport.assertEquals(j1.content(),j2.content(),"json deterministic");a++;
  TestSupport.assertEquals(j1.sha256(),j2.sha256(),"digest deterministic");a++;
  TestSupport.assertTrue(j1.content().contains("s6-authorization-report-v1"),"versioned json");a++;
  TestSupport.assertTrue(j1.content().contains("\"confirmedFindingCount\":0"),"json no confirmed finding");a++;
  TestSupport.assertTrue(j1.content().contains("candidate-report-1"),"candidate exported");a++;
  TestSupport.assertNotContains(j1.content(),"report-secret-12345","json secret exclusion");a++;
  TestSupport.assertTrue(j1.content().contains("<redacted>"),"redaction marker");a++;
  var md=w.exportMarkdown(NOW);
  TestSupport.assertTrue(md.content().contains("Finding Candidates - Review Only"),"review-only markdown");a++;
  TestSupport.assertTrue(md.content().contains("Confirmed findings: 0"),"markdown boundary");a++;
  TestSupport.assertNotContains(md.content(),"report-secret-12345","markdown secret exclusion");a++;
  TestSupport.assertEquals(j1.content(),new S6AuthorizationJsonReporter().render(Map.of("report",r)),"reporter adapter");a++;
  S6AuthorizationWorkspace empty=new S6AuthorizationWorkspace();var er=empty.report(NOW);
  TestSupport.assertEquals(S6AuthorizationReportStatus.POLICY_NOT_LOADED,er.status(),"empty policy status");a++;
  TestSupport.assertTrue(er.limitations().stream().anyMatch(x->x.contains("Policy is not loaded")),"empty limitation");a++;
  String out=System.getenv("ACRA_S6_REPORT_OUTPUT_DIR");if(out!=null&&!out.isBlank()){Path d=Path.of(out);Files.createDirectories(d);Files.writeString(d.resolve("S6-AUTHORIZATION-REPORT.json"),j1.content(),StandardCharsets.UTF_8);Files.writeString(d.resolve("S6-AUTHORIZATION-REPORT.json.sha256"),j1.sha256()+"  S6-AUTHORIZATION-REPORT.json\n",StandardCharsets.UTF_8);Files.writeString(d.resolve("S6-AUTHORIZATION-REPORT.md"),md.content(),StandardCharsets.UTF_8);System.out.println("SPRINT6_REPORT_ARTIFACT "+d);}
  return a;
 }
 private static S6AuthorizationWorkspace fixture(){
  S6AuthorizationWorkspace w=new S6AuthorizationWorkspace();
  AuthorizationPolicySnapshot p=AuthorizationPolicySnapshot.create("s6-report-policy","1","controlled-report-fixture",List.of(),List.of(new RoleAssignment("ra","user-a","viewer","tenant-a",AuthorizationScope.tenant("tenant-a"),true,List.of("e-role"))),List.of(),List.of(new Permission("p-admin","READ_ADMIN_SUMMARY","admin-summary","","",AuthorizationScope.tenant("tenant-a"),List.of("e-perm"))),List.of(new RolePermissionAssignment("rp","viewer","p-admin","tenant-a",List.of("e-rp"))),List.of(),List.of(),List.of("e-policy"),AuthorizationDecision.DENY,NOW);w.loadPolicy(p);
  EffectiveAuthorizationResolution res=new EffectiveAuthorizationResolution("resolution-report-1",p.fingerprint(),"user-a","tenant-a",TenantRelationship.SAME_TENANT,List.of("viewer"),List.of("p-admin"),List.of(),List.of(),AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,PolicyResolutionState.CONFLICTING,List.of("e-resolution"),List.of("mismatch"));
  EffectiveAuthorizationMatrixEntry matrix=new EffectiveAuthorizationMatrixEntry(res.resolutionId(),"user-a",List.of("viewer"),"tenant-a","tenant-a",TenantRelationship.SAME_TENANT,"admin-summary","READ_ADMIN_SUMMARY","/api/v1/s6/tenants/tenant-a/admin/summary",p.fingerprint(),AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,PolicyResolutionState.CONFLICTING,List.of("e-resolution"));
  PolicyConflictAssessment conflict=new PolicyConflictAssessment("conflict-report-1",true,PolicyResolutionState.CONFLICTING,List.of("mismatch"),List.of("e-conflict"));
  FindingCandidate c=new FindingCandidate("candidate-report-1",FindingCandidateState.CANDIDATE,"acra-s6",List.of("test"),List.of("exec"),List.of("obs"),List.of("assessment"),List.of("RBAC"),matrix.endpoint(),matrix.resourceId(),matrix.principalId(),matrix.tenantRelationship().name(),AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,List.of("e-candidate"),List.of(),List.of(p.fingerprint()),"HIGH","token=report-secret-12345",FindingFingerprint.of(matrix.endpoint(),matrix.resourceId(),matrix.principalId(),matrix.tenantRelationship().name(),"RBAC","POLICY_MISMATCH"));
  AuthorizationRiskAssessment risk=new AuthorizationRiskAssessment("risk-report-1",c.candidateId(),FindingSeverity.HIGH,FindingConfidence.HIGH,80,"controlled risk",List.of("expected deny observed allow"));
  w.record(new S6AuthorizationAnalysisResult(null,res,new TenantIsolationAssessment("t",TenantRelationship.SAME_TENANT,AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,TenantIsolationAssessmentState.CONFLICTING,List.of("e-resolution"),"review"),new RbacAssessment("r",List.of("viewer"),List.of("p-admin"),AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,RbacAssessmentState.CONFLICTING,List.of("e-resolution"),"review"),new RoleEscalationAssessment("re",true,"admin",List.of("viewer"),AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,RoleEscalationAssessmentState.CONFLICTING,List.of("e-resolution"),"review"),conflict,new AuthorizationPolicyCoverage(true,true,false,true,false,false),matrix,c,risk));
  return w;
 }
}
