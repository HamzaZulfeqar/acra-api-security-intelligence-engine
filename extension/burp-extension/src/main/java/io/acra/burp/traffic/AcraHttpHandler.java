package io.acra.burp.traffic;
import burp.api.montoya.http.handler.*;
public final class AcraHttpHandler implements HttpHandler {
    private final TrafficCollector collector;
    public AcraHttpHandler(TrafficCollector collector){this.collector=collector;}
    @Override public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent){collector.onRequest(requestToBeSent);return RequestToBeSentAction.continueWith(requestToBeSent);}
    @Override public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived){collector.onResponse(responseReceived);return ResponseReceivedAction.continueWith(responseReceived);}
}
