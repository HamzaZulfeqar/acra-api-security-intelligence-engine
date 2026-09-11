package io.acra.core.active.safety;

import io.acra.core.active.execution.RequestSet;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verifies that the executable mutation is the one declared by the test and
 * that every unrelated request dimension remains equivalent to the baseline.
 */
public final class RequestEquivalenceGuard {
    public ValidationDecision evaluate(SecurityTest test, RequestSet requests) {
        if (test == null || requests == null) {
            return ValidationDecision.invalid("test and built requests are required for equivalence validation");
        }
        HttpRequest declaredBaseline = test.baselineDefinition().request();
        HttpRequest builtBaseline = requests.baseline().request();
        if (!sameRequest(declaredBaseline, builtBaseline)) {
            return ValidationDecision.invalid("built baseline differs from the immutable test baseline");
        }

        Mutation mutation = test.mutation();
        if (!compatible(mutation.type(), mutation.targetLocation())) {
            return ValidationDecision.invalid("mutation type is incompatible with its declared location");
        }

        HttpRequest actual = requests.mutation().request();
        if (!sameAuthorityAndProtocol(builtBaseline, actual)) {
            return ValidationDecision.invalid("mutation changed request authority or protocol");
        }

        HttpRequest expected;
        try {
            expected = expectedMutation(builtBaseline, mutation);
        } catch (RuntimeException invalid) {
            return ValidationDecision.invalid("declared mutation is invalid: " + invalid.getMessage());
        }
        if (!sameRequest(expected, actual)) {
            return ValidationDecision.invalid("mutation request contains undeclared or missing changes");
        }
        return ValidationDecision.allowed();
    }

    private static HttpRequest expectedMutation(HttpRequest baseline, Mutation mutation) {
        HttpMethod method = baseline.method();
        String target = baseline.rawTarget();
        List<HttpHeader> headers = baseline.headers();
        Map<String, String> cookies = baseline.cookies();
        byte[] body = baseline.body();

        switch (mutation.targetLocation()) {
            case PATH, QUERY, URI_REPRESENTATION ->
                    target = replaceExactlyOnce(target, mutation.originalValue(), mutation.mutatedValue());
            case HEADER, IDENTITY, ROLE ->
                    headers = replaceHeaderExactlyOnce(headers, mutation.originalValue(), mutation.mutatedValue());
            case COOKIE -> cookies = replaceMapValueExactlyOnce(cookies, mutation.originalValue(), mutation.mutatedValue());
            case BODY -> body = replaceBody(body, mutation);
            case RESOURCE, TENANT -> {
                if (occurrences(target, mutation.originalValue()) == 1) {
                    target = replaceExactlyOnce(target, mutation.originalValue(), mutation.mutatedValue());
                } else {
                    body = replaceBody(body, mutation);
                }
            }
            case METHOD -> {
                HttpMethod declaredOriginal = HttpMethod.parse(mutation.originalValue());
                if (baseline.method() != declaredOriginal) {
                    throw new IllegalArgumentException("declared original method does not match baseline");
                }
                method = HttpMethod.parse(mutation.mutatedValue());
            }
        }
        return new HttpRequest(method, baseline.scheme(), baseline.host(), baseline.port(), target,
                headers, cookies, body, baseline.protocol(), new byte[0]);
    }

    private static boolean compatible(MutationType type, MutationLocation location) {
        EnumSet<MutationLocation> allowed = switch (type) {
            case IDENTITY_SUBSTITUTION -> EnumSet.of(MutationLocation.IDENTITY, MutationLocation.HEADER, MutationLocation.COOKIE);
            case ROLE_SUBSTITUTION -> EnumSet.of(MutationLocation.ROLE, MutationLocation.HEADER, MutationLocation.COOKIE);
            case TENANT_SUBSTITUTION -> EnumSet.of(MutationLocation.TENANT, MutationLocation.PATH,
                    MutationLocation.QUERY, MutationLocation.HEADER, MutationLocation.COOKIE, MutationLocation.BODY);
            case RESOURCE_SUBSTITUTION -> EnumSet.of(MutationLocation.RESOURCE, MutationLocation.PATH,
                    MutationLocation.QUERY, MutationLocation.HEADER, MutationLocation.COOKIE, MutationLocation.BODY);
            case REPRESENTATION_VARIATION, ENCODING_REPRESENTATION, NORMALIZATION_REPRESENTATION,
                    EQUIVALENT_ROUTE_REPRESENTATION -> EnumSet.of(MutationLocation.URI_REPRESENTATION,
                    MutationLocation.PATH, MutationLocation.QUERY);
            case METHOD_REPRESENTATION -> EnumSet.of(MutationLocation.METHOD);
            case PROPERTY -> EnumSet.of(MutationLocation.BODY, MutationLocation.QUERY, MutationLocation.HEADER);
            case COLLECTION, BATCH -> EnumSet.of(MutationLocation.BODY, MutationLocation.QUERY,
                    MutationLocation.PATH, MutationLocation.RESOURCE);
            case WORKFLOW_TRANSITION -> EnumSet.of(MutationLocation.BODY, MutationLocation.QUERY,
                    MutationLocation.PATH, MutationLocation.RESOURCE, MutationLocation.HEADER);
        };
        return allowed.contains(location);
    }

    private static List<HttpHeader> replaceHeaderExactlyOnce(List<HttpHeader> headers, String original, String replacement) {
        List<HttpHeader> result = new ArrayList<>();
        int matches = 0;
        for (HttpHeader header : headers) {
            if (header.value().equals(original)) {
                matches++;
                result.add(new HttpHeader(header.name(), replacement));
            } else {
                result.add(header);
            }
        }
        if (matches != 1) throw new IllegalArgumentException("header mutation must match exactly one value");
        return List.copyOf(result);
    }

    private static Map<String, String> replaceMapValueExactlyOnce(
            Map<String, String> values, String original, String replacement) {
        TreeMap<String, String> result = new TreeMap<>();
        int matches = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue().equals(original)) {
                matches++;
                result.put(entry.getKey(), replacement);
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        if (matches != 1) throw new IllegalArgumentException("cookie mutation must match exactly one value");
        return result;
    }

    private static byte[] replaceBody(byte[] body, Mutation mutation) {
        String text = new String(body, StandardCharsets.UTF_8);
        return replaceExactlyOnce(text, mutation.originalValue(), mutation.mutatedValue())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String replaceExactlyOnce(String source, String original, String replacement) {
        if (source == null || original == null || original.isEmpty() || replacement == null) {
            throw new IllegalArgumentException("replace values required");
        }
        int first = source.indexOf(original);
        if (first < 0 || source.indexOf(original, first + original.length()) >= 0) {
            throw new IllegalArgumentException("mutation must match exactly one value");
        }
        return source.substring(0, first) + replacement + source.substring(first + original.length());
    }

    private static int occurrences(String source, String value) {
        if (source == null || value == null || value.isEmpty()) return 0;
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }

    private static boolean sameAuthorityAndProtocol(HttpRequest first, HttpRequest second) {
        return first.scheme().equals(second.scheme())
                && first.host().equals(second.host())
                && first.port() == second.port()
                && first.protocol() == second.protocol();
    }

    private static boolean sameRequest(HttpRequest first, HttpRequest second) {
        return sameAuthorityAndProtocol(first, second)
                && first.method() == second.method()
                && first.rawTarget().equals(second.rawTarget())
                && first.headers().equals(second.headers())
                && first.cookies().equals(second.cookies())
                && Arrays.equals(first.body(), second.body());
    }
}
