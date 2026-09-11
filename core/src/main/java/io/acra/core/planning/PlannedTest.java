package io.acra.core.planning;
import java.util.List;
public record PlannedTest(TestFamily family,String reason,int estimatedRequests,List<String> requiredContext) {
    public PlannedTest {if(family==null)throw new IllegalArgumentException("family required");reason=reason==null?"":reason;if(estimatedRequests<0)throw new IllegalArgumentException("estimatedRequests");requiredContext=List.copyOf(requiredContext==null?List.of():requiredContext);}
}
