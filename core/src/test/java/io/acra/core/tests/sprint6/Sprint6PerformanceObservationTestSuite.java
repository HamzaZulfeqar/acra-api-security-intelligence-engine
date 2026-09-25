package io.acra.core.tests.sprint6;

import io.acra.core.active.planning.S6PolicyPlanningAdvisor;
import io.acra.core.domain.authorization.*;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class Sprint6PerformanceObservationTestSuite {
    private static final Instant NOW=Instant.parse("2026-09-23T16:40:00Z");
    private Sprint6PerformanceObservationTestSuite(){}

    public static void main(String[] args)throws Exception{
        int assertions=0;
        List<Row> rows=new ArrayList<>();
        for(int count:new int[]{100,1000,10000}){
            Row row=runWorkload(count);
            rows.add(row);
            TestSupport.assertEquals(count,row.resolved(),"all policy requests resolved"); assertions++;
            TestSupport.assertEquals(count,row.reportAnalyses(),"report contains each unique resolution"); assertions++;
            TestSupport.assertTrue(row.planningRecommendations()>=count/2,"planning recommendations produced"); assertions++;
            TestSupport.assertTrue(row.resolveMs()>=0&&row.planningMs()>=0&&row.reportMs()>=0,"observational timings non-negative"); assertions++;
            System.out.println("SPRINT6_PERF count="+count+" resolveMs="+row.resolveMs()+" planningMs="+row.planningMs()+" reportMs="+row.reportMs()+" approxMemoryDeltaBytes="+row.approxMemoryDeltaBytes()+" recommendations="+row.planningRecommendations());
        }
        String out=System.getenv("ACRA_S6_PERF_OUTPUT");
        if(out!=null&&!out.isBlank()){
            Path p=Path.of(out); Files.createDirectories(p.getParent());
            StringBuilder csv=new StringBuilder("count,resolveMs,planningMs,reportMs,approxMemoryDeltaBytes,resolved,recommendations,reportAnalyses\n");
            for(Row r:rows)csv.append(r.count()).append(',').append(r.resolveMs()).append(',').append(r.planningMs()).append(',').append(r.reportMs()).append(',').append(r.approxMemoryDeltaBytes()).append(',').append(r.resolved()).append(',').append(r.planningRecommendations()).append(',').append(r.reportAnalyses()).append('\n');
            Files.writeString(p,csv.toString(),StandardCharsets.UTF_8);
            System.out.println("SPRINT6_PERF_ARTIFACT "+p);
        }
        System.out.println("SPRINT6_PERFORMANCE_OBSERVATION PASS assertions="+assertions);
    }

    private static Row runWorkload(int count){
        Runtime rt=Runtime.getRuntime(); System.gc();
        long before=used(rt);
        AuthorizationPolicySnapshot policy=policy();
        EffectiveAuthorizationResolver resolver=new EffectiveAuthorizationResolver();
        S6PolicyPlanningAdvisor advisor=new S6PolicyPlanningAdvisor();
        List<EffectiveAuthorizationResolution> resolutions=new ArrayList<>(count);
        List<EffectiveAuthorizationRequest> requests=new ArrayList<>(count);

        long t0=System.nanoTime();
        for(int i=0;i<count;i++){
            boolean cross=(i&1)==1;
            EffectiveAuthorizationRequest request=new EffectiveAuthorizationRequest(
                    "user-a","tenant-a",cross?"tenant-b":"tenant-a","doc-"+i,"document",
                    "/api/v1/s6/documents/"+i,"","READ_DOCUMENT",AuthorizationDecision.ALLOW,false,NOW);
            requests.add(request);
            resolutions.add(resolver.resolve(policy,request));
        }
        long t1=System.nanoTime();

        int recommendations=0;
        for(EffectiveAuthorizationResolution resolution:resolutions) recommendations+=advisor.recommend(resolution).size();
        long t2=System.nanoTime();

        S6AuthorizationWorkspace workspace=new S6AuthorizationWorkspace(); workspace.loadPolicy(policy);
        for(int i=0;i<count;i++){
            EffectiveAuthorizationResolution resolution=resolutions.get(i);
            EffectiveAuthorizationRequest request=requests.get(i);
            workspace.record(new S6AuthorizationAnalysisResult(
                    null,resolution,null,null,null,null,
                    new AuthorizationPolicyCoverage(true,true,true,true,false,true),
                    EffectiveAuthorizationMatrixEntry.from(request,resolution),null,null));
        }
        var report=workspace.report(NOW);
        long t3=System.nanoTime();
        long after=used(rt);
        return new Row(count,ms(t1-t0),ms(t2-t1),ms(t3-t2),Math.max(0,after-before),resolutions.size(),recommendations,report.summary().analysisCount());
    }

    private static AuthorizationPolicySnapshot policy(){
        return AuthorizationPolicySnapshot.create("s6-perf-policy","1","perf-observation",
                List.of(new TenantMembership("tm","user-a","tenant-a",TenantMembershipType.DIRECT,true,List.of("e1"))),
                List.of(new RoleAssignment("ra","user-a","viewer","tenant-a",AuthorizationScope.tenant("tenant-a"),true,List.of("e2"))),
                List.of(),
                List.of(new Permission("p-read","READ_DOCUMENT","document","","",AuthorizationScope.tenant("tenant-a"),List.of("e3"))),
                List.of(new RolePermissionAssignment("rp","viewer","p-read","tenant-a",List.of("e4"))),
                List.of(),List.of(),List.of("e-policy"),AuthorizationDecision.DENY,NOW);
    }

    private static long used(Runtime rt){return rt.totalMemory()-rt.freeMemory();}
    private static long ms(long nanos){return Math.max(0,nanos/1_000_000L);}
    private record Row(int count,long resolveMs,long planningMs,long reportMs,long approxMemoryDeltaBytes,int resolved,int planningRecommendations,int reportAnalyses){}
}
