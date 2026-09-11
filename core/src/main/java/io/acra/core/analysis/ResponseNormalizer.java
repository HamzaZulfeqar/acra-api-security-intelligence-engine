package io.acra.core.analysis;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.security.UniversalRedactor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public final class ResponseNormalizer {
    private static final Set<String> VOLATILE_HEADERS=Set.of("date","server-timing","x-request-id","x-correlation-id","traceparent","tracestate","x-trace-id");
    private static final Pattern DYNAMIC_JSON=Pattern.compile("(?i)(\\\"(?:timestamp|time|request_?id|trace_?id|nonce|csrf(?:_token)?|generated_?at|updated_?at|created_?at)\\\"\\s*:\\s*)(\\\"[^\\\"]*\\\"|-?[0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern JSON_KEY=Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:");
    private final UniversalRedactor redactor=new UniversalRedactor();
    public NormalizedResponse normalize(HttpResponse response){
        TreeMap<String,String> stable=new TreeMap<>();
        for(HttpHeader h:response.headers()) if(!VOLATILE_HEADERS.contains(h.name().toLowerCase(Locale.ROOT))) stable.put(h.name().toLowerCase(Locale.ROOT),redactor.redactHeader(h.name(),h.value()));
        String body=redactor.redactText(response.bodyUtf8());
        body=DYNAMIC_JSON.matcher(body).replaceAll("$1\"<dynamic>\"");
        return new NormalizedResponse(response.status(),response.contentType(),stable,body,structure(body),semantic(body),response.body().length);
    }
    private static String structure(String body){
        TreeSet<String> keys=new TreeSet<>(); Matcher m=JSON_KEY.matcher(body); while(m.find()) keys.add(m.group(1));
        if(!keys.isEmpty()) return "json-keys:"+String.join(",",keys);
        String collapsed=body.replaceAll("[0-9]+","#").replaceAll("\\s+"," ").trim();
        return collapsed.length()>256?collapsed.substring(0,256):collapsed;
    }
    private static String semantic(String body){
        String lower=body.toLowerCase(Locale.ROOT);
        if(lower.isBlank()) return "EMPTY";
        if(lower.contains("error")||lower.contains("denied")||lower.contains("forbidden")||lower.contains("unauthorized")) return "ERROR_LIKE";
        if(lower.startsWith("{")||lower.startsWith("[")) return "STRUCTURED_DATA";
        return "TEXTUAL_DATA";
    }
}
