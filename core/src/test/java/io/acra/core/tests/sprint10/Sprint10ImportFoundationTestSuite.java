package io.acra.core.tests.sprint10;

import io.acra.core.imports.HarEndpointImporter;
import io.acra.core.imports.RawHttpRequestImporter;
import io.acra.core.openapi.OpenApiImporter;

import java.net.URI;

public final class Sprint10ImportFoundationTestSuite {
    private static int assertions;

    private Sprint10ImportFoundationTestSuite() {}

    public static void main(String[] args) {
        openApiImport();
        harImport();
        rawHttpImport();
        System.out.println("SPRINT10_IMPORT_FOUNDATION PASS assertions=" + assertions);
    }

    private static void openApiImport() {
        String spec = """
                {
                  "openapi":"3.0.3",
                  "info":{"title":"Demo","version":"1"},
                  "paths":{
                    "/api/v1/users/{userId}":{
                      "get":{"operationId":"getUser","responses":{"200":{"description":"ok"}}},
                      "patch":{"operationId":"patchUser","responses":{"200":{"description":"ok"}}}
                    }
                  }
                }
                """;
        var document = new OpenApiImporter().importText(spec, "unit-test");
        check(document.operations().size() == 2, "OpenAPI operations imported");
        check(document.operations().stream().anyMatch(op -> op.operationId().equals("getUser")), "operationId preserved");
    }

    private static void harImport() {
        String har = """
                {
                  "log":{
                    "version":"1.2",
                    "entries":[
                      {
                        "request":{"method":"GET","url":"https://api.example.test/api/v1/users/42"},
                        "response":{"status":200}
                      },
                      {
                        "request":{"method":"PATCH","url":"https://api.example.test/api/v1/users/42"},
                        "response":{"status":403}
                      }
                    ]
                  }
                }
                """;
        var imported = new HarEndpointImporter().importText(har, "demo.har");
        check(imported.size() == 2, "HAR entries imported");
        check(imported.getFirst().responseStatus() == 200, "HAR status preserved");
        check(imported.getLast().method().name().equals("PATCH"), "HAR method preserved");
    }

    private static void rawHttpImport() {
        String raw = "GET /api/v1/users/42?detail=full HTTP/1.1\r\nHost: api.example.test\r\n\r\n";
        var imported = new RawHttpRequestImporter().importText(
                raw,
                URI.create("https://api.example.test"),
                "request.txt"
        );
        check(imported.uri().getHost().equals("api.example.test"), "raw HTTP host resolved");
        check(imported.rawPath().equals("/api/v1/users/42"), "raw HTTP path preserved");

        expectFailure(() -> new RawHttpRequestImporter().importText(
                "GET / HTTP/1.1\r\nHost: attacker.invalid\r\n\r\n",
                URI.create("https://api.example.test"),
                "request.txt"
        ), "mismatched Host header rejected");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable runnable, String message) {
        assertions++;
        try {
            runnable.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
