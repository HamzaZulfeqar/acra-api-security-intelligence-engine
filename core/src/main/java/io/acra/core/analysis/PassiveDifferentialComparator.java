package io.acra.core.analysis;
import io.acra.core.domain.http.HttpResponse;
import java.util.*;
public final class PassiveDifferentialComparator {
    private final ResponseNormalizer normalizer=new ResponseNormalizer();
    public ResponseDifference compare(HttpResponse a,HttpResponse b,ResponseComparisonMode mode){
        if(a==null||b==null) throw new IllegalArgumentException("responses required");
        List<String> changes=new ArrayList<>();
        switch(mode){
            case RAW -> { if(a.status()!=b.status()) changes.add("status"); if(!a.headers().equals(b.headers())) changes.add("headers"); if(!Arrays.equals(a.body(),b.body())) changes.add("body"); }
            case NORMALIZED -> compareNormalized(normalizer.normalize(a),normalizer.normalize(b),changes,false,false);
            case STRUCTURAL -> compareNormalized(normalizer.normalize(a),normalizer.normalize(b),changes,true,false);
            case SEMANTIC -> compareNormalized(normalizer.normalize(a),normalizer.normalize(b),changes,false,true);
        }
        return new ResponseDifference(mode,changes.isEmpty(),changes);
    }
    private static void compareNormalized(NormalizedResponse a,NormalizedResponse b,List<String> changes,boolean structural,boolean semantic){
        if(a.status()!=b.status()) changes.add("status");
        if(!a.contentType().equalsIgnoreCase(b.contentType())) changes.add("contentType");
        if(structural){ if(!a.structuralSignature().equals(b.structuralSignature())) changes.add("structure"); return; }
        if(semantic){ if(!a.semanticSignature().equals(b.semanticSignature())) changes.add("semanticClass"); if(!a.structuralSignature().equals(b.structuralSignature())) changes.add("structure"); return; }
        if(!a.stableHeaders().equals(b.stableHeaders())) changes.add("headers"); if(!a.body().equals(b.body())) changes.add("body");
    }
}
