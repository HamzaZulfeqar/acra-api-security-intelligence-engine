package io.acra.core.recon;
import java.util.Map;
public record PaginationIntelligence(Map<String,String> parameters,boolean cursorBased,boolean offsetBased) {public PaginationIntelligence {parameters=Map.copyOf(parameters==null?Map.of():parameters);}}
