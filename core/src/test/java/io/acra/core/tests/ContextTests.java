package io.acra.core.tests;

import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.engine.*;
import io.acra.core.extraction.ResolutionStatus;
import java.util.*;

public final class ContextTests {
    public static int run(){int n=0; SecurityContextEngine e=new SecurityContextEngine();
        var s=e.analyze(Fixtures.tx("/api/v1/tenants/acme/documents/9821")); TestSupport.assertEquals("acme",s.tenant().resolved().orElseThrow().tenantId(),"tenant candidate"); TestSupport.assertEquals("9821",s.resource().resolved().orElseThrow().resourceId(),"resource id"); TestSupport.assertEquals("document",s.resource().resolved().orElseThrow().resourceType(),"resource type"); n++;
        String jwt=Fixtures.jwt(Map.of("sub","user-42")); var id=e.analyze(Fixtures.tx("/api/v1/documents/1",List.of(new HttpHeader("Authorization","Bearer "+jwt)))); TestSupport.assertEquals("user-42",id.identity().principal().resolved().orElseThrow().principalId(),"JWT sub principal"); TestSupport.assertFalse(id.identity().principal().evidence().isEmpty(),"principal provenance"); n++;
        var conflict=e.analyze(Fixtures.tx("/api/v1/tenants/A/documents/1",List.of(new HttpHeader("X-Tenant-Id","B")))); TestSupport.assertEquals(ResolutionStatus.CONFLICTING_EVIDENCE,conflict.tenant().status(),"tenant conflicts must remain explicit"); TestSupport.assertEquals("CONFLICTING_EVIDENCE",conflict.graph().nodes().stream().filter(x->x.type().name().equals("CONFLICT")).findFirst().orElseThrow().label(),"conflict node"); n++;
        TestSupport.assertEquals(ActionType.READ,s.action().actionType(),"GET action map"); n++;
        var approve=e.analyze(Fixtures.tx("/api/v1/documents/1/approve")); TestSupport.assertEquals(ActionType.APPROVE,approve.action().actionType(),"application action override"); n++;
        return n;}
}
