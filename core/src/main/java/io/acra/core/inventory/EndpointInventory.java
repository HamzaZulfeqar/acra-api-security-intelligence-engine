package io.acra.core.inventory;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.engine.SecurityContextSnapshot;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class EndpointInventory {
    private final ConcurrentHashMap<String,Mutable> entries=new ConcurrentHashMap<>();
    public void observe(SecurityContextSnapshot s,Instant at){
        Endpoint ep=s.endpoint(); entries.compute(ep.endpointId(),(k,v)->{ Mutable m=v==null?new Mutable(ep,at):v; m.observe(s,at); return m; });
    }
    public List<EndpointAggregate> snapshot(){ return entries.values().stream().map(Mutable::freeze).sorted(Comparator.comparing(x->x.endpoint().endpointId())).toList(); }
    public int size(){return entries.size();}
    private static final class Mutable {
        private final Endpoint endpoint; private long count; private final Instant first; private Instant last;
        private final Set<String> ids=new TreeSet<>(), principals=new TreeSet<>(), tenants=new TreeSet<>(), resources=new TreeSet<>();
        private final Set<AuthenticationType> auths=EnumSet.noneOf(AuthenticationType.class);
        Mutable(Endpoint endpoint,Instant at){this.endpoint=endpoint;this.first=at;this.last=at;}
        synchronized void observe(SecurityContextSnapshot s,Instant at){ count++; last=at; s.uri().identifierCandidates().forEach(i->ids.add(i.value())); s.identity().principal().resolved().ifPresent(p->principals.add(p.principalId())); s.tenant().resolved().ifPresent(t->tenants.add(t.tenantId())); s.resource().resolved().ifPresent(r->resources.add(r.resourceType()+":"+r.resourceId())); auths.add(s.identity().authenticationType()); }
        synchronized EndpointAggregate freeze(){return new EndpointAggregate(endpoint,count,first,last,ids,principals,tenants,resources,auths);}
    }
}
