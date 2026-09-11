package io.acra.core.extraction.defaults;

import io.acra.core.domain.common.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.*;
import io.acra.core.extraction.UriExtractor;
import java.net.URI;
import java.util.*;

public final class DefaultUriExtractor implements UriExtractor {
    private final IdentifierDetector identifierDetector = new IdentifierDetector();
    @Override public UriModel extract(HttpTransaction tx) {
        String rawUri = tx.request().rawTarget();
        String rawPath = tx.request().path();
        String decodedPath = PercentCodec.decode(rawPath);
        List<String> metadata = new ArrayList<>();
        String normalizedPath = normalize(decodedPath, metadata);
        List<PathSegment> segments = new ArrayList<>();
        List<IdentifierCandidate> ids = new ArrayList<>();
        Map<Integer,Map<String,String>> matrix = new TreeMap<>();
        String[] rawParts = rawPath.split("/", -1);
        int logicalIndex=0;
        String previousValue="";
        for (String rawPart : rawParts) {
            if (rawPart.isEmpty()) continue;
            String segmentRaw=rawPart;
            Map<String,String> mp=new TreeMap<>();
            int semi=rawPart.indexOf(';');
            if(semi>=0){ segmentRaw=rawPart.substring(0,semi); parseMatrix(rawPart.substring(semi+1),mp); if(!mp.isEmpty()) matrix.put(logicalIndex,mp); }
            String decoded=PercentCodec.decode(segmentRaw);
            IdentifierCandidate candidate=identifierDetector.detect(decoded, IdentifierLocation.PATH, "path["+logicalIndex+"]", null);
            PathSegmentClassification cls;
            Confidence confidence;
            if (isTenantCollection(previousValue)) { cls=PathSegmentClassification.TENANT_CANDIDATE; confidence=Confidence.of(ConfidenceBasis.STRUCTURAL_INFERENCE); }
            else if (isResourceCollection(previousValue)) { cls=PathSegmentClassification.RESOURCE_CANDIDATE; confidence=Confidence.of(ConfidenceBasis.STRUCTURAL_INFERENCE); }
            else if(candidate!=null){ cls=PathSegmentClassification.IDENTIFIER; confidence=candidate.confidence(); }
            else { cls=looksStatic(decoded)?PathSegmentClassification.STATIC:PathSegmentClassification.UNKNOWN; confidence=cls==PathSegmentClassification.STATIC?Confidence.of(ConfidenceBasis.EXACT_OBSERVED):Confidence.unknown(); }
            segments.add(new PathSegment(logicalIndex, segmentRaw, decoded, decoded, cls, confidence));
            if(candidate!=null) ids.add(candidate);
            if(cls==PathSegmentClassification.TENANT_CANDIDATE && candidate==null)
                ids.add(new IdentifierCandidate(decoded,IdentifierLocation.PATH,IdentifierType.NAMED_IDENTIFIER,Confidence.of(ConfidenceBasis.STRUCTURAL_INFERENCE),"tenant-collection-position","path["+logicalIndex+"]"));
            previousValue = decoded;
            logicalIndex++;
        }
        Map<String,List<String>> queryParams=parseQuery(tx.request().query(), ids);
        String canonical=canonicalize(segments);
        return new UriModel(rawUri,rawPath,decodedPath,normalizedPath,canonical,tx.request().query(),segments,queryParams,matrix,metadata,ids);
    }
    private Map<String,List<String>> parseQuery(String rawQuery,List<IdentifierCandidate> ids){
        TreeMap<String,List<String>> out=new TreeMap<>(); if(rawQuery==null||rawQuery.isBlank()) return out;
        for(String part:rawQuery.split("&")){ int eq=part.indexOf('='); String k=PercentCodec.decode(eq>=0?part.substring(0,eq):part); String v=PercentCodec.decode(eq>=0?part.substring(eq+1):""); out.computeIfAbsent(k,x->new ArrayList<>()).add(v); IdentifierCandidate c=identifierDetector.detect(v,IdentifierLocation.QUERY,"query:"+k,k); if(c!=null) ids.add(c); }
        return out;
    }
    private static void parseMatrix(String raw,Map<String,String> out){ for(String p:raw.split(";")){int eq=p.indexOf('='); if(eq>0) out.put(PercentCodec.decode(p.substring(0,eq)),PercentCodec.decode(p.substring(eq+1)));} }
    private static String normalize(String path,List<String> metadata){
        String p=path; if(p.contains("//")){ while(p.contains("//"))p=p.replace("//","/"); metadata.add("duplicate-separators-collapsed"); }
        try { String n=URI.create(p).normalize().getPath(); if(!Objects.equals(n,p)){metadata.add("dot-segments-normalized"); p=n;} } catch(Exception e){ metadata.add("malformed-uri-normalization-skipped"); }
        return p;
    }
    private static String canonicalize(List<PathSegment> segs){
        StringBuilder b=new StringBuilder();
        for(int i=0;i<segs.size();i++){ PathSegment s=segs.get(i); b.append('/');
            if(s.classification()==PathSegmentClassification.TENANT_CANDIDATE) b.append("{tenant}");
            else if(s.classification()==PathSegmentClassification.RESOURCE_CANDIDATE||s.classification()==PathSegmentClassification.IDENTIFIER){ String prev=i>0?segs.get(i-1).decodedValue():"resource"; b.append('{').append(singular(prev)).append("_id}"); }
            else b.append(s.normalizedValue()); }
        return b.length()==0?"/":b.toString();
    }
    static String singular(String value){ if(value==null||value.isBlank()) return "resource"; String v=value.toLowerCase(Locale.ROOT); if(v.endsWith("ies")&&v.length()>3)return v.substring(0,v.length()-3)+"y"; if(v.endsWith("s")&&v.length()>1)return v.substring(0,v.length()-1); return v; }
    private static boolean looksStatic(String s){ return s.matches("[A-Za-z][A-Za-z0-9._-]*"); }
    private static boolean isTenantCollection(String s){ return s.equalsIgnoreCase("tenants")||s.equalsIgnoreCase("organizations")||s.equalsIgnoreCase("orgs"); }
    private static boolean isResourceCollection(String s){ return !s.isBlank() && (s.endsWith("s")||Set.of("user","document","resource","account","order","invoice").contains(s.toLowerCase(Locale.ROOT))) && !isTenantCollection(s); }
}
