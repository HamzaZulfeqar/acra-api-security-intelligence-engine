package io.acra.burp.tests;

import burp.api.montoya.*;
import burp.api.montoya.core.*;
import burp.api.montoya.extension.*;
import burp.api.montoya.http.*;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.UserInterface;
import io.acra.burp.ACRAExtension;
import io.acra.burp.execution.*;
import io.acra.burp.scope.*;
import io.acra.burp.traffic.*;
import io.acra.core.analysis.*;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.serialization.DomainSerializer;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class Sprint2TestSuite {
    private static int tests;
    private static final String TOKEN_A=SyntheticJwt.token("user-a","tenant-a","viewer");
    private Sprint2TestSuite(){}
    public static void main(String[] args){
        System.setProperty("java.awt.headless","true");
        testBootstrap(); testScope(); testTrafficPipeline(); testEndpointFamily(); testRedaction(); testResponseNormalization(); testNegativeStates(); testNegativeTransport(); testSafetyExecutor();
        System.out.println("PASS Sprint2 tests="+tests);
    }
    private static void testBootstrap(){
        FakeApi api=new FakeApi(); new ACRAExtension().initialize(api); check("ACRA".equals(api.extension.name),"extension name"); check(api.http.handler!=null,"handler registered"); check(api.ui.component!=null,"suite tab registered"); check(api.extension.unload!=null,"unload registered"); api.extension.unload.extensionUnloaded(); tests+=4;
    }
    private static void testScope(){
        ScopeController s=new ScopeController(); check(!s.evaluate(false,"https","x",443,"/",false).accepted(),"default requires Burp scope"); check(s.evaluate(true,"https","x",443,"/",false).accepted(),"in-scope accepted");
        s.update(new ScopeConfiguration(ScopeMode.SELECTED_ENDPOINTS,List.of(new ScopeRule("api.lab",443,"https","/api/v1","v1","lab")),1024,100,1,1,1000,false));
        check(s.evaluate(false,"https","api.lab",443,"/api/v1/documents",false).accepted(),"selected endpoint"); check(!s.evaluate(false,"https","other",443,"/api/v1",false).accepted(),"other host rejected"); tests+=4;
    }
    private static void testTrafficPipeline(){
        ScopeController scope=new ScopeController(); scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),2*1024*1024,100,1,1,1000,false));
        TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(100); List<HttpTransaction> txs=new ArrayList<>();
        TrafficCollector collector=new TrafficCollector(scope,tx->{txs.add(tx);pipeline.process(tx);});
        FakeRequest req=request(41,"GET","/api/v1/tenants/tenant-a/documents/1001",TOKEN_A,List.of()); collector.onRequest(req); collector.onResponse(response(41,req,200,"{\"id\":\"1001\",\"tenant_id\":\"tenant-a\",\"owner_id\":\"user-a\"}"));
        check(txs.size()==1,"transaction created"); check("ACRA-TX-000001".equals(txs.getFirst().requestId()),"internal transaction id"); var obs=pipeline.store().observations().getFirst();
        check("user-a".equals(obs.principalId()),"identity correlated"); check("tenant-a".equals(obs.tenantId()),"tenant correlated"); check("1001".equals(obs.resourceId()),"resource identified"); check(obs.authenticationType()==AuthenticationType.JWT,"jwt fingerprinted"); check(obs.contextStatus()==ContextStatus.RESOLVED,"context resolved"); check(pipeline.inventory().size()==1,"inventory updated"); check(pipeline.store().timeline().size()>=2,"timeline updated"); check(pipeline.graph().nodeCount()>0,"global graph nodes updated"); check(pipeline.graph().edgeCount()>0,"global graph edges updated"); var context=pipeline.store().contexts().getFirst(); check("viewer".equalsIgnoreCase(context.role()),"role projected to context view"); check("user-a".equals(context.owner()),"owner projected to context view"); check(!"UNKNOWN".equals(context.session()),"session projected to context view"); check(context.evidenceIds().size()>=1,"context evidence exposed"); check(context.confidence()>0.0,"context confidence exposed"); check(pipeline.graph().outgoing("principal:user-a",io.acra.core.graph.RelationType.OWNS).stream().anyMatch(e->e.target().equals("resource:document:1001")),"owner provenance updates graph"); tests+=17;
    }
    private static void testEndpointFamily(){
        ScopeController scope=new ScopeController(); scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),100000,100,1,1,1000,false)); TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(100); TrafficCollector c=new TrafficCollector(scope,pipeline::process);
        for(int i=1;i<=3;i++){String id="100"+i; FakeRequest r=request(i,"GET","/api/v1/documents/"+id,TOKEN_A,List.of()); c.onRequest(r); c.onResponse(response(i,r,200,"{\"id\":\""+id+"\"}"));}
        var inv=pipeline.inventory().snapshot(); check(inv.size()==1,"same endpoint family"); check(inv.getFirst().observationCount()==3,"observation count"); check(inv.getFirst().observedIds().containsAll(Set.of("1001","1002","1003")),"observed ids"); tests+=3;
    }
    private static void testRedaction(){
        var req=request(1,"GET","/api/v1/documents/1001",TOKEN_A,List.of(new Header("X-API-Key","super-secret-key"),new Header("Cookie","session=abc123; theme=dark"))); MontoyaHttpMapper m=new MontoyaHttpMapper(); var core=m.request(req); String out=new DomainSerializer().serialize(core); check(core.cookies().get("session").equals("abc123"),"cookies preserved in transient domain model"); check(!out.contains(TOKEN_A),"bearer secret absent"); check(!out.contains("super-secret-key"),"api key absent"); check(!out.contains("abc123"),"cookie secret absent"); check(out.contains("<redacted>"),"redaction marker present"); tests+=5;
    }
    private static void testResponseNormalization(){
        var h=List.of(new io.acra.core.domain.http.HttpHeader("Content-Type","application/json"),new io.acra.core.domain.http.HttpHeader("X-Request-ID","abc"));
        var a=new io.acra.core.domain.http.HttpResponse(200,h,"{\"id\":1,\"timestamp\":\"2026-01-01\"}".getBytes(StandardCharsets.UTF_8),"application/json",io.acra.core.domain.http.HttpProtocol.HTTP_1_1,new byte[0]);
        var b=new io.acra.core.domain.http.HttpResponse(200,List.of(new io.acra.core.domain.http.HttpHeader("Content-Type","application/json"),new io.acra.core.domain.http.HttpHeader("X-Request-ID","xyz")),"{\"id\":1,\"timestamp\":\"2026-02-02\"}".getBytes(StandardCharsets.UTF_8),"application/json",io.acra.core.domain.http.HttpProtocol.HTTP_1_1,new byte[0]);
        PassiveDifferentialComparator d=new PassiveDifferentialComparator(); check(!d.compare(a,b,ResponseComparisonMode.RAW).equivalent(),"raw differs"); check(d.compare(a,b,ResponseComparisonMode.NORMALIZED).equivalent(),"normalized equivalent"); check(d.compare(a,b,ResponseComparisonMode.STRUCTURAL).equivalent(),"structural equivalent"); tests+=3;
    }
    private static void testNegativeStates(){
        ScopeController scope=new ScopeController(); scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),32,100,1,1,1000,false)); AtomicReference<HttpTransaction> ref=new AtomicReference<>(); TrafficCollector c=new TrafficCollector(scope,ref::set);
        FakeRequest noAuth=request(1,"GET","/api/v1/tenants/tenant-a/documents/1001",null,List.of()); c.onRequest(noAuth); c.onResponse(response(1,noAuth,200,"{}")); check(ref.get()!=null,"missing auth still observed");
        TrafficIntelligencePipeline p=new TrafficIntelligencePipeline(10); var result=p.process(ref.get()); check(result.snapshot().identity().authenticationType()==AuthenticationType.NONE,"none auth classified");
        FakeRequest conflict=request(2,"GET","/api/v1/tenants/tenant-a/documents/1001",TOKEN_A,List.of(new Header("X-Tenant-ID","tenant-b"))); ScopeController s2=new ScopeController(); s2.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),1000,10,1,1,1000,false)); AtomicReference<HttpTransaction> r2=new AtomicReference<>(); TrafficCollector c2=new TrafficCollector(s2,r2::set); c2.onRequest(conflict); c2.onResponse(response(2,conflict,200,"{}")); var cresult=p.process(r2.get()); check(cresult.snapshot().tenant().status()==io.acra.core.extraction.ResolutionStatus.CONFLICTING_EVIDENCE,"tenant conflict explicit");
        byte[] huge=new byte[64]; Arrays.fill(huge,(byte)'A'); FakeRequest big=new FakeRequest(3,"POST","/api/v1/x",huge,null,List.of()); c.onRequest(big); c.onResponse(new FakeResponse(3,big,(short)200,"{}".getBytes(StandardCharsets.UTF_8),List.of(new Header("Content-Type","application/json")))); check(ref.get().requestId().equals("ACRA-TX-000001"),"oversized body rejected"); tests+=4;
    }

    private static void testNegativeTransport(){
        ScopeController scope=new ScopeController(); scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),1024,20,1,1,1000,false)); AtomicReference<HttpTransaction> ref=new AtomicReference<>(); TrafficCollector c=new TrafficCollector(scope,ref::set);
        FakeRequest pending=request(71,"GET","/api/v1/documents/1001",null,List.of()); c.onRequest(pending); check(ref.get()==null,"missing response does not fabricate transaction"); check(c.pendingCount()==1,"missing response remains pending");
        FakeRequest duplicate=request(72,"GET","/api/v1/documents/1001",null,List.of()); c.onRequest(duplicate); c.onRequest(duplicate); check(c.pendingCount()==2,"duplicate message id does not duplicate pending key"); c.onResponse(response(72,duplicate,200,"{}")); check(ref.get()!=null,"duplicate request id produces one completed transaction");
        FakeRequest custom=request(73,"GET","/api/v1/documents/1001",null,List.of(new Header("Authorization","Token opaque-value"))); c.onRequest(custom); c.onResponse(response(73,custom,200,"{}")); TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(10); var customResult=pipeline.process(ref.get()); check(customResult.snapshot().identity().authenticationType()==AuthenticationType.CUSTOM,"unknown auth classified custom"); check(customResult.snapshot().identity().principal().resolved().isEmpty(),"custom auth does not invent principal");
        byte[] binary=new byte[]{0,1,2,3}; FakeResponse br=new FakeResponse(74,request(74,"GET","/download",null,List.of()),(short)200,binary,List.of(new Header("Content-Type","application/octet-stream"),new Header("Content-Encoding","gzip"))); var mapped=new MontoyaHttpMapper().response(br); check(Arrays.equals(binary,mapped.body()),"binary/compressed bytes preserved opaquely"); tests+=7;
    }
    private static void testSafetyExecutor(){ ScopeController s=new ScopeController(); s.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),1024,10,1,1,1000,false)); DisabledActiveRequestExecutor e=new DisabledActiveRequestExecutor(s); var h=e.execute(new ActiveRequest("http","127.0.0.1",18081,"/health","lab-check",true)); check(h.state()==ExecutionHandle.State.BLOCKED,"active executor disabled"); check(!e.rateLimit().allowed(),"rate limiter blocks active"); check(e.safetyControls().maxRequests()==0,"active request budget locked to zero"); check(e.safetyControls().maxMutations()==0,"mutation budget locked to zero"); check(e.safetyControls().userConfirmationRequired()&&e.safetyControls().killSwitchEngaged(),"confirmation and kill switch enforced"); tests+=5; }
    private static FakeRequest request(int id,String method,String path,String token,List<HttpHeader> extra){ List<HttpHeader> h=new ArrayList<>(); h.add(new Header("Host","api.lab")); h.add(new Header("Accept","application/json")); if(token!=null) h.add(new Header("Authorization","Bearer "+token)); h.addAll(extra); return new FakeRequest(id,method,path,new byte[0],token,h); }
    private static FakeResponse response(int id,FakeRequest req,int status,String body){return new FakeResponse(id,req,(short)status,body.getBytes(StandardCharsets.UTF_8),List.of(new Header("Content-Type","application/json"),new Header("X-Request-ID","volatile")));}
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    private record Header(String name,String value) implements HttpHeader {}
    private record Bytes(byte[] bytes) implements ByteArray { @Override public byte[] getBytes(){return bytes.clone();} @Override public int length(){return bytes.length;} }
    private static final class Service implements HttpService {public String host(){return "api.lab";}public int port(){return 443;}public boolean secure(){return true;}}
    private static final class Source implements ToolSource {public ToolType toolType(){return ToolType.PROXY;}}
    private static final class FakeRequest implements HttpRequestToBeSent {
        private final int id; private final String method,path; private final byte[] body; private final List<HttpHeader> headers;
        FakeRequest(int id,String method,String path,byte[] body,String ignored,List<HttpHeader> headers){this.id=id;this.method=method;this.path=path;this.body=body.clone();this.headers=List.copyOf(headers);}
        public int messageId(){return id;}public boolean isInScope(){return true;}public ToolSource toolSource(){return new Source();}public String method(){return method;}public String path(){return path;}public String pathWithoutQuery(){int q=path.indexOf('?');return q<0?path:path.substring(0,q);}public String httpVersion(){return "HTTP/1.1";}public List<HttpHeader> headers(){return headers;}public ByteArray body(){return new Bytes(body);}public ByteArray toByteArray(){return new Bytes((method+" "+path+" HTTP/1.1\r\n\r\n").getBytes(StandardCharsets.UTF_8));}public HttpService httpService(){return new Service();}
    }
    private static final class FakeResponse implements HttpResponseReceived {
        private final int id; private final HttpRequest request; private final short status; private final byte[] body; private final List<HttpHeader> headers;
        FakeResponse(int id,HttpRequest request,short status,byte[] body,List<HttpHeader> headers){this.id=id;this.request=request;this.status=status;this.body=body.clone();this.headers=List.copyOf(headers);}
        public int messageId(){return id;}public HttpRequest initiatingRequest(){return request;}public ToolSource toolSource(){return new Source();}public short statusCode(){return status;}public String httpVersion(){return "HTTP/1.1";}public List<HttpHeader> headers(){return headers;}public String headerValue(String name){return headers.stream().filter(h->h.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst().orElse(null);}public ByteArray body(){return new Bytes(body);}public ByteArray toByteArray(){return new Bytes(body);}
    }
    private static final class FakeRegistration implements Registration { boolean deregistered; public void deregister(){deregistered=true;} }
    private static final class FakeExtension implements Extension {String name; ExtensionUnloadingHandler unload; public void setName(String n){name=n;}public Registration registerUnloadingHandler(ExtensionUnloadingHandler h){unload=h;return new FakeRegistration();}}
    private static final class FakeHttp implements Http {HttpHandler handler;public Registration registerHttpHandler(HttpHandler h){handler=h;return new FakeRegistration();}}
    private static final class FakeLogging implements Logging {public void logToOutput(String message){}public void logToError(String message){}}
    private static final class FakeUi implements UserInterface {Component component;public Registration registerSuiteTab(String caption,Component component){this.component=component;return new FakeRegistration();}}
    private static final class FakeApi implements MontoyaApi {final FakeExtension extension=new FakeExtension();final FakeHttp http=new FakeHttp();final FakeLogging logging=new FakeLogging();final FakeUi ui=new FakeUi();public Extension extension(){return extension;}public Http http(){return http;}public Logging logging(){return logging;}public UserInterface userInterface(){return ui;}}
}
