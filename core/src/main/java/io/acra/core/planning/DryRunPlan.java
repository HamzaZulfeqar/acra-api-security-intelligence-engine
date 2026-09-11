package io.acra.core.planning;
import io.acra.core.recon.EndpointPriority;
import java.util.List;
public record DryRunPlan(String target,String endpoint,List<String> contexts,List<String> resources,List<PlannedTest> tests,
                         int estimatedRequests,int dispatchedRequests,EndpointPriority priority,List<String> safetyNotes) {
    public DryRunPlan {target=target==null?"":target;endpoint=endpoint==null?"":endpoint;contexts=List.copyOf(contexts==null?List.of():contexts);resources=List.copyOf(resources==null?List.of():resources);tests=List.copyOf(tests==null?List.of():tests);if(estimatedRequests<0||dispatchedRequests<0)throw new IllegalArgumentException("request counts");if(priority==null)priority=EndpointPriority.UNKNOWN;safetyNotes=List.copyOf(safetyNotes==null?List.of():safetyNotes);}
}
