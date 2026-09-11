package io.acra.core.tests;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.identity.*;
import io.acra.core.domain.resource.*;
import io.acra.core.domain.tenant.*;
import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.engine.BolaAssessmentEvaluator;
import java.util.*;

public final class BolaAssessmentTests {
    public static int run(){ int n=0;
        Principal a=new Principal("user-a","User A",AuthenticationType.UNKNOWN,Confidence.unknown());
        Role r=Role.unknown(); Tenant t=new Tenant("tenant-a","Tenant A",EvidenceSource.UNKNOWN,Confidence.unknown());
        Resource own=new Resource("doc-a","document",null,"user-a","tenant-a",null,Confidence.unknown());
        Resource other=new Resource("doc-b","document",null,"user-b","tenant-a",null,Confidence.unknown());
        Action read=new Action(ActionType.READ,EvidenceSource.UNKNOWN,Confidence.unknown(),"READ");
        BolaAssessmentEvaluator e=new BolaAssessmentEvaluator();
        AuthorizationContext ok=new AuthorizationContext(a,r,t,own,"user-a",read,null,AuthorizationDecision.ALLOW,AuthorizationDecision.ALLOW,List.of("ev-1"),ContextStatus.RESOLVED);
        TestSupport.assertEquals(BolaAssessmentStatus.NO_VIOLATION,e.evaluate(ok,"obs","exec","test").status(),"allow own object"); n++;
        AuthorizationContext bad=new AuthorizationContext(a,r,t,other,"user-b",read,null,AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,List.of("ev-2"),ContextStatus.RESOLVED);
        TestSupport.assertEquals(BolaAssessmentStatus.BOLA_CANDIDATE,e.evaluate(bad,"obs","exec","test").status(),"deny allow candidate"); n++;
        AuthorizationContext unknown=new AuthorizationContext(a,r,t,other,"user-b",read,null,AuthorizationDecision.UNKNOWN,AuthorizationDecision.ALLOW,List.of("ev-3"),ContextStatus.RESOLVED);
        TestSupport.assertEquals(BolaAssessmentStatus.INCONCLUSIVE,e.evaluate(unknown,"obs","exec","test").status(),"unknown policy"); n++;
        return n; }
}
