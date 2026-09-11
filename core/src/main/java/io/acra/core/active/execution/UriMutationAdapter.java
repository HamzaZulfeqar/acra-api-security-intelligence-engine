package io.acra.core.active.execution;

import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.route.RouteEquivalenceEngine;
import io.acra.core.route.RouteEquivalenceKind;

public final class UriMutationAdapter {
    private final RouteEquivalenceEngine equivalence = new RouteEquivalenceEngine();

    public String apply(String rawTarget, Mutation mutation) {
        if (rawTarget == null || mutation == null) throw new IllegalArgumentException("raw target and mutation required");
        if (mutation.targetLocation() != MutationLocation.URI_REPRESENTATION
                && mutation.targetLocation() != MutationLocation.PATH
                && mutation.targetLocation() != MutationLocation.QUERY) {
            throw new IllegalArgumentException("not a URI mutation");
        }
        String updated = RequestBuilder.replaceExactlyOnce(rawTarget, mutation.originalValue(), mutation.mutatedValue());
        if (!updated.startsWith("/") || updated.contains("://") || updated.indexOf('#') >= 0) {
            throw new IllegalArgumentException("URI mutation changed authority or introduced a fragment");
        }
        if (mutation.type() == MutationType.EQUIVALENT_ROUTE_REPRESENTATION) {
            String beforePath = path(rawTarget);
            String afterPath = path(updated);
            RouteEquivalenceKind kind = equivalence.compare(beforePath, afterPath).kind();
            if (kind != RouteEquivalenceKind.SYNTACTICALLY_EQUAL
                    && kind != RouteEquivalenceKind.CANONICALLY_EQUIVALENT
                    && kind != RouteEquivalenceKind.SAME_FAMILY) {
                throw new IllegalArgumentException("route representation is not equivalent");
            }
        }
        return updated;
    }

    private static String path(String target) {
        int query = target.indexOf('?');
        return query < 0 ? target : target.substring(0, query);
    }
}
