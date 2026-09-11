package io.acra.burp.traffic;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
public final class MontoyaHttpMapper {
    public io.acra.core.domain.http.HttpRequest request(HttpRequest request){
        HttpService service=request.httpService(); String scheme=service.secure()?"https":"http";
        List<io.acra.core.domain.http.HttpHeader> headers=request.headers().stream().map(this::header).toList();
        return new io.acra.core.domain.http.HttpRequest(HttpMethod.parse(request.method()),scheme,service.host(),service.port(),request.path(),headers,cookies(headers),request.body().getBytes(),protocol(request.httpVersion()),request.toByteArray().getBytes());
    }
    public io.acra.core.domain.http.HttpResponse response(HttpResponse response){
        List<io.acra.core.domain.http.HttpHeader> headers=response.headers().stream().map(this::header).toList(); String ct=response.headerValue("Content-Type");
        return new io.acra.core.domain.http.HttpResponse(response.statusCode(),headers,response.body().getBytes(),ct==null?"":ct,protocol(response.httpVersion()),response.toByteArray().getBytes());
    }
    private io.acra.core.domain.http.HttpHeader header(HttpHeader h){return new io.acra.core.domain.http.HttpHeader(h.name(),h.value());}
    private static Map<String,String> cookies(List<io.acra.core.domain.http.HttpHeader> headers){
        TreeMap<String,String> result=new TreeMap<>();
        for(var h:headers){ if(!h.name().equalsIgnoreCase("Cookie")) continue; for(String part:h.value().split(";")){ int eq=part.indexOf('='); if(eq>0) result.put(part.substring(0,eq).trim(),part.substring(eq+1).trim()); }}
        return result;
    }
    private static HttpProtocol protocol(String v){ if(v==null)return HttpProtocol.UNKNOWN; String x=v.toUpperCase(); if(x.contains("HTTP/3"))return HttpProtocol.HTTP_3; if(x.contains("HTTP/2"))return HttpProtocol.HTTP_2; if(x.contains("1.0"))return HttpProtocol.HTTP_1_0; if(x.contains("1.1"))return HttpProtocol.HTTP_1_1; return HttpProtocol.UNKNOWN; }
}
