package io.acra.standalone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.engine.SecurityContextEngine;
import io.acra.core.engine.SecurityContextSnapshot;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class AcraStandaloneApplication {
    private static final int DEFAULT_PORT = 8765;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_HISTORY = 200;
    private static final String VERSION = "0.3.0";
    private static final String HOST = "127.0.0.1";

    private final SecurityContextEngine engine = new SecurityContextEngine();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ArrayDeque<AnalysisResult> history = new ArrayDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        boolean openBrowser = true;
        for (String arg : args) {
            if (arg.startsWith("--port=")) port = Integer.parseInt(arg.substring("--port=".length()));
            if (arg.equals("--no-browser")) openBrowser = false;
        }
        new AcraStandaloneApplication().start(port, openBrowser);
    }

    private void start(int port, boolean openBrowser) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), port), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/analyses", this::handleAnalyses);
        server.createContext("/api/analyze", this::handleAnalyze);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        URI ui = URI.create("http://" + HOST + ":" + port + "/");
        System.out.println("ACRA standalone v" + VERSION + " started.");
        System.out.println("GUI: " + ui);
        System.out.println("Burp is not required for standalone analysis.");
        System.out.println("Target acquisition is limited to one explicitly authorized read-only GET per request.");

        if (openBrowser && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(ui);
            } catch (IOException ex) {
                System.out.println("Browser auto-open unavailable: " + ex.getMessage());
            }
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", html());
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJson(exchange, 405, jsonError("Method Not Allowed"));
            return;
        }
        String body = "{\"status\":\"ready\",\"version\":\"" + VERSION +
                "\",\"mode\":\"standalone-localhost\",\"burpRequired\":false," +
                "\"bind\":\"" + HOST + "\",\"defaultPort\":" + DEFAULT_PORT +
                ",\"acquisition\":\"READ_ONLY_SINGLE_GET\",\"historyCount\":" + historySize() + "}";
        sendJson(exchange, 200, body);
    }

    private void handleAnalyses(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJson(exchange, 405, jsonError("Method Not Allowed"));
            return;
        }
        List<AnalysisResult> snapshot;
        synchronized (history) {
            snapshot = List.copyOf(history);
        }
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < snapshot.size(); i++) {
            if (i > 0) out.append(',');
            out.append(snapshot.get(i).toJson());
        }
        out.append(']');
        sendJson(exchange, 200, out.toString());
    }

    private void handleAnalyze(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, jsonError("Method Not Allowed"));
            return;
        }
        String contentType = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Content-Type")).orElse("");
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            sendJson(exchange, 415, jsonError("Expected application/x-www-form-urlencoded"));
            return;
        }

        String encoded = new String(exchange.getRequestBody().readNBytes(32 * 1024), StandardCharsets.UTF_8);
        Map<String,String> form = form(encoded);
        if (!"true".equalsIgnoreCase(form.getOrDefault("authorized", ""))) {
            sendJson(exchange, 403, jsonError("Explicit authorization acknowledgement is required"));
            return;
        }

        try {
            URI target = validateTarget(form.getOrDefault("url", "").trim());
            AnalysisResult result = analyze(target);
            remember(result);
            sendJson(exchange, 200, result.toJson());
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, jsonError(ex.getMessage()));
        } catch (java.net.http.HttpTimeoutException ex) {
            sendJson(exchange, 504, jsonError("Target request timed out"));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            sendJson(exchange, 503, jsonError("Analysis interrupted"));
        } catch (Exception ex) {
            sendJson(exchange, 502, jsonError("Target acquisition failed: " + safeMessage(ex)));
        }
    }

    private AnalysisResult analyze(URI target) throws Exception {
        java.net.http.HttpRequest outbound = java.net.http.HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "ACRA-Standalone/" + VERSION)
                .GET()
                .build();

        java.net.http.HttpResponse<InputStream> response =
                client.send(outbound, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        byte[] responseBody;
        try (InputStream stream = response.body()) {
            responseBody = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
        }
        if (responseBody.length > MAX_RESPONSE_BYTES) {
            throw new IllegalArgumentException("Response exceeds the 2 MiB standalone safety limit");
        }

        List<HttpHeader> responseHeaders = new ArrayList<>();
        response.headers().map().forEach((name, values) -> {
            for (String value : values) responseHeaders.add(new HttpHeader(name, value));
        });

        String rawTarget = target.getRawPath();
        if (rawTarget == null || rawTarget.isBlank()) rawTarget = "/";
        if (target.getRawQuery() != null && !target.getRawQuery().isBlank()) {
            rawTarget += "?" + target.getRawQuery();
        }

        int port = target.getPort() > 0 ? target.getPort() :
                (target.getScheme().equalsIgnoreCase("https") ? 443 : 80);

        io.acra.core.domain.http.HttpRequest request =
                io.acra.core.domain.http.HttpRequest.of(
                        HttpMethod.GET,
                        target.getScheme(),
                        target.getHost(),
                        port,
                        rawTarget,
                        List.of(
                                new HttpHeader("User-Agent", "ACRA-Standalone/" + VERSION),
                                new HttpHeader("Accept", "*/*")),
                        new byte[0],
                        HttpProtocol.HTTP_1_1);

        String responseContentType = response.headers().firstValue("content-type").orElse("");
        io.acra.core.domain.http.HttpResponse coreResponse =
                new io.acra.core.domain.http.HttpResponse(
                        response.statusCode(),
                        responseHeaders,
                        responseBody,
                        responseContentType,
                        protocol(response.version()),
                        new byte[0]);

        String requestId = "standalone-" + sequence.incrementAndGet();
        HttpTransaction tx = new HttpTransaction(
                request,
                coreResponse,
                Instant.now(),
                requestId,
                "standalone-local-gui",
                Map.of(
                        "acquisition", "READ_ONLY_GET",
                        "authorized", "true",
                        "target", target.toString()));

        SecurityContextSnapshot snapshot = engine.analyze(tx);
        AuthorizationContext context = snapshot.authorizationContext();

        return new AnalysisResult(
                requestId,
                target.toString(),
                response.statusCode(),
                snapshot.endpoint().method().name(),
                snapshot.endpoint().host(),
                snapshot.endpoint().routeTemplate(),
                context.principal() == null ? "" : context.principal().principalId(),
                context.role() == null ? "" : context.role().name(),
                context.tenant() == null ? "" : context.tenant().tenantId(),
                context.resource() == null ? "" : context.resource().resourceType() + ":" + context.resource().resourceId(),
                context.ownerPrincipalId(),
                context.action().actionType().name(),
                context.status().name(),
                context.evidenceIds().size(),
                Instant.now().toString());
    }

    private static HttpProtocol protocol(HttpClient.Version version) {
        return version == HttpClient.Version.HTTP_2 ? HttpProtocol.HTTP_2 : HttpProtocol.HTTP_1_1;
    }

    private static URI validateTarget(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Target URL is required");
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Target URL is invalid");
        }
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("").toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only http:// and https:// targets are supported");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Target URL must include a host");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Credentials in target URLs are not accepted");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("169.254.169.254") || host.equals("metadata.google.internal") || host.equals("100.100.100.200")) {
            throw new IllegalArgumentException("Cloud metadata endpoints are blocked");
        }
        return uri;
    }

    private void remember(AnalysisResult result) {
        synchronized (history) {
            history.addFirst(result);
            while (history.size() > MAX_HISTORY) history.removeLast();
        }
    }

    private int historySize() {
        synchronized (history) {
            return history.size();
        }
    }

    private static Map<String,String> form(String body) {
        Map<String,String> result = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return result;
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", body);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String jsonError(String message) {
        return "{\"error\":\"" + json(message) + "\"}";
    }

    private static String json(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String safeMessage(Exception ex) {
        String message = Objects.toString(ex.getMessage(), ex.getClass().getSimpleName());
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private static String html() {
        return """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>ACRA Standalone</title>
<style>
:root{font-family:Inter,system-ui,sans-serif;color:#e8eef8;background:#07111f}
*{box-sizing:border-box}body{margin:0;background:#07111f;min-height:100vh}
header{padding:28px 34px;border-bottom:1px solid #203149;background:#0a1524}
h1{margin:0 0 6px;font-size:28px}.muted{color:#94a5ba}.good{color:#77e6a1}
main{max-width:1080px;margin:0 auto;padding:28px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}
.card{background:#0d1b2d;border:1px solid #223652;border-radius:14px;padding:22px}
label{display:block;margin:12px 0 7px;font-weight:700}input[type=url]{width:100%;padding:13px;border:1px solid #36506f;border-radius:8px;background:#07111f;color:#fff}
button{margin-top:14px;padding:12px 16px;border:0;border-radius:8px;background:#2d7ff9;color:white;font-weight:800;cursor:pointer}
button:disabled{opacity:.6}.row{display:flex;gap:10px;align-items:flex-start;margin-top:12px}
pre{white-space:pre-wrap;word-break:break-word;background:#07111f;border:1px solid #1c314b;padding:14px;border-radius:8px;min-height:220px}
.stat{font-size:30px;font-weight:800}.full{grid-column:1/-1}
table{width:100%;border-collapse:collapse;font-size:13px}th,td{text-align:left;padding:10px;border-bottom:1px solid #203149;vertical-align:top}
code{color:#8fc2ff}@media(max-width:780px){.grid{grid-template-columns:1fr}.full{grid-column:auto}}
</style>
</head>
<body>
<header>
<h1>ACRA <span class="muted">Standalone</span></h1>
<div class="muted">Local API security-context analysis. Burp is optional, not required.</div>
</header>
<main>
<div class="grid">
<section class="card">
<h2>Target Analyzer</h2>
<p class="muted">Enter one exact HTTP(S) URL. This first standalone slice performs one read-only GET and sends the observed exchange directly through ACRA Core.</p>
<form id="analyze">
<label for="url">Authorized target URL</label>
<input id="url" name="url" type="url" placeholder="http://127.0.0.1:8080/api/users/42" required>
<div class="row">
<input id="authorized" name="authorized" type="checkbox" value="true" required>
<label for="authorized" style="margin:0;font-weight:500">I own this target or have explicit authorization to test it.</label>
</div>
<button id="run" type="submit">Analyze with ACRA Core</button>
</form>
</section>
<section class="card">
<h2>Runtime</h2>
<div class="stat good" id="status">starting...</div>
<p id="runtime" class="muted"></p>
<p><strong>Default GUI:</strong> <code>http://127.0.0.1:8765</code></p>
<p><strong>Burp required:</strong> <span class="good">No</span></p>
<p><strong>Acquisition:</strong> one authorized read-only GET</p>
</section>
<section class="card full">
<h2>Latest Analysis</h2>
<pre id="result">No standalone analysis has been run yet.</pre>
</section>
<section class="card full">
<h2>Analysis History</h2>
<div style="overflow:auto">
<table>
<thead><tr><th>ID</th><th>Target</th><th>Status</th><th>Endpoint</th><th>Principal</th><th>Tenant</th><th>Resource</th><th>Action</th><th>Context</th></tr></thead>
<tbody id="history"></tbody>
</table>
</div>
</section>
</div>
</main>
<script>
var statusEl=document.getElementById('status');
var runtimeEl=document.getElementById('runtime');
var resultEl=document.getElementById('result');
var historyEl=document.getElementById('history');
var formEl=document.getElementById('analyze');
var buttonEl=document.getElementById('run');

function esc(value){
  return String(value===undefined||value===null?'':value).replace(/[&<>"']/g,function(c){
    return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];
  });
}

async function refreshStatus(){
  var response=await fetch('/api/status');
  var data=await response.json();
  statusEl.textContent=String(data.status).toUpperCase();
  runtimeEl.textContent='ACRA v'+data.version+' · '+data.mode+' · '+data.bind+':'+data.defaultPort;
}

async function refreshHistory(){
  var response=await fetch('/api/analyses');
  var rows=await response.json();
  historyEl.innerHTML=rows.map(function(x){
    return '<tr><td>'+esc(x.id)+'</td><td>'+esc(x.target)+'</td><td>'+esc(x.responseStatus)+'</td>'+
      '<td>'+esc(x.method)+' '+esc(x.route)+'</td><td>'+esc(x.principal||'—')+'</td>'+
      '<td>'+esc(x.tenant||'—')+'</td><td>'+esc(x.resource||'—')+'</td>'+
      '<td>'+esc(x.action)+'</td><td>'+esc(x.contextStatus)+'</td></tr>';
  }).join('');
}

formEl.addEventListener('submit',async function(event){
  event.preventDefault();
  buttonEl.disabled=true;
  resultEl.textContent='Analyzing...';
  try{
    var body=new URLSearchParams(new FormData(formEl));
    var response=await fetch('/api/analyze',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:body
    });
    var data=await response.json();
    resultEl.textContent=JSON.stringify(data,null,2);
    await refreshHistory();
    await refreshStatus();
  }catch(error){
    resultEl.textContent='Error: '+String(error);
  }finally{
    buttonEl.disabled=false;
  }
});

refreshStatus().then(refreshHistory);
</script>
</body>
</html>
""";
    }

    private record AnalysisResult(
            String id,
            String target,
            int responseStatus,
            String method,
            String host,
            String route,
            String principal,
            String role,
            String tenant,
            String resource,
            String owner,
            String action,
            String contextStatus,
            int evidenceCount,
            String analyzedAt) {
        String toJson() {
            return "{" +
                    "\"id\":\"" + json(id) + "\"," +
                    "\"target\":\"" + json(target) + "\"," +
                    "\"responseStatus\":" + responseStatus + "," +
                    "\"method\":\"" + json(method) + "\"," +
                    "\"host\":\"" + json(host) + "\"," +
                    "\"route\":\"" + json(route) + "\"," +
                    "\"principal\":\"" + json(principal) + "\"," +
                    "\"role\":\"" + json(role) + "\"," +
                    "\"tenant\":\"" + json(tenant) + "\"," +
                    "\"resource\":\"" + json(resource) + "\"," +
                    "\"owner\":\"" + json(owner) + "\"," +
                    "\"action\":\"" + json(action) + "\"," +
                    "\"contextStatus\":\"" + json(contextStatus) + "\"," +
                    "\"evidenceCount\":" + evidenceCount + "," +
                    "\"analyzedAt\":\"" + json(analyzedAt) + "\"" +
                    "}";
        }
    }
}
