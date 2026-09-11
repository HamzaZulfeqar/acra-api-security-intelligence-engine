package io.acra.core.extraction;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
public interface UriExtractor { UriModel extract(HttpTransaction transaction); }
