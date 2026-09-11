package io.acra.core.extraction.defaults;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.identity.*;
import io.acra.core.extraction.*;
import io.acra.core.security.TokenFingerprint;
import java.util.*;

public final class DefaultIdentityExtractor implements IdentityExtractor {
    @Override public IdentityExtraction extract(HttpTransaction tx) {
        Optional<String> auth=tx.request().firstHeader("Authorization");
        if(auth.isPresent()) return fromAuthorization(tx,auth.get());
        Optional<String> apiKey=firstHeader(tx,"X-API-Key","API-Key");
        if(apiKey.isPresent()) return opaqueCredential(tx,apiKey.get(),AuthenticationType.API_KEY,EvidenceSource.HEADER,"api-key-fingerprint");
        Optional<String> cookie=tx.request().firstHeader("Cookie");
        if(cookie.isPresent()&&!cookie.get().isBlank()) return opaqueCredential(tx,cookie.get(),AuthenticationType.SESSION_COOKIE,EvidenceSource.COOKIE,"cookie-session-fingerprint");
        return new IdentityExtraction(ExtractionResult.unknown("no identity evidence"),ExtractionResult.unknown("no role evidence"),"",AuthenticationType.NONE,List.of());
    }
    private IdentityExtraction fromAuthorization(HttpTransaction tx,String header){
        if(header.regionMatches(true,0,"Bearer ",0,7)){
            String token=header.substring(7).trim(); String fp=TokenFingerprint.sha256(token);
            boolean jwt=token.chars().filter(c->c=='.').count()>=2; AuthenticationType type=jwt?AuthenticationType.JWT:AuthenticationType.BEARER;
            List<Evidence> all=new ArrayList<>(); ExtractionResult<Principal> principal; ExtractionResult<Role> role=ExtractionResult.unknown("role not observed");
            if(jwt){
                String sub=SimpleJwtClaims.claim(token,"sub");
                if(sub!=null&&!sub.isBlank()){ Confidence c=Confidence.of(ConfidenceBasis.EXPLICIT_METADATA); Evidence e=Evidence.create(EvidenceSource.JWT_CLAIM,tx.requestId(),"jwt:sub",sub,"jwt-claim",c,tx.timestamp()); all.add(e); principal=ExtractionResult.resolved(new Principal(sub,"",type,c),List.of(e),"JWT sub claim"); }
                else principal=ExtractionResult.unknown("JWT did not expose sub claim");
                String roleValue=firstNonBlank(SimpleJwtClaims.claim(token,"role"),SimpleJwtClaims.claim(token,"roles"));
                if(roleValue!=null){ Confidence c=Confidence.of(ConfidenceBasis.EXPLICIT_METADATA); Evidence e=Evidence.create(EvidenceSource.JWT_CLAIM,tx.requestId(),"jwt:role",roleValue,"jwt-claim",c,tx.timestamp()); all.add(e); role=ExtractionResult.resolved(new Role(roleValue.toLowerCase(Locale.ROOT),roleValue,EvidenceSource.JWT_CLAIM,c),List.of(e),"JWT role claim"); }
            } else principal=ExtractionResult.unknown("opaque bearer token does not identify principal");
            all.add(Evidence.create(EvidenceSource.HEADER,tx.requestId(),"header:Authorization",fp,"sha256-token-fingerprint",Confidence.of(ConfidenceBasis.EXACT_OBSERVED),tx.timestamp()));
            return new IdentityExtraction(principal,role,fp,type,all);
        }
        if(header.regionMatches(true,0,"Basic ",0,6)) return opaqueCredential(tx,header.substring(6).trim(),AuthenticationType.BASIC,EvidenceSource.HEADER,"basic-auth-fingerprint");
        return opaqueCredential(tx,header,AuthenticationType.CUSTOM,EvidenceSource.HEADER,"custom-auth-fingerprint");
    }
    private static IdentityExtraction opaqueCredential(HttpTransaction tx,String raw,AuthenticationType type,EvidenceSource source,String method){
        String fp=TokenFingerprint.sha256(raw); Evidence e=Evidence.create(source,tx.requestId(),source==EvidenceSource.COOKIE?"header:Cookie":"credential",fp,method,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),tx.timestamp());
        return new IdentityExtraction(ExtractionResult.unknown("credential does not identify principal"),ExtractionResult.unknown("no role evidence"),fp,type,List.of(e));
    }
    private static Optional<String> firstHeader(HttpTransaction tx,String... names){ for(String n:names){ Optional<String> v=tx.request().firstHeader(n); if(v.isPresent()&&!v.get().isBlank()) return v; } return Optional.empty(); }
    private static String firstNonBlank(String a,String b){ return a!=null&&!a.isBlank()?a:(b!=null&&!b.isBlank()?b:null); }
}
