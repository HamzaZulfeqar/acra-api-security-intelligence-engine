package io.acra.standalone.tests;

import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** An observational offline workload; results are not an accuracy or throughput guarantee. */
public final class StandaloneImportPerformanceObservation {
    private StandaloneImportPerformanceObservation() {}

    public static void main(String[] args) throws Exception {
        for (int size : new int[] {100, 1000}) observe(size);
    }

    private static void observe(int size) throws Exception {
        Path root = Files.createTempDirectory("acra-import-observation-");
        try {
            LocalWorkspaceStore store = new LocalWorkspaceStore(root);
            var project = store.createProject("Offline workload", "No network requests");
            var target = store.addTarget(project.id(), "Scoped API", "https://api.example.test/api/v1/",
                    "STAGING", "PERF-LOCAL-001", "IMPORT_ONLY");
            String input = openApi(size);
            long before = System.nanoTime();
            var result = new StandaloneImportService(store).importText(
                    project.id(), target.id(), "OPENAPI", "generated-observation.json", input);
            long afterImport = System.nanoTime();
            int persisted = new LocalWorkspaceStore(root).listInventory(project.id()).size();
            long afterRead = System.nanoTime();
            if (result.observations() != size || persisted != size) {
                throw new AssertionError("offline import inventory count: " + result.observations() + "/" + persisted);
            }
            System.out.printf("OFFLINE_IMPORT_OBSERVATION endpoints=%d bytes=%d import_ms=%.3f "
                            + "reopen_list_ms=%.3f persisted=%d%n", size, input.length(),
                    (afterImport - before) / 1_000_000.0,
                    (afterRead - afterImport) / 1_000_000.0, persisted);
        } finally {
            try (var walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static String openApi(int size) {
        StringBuilder json = new StringBuilder("{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Workload\",\"version\":\"1\"},\"paths\":{");
        for (int i = 0; i < size; i++) {
            if (i != 0) json.append(',');
            int value = i;
            StringBuilder label = new StringBuilder();
            for (int n = 0; n < 3; n++) {
                label.append((char) ('a' + (value % 26)));
                value /= 26;
            }
            json.append("\"/operation").append(label).append("\":{\"get\":{\"responses\":{\"200\":{\"description\":\"ok\"}}}}");
        }
        return json.append("}}").toString();
    }
}
