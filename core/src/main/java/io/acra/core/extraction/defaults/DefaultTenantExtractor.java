package io.acra.core.extraction.defaults;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.uri.*;
import io.acra.core.extraction.*;
import java.util.*;
import java.util.regex.*;

public final class DefaultTenantExtractor implements TenantExtractor {
    private static final Set<String> TENANT_KEYS=Set.of("tenant","tenant_id","tenantid","organization","organization_id","org","org_id");
    @Override public ExtractionResult<Tenant> extract(HttpTransaction tx, UriModel uri) {
        LinkedHashMap<String,Tenant> tenants=new LinkedHashMap<>(); List<Evidence> ev=new ArrayList<>();
        for(PathSegment s:uri.pathSegments()) if(s.classification()==PathSegmentClassification.TENANT_CANDIDATE) add(tenants,ev,s.decodedValue(),EvidenceSource.PATH,ConfidenceBasis.STRUCTURAL_INFERENCE,tx,"path["+s.index()+"]","tenant-collection-position");
        uri.queryParameters().forEach((k,vs)->{if(TENANT_KEYS.contains(norm(k))) for(String v:vs) add(tenants,ev,v,EvidenceSource.QUERY,ConfidenceBasis.EXPLICIT_METADATA,tx,"query:"+k,"tenant-query-parameter");});
        for(String name:List.of("X-Tenant-Id","X-Tenant-ID","X-Organization-Id","X-Org-Id")) tx.request().firstHeader(name).ifPresent(v->add(tenants,ev,v,EvidenceSource.HEADER,ConfidenceBasis.EXPLICIT_METADATA,tx,"header:"+name,"explicit-tenant-header"));
        tx.request().firstHeader("Authorization").ifPresent(h->{ if(h.regionMatches(true,0,"Bearer ",0,7)){ String token=h.substring(7).trim(); String v=SimpleJwtClaims.claim(token,"tenant_id"); if(v==null) v=SimpleJwtClaims.claim(token,"tenant"); if(v!=null&&!v.isBlank()) add(tenants,ev,v,EvidenceSource.JWT_CLAIM,ConfidenceBasis.EXPLICIT_METADATA,tx,"jwt:tenant","jwt-claim"); } });
        extractJson(tx.request().bodyUtf8()).forEach((k,v)->{if(TENANT_KEYS.contains(norm(k))) add(tenants,ev,v,EvidenceSource.BODY,ConfidenceBasis.EXPLICIT_METADATA,tx,"body:"+k,"tenant-body-field");});
        if(tx.response()!=null) extractJson(tx.response().bodyUtf8()).forEach((k,v)->{if(TENANT_KEYS.contains(norm(k))) add(tenants,ev,v,EvidenceSource.RESPONSE,ConfidenceBasis.HEURISTIC,tx,"response:"+k,"tenant-response-field");});
        if(tenants.isEmpty()) return ExtractionResult.unknown("no tenant evidence");
        if(tenants.size()>1) return ExtractionResult.conflict(new ArrayList<>(tenants.values()),ev,"conflicting tenant evidence");
        return ExtractionResult.resolved(tenants.values().iterator().next(),ev,"tenant evidence resolved");
    }
    private static void add(Map<String,Tenant> tenants,List<Evidence> ev,String value,EvidenceSource source,ConfidenceBasis basis,HttpTransaction tx,String location,String method){ if(value==null||value.isBlank()) return; Confidence c=Confidence.of(basis); tenants.putIfAbsent(value,new Tenant(value,value,source,c)); ev.add(Evidence.create(source,tx.requestId(),location,value,method,c,tx.timestamp())); }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace('-','_');}
    private static Map<String,String> extractJson(String body){
        if(body==null||body.isBlank()) return Map.of(); LinkedHashMap<String,String> out=new LinkedHashMap<>();
        Matcher m=Pattern.compile("\\\"([A-Za-z0-9_-]+)\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9]+))").matcher(body);
        while(m.find()) out.put(m.group(1),m.group(2)!=null?m.group(2):m.group(3)); return out;
    }
}
