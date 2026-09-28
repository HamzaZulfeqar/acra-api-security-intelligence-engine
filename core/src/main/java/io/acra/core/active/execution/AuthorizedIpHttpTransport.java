package io.acra.core.active.execution;

import io.acra.core.active.evidence.RequestSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** One explicitly authorized IPv4 authority; no DNS, proxy, redirect, or response-driven URL. */
public final class AuthorizedIpHttpTransport implements HttpTransport {
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> SYNTHESIZED = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "upgrade", "expect");
    private final TargetDescriptor target;
    private final HttpClient client;

    public AuthorizedIpHttpTransport(TargetDescriptor target) {
        if (target == null || !target.authorized()
                || !(target.environment() == ExecutionEnvironment.AUTHORIZED_DEV
                    || target.environment() == ExecutionEnvironment.AUTHORIZED_STAGING)
                || !permittedIpv4(target.host())) {
            throw new IllegalArgumentException("external transport requires an authorized development/staging IPv4 target");
        }
        this.target = target;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(new ProxySelector() {
                    @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
                    @Override public void connectFailed(URI uri, SocketAddress address, java.io.IOException failure) { }
                })
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static boolean permittedIpv4(String host) {
        if (host == null || !host.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) return false;
        String[] octets = host.split("\\.");
        int[] numbers = new int[4];
        for (int i = 0; i < 4; i++) {
            if (octets[i].length() > 1 && octets[i].startsWith("0")) return false;
            numbers[i] = Integer.parseInt(octets[i]);
            if (numbers[i] > 255) return false;
        }
        return routable(numbers);
    }

    private static boolean routable(int[] ip) {
        int a = ip[0], b = ip[1];
        return a != 0 && a != 127 && a < 224 && a != 169 /* link-local/metadata */
                && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 192 && b == 0)
                && !(a == 198 && (b == 18 || b == 19));
    }

    @Override
    public TransportResult send(RequestSnapshot snapshot, Duration timeout, CancellationToken cancellation) throws Exception {
        if (snapshot == null || timeout == null || timeout.isZero() || timeout.isNegative() || cancellation == null) {
            throw new IllegalArgumentException("request snapshot, positive timeout and cancellation token required");
        }
        cancellation.throwIfCancelled();
        var request = snapshot.request();
        if (!request.scheme().equalsIgnoreCase(target.scheme())
                || !request.host().equalsIgnoreCase(target.host()) || request.port() != target.port()
                || !Set.of("GET", "HEAD", "OPTIONS").contains(request.method().name())) {
            throw new IllegalArgumentException("request is outside the authorized read-only authority");
        }
        String raw = request.rawTarget();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (!raw.startsWith("/") || raw.startsWith("//") || raw.contains("://")
                || raw.indexOf('#') >= 0 || raw.indexOf('\\') >= 0
                || lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")
                || lower.contains("%3f") || lower.contains("%23") || lower.contains("%40")
                || lower.contains("%25")) {
            throw new IllegalArgumentException("request path is not a safe origin-form path");
        }
        String path = raw.split("\\?", 2)[0];
        for (String segment : path.split("/")) {
            if (segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("dot segments are not accepted");
        }
        if (target.allowedPathPrefixes().stream().noneMatch(prefix -> prefix.equals("/")
                || path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"))) {
            throw new IllegalArgumentException("request path is outside authorized scope");
        }
        URI uri = URI.create(target.scheme() + "://" + target.host() + ":" + target.port() + raw);
        var builder = java.net.http.HttpRequest.newBuilder(uri).timeout(timeout);
        for (HttpHeader header : request.headers()) {
            if (!SYNTHESIZED.contains(header.name().toLowerCase(Locale.ROOT)) && !header.name().equalsIgnoreCase("cookie")) {
                builder.header(header.name(), header.value());
            }
        }
        if (!request.cookies().isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            request.cookies().forEach((name, value) -> {
                if (cookie.length() > 0) cookie.append("; ");
                cookie.append(name).append('=').append(value);
            });
            builder.header("Cookie", cookie.toString());
        }
        builder.method(request.method().name(), BodyPublishers.noBody());
        long started = System.nanoTime();
        java.net.http.HttpResponse<InputStream> live = client.send(builder.build(), BodyHandlers.ofInputStream());
        byte[] bytes;
        try (InputStream stream = live.body()) { bytes = stream.readNBytes(MAX_RESPONSE_BYTES + 1); }
        if (bytes.length > MAX_RESPONSE_BYTES) throw new IllegalStateException("response exceeds safety limit");
        Duration timing = Duration.ofNanos(Math.max(0L, System.nanoTime() - started));
        cancellation.throwIfCancelled();
        List<HttpHeader> headers = new ArrayList<>();
        live.headers().map().forEach((name, values) -> values.forEach(value -> headers.add(new HttpHeader(name, value))));
        HttpResponse response = new HttpResponse(live.statusCode(), headers, bytes,
                live.headers().firstValue("Content-Type").orElse(""),
                live.version() == HttpClient.Version.HTTP_2 ? HttpProtocol.HTTP_2 : HttpProtocol.HTTP_1_1,
                new byte[0]);
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String header : live.headers().allValues("Set-Cookie")) {
            String pair = header.split(";", 2)[0];
            int equals = pair.indexOf('=');
            if (equals > 0) cookies.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
        }
        return new TransportResult(response, cookies, timing);
    }
}
