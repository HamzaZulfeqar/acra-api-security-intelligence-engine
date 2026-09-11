package io.acra.core.domain.http;

import io.acra.core.domain.common.DomainLimits;
import io.acra.core.domain.common.DomainValidationException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class HttpResponse {
    private final int status;
    private final List<HttpHeader> headers;
    private final byte[] body;
    private final String contentType;
    private final HttpProtocol protocol;
    private final byte[] rawBytes;

    public HttpResponse(int status, List<HttpHeader> headers, byte[] body, String contentType, HttpProtocol protocol, byte[] rawBytes) {
        if (status < 100 || status > 599) throw new DomainValidationException("HTTP status must be 100-599");
        this.status = status;
        this.headers = headers == null ? List.of() : List.copyOf(headers);
        if (this.headers.size() > DomainLimits.MAX_HEADER_COUNT) throw new DomainValidationException("too many response headers");
        byte[] copy = body == null ? new byte[0] : body.clone();
        if (copy.length > DomainLimits.MAX_BODY_BYTES) throw new DomainValidationException("response body exceeds domain safety limit");
        this.body = copy;
        this.contentType = contentType == null ? "" : contentType;
        this.protocol = protocol == null ? HttpProtocol.UNKNOWN : protocol;
        this.rawBytes = rawBytes == null ? new byte[0] : rawBytes.clone();
    }
    public int status() { return status; }
    public int length() { return body.length; }
    public java.util.Optional<String> firstHeader(String name) { return headers.stream().filter(h -> h.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst(); }
    public List<HttpHeader> headers() { return headers; }
    public byte[] body() { return body.clone(); }
    public String contentType() { return contentType; }
    public HttpProtocol protocol() { return protocol; }
    public byte[] rawBytes() { return rawBytes.clone(); }
    public String bodyUtf8() { return new String(body, StandardCharsets.UTF_8); }
}
