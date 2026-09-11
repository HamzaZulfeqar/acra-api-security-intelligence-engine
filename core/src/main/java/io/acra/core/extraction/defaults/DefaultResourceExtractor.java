package io.acra.core.extraction.defaults;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.uri.*;
import io.acra.core.extraction.*;
import java.util.*;
import java.util.regex.*;

public final class DefaultResourceExtractor implements ResourceExtractor {
    private static final Set<String> NON_RESOURCE_IDS=Set.of("tenant_id","tenantid","user_id","userid","owner_id","ownerid","role_id","roleid","org_id","organization_id");
    @Override public ExtractionResult<Resource> extract(HttpTransaction tx, UriModel uri, ExtractionResult<Tenant> tenantResult) {
        List<PathSegment> segs=uri.pathSegments();
        for(int i=segs.size()-1;i>=0;i--){ PathSegment s=segs.get(i); if(s.classification()==PathSegmentClassification.RESOURCE_CANDIDATE||s.classification()==PathSegmentClassification.IDENTIFIER){
            String type=i>0?DefaultUriExtractor.singular(segs.get(i-1).decodedValue()):"resource"; if(type.equals("tenant")||type.equals("organization")||type.equals("org")) continue;
            return resolved(tx,tenantResult,s.decodedValue(),type,EvidenceSource.PATH,ConfidenceBasis.STRUCTURAL_INFERENCE,"path["+s.index()+"]","resource-collection-position");
        }}
        for(var e:uri.queryParameters().entrySet()) if(isResourceKey(e.getKey())&&!e.getValue().isEmpty()) return resolved(tx,tenantResult,e.getValue().getFirst(),resourceType(e.getKey()),EvidenceSource.QUERY,ConfidenceBasis.EXPLICIT_METADATA,"query:"+e.getKey(),"resource-query-parameter");
        Map<String,String> body=extractJson(tx.request().bodyUtf8()); for(var e:body.entrySet()) if(isResourceKey(e.getKey())) return resolved(tx,tenantResult,e.getValue(),resourceType(e.getKey()),EvidenceSource.BODY,ConfidenceBasis.EXPLICIT_METADATA,"body:"+e.getKey(),"resource-body-field");
        if(tx.response()!=null){ Map<String,String> response=extractJson(tx.response().bodyUtf8()); for(var e:response.entrySet()) if(isResourceKey(e.getKey())) return resolved(tx,tenantResult,e.getValue(),resourceType(e.getKey()),EvidenceSource.RESPONSE,ConfidenceBasis.HEURISTIC,"response:"+e.getKey(),"resource-response-field"); }
        return ExtractionResult.unknown("no resource candidate");
    }
    private static ExtractionResult<Resource> resolved(HttpTransaction tx,ExtractionResult<Tenant> tenantResult,String id,String type,EvidenceSource source,ConfidenceBasis basis,String location,String method){
        Confidence c=Confidence.of(basis); String tenantId=tenantResult.resolved().map(Tenant::tenantId).orElse(null);
        Map<String,String> requestFields=extractJson(tx.request().bodyUtf8()); Map<String,String> responseFields=tx.response()==null?Map.of():extractJson(tx.response().bodyUtf8());
        String owner=firstNonBlank(requestFields,"owner_id","ownerId","owner_principal_id"); EvidenceSource ownerSource=EvidenceSource.BODY; String ownerLocation="body:owner_id";
        if(owner==null){owner=firstNonBlank(responseFields,"owner_id","ownerId","owner_principal_id"); ownerSource=EvidenceSource.RESPONSE; ownerLocation="response:owner_id";}
        String state=firstNonBlank(requestFields,"state","workflow_state"); if(state==null) state=firstNonBlank(responseFields,"state","workflow_state");
        Resource r=new Resource(id,type,null,owner,tenantId,state,c); List<Evidence> evidence=new ArrayList<>(); evidence.add(Evidence.create(source,tx.requestId(),location,id,method,c,tx.timestamp()));
        if(owner!=null){Confidence ownerConfidence=Confidence.of(ConfidenceBasis.EXPLICIT_METADATA); evidence.add(Evidence.create(ownerSource,tx.requestId(),ownerLocation,owner,"resource-owner-metadata",ownerConfidence,tx.timestamp()));}
        return ExtractionResult.resolved(r,evidence,"resource candidate from "+source.name().toLowerCase(Locale.ROOT));
    }
    private static String firstNonBlank(Map<String,String> fields,String... keys){ for(String key:keys){String value=fields.get(key); if(value!=null&&!value.isBlank()) return value;} return null; }
    private static boolean isResourceKey(String key){ String k=norm(key); if(NON_RESOURCE_IDS.contains(k)) return false; return k.equals("id")||k.endsWith("_id")||k.endsWith("id"); }
    private static String resourceType(String key){ String k=norm(key); if(k.equals("id")) return "resource"; if(k.endsWith("_id")) k=k.substring(0,k.length()-3); else if(k.endsWith("id")) k=k.substring(0,k.length()-2); return k.isBlank()?"resource":k; }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace('-','_');}
    private static Map<String,String> extractJson(String body){ if(body==null||body.isBlank()) return Map.of(); LinkedHashMap<String,String> out=new LinkedHashMap<>(); Matcher m=Pattern.compile("\\\"([A-Za-z0-9_-]+)\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|([0-9]+))").matcher(body); while(m.find()) out.put(m.group(1),m.group(2)!=null?m.group(2):m.group(3)); return out; }
}
