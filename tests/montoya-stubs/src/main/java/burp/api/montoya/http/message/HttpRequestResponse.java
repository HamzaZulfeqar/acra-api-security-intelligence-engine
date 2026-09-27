package burp.api.montoya.http.message;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

/**
 * Minimal local-contract Montoya stub used only by legacy offline compilation.
 *
 * This is not a runtime implementation and does not replace the official
 * Montoya dependency used by the Maven build.
 */
public interface HttpRequestResponse {
    HttpRequest request();

    HttpResponse response();
}
