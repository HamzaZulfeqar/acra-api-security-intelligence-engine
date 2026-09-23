package io.acra.core.tests.sprint6;

import io.acra.core.active.execution.*;
import io.acra.core.active.model.*;
import io.acra.core.active.planning.*;
import io.acra.core.active.safety.*;
import io.acra.core.domain.authorization.*;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.*;
import io.acra.core.domain.resource.Resource;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.*;

public final class Sprint6SecurityHardeningTestSuite {
    private static final Instant NOW=Instant.parse("2026-09-23T16:45:00Z");
    private static final String VIEWER_TOKEN="viewer-secret-token-123";
    private static final String ADMIN_TOKEN="admin-secret-token-456";
    private Sprint6SecurityHardeningTestSuite(){}

    public static void main(String[] args){
        int a=0;
        RequestBuilder builder=new RequestBuilder();
        RequestEquivalenceGuard guard=new RequestEquivalenceGuard();

        SecurityTest valid=test(def("base","ctx-viewer","resource:1",VIEWER_TOKEN,"/admin/summary","stable"),
                def("admin","ctx-admin","resource:1",ADMIN_TOKEN,"/admin/summary","stable"),
                def("deny","ctx-viewer-neg","resource:1",VIEWER_TOKEN,"/admin/summary","stable"),
                mutation("ctx-viewer","ctx-admin"));
        TestSupport.assertEquals(ValidationStatus.ALLOWED,guard.evaluate(valid,builder.build(valid)).status(),"valid context substitution allowed");a++;

        expectFailure(()->builder.build(test(valid.baselineDefinition(),valid.positiveControl(),valid.negativeControl(),mutation("ctx-viewer","ctx-missing"))),"missing context blocked");a++;

        SecurityTest ambiguous=test(valid.baselineDefinition(),
                def("admin1","ctx-admin","resource:1",ADMIN_TOKEN,"/admin/summary","stable"),
                def("admin2","ctx-admin","resource:1","other-admin-token","/admin/summary","stable"),
                mutation("ctx-viewer","ctx-admin"));
        expectFailure(()->builder.build(ambiguous),"ambiguous context blocked");a++;

        expectFailure(()->builder.build(test(valid.baselineDefinition(),valid.positiveControl(),valid.negativeControl(),mutation("ctx-spoof","ctx-admin"))),"spoofed source context blocked");a++;

        SecurityTest pathChange=test(valid.baselineDefinition(),def("admin","ctx-admin","resource:1",ADMIN_TOKEN,"/admin/other","stable"),valid.negativeControl(),mutation("ctx-viewer","ctx-admin"));
        TestSupport.assertEquals(ValidationStatus.INVALID,guard.evaluate(pathChange,builder.build(pathChange)).status(),"path change rejected");a++;

        SecurityTest headerChange=test(valid.baselineDefinition(),def("admin","ctx-admin","resource:1",ADMIN_TOKEN,"/admin/summary","changed"),valid.negativeControl(),mutation("ctx-viewer","ctx-admin"));
        TestSupport.assertEquals(ValidationStatus.INVALID,guard.evaluate(headerChange,builder.build(headerChange)).status(),"non-auth header change rejected");a++;

        SecurityTest resourceChange=test(valid.baselineDefinition(),def("admin","ctx-admin","resource:2",ADMIN_TOKEN,"/admin/summary","stable"),valid.negativeControl(),mutation("ctx-viewer","ctx-admin"));
        TestSupport.assertEquals(ValidationStatus.INVALID,guard.evaluate(resourceChange,builder.build(resourceChange)).status(),"resource change rejected");a++;

        SecurityTest noAuthChange=test(valid.baselineDefinition(),def("admin","ctx-admin","resource:1",VIEWER_TOKEN,"/admin/summary","stable"),valid.negativeControl(),mutation("ctx-viewer","ctx-admin"));
        TestSupport.assertEquals(ValidationStatus.INVALID,guard.evaluate(noAuthChange,builder.build(noAuthChange)).status(),"same authentication material rejected");a++;

        String serialized=new DomainSerializer().serialize(valid);
        TestSupport.assertNotContains(serialized,VIEWER_TOKEN,"viewer secret excluded");a++;
        TestSupport.assertNotContains(serialized,ADMIN_TOKEN,"admin secret excluded");a++;
        TestSupport.assertTrue(serialized.contains("<redacted>"),"serialized test visibly redacted");a++;

        EffectiveAuthorizationResolution conflict=new EffectiveAuthorizationResolution("r-conflict","pf","user-a","tenant-a",TenantRelationship.SAME_TENANT,List.of("viewer"),List.of(),List.of(),List.of(),AuthorizationDecision.UNKNOWN,AuthorizationDecision.ALLOW,PolicyResolutionState.CONFLICTING,List.of("e"),List.of("conflict"));
        S6PolicyPlanningCandidate candidate=new S6PolicyPlanningCandidate("c-conflict",endpoint(),valid.baselineDefinition(),valid.positiveControl(),valid.negativeControl(),sourceContext(),targetContext(),resource(),resource(),conflict,conflict,Set.of(TestContract.ROLE_COMPARISON),List.of(),List.of(),false,false);
        S6PolicySeedGenerationResult generated=new S6PolicyTestSeedFactory().generate(candidate);
        TestSupport.assertTrue(generated.seeds().isEmpty(),"conflicting policy generates no executable seed");a++;
        TestSupport.assertTrue(generated.skippedReasons().stream().anyMatch(x->x.contains("TARGET_POLICY_UNRESOLVED")),"conflict skip reason explicit");a++;

        System.out.println("SPRINT6_SECURITY_HARDENING PASS assertions="+a);
    }

    private static SecurityTest test(RequestDefinition base,RequestDefinition pos,RequestDefinition neg,Mutation mutation){
        return new SecurityTest("S6-SEC-"+Math.abs((base.definitionId()+pos.definitionId()+mutation.targetContext()).hashCode()),"1",TestContract.ROLE_COMPARISON,HttpProtocol.HTTP_1_1,
                new TargetDescriptor("acra-s6","local","http","localhost",18082,ExecutionEnvironment.LAB,true,List.of("/admin"),Set.of(HttpMethod.GET)),
                endpoint(),HttpMethod.GET,base,pos,neg,mutation,sourceContext(),targetContext(),resource(),resource(),AuthorizationDecision.ALLOW,List.of("e"),
                SafetyPolicy.safeLabReadOnly(20,5),80,"security hardening fixture",ConfigurationSnapshot.of(Map.of("environment","LAB")),List.of(),
                new ReproducibilityMetadata("s6",606L,NOW,List.of("S6-SEC")),4,List.of("same endpoint","same resource"));
    }
    private static RequestDefinition def(String id,String ctx,String resource,String token,String path,String mode){
        return new RequestDefinition(id,HttpRequest.of(HttpMethod.GET,"http","localhost",18082,path,
                List.of(new HttpHeader("Authorization","Bearer "+token),new HttpHeader("X-Mode",mode)),new byte[0],HttpProtocol.HTTP_1_1),ctx,resource);
    }
    private static Mutation mutation(String source,String target){return new Mutation("m-"+source+"-"+target,MutationType.AUTHENTICATED_CONTEXT_SUBSTITUTION,MutationLocation.CONTEXT,"viewer","admin",source,target,"credential-safe context substitution","role-specific authorization outcome",SafetyClass.SAFE_READ_ONLY,"d-"+source+"-"+target);}
    private static Endpoint endpoint(){return new Endpoint("ep-sec",HttpMethod.GET,"/admin/summary","/admin/summary","/admin/summary","localhost","v1",List.of());}
    private static Resource resource(){return new Resource("1","resource",null,"owner","tenant-a","ACTIVE",Confidence.unknown());}
    private static SecurityContextFingerprint sourceContext(){return new SecurityContextFingerprint("user-a","viewer","tenant-a","resource:1","owner","READ_ADMIN","ACTIVE","GET /admin/summary","raw","ctx-viewer",AuthorizationDecision.DENY,AuthorizationDecision.UNKNOWN,List.of("e"));}
    private static SecurityContextFingerprint targetContext(){return new SecurityContextFingerprint("admin-a","admin","tenant-a","resource:1","owner","READ_ADMIN","ACTIVE","GET /admin/summary","raw","ctx-admin",AuthorizationDecision.ALLOW,AuthorizationDecision.UNKNOWN,List.of("e"));}
    private static void expectFailure(Runnable action,String message){try{action.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
}
