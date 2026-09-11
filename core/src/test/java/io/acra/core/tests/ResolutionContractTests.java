package io.acra.core.tests;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.common.*;
import io.acra.core.domain.groundtruth.GroundTruthContext;
import io.acra.core.domain.identity.*;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.plugin.*;
import io.acra.core.resolution.*;
import io.acra.core.extraction.*;
import io.acra.core.domain.evidence.*;
import java.time.Instant;
import java.util.*;

public final class ResolutionContractTests {
    public static int run(){int n=0; EntityResolver r=new EntityResolver();
        TestSupport.assertEquals(EntityResolutionStatus.SAME,r.compare("42","42",List.of()).status(),"same entity"); n++;
        TestSupport.assertEquals(EntityResolutionStatus.POSSIBLE_SAME,r.compare("User-A","user-a",List.of()).status(),"possible same must not auto merge"); n++;
        TestSupport.assertThrows(DomainValidationException.class,()->new Resource("","document",null,null,null,null,Confidence.unknown()),"empty resource id"); n++;
        GroundTruthContext gt=new GroundTruthContext("GT-BOLA-001",new Principal("u1","",AuthenticationType.JWT,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)),Role.unknown(),new Tenant("A","A",null,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)),new Resource("1","document",null,"u1","A",null,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)),"u1",new Action(ActionType.READ,null,null,""),AuthorizationDecision.ALLOW,null,false); TestSupport.assertEquals("GT-BOLA-001",gt.groundTruthId(),"ground truth compatibility"); n++;
        Principal p1=new Principal("u1","",AuthenticationType.JWT,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)); Principal p2=new Principal("u2","",AuthenticationType.JWT,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)); Evidence ce1=Evidence.create(EvidenceSource.JWT_CLAIM,"req-c","jwt:sub","u1","fixture",Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),Instant.parse("2026-08-30T12:00:00Z")); Evidence ce2=Evidence.create(EvidenceSource.HEADER,"req-c","header:X-User","u2","fixture",Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),Instant.parse("2026-08-30T12:00:00Z")); ExtractionResult<Principal> conflicting=ExtractionResult.conflict(List.of(p1,p2),List.of(ce1,ce2),"conflicting identity evidence"); TestSupport.assertEquals(ResolutionStatus.CONFLICTING_EVIDENCE,conflicting.status(),"conflicting identity evidence represented explicitly"); n++;
        Analyzer dummy=new Analyzer(){public String id(){return "dummy";}public String name(){return "Dummy";}public Set<String> supportedContexts(){return Set.of("SECURITY_CONTEXT");}public AnalyzerOutput analyze(AnalysisContext c){return new AnalyzerOutput(id(),List.of(),List.of());}public Set<Capability> capabilities(){return Set.of(Capability.PASSIVE_ANALYSIS);}}; TestSupport.assertTrue(dummy.capabilities().contains(Capability.PASSIVE_ANALYSIS),"analyzer contract"); n++;
        return n;}
}
