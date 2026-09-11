package io.acra.core.recon;
import java.util.List;
public record EndpointRiskAssessment(EndpointPriority priority, int score, List<String> reasons) {
    public EndpointRiskAssessment { if(priority==null) priority=EndpointPriority.UNKNOWN; if(score<0) throw new IllegalArgumentException("score cannot be negative"); reasons=List.copyOf(reasons==null?List.of():reasons); }
}
