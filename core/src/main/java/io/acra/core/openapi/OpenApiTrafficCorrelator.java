package io.acra.core.openapi;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.route.RouteEquivalenceEngine;
import io.acra.core.route.RouteEquivalenceKind;
import java.util.*;
public final class OpenApiTrafficCorrelator {
    private final RouteEquivalenceEngine routes=new RouteEquivalenceEngine();
    public OpenApiCorrelationStatus correlate(Endpoint endpoint,OpenApiDocument spec){
        if(endpoint==null||spec==null)return OpenApiCorrelationStatus.UNKNOWN;
        boolean pathMatch=false;
        for(OpenApiOperation op:spec.operations()){
            var eq=routes.compare(endpoint.routeTemplate(),op.path());
            if(eq.kind()==RouteEquivalenceKind.SYNTACTICALLY_EQUAL||eq.kind()==RouteEquivalenceKind.CANONICALLY_EQUIVALENT||eq.kind()==RouteEquivalenceKind.SAME_FAMILY){
                pathMatch=true; if(op.method()==endpoint.method())return OpenApiCorrelationStatus.DOCUMENTED_OBSERVED;
            }
        }
        return pathMatch?OpenApiCorrelationStatus.CONFLICTING:OpenApiCorrelationStatus.UNDOCUMENTED_OBSERVED;
    }
    public Map<String,OpenApiCorrelationStatus> correlateObserved(Collection<Endpoint> observed,OpenApiDocument spec){
        TreeMap<String,OpenApiCorrelationStatus> out=new TreeMap<>();Set<String> matched=new HashSet<>();
        for(Endpoint e:observed){OpenApiCorrelationStatus s=correlate(e,spec);out.put(e.method()+" "+e.routeTemplate(),s);if(s==OpenApiCorrelationStatus.DOCUMENTED_OBSERVED)for(OpenApiOperation op:spec.operations())if(op.method()==e.method()&&routes.compare(e.routeTemplate(),op.path()).kind()!=RouteEquivalenceKind.DIFFERENT)matched.add(op.key());}
        for(OpenApiOperation op:spec.operations())if(!matched.contains(op.key()))out.putIfAbsent(op.key(),OpenApiCorrelationStatus.DOCUMENTED_UNOBSERVED);
        return Map.copyOf(out);
    }
}
