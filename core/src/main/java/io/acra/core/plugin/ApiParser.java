package io.acra.core.plugin;

import io.acra.core.domain.http.HttpTransaction;
import java.util.Map;

public interface ApiParser { String id(); boolean supports(HttpTransaction transaction); Map<String,Object> parse(HttpTransaction transaction); }
