package io.acra.core.route;
public record RouteSegmentModel(int index,String raw,RouteSegmentKind kind,String name) {
    public RouteSegmentModel { if(index<0)throw new IllegalArgumentException("index"); raw=raw==null?"":raw;if(kind==null)kind=RouteSegmentKind.UNKNOWN;name=name==null?"":name; }
}
