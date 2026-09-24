package io.acra.core.tests.sprint10;

import io.acra.core.tests.TestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public final class Sprint10ControlledBatchIndirectLabTestSuite {
    private static final int SECURE_PORT = 18082;
    private static final int VULNERABLE_PORT = 18081;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint10ControlledBatchIndirectLabTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT10_CONTROLLED_BATCH_INDIRECT_LAB PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;

        HttpResponse<String> secureMixed = batch(
                SECURE_PORT, "user-a", "{\"resource_ids\":[\"resource-a\",\"resource-b\"]}");
        TestSupport.assertEquals(200, secureMixed.statusCode(),
                "secure mixed batch retains aggregate HTTP success");
        assertions++;
        TestSupport.assertContains(secureMixed.body(),
                "\"resource_id\":\"resource-a\",\"decision\":\"ALLOW\"",
                "secure mixed batch allows owned resource");
        assertions++;
        TestSupport.assertContains(secureMixed.body(),
                "\"resource_id\":\"resource-b\",\"decision\":\"DENY\"",
                "secure mixed batch denies foreign resource per item");
        assertions++;
        TestSupport.assertContains(secureMixed.body(), "\"persisted\":false",
                "controlled batch-read fixture is non-persistent");
        assertions++;

        HttpResponse<String> vulnerableMixed = batch(
                VULNERABLE_PORT, "user-a", "{\"resource_ids\":[\"resource-a\",\"resource-b\"]}");
        TestSupport.assertEquals(200, vulnerableMixed.statusCode(),
                "deliberately vulnerable mixed batch also returns aggregate HTTP success");
        assertions++;
        TestSupport.assertContains(vulnerableMixed.body(),
                "\"resource_id\":\"resource-b\",\"decision\":\"ALLOW\"",
                "vulnerable batch-level shortcut incorrectly allows foreign item");
        assertions++;

        HttpResponse<String> secureOwn = batch(
                SECURE_PORT, "user-a", "{\"resource_ids\":[\"resource-a\"]}");
        TestSupport.assertEquals(200, secureOwn.statusCode(),
                "secure owned-only batch remains an allow control");
        assertions++;
        TestSupport.assertContains(secureOwn.body(),
                "\"resource_id\":\"resource-a\",\"decision\":\"ALLOW\"",
                "secure owned-only item is allowed");
        assertions++;

        HttpResponse<String> secureOwnAlias = getAlias(SECURE_PORT, "share-a", "user-a");
        TestSupport.assertEquals(200, secureOwnAlias.statusCode(),
                "secure owned indirect reference remains allowed");
        assertions++;
        TestSupport.assertContains(secureOwnAlias.body(), "\"resolved_resource_id\":\"resource-a\"",
                "secure owned alias resolves to the declared target");
        assertions++;

        HttpResponse<String> secureForeignAlias = getAlias(SECURE_PORT, "share-b", "user-a");
        TestSupport.assertEquals(403, secureForeignAlias.statusCode(),
                "secure foreign indirect reference is denied after resolution");
        assertions++;
        TestSupport.assertContains(secureForeignAlias.body(), "\"resolved_resource_id\":\"resource-b\"",
                "secure denial demonstrates the fixed alias resolved before authorization");
        assertions++;

        HttpResponse<String> vulnerableForeignAlias = getAlias(VULNERABLE_PORT, "share-b", "user-a");
        TestSupport.assertEquals(200, vulnerableForeignAlias.statusCode(),
                "deliberately vulnerable indirect fixture skips resolved-target authorization");
        assertions++;
        TestSupport.assertContains(vulnerableForeignAlias.body(), "\"resolved_resource_id\":\"resource-b\"",
                "vulnerable indirect fixture exposes the resolved foreign target");
        assertions++;

        HttpResponse<String> unauthenticatedBatch = batchWithoutAuth(
                SECURE_PORT, "{\"resource_ids\":[\"resource-a\"]}");
        TestSupport.assertEquals(401, unauthenticatedBatch.statusCode(),
                "batch fixture still requires authentication");
        assertions++;

        HttpResponse<String> unauthenticatedAlias = getAliasWithoutAuth(SECURE_PORT, "share-a");
        TestSupport.assertEquals(401, unauthenticatedAlias.statusCode(),
                "indirect-reference fixture still requires authentication");
        assertions++;

        return assertions;
    }

    private static HttpResponse<String> batch(int port, String actor, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/s10/documents/batch-read"))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "Bearer " + token(actor, "tenant-a", "viewer"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> batchWithoutAuth(int port, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/s10/documents/batch-read"))
                .timeout(Duration.ofSeconds(2))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getAlias(int port, String alias, String actor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/s10/share/" + alias))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "Bearer " + token(actor, "tenant-a", "viewer"))
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getAliasWithoutAuth(int port, String alias) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/s10/share/" + alias))
                .timeout(Duration.ofSeconds(2))
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s10synthetic";
    }
}
