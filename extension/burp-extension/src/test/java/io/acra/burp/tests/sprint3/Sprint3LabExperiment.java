package io.acra.burp.tests.sprint3;
import burp.api.montoya.core.*;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import io.acra.burp.scope.*;
import io.acra.burp.tests.SyntheticJwt;
import io.acra.burp.traffic.*;
import io.acra.core.openapi.OpenApiCorrelationStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
public final class Sprint3LabExperiment {
    private static final String TOKEN_VIEWER=SyntheticJwt.token("user-a","tenant-a","viewer");
    private static final String TOKEN_EDITOR=SyntheticJwt.token("user-a","tenant-a","editor");
    private Sprint3LabExperiment(){}
    public static void main(String[] args) throws Exception {
        TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(200);new OpenApiImportService(pipeline).importFile(Path.of("lab/openapi/acra-lab-openapi.json"));ScopeController scope=new ScopeController();scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),2*1024*1024,500,1,1,3000,false));TrafficCollector collector=new TrafficCollector(scope,pipeline::process);HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        List<Op> ops=List.of(
            new Op("GET","/health",null,""),
            new Op("GET","/api/v1/tenants/tenant-a/documents/1001",TOKEN_VIEWER,""),
            new Op("PATCH","/api/v1/tenants/tenant-a/documents/1001",TOKEN_VIEWER,"{\"title\":\"S3\"}"),
            new Op("GET","/api/v2/tenants/tenant-a/documents/1001",TOKEN_VIEWER,""),
            new Op("GET","/api/v1/tenants/tenant-a/documents",TOKEN_VIEWER,""),
            new Op("GET","/api/v1/tenants/tenant-a/users/user-a",TOKEN_VIEWER,""),
            new Op("GET","/api/v1/users/user-a",TOKEN_VIEWER,""),
            new Op("POST","/api/v1/tenants/tenant-a/documents/1001/approve",TOKEN_EDITOR,"{}"),
            new Op("GET","/api/v1/tenants/tenant-a/documents/1001/comments",TOKEN_VIEWER,""),
            new Op("GET","/api/v1/search?tenant_id=tenant-a",TOKEN_VIEWER,"")
        );
        int id=1000;for(Op op:ops){URI uri=URI.create("http://127.0.0.1:18082"+op.path);var rb=java.net.http.HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).header("Accept","application/json");if(op.token!=null)rb.header("Authorization","Bearer "+op.token);switch(op.method){case "PATCH"->rb.method("PATCH",java.net.http.HttpRequest.BodyPublishers.ofString(op.body)).header("Content-Type","application/json");case "POST"->rb.POST(java.net.http.HttpRequest.BodyPublishers.ofString(op.body)).header("Content-Type","application/json");default->rb.GET();}var nr=client.send(rb.build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());if(nr.statusCode()!=200)throw new AssertionError(op.method+" "+op.path+" status="+nr.statusCode());List<HttpHeader> h=new ArrayList<>();h.add(new Header("Host","127.0.0.1:18082"));h.add(new Header("Accept","application/json"));if(op.token!=null)h.add(new Header("Authorization","Bearer "+op.token));if(!op.body.isEmpty())h.add(new Header("Content-Type","application/json"));FakeRequest fr=new FakeRequest(id,op.method,op.path,op.body.getBytes(StandardCharsets.UTF_8),h);List<HttpHeader> rh=new ArrayList<>();nr.headers().map().forEach((k,vs)->vs.forEach(v->rh.add(new Header(k,v))));collector.onRequest(fr);collector.onResponse(new FakeResponse(id,fr,(short)nr.statusCode(),nr.body(),rh));id++;}
        if(pipeline.inventory().size()!=10)throw new AssertionError("expected 10 endpoint operations, actual="+pipeline.inventory().size());long documented=pipeline.reconnaissanceStore().entries().stream().filter(e->e.getValue().reconnaissance().documentationStatus()==OpenApiCorrelationStatus.DOCUMENTED_OBSERVED).count();if(documented!=10)throw new AssertionError("documented observed="+documented);long dryDispatched=pipeline.reconnaissanceStore().entries().stream().mapToLong(e->e.getValue().dryRunPlan().dispatchedRequests()).sum();if(dryDispatched!=0)throw new AssertionError("dry run dispatched requests");var doc=pipeline.reconnaissanceStore().entries().stream().filter(e->e.getValue().securityContextFingerprint().resource().contains("1001")).findFirst().orElseThrow().getValue();if(!doc.responseFingerprint().ownerIds().contains("user-a"))throw new AssertionError("owner semantic evidence missing");if(pipeline.graph().edgeCount()==0)throw new AssertionError("graph not hydrated");
        System.out.println("EXP-RECON-001 LOCAL-LAB PASS");System.out.println("knownOperations=10");System.out.println("observedEndpointOperations="+pipeline.inventory().size());System.out.println("documentedObserved="+documented);System.out.println("reconRecords="+pipeline.reconnaissanceStore().size());System.out.println("graphNodes="+pipeline.graph().nodeCount());System.out.println("graphEdges="+pipeline.graph().edgeCount());System.out.println("dryRunDispatched="+dryDispatched);System.out.println("burpRuntime=UNVERIFIED_BLOCKED");
    }
    private record Op(String method,String path,String token,String body){}
    private record Header(String name,String value) implements HttpHeader {}
    private record Bytes(byte[] data) implements ByteArray {public Bytes{data=data.clone();}public byte[] getBytes(){return data.clone();}public int length(){return data.length;}}
    private static final class Service implements HttpService {public String host(){return "127.0.0.1";}public int port(){return 18082;}public boolean secure(){return false;}}
    private static final class Source implements ToolSource {public ToolType toolType(){return ToolType.PROXY;}}
    private static final class FakeRequest implements HttpRequestToBeSent {private final int id;private final String method,path;private final byte[] body;private final List<HttpHeader> headers;FakeRequest(int id,String method,String path,byte[] body,List<HttpHeader> headers){this.id=id;this.method=method;this.path=path;this.body=body.clone();this.headers=List.copyOf(headers);}public int messageId(){return id;}public boolean isInScope(){return true;}public ToolSource toolSource(){return new Source();}public String method(){return method;}public String path(){return path;}public String pathWithoutQuery(){int q=path.indexOf('?');return q<0?path:path.substring(0,q);}public String httpVersion(){return "HTTP/1.1";}public List<HttpHeader> headers(){return headers;}public ByteArray body(){return new Bytes(body);}public ByteArray toByteArray(){return new Bytes((method+" "+path+" HTTP/1.1\r\n\r\n").getBytes(StandardCharsets.UTF_8));}public HttpService httpService(){return new Service();}}
    private static final class FakeResponse implements HttpResponseReceived {private final int id;private final HttpRequest req;private final short status;private final byte[] body;private final List<HttpHeader> headers;FakeResponse(int id,HttpRequest req,short status,byte[] body,List<HttpHeader> headers){this.id=id;this.req=req;this.status=status;this.body=body.clone();this.headers=List.copyOf(headers);}public int messageId(){return id;}public HttpRequest initiatingRequest(){return req;}public ToolSource toolSource(){return new Source();}public short statusCode(){return status;}public String httpVersion(){return "HTTP/1.1";}public List<HttpHeader> headers(){return headers;}public String headerValue(String name){return headers.stream().filter(h->h.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst().orElse(null);}public ByteArray body(){return new Bytes(body);}public ByteArray toByteArray(){return new Bytes(body);}}
}
