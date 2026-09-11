package io.acra.core.recon;
import io.acra.core.domain.common.*;
import io.acra.core.engine.SecurityContextSnapshot;
import java.util.*;
public final class ResourceRelationshipInferer {
    public List<ResourceRelationship> infer(SecurityContextSnapshot s){List<ResourceRelationship> out=new ArrayList<>();
        s.resource().resolved().ifPresent(r->{String res="resource:"+r.resourceType()+":"+r.resourceId();
            if(r.ownerPrincipalId()!=null) out.add(new ResourceRelationship("principal:"+r.ownerPrincipalId(),res,ResourceRelationshipType.OWNS,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),s.resource().evidence().stream().map(e->e.evidenceId()).toList(),"owner-metadata"));
            if(r.tenantId()!=null) out.add(new ResourceRelationship(res,"tenant:"+r.tenantId(),ResourceRelationshipType.BELONGS_TO,r.confidence(),s.resource().evidence().stream().map(e->e.evidenceId()).toList(),"resource-tenant-metadata"));
            if(r.parentResourceId()!=null) out.add(new ResourceRelationship("resource:parent:"+r.parentResourceId(),res,ResourceRelationshipType.PARENT_OF,r.confidence(),s.resource().evidence().stream().map(e->e.evidenceId()).toList(),"parent-resource-metadata"));
        }); return List.copyOf(out);}
}
