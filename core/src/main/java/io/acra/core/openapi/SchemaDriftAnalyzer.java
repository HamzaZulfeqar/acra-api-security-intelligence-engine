package io.acra.core.openapi;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.recon.ParameterIntelligence;
import io.acra.core.route.RouteEquivalenceEngine;
import io.acra.core.route.RouteEquivalenceKind;
import java.util.*;
public final class SchemaDriftAnalyzer {
    private final RouteEquivalenceEngine routes=new RouteEquivalenceEngine();
    public List<SchemaDriftObservation> analyze(Endpoint observed,List<ParameterIntelligence> observedParams,Set<String> observedSecurity,Set<String> observedResponseCodes,OpenApiDocument spec){
        List<SchemaDriftObservation> out=new ArrayList<>();OpenApiOperation match=null;boolean pathSeen=false;
        for(OpenApiOperation op:spec.operations()){var k=routes.compare(observed.routeTemplate(),op.path()).kind();if(k!=RouteEquivalenceKind.DIFFERENT&&k!=RouteEquivalenceKind.UNKNOWN){pathSeen=true;if(op.method()==observed.method()){match=op;break;}}}
        String key=observed.method()+" "+observed.routeTemplate();
        if(match==null){out.add(new SchemaDriftObservation(pathSeen?SchemaDriftType.METHOD_MISMATCH:SchemaDriftType.UNDOCUMENTED_ENDPOINT,key,pathSeen?"route documented with different method":"observed route absent from specification"));return List.copyOf(out);}
        Set<String> declared=match.parameters().stream().map(OpenApiParameter::name).filter(n->!n.isBlank()).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        Set<String> seen=observedParams.stream().map(ParameterIntelligence::name).filter(n->!n.isBlank()).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        for(String p:seen)if(!declared.contains(p))out.add(new SchemaDriftObservation(SchemaDriftType.UNDOCUMENTED_PARAMETER,key,p));
        for(String p:declared)if(!seen.contains(p))out.add(new SchemaDriftObservation(SchemaDriftType.DECLARED_PARAMETER_UNOBSERVED,key,p));
        if(!match.securitySchemes().isEmpty()&&!observedSecurity.isEmpty()&&Collections.disjoint(new HashSet<>(match.securitySchemes()),observedSecurity))out.add(new SchemaDriftObservation(SchemaDriftType.AUTHENTICATION_MISMATCH,key,"declared="+match.securitySchemes()+" observed="+observedSecurity));
        if(!match.responseCodes().isEmpty()&&!observedResponseCodes.isEmpty()&&Collections.disjoint(new HashSet<>(match.responseCodes()),observedResponseCodes))out.add(new SchemaDriftObservation(SchemaDriftType.RESPONSE_MISMATCH,key,"declared="+match.responseCodes()+" observed="+observedResponseCodes));
        return List.copyOf(out);
    }
}
