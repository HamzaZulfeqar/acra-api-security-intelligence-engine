package io.acra.core.extraction;
import io.acra.core.domain.http.HttpTransaction;
public interface IdentityExtractor { IdentityExtraction extract(HttpTransaction transaction); }
