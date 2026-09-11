package io.acra.core.engine;

import io.acra.core.domain.authorization.*;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.extraction.*;
import io.acra.core.graph.SecurityContextGraph;
import java.util.List;

public record SecurityContextSnapshot(UriModel uri, IdentityExtraction identity, ExtractionResult<Tenant> tenant,
                                      ExtractionResult<Resource> resource, Action action, Endpoint endpoint,
                                      AuthorizationContext authorizationContext, List<Evidence> evidence,
                                      SecurityContextGraph graph) {
    public SecurityContextSnapshot { evidence=List.copyOf(evidence==null?List.of():evidence); }
}
