package io.acra.core.openapi;
public record OpenApiParameter(String name,String location,boolean required,String schemaType) {
    public OpenApiParameter {name=name==null?"":name;location=location==null?"":location;schemaType=schemaType==null?"":schemaType;}
}
