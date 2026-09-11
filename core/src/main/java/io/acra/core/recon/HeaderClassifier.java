package io.acra.core.recon;
import io.acra.core.domain.common.*;
import io.acra.core.domain.http.HttpHeader;
import java.util.*;
public final class HeaderClassifier {
    private static final Set<String> SENSITIVE=Set.of("authorization","cookie","set-cookie","x-api-key","api-key","proxy-authorization");
    public List<HeaderIntelligence> classify(List<HttpHeader> headers){List<HeaderIntelligence> out=new ArrayList<>(); for(HttpHeader h:headers)out.add(classify(h.name())); return List.copyOf(out);}
    public HeaderIntelligence classify(String name){String n=name==null?"":name.toLowerCase(Locale.ROOT); HeaderRole r; ConfidenceBasis b=ConfidenceBasis.EXACT_OBSERVED; String reason;
        if(n.equals("authorization")||n.equals("proxy-authorization")||n.contains("api-key")||n.equals("api-key")){r=HeaderRole.AUTHENTICATION;reason="known-auth-header";}
        else if(n.equals("cookie")||n.equals("set-cookie")){r=HeaderRole.AUTHENTICATION;reason="session-cookie-header";}
        else if(n.matches("x-(user|principal|subject)(-id)?")){r=HeaderRole.IDENTITY;reason="identity-header-name";}
        else if(n.matches("x-(tenant|org|organization)(-id)?")){r=HeaderRole.TENANT;reason="tenant-header-name";}
        else if(n.matches("x-(role|roles|permission|permissions)")){r=HeaderRole.ROLE;reason="role-header-name";}
        else if(Set.of("x-original-url","x-rewrite-url","x-forwarded-uri","x-forwarded-prefix").contains(n)){r=HeaderRole.ROUTING;reason="routing-header";}
        else if(n.startsWith("x-forwarded-")||n.equals("forwarded")){r=HeaderRole.PROXY;reason="proxy-header";}
        else if(Set.of("origin","referer").contains(n)){r=HeaderRole.ORIGIN;reason="origin-header";}
        else if(n.startsWith("x-")&&(n.contains("auth")||n.contains("security")||n.contains("tenant")||n.contains("user"))){r=HeaderRole.CUSTOM_SECURITY;b=ConfidenceBasis.HEURISTIC;reason="custom-security-like-header";}
        else {r=HeaderRole.UNKNOWN;b=ConfidenceBasis.UNKNOWN;reason="unclassified-header";}
        return new HeaderIntelligence(name,r,Confidence.of(b),reason,SENSITIVE.contains(n)||n.contains("token")||n.contains("secret"));}
}
