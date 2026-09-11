package io.acra.core.inventory;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.identity.AuthenticationType;
import java.time.Instant;
import java.util.*;
public record EndpointAggregate(Endpoint endpoint,long observationCount,Instant firstObserved,Instant lastObserved,
                                Set<String> observedIds,Set<String> principals,Set<String> tenants,Set<String> resources,
                                Set<AuthenticationType> authenticationTypes) {
    public EndpointAggregate {
        if(endpoint==null||firstObserved==null||lastObserved==null) throw new IllegalArgumentException("endpoint and timestamps required");
        if(observationCount<1) throw new IllegalArgumentException("observationCount must be positive");
        observedIds=Set.copyOf(observedIds==null?Set.of():observedIds); principals=Set.copyOf(principals==null?Set.of():principals);
        tenants=Set.copyOf(tenants==null?Set.of():tenants); resources=Set.copyOf(resources==null?Set.of():resources);
        authenticationTypes=Set.copyOf(authenticationTypes==null?Set.of():authenticationTypes);
    }
}
