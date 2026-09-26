package io.acra.core.imports;

import io.acra.core.domain.http.HttpMethod;
import io.acra.core.openapi.MiniJson;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HarEndpointImporter {
    private static final int MAX_ENTRIES = 10_000;

    public List<ImportedEndpointObservation> importText(String text, String sourceReference) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("HAR text is required");
        Object parsed = MiniJson.parse(text);
        if (!(parsed instanceof Map<?, ?> root)) throw new IllegalArgumentException("HAR root must be a JSON object");

        Object logObject = root.get("log");
        if (!(logObject instanceof Map<?, ?> log)) throw new IllegalArgumentException("HAR log object is required");

        Object entriesObject = log.get("entries");
        if (!(entriesObject instanceof List<?> entries)) throw new IllegalArgumentException("HAR log.entries array is required");
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("HAR exceeds " + MAX_ENTRIES + " entries");

        List<ImportedEndpointObservation> observations = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            Object entryObject = entries.get(index);
            if (!(entryObject instanceof Map<?, ?> entry)) {
                throw new IllegalArgumentException("HAR entry " + index + " must be an object");
            }
            Object requestObject = entry.get("request");
            if (!(requestObject instanceof Map<?, ?> request)) {
                throw new IllegalArgumentException("HAR entry " + index + " request object is required");
            }

            String methodText = text(request.get("method"));
            String url = text(request.get("url"));
            if (methodText.isBlank() || url.isBlank()) {
                throw new IllegalArgumentException("HAR entry " + index + " requires request.method and request.url");
            }

            int status = 0;
            Object responseObject = entry.get("response");
            if (responseObject instanceof Map<?, ?> response) {
                Object statusObject = response.get("status");
                if (statusObject instanceof Number number) status = number.intValue();
            }

            final URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("HAR entry " + index + " contains invalid request.url", ex);
            }

            observations.add(new ImportedEndpointObservation(
                    HttpMethod.parse(methodText),
                    uri,
                    status,
                    "HAR",
                    sourceReference
            ));
        }
        return List.copyOf(observations);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
