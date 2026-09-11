package io.acra.core.domain.http;

import io.acra.core.domain.common.DomainLimits;
import io.acra.core.domain.common.DomainValidationException;
import io.acra.core.domain.common.Validation;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HttpRequest {
    private final HttpMethod method;
    private final String scheme;
    private final String host;
    private final int port;
    private final String rawTarget;
    private final String path;
    private final String query;
    private final List<HttpHeader> headers;
    private final Map<String,String> cookies;
    private final byte[] body;
    private final HttpProtocol protocol;
    private final byte[] rawBytes;

    public HttpRequest(HttpMethod method, String scheme, String host, int port, String rawTarget,
                       List<HttpHeader> headers, Map<String,String> cookies, byte[] body,
                       HttpProtocol protocol, byte[] rawBytes) {
        if (method == null) throw new DomainValidationException("HTTP method is required");
        this.method = method;
        this.scheme = Validation.requireNonBlank(scheme, "scheme").toLowerCase(Locale.ROOT);
        this.host = Validation.requireNonBlank(host, "host");
        if (port < 1 || port > 65535) throw new DomainValidationException("port must be 1-65535");
        this.port = port;
        this.rawTarget = Validation.requireNonBlank(rawTarget, "rawTarget");
        int q = rawTarget.indexOf('?');
        this.path = q >= 0 ? rawTarget.substring(0, q) : rawTarget;
        this.query = q >= 0 ? rawTarget.substring(q + 1) : "";
        List<HttpHeader> safeHeaders = headers == null ? List.of() : List.copyOf(headers);
        if (safeHeaders.size() > DomainLimits.MAX_HEADER_COUNT) throw new DomainValidationException("too many headers");
        for (HttpHeader h : safeHeaders) {
            if (h.value().length() > DomainLimits.MAX_HEADER_VALUE_CHARS) throw new DomainValidationException("header value too large: " + h.name());
        }
        this.headers = safeHeaders;
        TreeMap<String,String> cookieCopy = new TreeMap<>();
        if (cookies != null) cookieCopy.putAll(cookies);
        this.cookies = Collections.unmodifiableMap(cookieCopy);
        byte[] bodyCopy = body == null ? new byte[0] : body.clone();
        if (bodyCopy.length > DomainLimits.MAX_BODY_BYTES) throw new DomainValidationException("body exceeds domain safety limit");
        this.body = bodyCopy;
        this.protocol = protocol == null ? HttpProtocol.UNKNOWN : protocol;
        this.rawBytes = rawBytes == null ? new byte[0] : rawBytes.clone();
    }

    public static HttpRequest of(HttpMethod method, String scheme, String host, int port, String rawTarget,
                                 List<HttpHeader> headers, byte[] body, HttpProtocol protocol) {
        return new HttpRequest(method, scheme, host, port, rawTarget, headers, parseCookies(headers), body, protocol, new byte[0]);
    }

    private static Map<String,String> parseCookies(List<HttpHeader> headers) {
        TreeMap<String,String> result = new TreeMap<>();
        if (headers == null) return result;
        for (HttpHeader h : headers) {
            if (h.name().equalsIgnoreCase("Cookie")) {
                for (String part : h.value().split(";")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) result.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                }
            }
        }
        return result;
    }

    public Optional<String> firstHeader(String name) {
        return headers.stream().filter(h -> h.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst();
    }
    public HttpMethod method() { return method; }
    public String scheme() { return scheme; }
    public String host() { return host; }
    public int port() { return port; }
    public String rawTarget() { return rawTarget; }
    public String path() { return path; }
    public String query() { return query; }
    public List<HttpHeader> headers() { return headers; }
    public Map<String,String> cookies() { return cookies; }
    public byte[] body() { return body.clone(); }
    public HttpProtocol protocol() { return protocol; }
    public byte[] rawBytes() { return rawBytes.clone(); }
    public String bodyUtf8() { return new String(body, StandardCharsets.UTF_8); }
}
