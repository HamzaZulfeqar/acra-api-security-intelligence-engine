package io.acra.core.recon;
import io.acra.core.domain.common.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
import java.util.*;
public final class ApiVersionDiscovery {
    public List<ApiVersionSignal> discover(HttpTransaction tx,UriModel uri){List<ApiVersionSignal> out=new ArrayList<>();
        uri.pathSegments().stream().map(s->s.decodedValue()).filter(v->v.matches("(?i)v[0-9]+(?:\\.[0-9]+)?")).findFirst().ifPresent(v->out.add(new ApiVersionSignal(v.toLowerCase(Locale.ROOT),"path",Confidence.of(ConfidenceBasis.EXACT_OBSERVED))));
        for(String k:List.of("version","api_version","v")) if(uri.queryParameters().containsKey(k)) for(String v:uri.queryParameters().get(k)) out.add(new ApiVersionSignal(v,"query:"+k,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA)));
        for(String h:List.of("X-API-Version","API-Version","Accept-Version")) tx.request().firstHeader(h).ifPresent(v->out.add(new ApiVersionSignal(v,"header:"+h,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA))));
        tx.request().firstHeader("Accept").filter(v->v.toLowerCase(Locale.ROOT).contains("version=")).ifPresent(v->{String m=v.replaceAll("(?i).*version=([^;,+ ]+).*","$1");out.add(new ApiVersionSignal(m,"accept-media-type",Confidence.of(ConfidenceBasis.STRUCTURAL_INFERENCE)));});
        return out.stream().distinct().toList();}
}
