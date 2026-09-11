package io.acra.core.route;
import java.util.List;
public record RouteEquivalenceResult(RouteEquivalenceKind kind,String canonicalA,String canonicalB,List<String> reasons) {
    public RouteEquivalenceResult { if(kind==null)kind=RouteEquivalenceKind.UNKNOWN;canonicalA=canonicalA==null?"":canonicalA;canonicalB=canonicalB==null?"":canonicalB;reasons=List.copyOf(reasons==null?List.of():reasons); }
}
