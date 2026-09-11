package io.acra.core.tests;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.graph.*;
import java.time.Instant;
import java.util.*;

public final class GraphTests {
    public static int run(){int n=0; Instant t=Instant.parse("2026-08-30T12:00:00Z");
        SecurityContextGraph g=new SecurityContextGraph(); Evidence ev=Evidence.create(EvidenceSource.USER_POLICY,"req-1","policy","owner=user-42","fixture",Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),t); g.addEvidence(ev); GraphNode u=new GraphNode("principal:user-42",NodeType.PRINCIPAL,"user-42",Map.of()); GraphNode ten=new GraphNode("tenant:A",NodeType.TENANT,"A",Map.of()); GraphNode doc=new GraphNode("resource:document:9821",NodeType.RESOURCE,"document:9821",Map.of()); g.addNode(u);g.addNode(ten);g.addNode(doc);
        g.addEdge(GraphEdge.create(u.id(),ten.id(),RelationType.BELONGS_TO,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),List.of(ev.evidenceId()),t)); TestSupport.assertTrue(new GraphQuery(g).hasDirectRelation(u.id(),ten.id(),RelationType.BELONGS_TO),"user tenant relationship"); n++;
        g.addEdge(GraphEdge.create(u.id(),doc.id(),RelationType.OWNS,Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),List.of(ev.evidenceId()),t)); TestSupport.assertEquals(doc.id(),new GraphQuery(g).targets(u.id(),RelationType.OWNS).getFirst().id(),"direct ownership query"); n++;
        SecurityContextGraph bad=new SecurityContextGraph(); bad.addNode(u);bad.addNode(doc); GraphEdge missing=GraphEdge.create(u.id(),doc.id(),RelationType.OWNS,Confidence.of(ConfidenceBasis.HEURISTIC),List.of("missing"),t); TestSupport.assertThrows(IllegalArgumentException.class,()->bad.addEdge(missing),"edge must reference valid evidence"); n++;
        TestSupport.assertThrows(IllegalArgumentException.class,()->g.addNode(new GraphNode(u.id(),NodeType.ROLE,"wrong",Map.of())),"conflicting duplicate node"); n++;
        TestSupport.assertThrows(IllegalArgumentException.class,()->g.addEvidence(ev),"duplicate evidence id"); n++;
        return n;}
}
