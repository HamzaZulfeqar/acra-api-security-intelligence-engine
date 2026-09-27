package io.acra.core.imports;

import io.acra.core.domain.http.HttpMethod;

import java.net.URI;
import java.util.Locale;

public final class RawHttpRequestImporter {
    public ImportedEndpointObservation importText(String text, URI targetBaseUri, String sourceReference) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("raw HTTP request is required");
        if (targetBaseUri == null) throw new IllegalArgumentException("target base URI is required");

        String normalized = text.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        if (lines.length == 0 || lines[0].isBlank()) throw new IllegalArgumentException("HTTP request line is required");

        String[] requestLine = lines[0].trim().split("\\s+");
        if (requestLine.length < 2) throw new IllegalArgumentException("HTTP request line must contain method and target");

        HttpMethod method = HttpMethod.parse(requestLine[0]);
        String requestTarget = requestLine[1];

        String hostHeader = "";
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (line.isBlank()) break;
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String name = line.substring(0, colon).trim();
            if (name.equalsIgnoreCase("Host")) {
                hostHeader = line.substring(colon + 1).trim();
                break;
            }
        }

        URI uri = absoluteUri(requestTarget, targetBaseUri);
        validateHostHeader(hostHeader, uri);

        return new ImportedEndpointObservation(method, uri, 0, "RAW_HTTP", sourceReference);
    }

    private static URI absoluteUri(String requestTarget, URI base) {
        if (requestTarget.startsWith("http://") || requestTarget.startsWith("https://")) {
            return URI.create(requestTarget);
        }
        if (!requestTarget.startsWith("/")) {
            throw new IllegalArgumentException("raw HTTP request target must be origin-form or absolute-form");
        }
        String authority = base.getHost() + (base.getPort() > 0 ? ":" + base.getPort() : "");
        return URI.create(base.getScheme() + "://" + authority + requestTarget);
    }

    private static void validateHostHeader(String hostHeader, URI uri) {
        if (hostHeader == null || hostHeader.isBlank()) return;
        String normalizedHeader = hostHeader.toLowerCase(Locale.ROOT);
        String expectedHost = uri.getHost().toLowerCase(Locale.ROOT);
        int effectivePort = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
        String expectedWithPort = expectedHost + ":" + effectivePort;
        if (!normalizedHeader.equals(expectedHost) && !normalizedHeader.equals(expectedWithPort)) {
            throw new IllegalArgumentException("Host header does not match the resolved request target");
        }
    }
}
