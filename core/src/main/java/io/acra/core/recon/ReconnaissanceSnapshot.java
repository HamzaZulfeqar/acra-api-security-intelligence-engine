package io.acra.core.recon;
import io.acra.core.openapi.OpenApiCorrelationStatus;
import io.acra.core.route.RouteTemplateModel;
import java.util.List;
public record ReconnaissanceSnapshot(List<SemanticIdentifier> identifiers, List<ParameterIntelligence> parameters,
                                     List<HeaderIntelligence> headers, List<ApiVersionSignal> versions,
                                     List<ResourceRelationship> relationships, RouteTemplateModel route,
                                     ContextCoverage coverage, EndpointRiskAssessment priority,
                                     OpenApiCorrelationStatus documentationStatus) {
    public ReconnaissanceSnapshot {
        identifiers=List.copyOf(identifiers==null?List.of():identifiers); parameters=List.copyOf(parameters==null?List.of():parameters); headers=List.copyOf(headers==null?List.of():headers); versions=List.copyOf(versions==null?List.of():versions); relationships=List.copyOf(relationships==null?List.of():relationships);
        if(coverage==null) coverage=new ContextCoverage(false,false,false,false,false,false,false,false,false);
        if(priority==null) priority=new EndpointRiskAssessment(EndpointPriority.UNKNOWN,0,List.of());
        if(documentationStatus==null) documentationStatus=OpenApiCorrelationStatus.UNKNOWN;
    }
}
