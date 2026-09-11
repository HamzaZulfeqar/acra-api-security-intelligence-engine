package io.acra.core.analysis;
import java.util.Map;
public record NormalizedResponse(int status, String contentType, Map<String,String> stableHeaders, String body,
                                 String structuralSignature, String semanticSignature, int originalLength) {
    public NormalizedResponse {
        contentType=contentType==null?"":contentType;
        stableHeaders=Map.copyOf(stableHeaders==null?Map.of():stableHeaders);
        body=body==null?"":body; structuralSignature=structuralSignature==null?"":structuralSignature;
        semanticSignature=semanticSignature==null?"":semanticSignature;
        if(originalLength<0) throw new IllegalArgumentException("originalLength must be >=0");
    }
}
