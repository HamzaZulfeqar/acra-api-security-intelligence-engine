package io.acra.core.recon;
import io.acra.core.domain.common.*;
import io.acra.core.domain.uri.*;
import java.util.*;
public final class SemanticIdentifierClassifier {
    public List<SemanticIdentifier> classify(UriModel uri){
        List<SemanticIdentifier> out=new ArrayList<>();
        for(IdentifierCandidate c:uri.identifierCandidates()) out.add(classifyOne(c,uri));
        return List.copyOf(out);
    }
    public SemanticIdentifier classifyOne(IdentifierCandidate c,UriModel uri){
        String source=c.source().toLowerCase(Locale.ROOT); String hint="";
        if(source.startsWith("path[")){
            try{int idx=Integer.parseInt(source.substring(5,source.length()-1)); if(idx>0&&idx-1<uri.pathSegments().size())hint=uri.pathSegments().get(idx-1).decodedValue().toLowerCase(Locale.ROOT);}catch(Exception ignored){}
        } else if(source.startsWith("query:")) hint=source.substring(6);
        SemanticIdentifierType t=SemanticIdentifierType.UNKNOWN; String resourceType=""; ConfidenceBasis basis=ConfidenceBasis.HEURISTIC; String reason="insufficient-semantic-evidence";
        if(matches(hint,"tenant","tenants")){t=SemanticIdentifierType.TENANT_ID;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="tenant-collection-or-name";}
        else if(matches(hint,"org","orgs","organization","organizations")){t=SemanticIdentifierType.ORGANIZATION_ID;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="organization-collection-or-name";}
        else if(matches(hint,"user","users","member","members","principal","principals")){t=SemanticIdentifierType.USER_ID;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="identity-collection-or-name";}
        else if(matches(hint,"role","roles")){t=SemanticIdentifierType.ROLE_ID;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="role-collection-or-name";}
        else if(matches(hint,"workflow","workflows","state","states")){t=SemanticIdentifierType.WORKFLOW_ID;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="workflow-collection-or-name";}
        else if(hint.endsWith("_id")||hint.equals("id")){t=SemanticIdentifierType.RESOURCE_ID;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="identifier-name";}
        else if(!hint.isBlank()){t=SemanticIdentifierType.RESOURCE_ID;resourceType=singular(hint);basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="resource-collection-position";}
        return new SemanticIdentifier(c,t,resourceType,Confidence.of(basis),reason,c.source());
    }
    private static boolean matches(String v,String... names){for(String n:names)if(v.equals(n)||v.equals(n+"_id"))return true;return false;}
    private static String singular(String v){if(v.endsWith("ies")&&v.length()>3)return v.substring(0,v.length()-3)+"y"; if(v.endsWith("s")&&v.length()>1)return v.substring(0,v.length()-1); return v;}
}
