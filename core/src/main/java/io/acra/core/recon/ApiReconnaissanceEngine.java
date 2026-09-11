package io.acra.core.recon;
import io.acra.core.analysis.semantic.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.engine.SecurityContextSnapshot;
import io.acra.core.openapi.*;
import io.acra.core.planning.*;
import io.acra.core.route.*;
import java.util.*;
public final class ApiReconnaissanceEngine {
    private final SemanticIdentifierClassifier ids=new SemanticIdentifierClassifier();
    private final ParameterClassifier params=new ParameterClassifier();
    private final HeaderClassifier headers=new HeaderClassifier();
    private final ApiVersionDiscovery versions=new ApiVersionDiscovery();
    private final ResourceRelationshipInferer relationships=new ResourceRelationshipInferer();
    private final RouteTemplateEngine routes=new RouteTemplateEngine();
    private final ResponseSemanticAnalyzer responses=new ResponseSemanticAnalyzer();
    private final CollectionAnalyzer collections=new CollectionAnalyzer();
    private final OpenApiTrafficCorrelator correlator=new OpenApiTrafficCorrelator();
    private final SchemaDriftAnalyzer drift=new SchemaDriftAnalyzer();
    private final EndpointRiskPrioritizer priorities=new EndpointRiskPrioritizer();
    private final ReconTestPlanner planner=new ReconTestPlanner();
    public ApiReconnaissanceResult analyze(HttpTransaction tx,SecurityContextSnapshot s,OpenApiDocument spec,Collection<String> contexts,Collection<String> resources){
        var semanticIds=ids.classify(s.uri());var parameterList=params.classify(tx,s.uri());var headerList=headers.classify(tx.request().headers());var versionList=versions.discover(tx,s.uri());var rel=relationships.infer(s);var route=routes.fromObserved(s.uri());
        OpenApiCorrelationStatus doc=spec==null?OpenApiCorrelationStatus.UNKNOWN:correlator.correlate(s.endpoint(),spec);
        boolean schema=doc==OpenApiCorrelationStatus.DOCUMENTED_OBSERVED;var coverage=new ContextCoverage(s.identity().principal().resolved().isPresent(),s.identity().role().resolved().isPresent(),s.tenant().resolved().isPresent(),s.resource().resolved().isPresent(),s.resource().resolved().map(r->r.ownerPrincipalId()!=null).orElse(false),s.action().actionType()!=io.acra.core.domain.authorization.ActionType.UNKNOWN,true,schema,false);
        var partial=new ReconnaissanceSnapshot(semanticIds,parameterList,headerList,versionList,rel,route,coverage,new EndpointRiskAssessment(EndpointPriority.UNKNOWN,0,List.of()),doc);var priority=priorities.assess(s,partial);var recon=new ReconnaissanceSnapshot(semanticIds,parameterList,headerList,versionList,rel,route,coverage,priority,doc);
        ResponseSemanticFingerprint fp=tx.response()==null?new ResponseSemanticFingerprint(0,"",0,SemanticResponseClass.UNKNOWN,Set.of(),Set.of(),Set.of(),Set.of(),Set.of(),"",""):responses.fingerprint(tx.response());var collection=collections.collection(tx,fp);var pagination=collections.pagination(s.uri());
        String principal=s.identity().principal().resolved().map(p->p.principalId()).orElse("");String role=s.identity().role().resolved().map(r->r.name()).orElse("");String tenant=s.tenant().resolved().map(t->t.tenantId()).orElse("");String resource=s.resource().resolved().map(r->r.resourceType()+":"+r.resourceId()).orElse("");String owner=s.resource().resolved().map(r->Objects.toString(r.ownerPrincipalId(),"")).orElse("");String token=s.identity().tokenFingerprint().isBlank()?"":s.identity().tokenFingerprint().substring(0,Math.min(16,s.identity().tokenFingerprint().length()));var scf=new SecurityContextFingerprint(principal,role,tenant,resource,owner,s.action().actionType().name(),s.authorizationContext().workflowState().name(),route.canonical(),s.uri().normalizationMetadata().isEmpty()?"RAW_EQ_CANONICAL":"NORMALIZED",token,s.authorizationContext().expectedDecision(),s.authorizationContext().observedDecision(),s.evidence().stream().map(e->e.evidenceId()).toList());
        List<SchemaDriftObservation> driftObs=spec==null?List.of():drift.analyze(s.endpoint(),parameterList,observedSecurity(s),tx.response()==null?Set.of():Set.of(String.valueOf(tx.response().status())),spec);var dry=planner.plan(tx.request().host(),s,recon,contexts,resources);return new ApiReconnaissanceResult(recon,fp,collection,pagination,scf,driftObs,dry);
    }
    private static Set<String> observedSecurity(SecurityContextSnapshot s){String n=s.identity().authenticationType().name();return n.equals("NONE")||n.equals("UNKNOWN")?Set.of():Set.of(n);}
}
