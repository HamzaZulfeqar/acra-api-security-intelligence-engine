package io.acra.core.engine;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.common.*;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.identity.*;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.extraction.*;
import io.acra.core.extraction.defaults.*;
import io.acra.core.graph.*;
import io.acra.core.security.TokenFingerprint;
import java.util.*;

public final class SecurityContextEngine {
    private final UriExtractor uriExtractor;
    private final IdentityExtractor identityExtractor;
    private final TenantExtractor tenantExtractor;
    private final ResourceExtractor resourceExtractor;
    private final ActionExtractor actionExtractor;

    public SecurityContextEngine(){ this(new DefaultUriExtractor(),new DefaultIdentityExtractor(),new DefaultTenantExtractor(),new DefaultResourceExtractor(),new DefaultActionExtractor()); }
    public SecurityContextEngine(UriExtractor u,IdentityExtractor i,TenantExtractor t,ResourceExtractor r,ActionExtractor a){uriExtractor=u;identityExtractor=i;tenantExtractor=t;resourceExtractor=r;actionExtractor=a;}

    public SecurityContextSnapshot analyze(HttpTransaction tx){
        UriModel uri=uriExtractor.extract(tx);
        IdentityExtraction identity=identityExtractor.extract(tx);
        ExtractionResult<Tenant> tenant=tenantExtractor.extract(tx,uri);
        ExtractionResult<Resource> resource=resourceExtractor.extract(tx,uri,tenant);
        Action action=actionExtractor.extract(tx,uri);
        Endpoint endpoint=endpoint(tx,uri);
        List<Evidence> all=new ArrayList<>(); all.addAll(identity.evidence()); all.addAll(tenant.evidence()); all.addAll(resource.evidence());
        Evidence txEv=Evidence.create(EvidenceSource.TRANSACTION,tx.requestId(),"transaction",tx.request().method()+" "+uri.canonicalPath(),"observed-http-transaction",Confidence.of(ConfidenceBasis.EXACT_OBSERVED),tx.timestamp()); all.add(txEv);
        Evidence actionEv=Evidence.create(action.source(),tx.requestId(),"action",action.actionType().name(),"action-mapping",action.confidence(),tx.timestamp()); all.add(actionEv);
        ContextStatus status=(identity.principal().status()==ResolutionStatus.CONFLICTING_EVIDENCE||identity.role().status()==ResolutionStatus.CONFLICTING_EVIDENCE||tenant.status()==ResolutionStatus.CONFLICTING_EVIDENCE||resource.status()==ResolutionStatus.CONFLICTING_EVIDENCE)?ContextStatus.CONFLICTING_EVIDENCE:ContextStatus.PARTIAL;
        Principal p=identity.principal().resolved().orElse(null); Role role=identity.role().resolved().orElse(null); Tenant ten=tenant.resolved().orElse(null); Resource res=resource.resolved().orElse(null);
        if(p!=null&&ten!=null&&res!=null&&action.actionType()!=ActionType.UNKNOWN) status=ContextStatus.RESOLVED;
        AuthorizationContext authz=new AuthorizationContext(p,role,ten,res,res==null?"":Objects.toString(res.ownerPrincipalId(),""),action,null,AuthorizationDecision.UNKNOWN,AuthorizationDecision.UNKNOWN,all.stream().map(Evidence::evidenceId).toList(),status);
        SecurityContextGraph graph=buildGraph(tx,endpoint,identity,tenant,resource,action,all,txEv,actionEv);
        return new SecurityContextSnapshot(uri,identity,tenant,resource,action,endpoint,authz,all,graph);
    }

    private Endpoint endpoint(HttpTransaction tx,UriModel uri){
        String version=uri.pathSegments().stream().map(s->s.decodedValue()).filter(v->v.matches("(?i)v[0-9]+(?:\\.[0-9]+)?")).findFirst().orElse("");
        String material=tx.request().method()+"|"+tx.request().host()+"|"+uri.canonicalPath();
        return new Endpoint("ep-"+TokenFingerprint.sha256(material).substring(0,20),tx.request().method(),uri.rawPath(),uri.normalizedPath(),uri.canonicalPath(),tx.request().host(),version,List.of(tx.requestId()));
    }

    private SecurityContextGraph buildGraph(HttpTransaction tx,Endpoint endpoint,IdentityExtraction identity,ExtractionResult<Tenant> tenant,ExtractionResult<Resource> resource,Action action,List<Evidence> all,Evidence txEv,Evidence actionEv){
        SecurityContextGraph g=new SecurityContextGraph(); for(Evidence e:all) g.addEvidence(e);
        GraphNode epNode=new GraphNode(endpoint.endpointId(),NodeType.ENDPOINT,endpoint.method()+" "+endpoint.routeTemplate(),Map.of("routeTemplate",endpoint.routeTemplate())); g.addNode(epNode);
        String actionId="action:"+action.actionType().name(); GraphNode actionNode=new GraphNode(actionId,NodeType.ACTION,action.actionType().name(),Map.of()); g.addNode(actionNode);
        g.addEdge(GraphEdge.create(endpoint.endpointId(),actionId,RelationType.PERFORMS,action.confidence(),List.of(actionEv.evidenceId()),tx.timestamp()));
        Principal p=identity.principal().resolved().orElse(null); Role role=identity.role().resolved().orElse(null); Tenant ten=tenant.resolved().orElse(null); Resource res=resource.resolved().orElse(null);
        if(p!=null){ GraphNode pn=new GraphNode("principal:"+p.principalId(),NodeType.PRINCIPAL,p.principalId(),Map.of()); g.addNode(pn); g.addEdge(GraphEdge.create(pn.id(),actionId,RelationType.PERFORMS,action.confidence(),List.of(actionEv.evidenceId()),tx.timestamp()));
            if(role!=null){ GraphNode rn=new GraphNode("role:"+role.roleId(),NodeType.ROLE,role.name(),Map.of()); g.addNode(rn); List<String> ids=identity.role().evidence().stream().map(Evidence::evidenceId).toList(); g.addEdge(GraphEdge.create(pn.id(),rn.id(),RelationType.HAS_ROLE,role.confidence(),ids,tx.timestamp())); }
            if(ten!=null){ GraphNode tn=tenantNode(ten); g.addNode(tn); List<String> ids=tenant.evidence().stream().map(Evidence::evidenceId).toList(); g.addEdge(GraphEdge.create(pn.id(),tn.id(),RelationType.BELONGS_TO,ten.confidence(),ids,tx.timestamp())); }
            if(res!=null){ GraphNode rn=resourceNode(res); g.addNode(rn); g.addEdge(GraphEdge.create(pn.id(),rn.id(),RelationType.ACCESSES,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),List.of(txEv.evidenceId()),tx.timestamp())); }
            if(!identity.tokenFingerprint().isBlank()){ String sid="session:"+identity.tokenFingerprint().substring(0,16); GraphNode sn=new GraphNode(sid,NodeType.SESSION,"session",Map.of("tokenFingerprint",identity.tokenFingerprint())); g.addNode(sn); List<String> ids=identity.evidence().stream().filter(e->e.extractionMethod().contains("fingerprint")).map(Evidence::evidenceId).toList(); if(ids.isEmpty()) ids=List.of(txEv.evidenceId()); g.addEdge(GraphEdge.create(sn.id(),pn.id(),RelationType.AUTHENTICATES,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),ids,tx.timestamp())); }
        }
        if(ten!=null && g.node("tenant:"+ten.tenantId()).isEmpty()) g.addNode(tenantNode(ten));
        if(res!=null){ GraphNode rn=resourceNode(res); if(g.node(rn.id()).isEmpty())g.addNode(rn); g.addEdge(GraphEdge.create(endpoint.endpointId(),rn.id(),RelationType.REACHES,res.confidence(),resource.evidence().stream().map(Evidence::evidenceId).toList(),tx.timestamp())); if(ten!=null) g.addEdge(GraphEdge.create(rn.id(),"tenant:"+ten.tenantId(),RelationType.IN_TENANT,ten.confidence(),tenant.evidence().stream().map(Evidence::evidenceId).toList(),tx.timestamp()));
            if(res.ownerPrincipalId()!=null){ String ownerId="principal:"+res.ownerPrincipalId(); if(g.node(ownerId).isEmpty()) g.addNode(new GraphNode(ownerId,NodeType.PRINCIPAL,res.ownerPrincipalId(),Map.of("source","resource-owner-metadata"))); List<String> ownerEvidence=resource.evidence().stream().filter(e->e.extractionMethod().equals("resource-owner-metadata")).map(Evidence::evidenceId).toList(); if(!ownerEvidence.isEmpty()) g.addEdge(GraphEdge.create(ownerId,rn.id(),RelationType.OWNS,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),ownerEvidence,tx.timestamp())); }
        }
        if(tenant.status()==ResolutionStatus.CONFLICTING_EVIDENCE){ addConflictNode(g,"tenant",tx,tenant.evidence()); }
        if(identity.principal().status()==ResolutionStatus.CONFLICTING_EVIDENCE){ addConflictNode(g,"identity",tx,identity.principal().evidence()); }
        return g;
    }
    private static GraphNode tenantNode(Tenant t){return new GraphNode("tenant:"+t.tenantId(),NodeType.TENANT,t.name(),Map.of());}
    private static GraphNode resourceNode(Resource r){return new GraphNode("resource:"+r.resourceType()+":"+r.resourceId(),NodeType.RESOURCE,r.resourceType()+":"+r.resourceId(),Map.of("resourceType",r.resourceType()));}
    private static void addConflictNode(SecurityContextGraph g,String kind,HttpTransaction tx,List<Evidence> evidence){ String id="conflict:"+kind+":"+tx.requestId(); g.addNode(new GraphNode(id,NodeType.CONFLICT,"CONFLICTING_EVIDENCE",Map.of("kind",kind))); }
}
