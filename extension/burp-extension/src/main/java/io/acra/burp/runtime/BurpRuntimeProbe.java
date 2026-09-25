package io.acra.burp.runtime;

import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import io.acra.burp.traffic.TrafficProcessingResult;
import io.acra.core.domain.observation.TrafficObservation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Opt-in, non-production Phase 7 runtime evidence probe.
 *
 * Enabled only when -Dacra.phase7.probeFile=/path/to/file is supplied.
 * It deliberately records no headers, cookies, tokens, request bodies, or response bodies.
 */
public final class BurpRuntimeProbe {
    private static final String PROPERTY = "acra.phase7.probeFile";
    private final Path path;

    private BurpRuntimeProbe(Path path) {
        this.path = path;
    }

    public static BurpRuntimeProbe fromSystemProperty() {
        String configured = System.getProperty(PROPERTY, "").trim();
        return configured.isEmpty() ? new BurpRuntimeProbe(null) : new BurpRuntimeProbe(Path.of(configured).toAbsolutePath());
    }

    public boolean enabled() {
        return path != null;
    }

    public void initialized() {
        write("{\"event\":\"INITIALIZED\",\"extension\":\"ACRA\",\"timestamp\":\"" + esc(Instant.now().toString()) + "\"}");
    }

    public void request(HttpRequestToBeSent request) {
        if (!enabled()) return;
        var service = request.httpService();
        write("{\"event\":\"REQUEST\",\"messageId\":" + request.messageId()
                + ",\"method\":\"" + esc(request.method()) + "\""
                + ",\"host\":\"" + esc(service.host()) + "\""
                + ",\"port\":" + service.port()
                + ",\"secure\":" + service.secure()
                + ",\"path\":\"" + esc(request.pathWithoutQuery()) + "\"}");
    }

    public void response(HttpResponseReceived response) {
        if (!enabled()) return;
        write("{\"event\":\"RESPONSE\",\"messageId\":" + response.messageId()
                + ",\"status\":" + response.statusCode()
                + ",\"tool\":\"" + esc(response.toolSource().toolType().name()) + "\""
                + ",\"path\":\"" + esc(response.initiatingRequest().pathWithoutQuery()) + "\"}");
    }

    public void processed(TrafficProcessingResult result) {
        if (!enabled()) return;
        TrafficObservation o = result.observation();
        write("{\"event\":\"PROCESSED\",\"transactionId\":\"" + esc(o.transactionId()) + "\""
                + ",\"responseStatus\":" + o.responseStatus()
                + ",\"principal\":\"" + esc(o.principalId()) + "\""
                + ",\"tenant\":\"" + esc(o.tenantId()) + "\""
                + ",\"resourceType\":\"" + esc(o.resourceType()) + "\""
                + ",\"resourceId\":\"" + esc(o.resourceId()) + "\""
                + ",\"action\":\"" + esc(o.action().name()) + "\""
                + ",\"contextStatus\":\"" + esc(o.contextStatus().name()) + "\""
                + ",\"observationStatus\":\"" + esc(o.status().name()) + "\"}");
    }

    public void unloading() {
        write("{\"event\":\"UNLOADING\",\"timestamp\":\"" + esc(Instant.now().toString()) + "\"}");
    }

    public void error(String phase, RuntimeException ex) {
        if (!enabled()) return;
        write("{\"event\":\"ERROR\",\"phase\":\"" + esc(phase) + "\",\"type\":\""
                + esc(ex.getClass().getSimpleName()) + "\",\"message\":\"" + esc(String.valueOf(ex.getMessage())) + "\"}");
    }

    private synchronized void write(String line) {
        if (!enabled()) return;
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write Phase 7 Burp runtime probe", ex);
        }
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}
