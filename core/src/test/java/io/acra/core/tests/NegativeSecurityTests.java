package io.acra.core.tests;

import io.acra.core.domain.http.*;
import io.acra.core.engine.SecurityContextEngine;
import io.acra.core.extraction.ResolutionStatus;
import io.acra.core.serialization.DomainSerializer;
import java.util.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import io.acra.core.domain.common.DomainLimits;

public final class NegativeSecurityTests {
    public static int run(){int n=0; SecurityContextEngine e=new SecurityContextEngine();
        String malformed=Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes(StandardCharsets.UTF_8))+"."+Base64.getUrlEncoder().withoutPadding().encodeToString("{bad".getBytes(StandardCharsets.UTF_8))+".x";
        var s=e.analyze(Fixtures.tx("/api/v1/documents/1",List.of(new HttpHeader("Authorization","Bearer "+malformed)))); TestSupport.assertEquals(ResolutionStatus.UNKNOWN,s.identity().principal().status(),"malformed JWT must not invent identity"); n++;
        var unknown=e.analyze(Fixtures.tx("/lookup?foo=bar")); TestSupport.assertEquals(ResolutionStatus.UNKNOWN,unknown.resource().status(),"unknown query must not invent resource"); n++;
        String injected="safe\r\nX-Forged: yes"; HttpRequest req=HttpRequest.of(HttpMethod.GET,"https","api.example.test",443,"/",List.of(new HttpHeader("X-Input",injected)),new byte[0],HttpProtocol.HTTP_1_1); String json=new DomainSerializer().serialize(req); TestSupport.assertContains(json,"\\r\\n","log/serialization control chars escaped"); n++;
        HttpRequest malformedJson=HttpRequest.of(HttpMethod.POST,"https","api.example.test",443,"/documents",List.of(new HttpHeader("Content-Type","application/json")),"{bad".getBytes(StandardCharsets.UTF_8),HttpProtocol.HTTP_1_1); String malformedOut=new DomainSerializer().serialize(malformedJson); TestSupport.assertContains(malformedOut,"bodySha256","malformed JSON is opaque/safe in Sprint 1"); n++;
        TestSupport.assertThrows(RuntimeException.class,()->HttpRequest.of(HttpMethod.POST,"https","api.example.test",443,"/upload",List.of(),new byte[DomainLimits.MAX_BODY_BYTES+1],HttpProtocol.HTTP_1_1),"oversized body rejected"); n++;
        return n;}
}
