package burp.api.montoya.http.message.requests;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;
import java.util.List;

public interface HttpRequest {
    String method();
    String path();
    String pathWithoutQuery();
    String httpVersion();
    List<HttpHeader> headers();
    ByteArray body();
    ByteArray toByteArray();
    HttpService httpService();

    default String url() {
        HttpService service = httpService();
        if (service == null) return path();
        String scheme = service.secure() ? "https" : "http";
        boolean standard = (service.secure() && service.port() == 443)
                || (!service.secure() && service.port() == 80);
        return scheme + "://" + service.host()
                + (standard ? "" : ":" + service.port())
                + path();
    }
}
