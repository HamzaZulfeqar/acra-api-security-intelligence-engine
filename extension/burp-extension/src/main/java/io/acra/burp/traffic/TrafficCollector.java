package io.acra.burp.traffic;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import io.acra.burp.scope.*;
import io.acra.core.domain.http.HttpTransaction;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
public final class TrafficCollector {
    private record Pending(Instant observedAt,long nanoStart,boolean accepted,String reason) {}
    private final ConcurrentHashMap<Integer,Pending> pending=new ConcurrentHashMap<>(); private final AtomicLong ids=new AtomicLong();
    private final MontoyaHttpMapper mapper=new MontoyaHttpMapper(); private final ScopeController scope; private final Consumer<HttpTransaction> sink;
    public TrafficCollector(ScopeController scope,Consumer<HttpTransaction> sink){this.scope=Objects.requireNonNull(scope);this.sink=Objects.requireNonNull(sink);}
    public void onRequest(HttpRequestToBeSent request){
        var svc=request.httpService(); var decision=scope.evaluate(request.isInScope(),svc.secure()?"https":"http",svc.host(),svc.port(),request.pathWithoutQuery(),false);
        pending.put(request.messageId(),new Pending(Instant.now(),System.nanoTime(),decision.accepted(),decision.reason()));
    }
    public void onResponse(HttpResponseReceived response){
        Pending p=pending.remove(response.messageId()); if(p==null) p=new Pending(Instant.now(),System.nanoTime(),true,"response-without-recorded-request"); if(!p.accepted()) return;
        var req=response.initiatingRequest(); var coreReq=mapper.request(req); var coreResp=mapper.response(response);
        if(coreReq.body().length>scope.configuration().maxBodyBytes()||coreResp.body().length>scope.configuration().maxBodyBytes()) return;
        String id=String.format("ACRA-TX-%06d",ids.incrementAndGet()); long elapsed=System.nanoTime()-p.nanoStart();
        Map<String,String> meta=new TreeMap<>(); meta.put("burpMessageId",Integer.toString(response.messageId())); meta.put("burpTool",response.toolSource().toolType().name()); meta.put("scopeReason",p.reason()); meta.put("roundTripNanos",Long.toString(Math.max(0L,elapsed)));
        sink.accept(new HttpTransaction(coreReq,coreResp,p.observedAt(),id,"BURP_MONTOYA",meta));
    }
    public int pendingCount(){return pending.size();}
}
