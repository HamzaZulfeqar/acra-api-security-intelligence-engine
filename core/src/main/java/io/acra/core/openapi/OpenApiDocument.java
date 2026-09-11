package io.acra.core.openapi;
import java.util.*;
public record OpenApiDocument(OpenApiSpecKind kind,String version,String title,List<OpenApiOperation> operations,Map<String,OpenApiSecurityScheme> securitySchemes,String source) {
    public OpenApiDocument {if(kind==null)kind=OpenApiSpecKind.UNKNOWN;version=s(version);title=s(title);operations=List.copyOf(operations==null?List.of():operations);securitySchemes=Map.copyOf(securitySchemes==null?Map.of():securitySchemes);source=s(source);}private static String s(String v){return v==null?"":v;}
}
