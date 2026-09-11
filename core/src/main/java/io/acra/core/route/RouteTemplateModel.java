package io.acra.core.route;
import java.util.List;
public record RouteTemplateModel(String original,String canonical,RouteSyntax syntax,List<RouteSegmentModel> segments) {
    public RouteTemplateModel { original=original==null?"":original;canonical=canonical==null?original:canonical;if(syntax==null)syntax=RouteSyntax.UNKNOWN;segments=List.copyOf(segments==null?List.of():segments); }
}
