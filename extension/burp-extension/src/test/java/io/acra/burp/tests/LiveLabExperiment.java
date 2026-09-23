package io.acra.burp.tests;

import burp.api.montoya.core.*;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import io.acra.burp.scope.*;
import io.acra.burp.traffic.*;
import io.acra.core.domain.http.HttpTransaction;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class LiveLabExperiment {
    private static final String TOKEN=SyntheticJwt.token("user-a","tenant-a","viewer");
    private LiveLabExperiment(){}
    public static void main(String[] args) throws Exception {
        URI uri=URI.create("http://127.0.0.1:18082/api/v1/tenants/tenant-a/documents/1001");
        HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        java.net.http.HttpRequest networkRequest=java.net.http.HttpRequest.newBuilder(uri).header("Authorization","Bearer "+TOKEN).header("Accept","application/json").GET().build();
        java.net.http.HttpResponse<byte[]> networkResponse=client.send(networkRequest,java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if(networkResponse.statusCode()!=200) throw new AssertionError("lab status "+networkResponse.statusCode());
        FakeRequest req=new FakeRequest(900,"GET",uri.getRawPath(),List.of(new Header("Host","127.0.0.1:18082"),new Header("Authorization","Bearer "+TOKEN),new Header("Accept","application/json")));
        List<HttpHeader> responseHeaders=new ArrayList<>(); networkResponse.headers().map().forEach((k,vs)->vs.forEach(v->responseHeaders.add(new Header(k,v))));
        FakeResponse resp=new FakeResponse(900,req,(short)networkResponse.statusCode(),networkResponse.body(),responseHeaders);
        ScopeController scope=new ScopeController(); scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),2*1024*1024,100,1,1,2000,false));
        TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(100); AtomicReference<HttpTransaction> tx=new AtomicReference<>(); TrafficCollector collector=new TrafficCollector(scope,t->{tx.set(t);pipeline.process(t);});
        collector.onRequest(req); collector.onResponse(resp);
        var observation=pipeline.store().observations().getFirst();
        if(!"user-a".equals(observation.principalId())) throw new AssertionError("principal="+observation.principalId());
        if(!"tenant-a".equals(observation.tenantId())) throw new AssertionError("tenant="+observation.tenantId());
        if(!"1001".equals(observation.resourceId())) throw new AssertionError("resource="+observation.resourceId());
        if(!"READ".equals(observation.action().name())) throw new AssertionError("action="+observation.action());
        String serialized=new io.acra.core.serialization.DomainSerializer().serialize(tx.get());
        if(serialized.contains(TOKEN)) throw new AssertionError("raw token leaked into serialized transaction");
        System.out.println("EXP-INTEGRATION-001 LOCAL-LAB PASS");
        System.out.println("httpStatus="+networkResponse.statusCode());
        System.out.println("transactionId="+observation.transactionId());
        System.out.println("principal="+observation.principalId());
        System.out.println("tenant="+observation.tenantId());
        System.out.println("resource="+observation.resourceType()+":"+observation.resourceId());
        System.out.println("action="+observation.action());
        System.out.println("endpoint="+observation.endpoint().method()+" "+observation.endpoint().routeTemplate());
        System.out.println("evidenceCount="+observation.evidenceIds().size());
        System.out.println("burpStage=UNVERIFIED_NOT_AVAILABLE_IN_EXECUTION_ENVIRONMENT");
    }
    private record Header(String name,String value) implements HttpHeader {}
    private record Bytes(byte[] data) implements ByteArray { public Bytes{data=data.clone();} public byte[] getBytes(){return data.clone();} public int length(){return data.length;} }
    private static final class Svc implements HttpService {public String host(){return "127.0.0.1";} public int port(){return 18082;} public boolean secure(){return false;}}
    private static final class Src implements ToolSource {public ToolType toolType(){return ToolType.PROXY;}}
    private static final class FakeRequest implements HttpRequestToBeSent {
        private final int id; private final String method,path; private final List<HttpHeader> headers;
        FakeRequest(int id,String method,String path,List<HttpHeader> headers){this.id=id;this.method=method;this.path=path;this.headers=List.copyOf(headers);}
        public int messageId(){return id;} public boolean isInScope(){return true;} public ToolSource toolSource(){return new Src();} public String method(){return method;} public String path(){return path;} public String pathWithoutQuery(){return path;} public String httpVersion(){return "HTTP/1.1";} public List<HttpHeader> headers(){return headers;} public ByteArray body(){return new Bytes(new byte[0]);} public ByteArray toByteArray(){return new Bytes((method+" "+path+" HTTP/1.1\r\nAuthorization: Bearer "+TOKEN+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));} public HttpService httpService(){return new Svc();}
    }
    private static final class FakeResponse implements HttpResponseReceived {
        private final int id;private final HttpRequest req;private final short status;private final byte[] body;private final List<HttpHeader> headers;
        FakeResponse(int id,HttpRequest req,short status,byte[] body,List<HttpHeader> headers){this.id=id;this.req=req;this.status=status;this.body=body.clone();this.headers=List.copyOf(headers);}
        public int messageId(){return id;}public HttpRequest initiatingRequest(){return req;}public ToolSource toolSource(){return new Src();}public short statusCode(){return status;}public String httpVersion(){return "HTTP/1.1";}public List<HttpHeader> headers(){return headers;}public String headerValue(String name){return headers.stream().filter(h->h.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst().orElse(null);}public ByteArray body(){return new Bytes(body);}public ByteArray toByteArray(){return new Bytes(body);}
    }
}
