package io.acra.core.route;

public enum RouteSecurityBoundaryState {
    STABLE,
    ROUTING_DIVERGENCE,
    AUTHORIZATION_BOUNDARY_CHANGE,
    COMBINED_DIVERGENCE,
    INCONCLUSIVE
}
