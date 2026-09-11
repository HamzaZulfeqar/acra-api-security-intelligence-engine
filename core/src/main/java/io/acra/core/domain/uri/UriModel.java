package io.acra.core.domain.uri;

import io.acra.core.domain.common.Validation;
import java.util.*;

public record UriModel(String rawUri, String rawPath, String decodedPath, String normalizedPath,
                       String canonicalPath, String query, List<PathSegment> pathSegments,
                       Map<String,List<String>> queryParameters, Map<Integer,Map<String,String>> matrixParameters,
                       List<String> normalizationMetadata, List<IdentifierCandidate> identifierCandidates) {
    public UriModel {
        rawUri = Validation.requireNonBlank(rawUri, "rawUri");
        rawPath = Validation.requireNonBlank(rawPath, "rawPath");
        decodedPath = decodedPath == null ? rawPath : decodedPath;
        normalizedPath = normalizedPath == null ? decodedPath : normalizedPath;
        canonicalPath = canonicalPath == null ? normalizedPath : canonicalPath;
        query = query == null ? "" : query;
        pathSegments = List.copyOf(pathSegments == null ? List.of() : pathSegments);
        queryParameters = freezeMultiMap(queryParameters);
        matrixParameters = freezeMatrix(matrixParameters);
        normalizationMetadata = List.copyOf(normalizationMetadata == null ? List.of() : normalizationMetadata);
        identifierCandidates = List.copyOf(identifierCandidates == null ? List.of() : identifierCandidates);
    }
    private static Map<String,List<String>> freezeMultiMap(Map<String,List<String>> input) {
        TreeMap<String,List<String>> out = new TreeMap<>();
        if (input != null) input.forEach((k,v) -> out.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(out);
    }
    private static Map<Integer,Map<String,String>> freezeMatrix(Map<Integer,Map<String,String>> input) {
        TreeMap<Integer,Map<String,String>> out = new TreeMap<>();
        if (input != null) input.forEach((k,v) -> out.put(k, Collections.unmodifiableMap(new TreeMap<>(v))));
        return Collections.unmodifiableMap(out);
    }
}
