package io.acra.core.analysis.semantic;
import io.acra.core.analysis.ResponseNormalizer;
import io.acra.core.domain.http.HttpResponse;
import java.util.*;
import java.util.regex.*;
public final class ResponseSemanticAnalyzer {
    private static final Pattern FIELD=Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:");
    private static final Pattern SCALAR=Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(-?[0-9]+(?:\\.[0-9]+)?))");
    private static final Set<String> VOLATILE=Set.of("timestamp","time","request_id","requestid","trace_id","traceid","nonce","csrf","csrf_token","generated_at","updated_at","created_at","server_timing");
    public ResponseSemanticFingerprint fingerprint(HttpResponse response){String body=response.bodyUtf8();String ct=response.contentType().toLowerCase(Locale.ROOT);TreeSet<String> fields=new TreeSet<>(),res=new TreeSet<>(),owners=new TreeSet<>(),tenants=new TreeSet<>(),volatileFields=new TreeSet<>();Matcher fm=FIELD.matcher(body);while(fm.find()){String k=fm.group(1);fields.add(k);if(VOLATILE.contains(norm(k)))volatileFields.add(k);}Matcher sm=SCALAR.matcher(body);while(sm.find()){String k=norm(sm.group(1));String v=sm.group(2)!=null?sm.group(2):sm.group(3);if(k.equals("owner")||k.equals("owner_id")||k.equals("created_by"))owners.add(v);else if(k.equals("tenant")||k.equals("tenant_id")||k.equals("org_id")||k.equals("organization_id"))tenants.add(v);else if(k.equals("id")||k.endsWith("_id"))res.add(v);}SemanticResponseClass cls=classify(body,ct);var normalized=new ResponseNormalizer().normalize(response);return new ResponseSemanticFingerprint(response.status(),response.contentType(),response.length(),cls,fields,res,owners,tenants,volatileFields,normalized.structuralSignature(),normalized.semanticSignature());}
    private static SemanticResponseClass classify(String body,String ct){if(body==null||body.isBlank())return SemanticResponseClass.EMPTY;if(ct.contains("octet-stream")||ct.contains("image/")||ct.contains("application/pdf"))return SemanticResponseClass.BINARY;String l=body.toLowerCase(Locale.ROOT);if(l.contains("access denied")||l.contains("unauthorized")||l.contains("forbidden")||l.matches("(?s).*\\\"(?:error|success)\\\"\\s*:\\s*(?:\\\"[^\\\"]+\\\"|false).*"))return SemanticResponseClass.ERROR_LIKE;String t=body.stripLeading();if(t.startsWith("["))return SemanticResponseClass.COLLECTION;if(t.startsWith("{"))return SemanticResponseClass.OBJECT;return SemanticResponseClass.TEXTUAL;}
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace('-','_');}
}
