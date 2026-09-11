package io.acra.core.plugin;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.graph.SecurityContextGraph;

public record AnalysisContext(HttpTransaction transaction, UriModel uri, AuthorizationContext authorizationContext, SecurityContextGraph graph) {}
