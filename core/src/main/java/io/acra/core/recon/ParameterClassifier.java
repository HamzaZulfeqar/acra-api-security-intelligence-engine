package io.acra.core.recon;
import io.acra.core.domain.common.*;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.domain.uri.*;
import java.util.*;
public final class ParameterClassifier {
    public List<ParameterIntelligence> classify(HttpTransaction tx,UriModel uri){
        List<ParameterIntelligence> out=new ArrayList<>();
        uri.queryParameters().forEach((k,v)->out.add(classify(k,v,IdentifierLocation.QUERY)));
        parseFormOrJson(tx.request().bodyUtf8(),tx.request().firstHeader("Content-Type").orElse(""),out);
        return List.copyOf(out);
    }
    private static ParameterIntelligence classify(String name,List<String> values,IdentifierLocation location){
        String n=norm(name); ParameterRole role; ConfidenceBasis basis=ConfidenceBasis.HEURISTIC; String reason;
        if(n.matches("(user|user_id|userid|principal|principal_id|subject|sub)")){role=ParameterRole.IDENTITY;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="identity-name";}
        else if(n.matches("(tenant|tenant_id|tenantid|org|org_id|organization|organization_id)")){role=ParameterRole.TENANT;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="tenant-name";}
        else if(n.matches("(owner|owner_id|ownerid|created_by|assigned_to)")){role=ParameterRole.OWNER;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="owner-name";}
        else if(n.matches("(role|role_id|permission|permissions)")){role=ParameterRole.ROLE;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="role-name";}
        else if(n.matches("(workflow|workflow_id|state|status|transition)")){role=ParameterRole.WORKFLOW;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="workflow-name";}
        else if(n.matches("(page|offset|limit|cursor|next|previous|per_page|pagesize|page_size)")){role=ParameterRole.PAGINATION;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="pagination-name";}
        else if(n.matches("(sort|order|orderby|order_by)")){role=ParameterRole.SORTING;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="sorting-name";}
        else if(n.matches("(version|api_version|v)")){role=ParameterRole.VERSION;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="version-name";}
        else if(n.matches("(route|path|redirect|return_url|next_url|url)")){role=ParameterRole.ROUTING;basis=ConfidenceBasis.HEURISTIC;reason="routing-like-name";}
        else if(n.equals("id")||n.endsWith("_id")||n.endsWith("id")){role=ParameterRole.RESOURCE;basis=ConfidenceBasis.EXPLICIT_METADATA;reason="identifier-name";}
        else if(n.matches("(filter|q|query|search|where)")){role=ParameterRole.FILTER;basis=ConfidenceBasis.STRUCTURAL_INFERENCE;reason="filter-name";}
        else {role=ParameterRole.UNKNOWN;reason="unclassified-parameter";}
        return new ParameterIntelligence(name,values,location,role,Confidence.of(basis),reason);
    }
    private static void parseFormOrJson(String body,String contentType,List<ParameterIntelligence> out){
        if(body==null||body.isBlank())return; String ct=contentType.toLowerCase(Locale.ROOT);
        if(ct.contains("application/x-www-form-urlencoded")){for(String p:body.split("&")){int eq=p.indexOf('=');String k=eq<0?p:p.substring(0,eq);String v=eq<0?"":p.substring(eq+1);out.add(classify(k,List.of(v),IdentifierLocation.BODY));}}
        else if(ct.contains("json")||body.stripLeading().startsWith("{")){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(-?[0-9]+(?:\\.[0-9]+)?|true|false|null))").matcher(body);while(m.find()){String v=m.group(2)!=null?m.group(2):m.group(3);out.add(classify(m.group(1),List.of(v==null?"":v),IdentifierLocation.BODY));}}
    }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace('-','_');}
}
