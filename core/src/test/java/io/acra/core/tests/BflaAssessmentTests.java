package io.acra.core.tests;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.identity.*;
import io.acra.core.domain.tenant.*;
import io.acra.core.domain.resource.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.common.*;
import io.acra.core.engine.BflaAssessmentEvaluator;
import java.util.*;

public final class BflaAssessmentTests {
 public static int run(){ int n=0;
  Principal p=new Principal("user-a","User A",AuthenticationType.UNKNOWN,Confidence.unknown());
  Role role=new Role("user","USER",EvidenceSource.UNKNOWN,Confidence.unknown());
  Tenant t=new Tenant("tenant-a","Tenant A",EvidenceSource.UNKNOWN,Confidence.unknown());
  Resource r=new Resource("r1","profile",null,"user-a","tenant-a",null,Confidence.unknown());
  Action read=new Action(ActionType.READ,EvidenceSource.UNKNOWN,Confidence.unknown(),"USER_PROFILE_READ");
  BflaAssessmentEvaluator e=new BflaAssessmentEvaluator();
  var ok=new AuthorizationContext(p,role,t,r,"user-a",read,null,AuthorizationDecision.ALLOW,AuthorizationDecision.ALLOW,List.of("ev"),ContextStatus.RESOLVED);
  TestSupport.assertEquals(BflaAssessmentStatus.NO_VIOLATION,e.evaluate(ok,"o","x","t").status(),"allowed function"); n++;
  var denied=new AuthorizationContext(p,role,t,r,"user-a",new Action(ActionType.DELETE,EvidenceSource.UNKNOWN,Confidence.unknown(),"ADMIN_EXPORT"),null,AuthorizationDecision.DENY,AuthorizationDecision.ALLOW,List.of("ev"),ContextStatus.RESOLVED);
  TestSupport.assertEquals(BflaAssessmentStatus.BFLA_CANDIDATE,e.evaluate(denied,"o","x","t").status(),"unauthorized function"); n++;
  var unknown=new AuthorizationContext(p,role,t,r,"user-a",read,null,AuthorizationDecision.UNKNOWN,AuthorizationDecision.ALLOW,List.of("ev"),ContextStatus.RESOLVED);
  TestSupport.assertEquals(BflaAssessmentStatus.INCONCLUSIVE,e.evaluate(unknown,"o","x","t").status(),"unknown policy"); n++;
  return n;
 }
}
