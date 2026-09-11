package io.acra.core.recon;
import java.time.Instant;
public record ManualContextMapping(String key,String value,ParameterRole role,String source,Instant createdAt) {public ManualContextMapping {key=key==null?"":key;value=value==null?"":value;if(role==null)role=ParameterRole.UNKNOWN;source=source==null?"manual":source;if(createdAt==null)createdAt=Instant.now();}}
