package io.acra.core.extraction;
import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.UriModel;
public interface ActionExtractor { Action extract(HttpTransaction transaction, UriModel uri); }
