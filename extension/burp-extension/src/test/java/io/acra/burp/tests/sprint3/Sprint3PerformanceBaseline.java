package io.acra.burp.tests.sprint3;
import io.acra.burp.traffic.*;
import io.acra.burp.tests.SyntheticJwt;
import io.acra.core.domain.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
public final class Sprint3PerformanceBaseline {
    private Sprint3PerformanceBaseline(){}
    public static void main(String[] args){if(args.length==1){run(Integer.parseInt(args[0]));return;}for(int n:new int[]{100,1000,10000})run(n);}
    private static void run(int n){System.gc();long before=used();TrafficIntelligencePipeline p=new TrafficIntelligencePipeline(n+10);long start=System.nanoTime();for(int i=0;i<n;i++){String id=String.valueOf(100000+i);p.process(tx(i,id));}long ns=System.nanoTime()-start;long after=used();System.out.printf(Locale.ROOT,"n=%d totalMs=%.3f avgUs=%.3f memoryDeltaMB=%.3f endpoints=%d recon=%d graphNodes=%d graphEdges=%d evidence=%d%n",n,ns/1_000_000.0,ns/1000.0/n,(after-before)/(1024.0*1024.0),p.inventory().size(),p.reconnaissanceStore().size(),p.graph().nodeCount(),p.graph().edgeCount(),p.graph().evidenceCount());}
    private static HttpTransaction tx(int i,String id){String token=SyntheticJwt.token("user-a","tenant-a","viewer");var req=new HttpRequest(HttpMethod.GET,"https","api.lab",443,"/api/v1/tenants/tenant-a/documents/"+id+"?page="+(i%10),List.of(new HttpHeader("Authorization","Bearer "+token),new HttpHeader("X-Tenant-ID","tenant-a")),Map.of(),new byte[0],HttpProtocol.HTTP_1_1,new byte[0]);String body="{\"id\":\""+id+"\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\",\"request_id\":\"r-"+i+"\"}";var res=new HttpResponse(200,List.of(new HttpHeader("Content-Type","application/json"),new HttpHeader("X-Request-ID","r-"+i)),body.getBytes(StandardCharsets.UTF_8),"application/json",HttpProtocol.HTTP_1_1,new byte[0]);return new HttpTransaction(req,res,Instant.ofEpochMilli(1_800_000_000_000L+i),"perf3-"+i,"benchmark",Map.of());}
    private static long used(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
}
