package io.acra.core.extraction;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.domain.tenant.Tenant;
public interface TenantExtractor { ExtractionResult<Tenant> extract(HttpTransaction transaction, UriModel uri); }
