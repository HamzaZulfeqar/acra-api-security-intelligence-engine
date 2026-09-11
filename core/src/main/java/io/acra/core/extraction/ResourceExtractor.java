package io.acra.core.extraction;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.domain.resource.Resource;
public interface ResourceExtractor { ExtractionResult<Resource> extract(HttpTransaction transaction, UriModel uri, ExtractionResult<io.acra.core.domain.tenant.Tenant> tenant); }
