package io.acra.burp.tests;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.core.domain.http.*;
import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
public final class Sprint2PerformanceBaseline {
    private static final String TOKEN=SyntheticJwt.token("user-a","tenant-a","viewer");
    private Sprint2PerformanceBaseline(){}
    public static void main(String[] args) throws Exception { System.setProperty("java.awt.headless","true"); System.out.println("count,totalMs,avgAnalysisMicros,approxMemoryDeltaBytes,graphNodes,graphEdges,graphEvidence,endpoints,observations,edtRoundTripMicros"); for(int n:new int[]{100,1000,10000}) run(n); }
    private static void run(int n) throws Exception {
        System.gc(); long before=used(); TrafficIntelligencePipeline p=new TrafficIntelligencePipeline(Math.max(n,100)); long start=System.nanoTime();
        for(int i=0;i<n;i++) p.process(tx(i)); long elapsed=System.nanoTime()-start; long after=used();
        long uiStart=System.nanoTime(); SwingUtilities.invokeAndWait(()->{ p.store().observations().size(); p.inventory().snapshot().size(); }); long ui=System.nanoTime()-uiStart;
        System.out.printf(Locale.ROOT,"%d,%d,%.2f,%d,%d,%d,%d,%d,%d,%.2f%n",n,elapsed/1_000_000,(elapsed/1000.0)/n,Math.max(0,after-before),p.graph().nodeCount(),p.graph().edgeCount(),p.graph().evidenceCount(),p.inventory().size(),p.store().size(),ui/1000.0);
    }
    private static HttpTransaction tx(int i){
        String id=Integer.toString(1000+(i%100)); String path="/api/v1/tenants/tenant-a/documents/"+id;
        List<HttpHeader> headers=List.of(new HttpHeader("Authorization","Bearer "+TOKEN),new HttpHeader("Accept","application/json"));
        HttpRequest req=HttpRequest.of(HttpMethod.GET,"https","api.lab",443,path,headers,new byte[0],HttpProtocol.HTTP_1_1);
        String body="{\"id\":\""+id+"\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\",\"timestamp\":\"2026-08-31T00:00:00Z\"}";
        HttpResponse resp=new HttpResponse(200,List.of(new HttpHeader("Content-Type","application/json"),new HttpHeader("X-Request-ID","req-"+i)),body.getBytes(StandardCharsets.UTF_8),"application/json",HttpProtocol.HTTP_1_1,new byte[0]);
        return new HttpTransaction(req,resp,Instant.ofEpochMilli(1_700_000_000_000L+i),String.format("PERF-%06d",i),"PERF_FIXTURE",Map.of());
    }
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}

