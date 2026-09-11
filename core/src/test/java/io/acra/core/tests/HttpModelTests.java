package io.acra.core.tests;

import io.acra.core.domain.common.*;
import io.acra.core.domain.http.*;
import java.util.*;

public final class HttpModelTests {
    public static int run(){int n=0;
        byte[] body={1,2,3}; HttpRequest r=new HttpRequest(HttpMethod.POST,"HTTPS","example.test",443,"/x?a=1",List.of(),Map.of(),body,HttpProtocol.HTTP_1_1,new byte[]{9}); body[0]=99; TestSupport.assertEquals(1,(int)r.body()[0],"request body must be defensively copied"); n++;
        TestSupport.assertEquals("/x",r.path(),"path derivation"); TestSupport.assertEquals("a=1",r.query(),"query derivation"); n++;
        TestSupport.assertThrows(DomainValidationException.class,()->new HttpRequest(HttpMethod.GET,"https","",443,"/",List.of(),Map.of(),new byte[0],HttpProtocol.HTTP_1_1,new byte[0]),"missing host"); n++;
        TestSupport.assertThrows(DomainValidationException.class,()->HttpMethod.parse("BREW"),"invalid method"); n++;
        String huge="A".repeat(DomainLimits.MAX_HEADER_VALUE_CHARS+1); TestSupport.assertThrows(DomainValidationException.class,()->HttpRequest.of(HttpMethod.GET,"https","x",443,"/",List.of(new HttpHeader("X-Huge",huge)),new byte[0],HttpProtocol.HTTP_1_1),"oversized header"); n++;
        return n;}
}
