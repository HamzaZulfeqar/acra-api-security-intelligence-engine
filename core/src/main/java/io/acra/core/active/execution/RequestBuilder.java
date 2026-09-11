package io.acra.core.active.execution;

import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RequestBuilder {
    private final UriMutationAdapter uriAdapter = new UriMutationAdapter();

    public RequestSet build(SecurityTest test) {
        if (test == null) throw new IllegalArgumentException("test required");
        BuiltRequest baseline = from(RequestVariantKind.BASELINE, test.baselineDefinition());
        BuiltRequest positive = from(RequestVariantKind.POSITIVE_CONTROL, test.positiveControl());
        BuiltRequest negative = from(RequestVariantKind.NEGATIVE_CONTROL, test.negativeControl());
        HttpRequest mutated = mutate(test.baselineDefinition().request(), test.mutation());
        BuiltRequest mutation = new BuiltRequest(RequestVariantKind.MUTATION,
                test.baselineDefinition().definitionId() + ":" + test.mutation().mutationId(),
                mutated, test.mutation().targetContext(),
                test.targetResource() == null ? test.baselineDefinition().resourceRef()
                        : test.targetResource().resourceType() + ':' + test.targetResource().resourceId());
        return new RequestSet(baseline, positive, negative, mutation);
    }

    private static BuiltRequest from(RequestVariantKind kind, RequestDefinition definition) {
        return new BuiltRequest(kind, definition.definitionId(), definition.request(), definition.contextRef(), definition.resourceRef());
    }

    public HttpRequest mutate(HttpRequest baseline, Mutation mutation) {
        if (baseline == null || mutation == null) throw new IllegalArgumentException("baseline and mutation required");
        HttpMethod method = baseline.method();
        String target = baseline.rawTarget();
        List<HttpHeader> headers = new ArrayList<>(baseline.headers());
        Map<String, String> cookies = new TreeMap<>(baseline.cookies());
        byte[] body = baseline.body();

        switch (mutation.targetLocation()) {
            case PATH, QUERY, URI_REPRESENTATION -> target = uriAdapter.apply(target, mutation);
            case HEADER, IDENTITY, ROLE -> headers = replaceHeaderExactlyOnce(headers, mutation.originalValue(), mutation.mutatedValue());
            case COOKIE -> cookies = replaceMapValueExactlyOnce(cookies, mutation.originalValue(), mutation.mutatedValue());
            case BODY -> body = replaceBody(body, mutation);
            case RESOURCE, TENANT -> {
                if (target.contains(mutation.originalValue())) target = replaceExactlyOnce(target, mutation.originalValue(), mutation.mutatedValue());
                else body = replaceBody(body, mutation);
            }
            case METHOD -> method = HttpMethod.parse(mutation.mutatedValue());
        }
        return new HttpRequest(method, baseline.scheme(), baseline.host(), baseline.port(), target,
                headers, cookies, body, baseline.protocol(), new byte[0]);
    }

    private static List<HttpHeader> replaceHeaderExactlyOnce(List<HttpHeader> headers, String original, String replacement) {
        List<HttpHeader> out = new ArrayList<>();
        int matches = 0;
        for (HttpHeader header : headers) {
            if (header.value().equals(original)) {
                matches++;
                out.add(new HttpHeader(header.name(), replacement));
            } else {
                out.add(header);
            }
        }
        if (matches != 1) throw new IllegalArgumentException("header mutation must match exactly one value");
        return List.copyOf(out);
    }

    private static Map<String, String> replaceMapValueExactlyOnce(Map<String, String> values, String original, String replacement) {
        TreeMap<String, String> out = new TreeMap<>();
        int matches = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue().equals(original)) {
                matches++;
                out.put(entry.getKey(), replacement);
            } else {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        if (matches != 1) throw new IllegalArgumentException("cookie mutation must match exactly one value");
        return out;
    }

    private static byte[] replaceBody(byte[] body, Mutation mutation) {
        String text = new String(body, StandardCharsets.UTF_8);
        return replaceExactlyOnce(text, mutation.originalValue(), mutation.mutatedValue()).getBytes(StandardCharsets.UTF_8);
    }

    static String replaceExactlyOnce(String source, String original, String replacement) {
        if (source == null || original == null || original.isEmpty() || replacement == null) {
            throw new IllegalArgumentException("replace values required");
        }
        int first = source.indexOf(original);
        if (first < 0 || source.indexOf(original, first + original.length()) >= 0) {
            throw new IllegalArgumentException("mutation must match exactly one value");
        }
        return source.substring(0, first) + replacement + source.substring(first + original.length());
    }
}
