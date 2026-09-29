package io.acra.burp.bridge;

import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;

import java.nio.charset.StandardCharsets;

public final class BridgeRequestSerializer {
    public String raw(HttpRequest request) {
        if (request == null) throw new IllegalArgumentException("request required");
        byte[] raw = request.rawBytes();
        if (raw.length > 0) return new String(raw, StandardCharsets.ISO_8859_1);

        StringBuilder out = new StringBuilder();
        out.append(request.method()).append(' ')
                .append(request.rawTarget()).append(' ')
                .append(protocol(request.protocol())).append("\r\n");

        boolean hasHost = request.headers().stream().anyMatch(header -> header.name().equalsIgnoreCase("Host"));
        if (!hasHost) {
            out.append("Host: ").append(request.host());
            int defaultPort = request.scheme().equalsIgnoreCase("https") ? 443 : 80;
            if (request.port() != defaultPort) out.append(':').append(request.port());
            out.append("\r\n");
        }

        request.headers().forEach(header ->
                out.append(header.name()).append(": ").append(header.value()).append("\r\n"));
        out.append("\r\n");
        out.append(new String(request.body(), StandardCharsets.ISO_8859_1));
        return out.toString();
    }

    private static String protocol(HttpProtocol protocol) {
        return switch (protocol) {
            case HTTP_1_0 -> "HTTP/1.0";
            case HTTP_2 -> "HTTP/2";
            case HTTP_3 -> "HTTP/3";
            case HTTP_1_1, UNKNOWN -> "HTTP/1.1";
        };
    }
}
