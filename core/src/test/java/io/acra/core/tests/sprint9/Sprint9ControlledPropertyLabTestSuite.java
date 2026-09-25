package io.acra.core.tests.sprint9;

import io.acra.core.tests.TestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public final class Sprint9ControlledPropertyLabTestSuite {
    private static final int SECURE_PORT = 18082;
    private static final int VULNERABLE_PORT = 18081;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint9ControlledPropertyLabTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT9_CONTROLLED_PROPERTY_LAB PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;

        HttpResponse<String> secureRead = get(SECURE_PORT, "user-a");
        TestSupport.assertEquals(200, secureRead.statusCode(),
                "secure self-profile read must remain available");
        assertions++;
        TestSupport.assertContains(secureRead.body(), "\"display_name\"",
                "secure response retains explicitly allowed display_name");
        assertions++;
        TestSupport.assertNotContains(secureRead.body(), "\"salary_band\"",
                "secure response excludes denied salary_band property");
        assertions++;
        TestSupport.assertNotContains(secureRead.body(), "\"is_admin\"",
                "secure response excludes privileged is_admin property");
        assertions++;

        HttpResponse<String> vulnerableRead = get(VULNERABLE_PORT, "user-a");
        TestSupport.assertEquals(200, vulnerableRead.statusCode(),
                "vulnerable self-profile read remains an object-level allow");
        assertions++;
        TestSupport.assertContains(vulnerableRead.body(), "\"salary_band\"",
                "deliberately vulnerable response exposes denied salary_band property");
        assertions++;
        TestSupport.assertContains(vulnerableRead.body(), "\"is_admin\"",
                "deliberately vulnerable response exposes privileged property");
        assertions++;

        HttpResponse<String> securePrivilegedUpdate = patch(
                SECURE_PORT, "user-a", "{\"is_admin\":true}");
        TestSupport.assertEquals(403, securePrivilegedUpdate.statusCode(),
                "secure fixture denies privileged property update");
        assertions++;
        TestSupport.assertContains(securePrivilegedUpdate.body(), "property_access_denied",
                "secure property denial is explicit");
        assertions++;

        HttpResponse<String> vulnerablePrivilegedUpdate = patch(
                VULNERABLE_PORT, "user-a", "{\"is_admin\":true}");
        TestSupport.assertEquals(200, vulnerablePrivilegedUpdate.statusCode(),
                "deliberately vulnerable fixture accepts privileged property update");
        assertions++;
        TestSupport.assertContains(vulnerablePrivilegedUpdate.body(), "\"is_admin\":true",
                "vulnerable fixture demonstrates applied privileged property change");
        assertions++;
        TestSupport.assertContains(vulnerablePrivilegedUpdate.body(), "\"applied_properties\":[\"is_admin\"]",
                "vulnerable fixture records the explicit applied property");
        assertions++;

        HttpResponse<String> secureAllowedUpdate = patch(
                SECURE_PORT, "user-a", "{\"display_name\":\"User A Updated\"}");
        TestSupport.assertEquals(200, secureAllowedUpdate.statusCode(),
                "secure fixture permits explicitly allowed display_name update");
        assertions++;
        TestSupport.assertContains(secureAllowedUpdate.body(), "\"applied_properties\":[\"display_name\"]",
                "secure allow control records only the permitted property");
        assertions++;

        HttpResponse<String> secureCrossObject = getAs(SECURE_PORT, "user-b", "user-a");
        TestSupport.assertEquals(403, secureCrossObject.statusCode(),
                "secure cross-object control remains denied");
        assertions++;

        HttpResponse<String> vulnerableCrossObject = getAs(VULNERABLE_PORT, "user-b", "user-a");
        TestSupport.assertEquals(403, vulnerableCrossObject.statusCode(),
                "property fixture does not introduce a separate BOLA flaw");
        assertions++;

        return assertions;
    }

    private static HttpResponse<String> get(int port, String targetUser) throws Exception {
        return getAs(port, targetUser, targetUser);
    }

    private static HttpResponse<String> getAs(int port, String targetUser, String actor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(port, targetUser))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "Bearer " + token(actor, "tenant-a", "viewer"))
                .header("Accept", "application/json")
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> patch(int port, String targetUser, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(port, targetUser))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "Bearer " + token(targetUser, "tenant-a", "viewer"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static URI uri(int port, String targetUser) {
        return URI.create("http://127.0.0.1:" + port + "/api/v1/s9/users/" + targetUser + "/profile");
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s9synthetic";
    }
}
