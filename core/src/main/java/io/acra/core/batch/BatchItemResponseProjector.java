package io.acra.core.batch;

import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BatchItemResponseProjector {
    private static final Pattern FLAT_OBJECT = Pattern.compile("\\{[^{}]*}");
    private static final Pattern RESOURCE = Pattern.compile("\\"resource_id\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern DECISION = Pattern.compile("\\"decision\\"\\s*:\\s*\\\"(ALLOW|DENY)\\\"");

    public List<BatchItemObservation> project(
            ResponseSnapshot response,
            String sourceObservationId,
            String executionId,
            String testId,
            String batchId,
            String endpoint,
            List<BatchItemProjectionSpec> specs,
            List<String> evidenceIds) {

        if (response == null) throw new IllegalArgumentException("response required");
        List<BatchItemProjectionSpec> configured = List.copyOf(specs == null ? List.of() : specs);
        if (configured.isEmpty()) throw new IllegalArgumentException("batch projection specs required");
        if (configured.stream().map(BatchItemProjectionSpec::resourceId).distinct().count() != configured.size()) {
            throw new IllegalArgumentException("batch projection resources must be unique");
        }

        Map<String, AuthorizationDecision> observed = parse(response.response().bodyUtf8());
        Set<String> expectedResources = configured.stream()
                .map(BatchItemProjectionSpec::resourceId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!observed.keySet().equals(expectedResources)) {
            throw new IllegalArgumentException("batch response resources differ from explicit projection set");
        }

        List<BatchItemObservation> result = new ArrayList<>();
        configured.stream()
                .sorted(Comparator.comparing(BatchItemProjectionSpec::itemKey))
                .forEach(spec -> result.add(new BatchItemObservation(
                        "s10-batch-item-" + batchId + "-" + spec.itemKey(),
                        sourceObservationId,
                        executionId,
                        testId,
                        batchId,
                        spec.itemKey(),
                        endpoint,
                        spec.resourceId(),
                        spec.action(),
                        observed.get(spec.resourceId()),
                        evidenceIds)));
        return List.copyOf(result);
    }

    private static Map<String, AuthorizationDecision> parse(String body) {
        LinkedHashMap<String, AuthorizationDecision> result = new LinkedHashMap<>();
        Matcher objectMatcher = FLAT_OBJECT.matcher(body == null ? "" : body);
        while (objectMatcher.find()) {
            String object = objectMatcher.group();
            Matcher resource = RESOURCE.matcher(object);
            Matcher decision = DECISION.matcher(object);
            if (!resource.find() || !decision.find()) continue;
            String resourceId = resource.group(1);
            if (result.putIfAbsent(resourceId, AuthorizationDecision.valueOf(decision.group(1))) != null) {
                throw new IllegalArgumentException("duplicate resource in batch response");
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("no explicit batch item decisions in response");
        return result;
    }
}
