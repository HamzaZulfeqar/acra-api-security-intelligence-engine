package io.acra.core.openapi;
import io.acra.core.domain.http.HttpMethod;
import java.util.List;
public record OpenApiOperation(String path,HttpMethod method,String operationId,List<String> tags,List<OpenApiParameter> parameters,List<String> securitySchemes,List<String> responseCodes) {
    public OpenApiOperation {path=path==null?"":path;if(method==null)throw new IllegalArgumentException("method required");operationId=operationId==null?"":operationId;tags=List.copyOf(tags==null?List.of():tags);parameters=List.copyOf(parameters==null?List.of():parameters);securitySchemes=List.copyOf(securitySchemes==null?List.of():securitySchemes);responseCodes=List.copyOf(responseCodes==null?List.of():responseCodes);}
    public String key(){return method+" "+path;}
}
