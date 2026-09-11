package io.acra.burp.traffic;
import io.acra.core.domain.observation.*;
import io.acra.burp.model.ContextObservation;
import io.acra.core.engine.*;
import io.acra.core.inventory.EndpointInventory;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.recon.*;
import io.acra.core.openapi.OpenApiDocument;
import java.util.*;
public final class TrafficIntelligencePipeline {
    private final SecurityContextEngine engine=new SecurityContextEngine(); private final EndpointInventory inventory=new EndpointInventory();
    private final SessionCorrelationStore sessions=new SessionCorrelationStore(); private final IdentityConfirmationRegistry identityConfirmations=new IdentityConfirmationRegistry(); private final ObservationStore store; private final ReconnaissanceStore reconStore; private final SecurityContextGraph graph=new SecurityContextGraph(); private final ApiReconnaissanceEngine reconnaissance=new ApiReconnaissanceEngine(); private volatile OpenApiDocument openApi;
    public TrafficIntelligencePipeline(int maxObservations){store=new ObservationStore(maxObservations);reconStore=new ReconnaissanceStore(maxObservations);}
    public TrafficProcessingResult process(HttpTransaction tx){
        SecurityContextSnapshot s=engine.analyze(tx); graph.mergeFrom(s.graph()); inventory.observe(s,tx.timestamp()); String session=sessions.sessionId(s.identity().tokenFingerprint());
        String principal=s.identity().principal().resolved().map(p->p.principalId()).orElse(""); String tenant=s.tenant().resolved().map(t->t.tenantId()).orElse("");
        String resourceType=s.resource().resolved().map(r->r.resourceType()).orElse(""); String resourceId=s.resource().resolved().map(r->r.resourceId()).orElse("");
        ObservationStatus os=s.authorizationContext().status()==io.acra.core.domain.authorization.ContextStatus.RESOLVED?ObservationStatus.OBSERVED:ObservationStatus.PARTIAL;
        int responseStatus=tx.response()==null?0:tx.response().status(); List<String> ids=s.evidence().stream().map(e->e.evidenceId()).toList();
        TrafficObservation o=new TrafficObservation(tx.requestId(),tx.timestamp(),s.endpoint(),responseStatus,s.identity().authenticationType(),principal,tenant,resourceType,resourceId,s.action().actionType(),s.authorizationContext().status(),os,ids);
        List<EvidenceTimelineEntry> timeline=List.of(
            new EvidenceTimelineEntry(tx.requestId(),tx.timestamp(),"TRAFFIC","Transaction observed",List.of()),
            new EvidenceTimelineEntry(tx.requestId(),tx.timestamp(),"CONTEXT","Passive security context assembled",ids));
        String role=s.identity().role().resolved().map(r->r.name()).orElse("");
        String resourceLabel=s.resource().resolved().map(r->r.resourceType()+":"+r.resourceId()).orElse("");
        String owner=s.resource().resolved().map(r->Objects.toString(r.ownerPrincipalId(),"")).orElse("");
        double confidence=contextConfidence(s);
        ContextObservation context=new ContextObservation(tx.requestId(),tx.timestamp(),principal,role,tenant,session,resourceLabel,owner,s.action().actionType(),s.uri().rawUri(),ids,confidence,s.authorizationContext().status());
        store.add(o,context,timeline); var recon=reconnaissance.analyze(tx,s,openApi,inventory.snapshot().stream().flatMap(e->e.principals().stream()).distinct().toList(),inventory.snapshot().stream().flatMap(e->e.resources().stream()).distinct().toList()); reconStore.put(tx.requestId(),recon); return new TrafficProcessingResult(o,s,session);
    }
    private static double contextConfidence(SecurityContextSnapshot s){
        List<Double> values=new ArrayList<>();
        s.identity().principal().resolved().ifPresent(v->values.add(v.confidence().score()));
        s.identity().role().resolved().ifPresent(v->values.add(v.confidence().score()));
        s.tenant().resolved().ifPresent(v->values.add(v.confidence().score()));
        s.resource().resolved().ifPresent(v->values.add(v.confidence().score()));
        if(s.action().actionType()!=io.acra.core.domain.authorization.ActionType.UNKNOWN) values.add(s.action().confidence().score());
        return values.isEmpty()?0.0:values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }
    public void openApi(OpenApiDocument document){this.openApi=document;} public OpenApiDocument openApi(){return openApi;} public IdentityConfirmationRegistry identityConfirmations(){return identityConfirmations;} public EndpointInventory inventory(){return inventory;} public ObservationStore store(){return store;} public ReconnaissanceStore reconnaissanceStore(){return reconStore;} public SessionCorrelationStore sessions(){return sessions;} public SecurityContextGraph graph(){return graph;}
}
