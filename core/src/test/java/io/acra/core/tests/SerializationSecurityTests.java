package io.acra.core.tests;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.http.*;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.graph.*;
import io.acra.core.security.UniversalRedactor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class SerializationSecurityTests {
    public static int run(){int n=0; DomainSerializer s=new DomainSerializer(); String secret=Fixtures.runtimeSecret();
        HttpRequest req=new HttpRequest(HttpMethod.POST,"https","api.example.test",443,"/login",List.of(new HttpHeader("Authorization","Bearer "+secret),new HttpHeader("Cookie","sid="+secret),new HttpHeader("X-Test","safe")),Map.of("sid",secret),("{\"password\":\""+secret+"\"}").getBytes(StandardCharsets.UTF_8),HttpProtocol.HTTP_1_1,("Authorization: Bearer "+secret).getBytes(StandardCharsets.UTF_8));
        String json=s.serialize(req); TestSupport.assertNotContains(json,secret,"raw credential must not persist"); TestSupport.assertContains(json,"<redacted>","redaction marker"); TestSupport.assertContains(json,"sha256","raw bytes represented by fingerprint"); n++;
        String json2=s.serialize(req); TestSupport.assertEquals(json,json2,"deterministic serialization"); n++;
        String embedded="{\"rationale\":\"Authorization: Bearer "+secret+"; review-only\",\"reportVersion\":\"s8-routing-report-v1\",\"summary\":{\"confirmedFindingCount\":0}}";
        String embeddedRedacted=new UniversalRedactor().redactText(embedded);
        TestSupport.assertNotContains(embeddedRedacted,secret,"embedded authorization secret must be redacted");
        TestSupport.assertContains(embeddedRedacted,"s8-routing-report-v1","later JSON fields must survive embedded authorization redaction");
        TestSupport.assertContains(embeddedRedacted,"confirmedFindingCount","redaction must not truncate subsequent JSON fields"); n++;
        Evidence ev=Evidence.create(EvidenceSource.BODY,"req-1","body","line1\n\"quoted\"","fixture",Confidence.of(ConfidenceBasis.EXACT_OBSERVED),Instant.parse("2026-08-30T12:00:00Z")); String ej=s.serialize(ev); TestSupport.assertContains(ej,"\\n","newline escaped"); TestSupport.assertContains(ej,"\\\"quoted\\\"","quotes escaped"); n++;
        TestSupport.assertThrows(DomainValidationException.class,()->new Confidence(1.1,ConfidenceBasis.HEURISTIC),"invalid confidence"); n++;
        SecurityContextGraph g=new SecurityContextGraph(); Evidence ge=Evidence.create(EvidenceSource.PATH,"req-g","path","1","fixture",Confidence.of(ConfidenceBasis.EXACT_OBSERVED),Instant.parse("2026-08-30T12:00:00Z")); g.addEvidence(ge); GraphNode a=new GraphNode("a",NodeType.PRINCIPAL,"a",Map.of()); GraphNode b=new GraphNode("b",NodeType.RESOURCE,"b",Map.of()); g.addNode(a);g.addNode(b);g.addEdge(GraphEdge.create("a","b",RelationType.ACCESSES,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),List.of(ge.evidenceId()),Instant.parse("2026-08-30T12:00:00Z"))); String gj1=s.serialize(g), gj2=s.serialize(g); TestSupport.assertEquals(gj1,gj2,"graph serialization deterministic"); TestSupport.assertContains(gj1,"ACCESS", "graph relation serialized"); n++;
        return n;}
}
