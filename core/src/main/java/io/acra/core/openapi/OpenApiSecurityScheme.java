package io.acra.core.openapi;
public record OpenApiSecurityScheme(String name,String type,String scheme,String in,String bearerFormat) {
    public OpenApiSecurityScheme {name=s(name);type=s(type);scheme=s(scheme);in=s(in);bearerFormat=s(bearerFormat);}private static String s(String v){return v==null?"":v;}
}
