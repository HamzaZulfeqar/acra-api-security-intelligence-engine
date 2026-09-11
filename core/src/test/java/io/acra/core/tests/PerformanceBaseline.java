package io.acra.core.tests;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.*;
import io.acra.core.domain.uri.UriModel;
import io.acra.core.extraction.defaults.DefaultUriExtractor;
import io.acra.core.graph.*;
import io.acra.core.serialization.DomainSerializer;
import java.time.Instant;
import java.util.*;

public final class PerformanceBaseline {
    public static void main(String[] args) {
        int[] sizes = {1_000, 10_000, 100_000};
        System.out.println("ACRA Sprint 1 performance baseline");
        System.out.println("java=" + System.getProperty("java.version"));
        System.out.println("availableProcessors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("count,parseMs,graphMs,serializeMs,approxMemoryDeltaBytes");
        DefaultUriExtractor extractor = new DefaultUriExtractor();
        DomainSerializer serializer = new DomainSerializer();
        for (int count : sizes) {
            gcPause();
            long beforeMem = usedMemory();
            long t0 = System.nanoTime();
            UriModel last = null;
            for (int i=0;i<count;i++) last = extractor.extract(Fixtures.tx("/api/v1/tenants/acme/documents/" + (1000+i)));
            long t1 = System.nanoTime();

            Instant ts = Instant.parse("2026-08-30T12:00:00Z");
            long t2 = System.nanoTime();
            for (int i=0;i<count;i++) {
                SecurityContextGraph g = new SecurityContextGraph();
                Evidence e = Evidence.create(EvidenceSource.PATH,"perf-"+i,"path","doc-"+i,"fixture",Confidence.of(ConfidenceBasis.STRUCTURAL_INFERENCE),ts);
                g.addEvidence(e);
                GraphNode p = new GraphNode("p:"+i,NodeType.PRINCIPAL,"p",Map.of());
                GraphNode r = new GraphNode("r:"+i,NodeType.RESOURCE,"r",Map.of());
                g.addNode(p);g.addNode(r);
                g.addEdge(GraphEdge.create(p.id(),r.id(),RelationType.ACCESSES,Confidence.of(ConfidenceBasis.EXACT_OBSERVED),List.of(e.evidenceId()),ts));
            }
            long t3 = System.nanoTime();

            for (int i=0;i<count;i++) serializer.serialize(last);
            long t4 = System.nanoTime();
            long afterMem = usedMemory();
            System.out.printf(Locale.ROOT,"%d,%d,%d,%d,%d%n",count,ms(t0,t1),ms(t2,t3),ms(t3,t4),Math.max(0,afterMem-beforeMem));
        }
    }
    private static long ms(long a,long b){return (b-a)/1_000_000;}
    private static long usedMemory(){Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
    private static void gcPause(){System.gc();try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
