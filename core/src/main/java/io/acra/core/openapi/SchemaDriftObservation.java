package io.acra.core.openapi;
public record SchemaDriftObservation(SchemaDriftType type,String endpointKey,String detail) {public SchemaDriftObservation {if(type==null)type=SchemaDriftType.UNKNOWN;endpointKey=endpointKey==null?"":endpointKey;detail=detail==null?"":detail;}}
