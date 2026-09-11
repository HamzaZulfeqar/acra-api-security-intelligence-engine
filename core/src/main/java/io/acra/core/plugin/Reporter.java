package io.acra.core.plugin;

import java.util.Map;

public interface Reporter { String id(); String render(Map<String,Object> reportModel); }
