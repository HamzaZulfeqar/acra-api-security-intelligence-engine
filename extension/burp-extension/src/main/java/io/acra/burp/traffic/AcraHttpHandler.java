package io.acra.burp.traffic;
import burp.api.montoya.http.handler.*;
import io.acra.burp.runtime.BurpRuntimeProbe;
public final class AcraHttpHandler implements HttpHandler {
    private final TrafficCollector collector;
    private final BurpRuntimeProbe runtimeProbe;
    public AcraHttpHandler(TrafficCollector collector){this(collector,BurpRuntimeProbe.fromSystemProperty());}
    public AcraHttpHandler(TrafficCollector collector,BurpRuntimeProbe runtimeProbe){this.collector=collector;this.runtimeProbe=runtimeProbe;}
    @Override public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent){runtimeProbe.request(requestToBeSent);collector.onRequest(requestToBeSent);return RequestToBeSentAction.continueWith(requestToBeSent);}
    @Override public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived){collector.onResponse(responseReceived);runtimeProbe.response(responseReceived);return ResponseReceivedAction.continueWith(responseReceived);}
}
