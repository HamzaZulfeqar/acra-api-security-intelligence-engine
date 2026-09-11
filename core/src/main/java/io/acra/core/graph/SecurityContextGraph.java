package io.acra.core.graph;

import io.acra.core.domain.evidence.Evidence;
import java.util.*;

public final class SecurityContextGraph {
    private final NavigableMap<String,GraphNode> nodes=new TreeMap<>();
    private final NavigableMap<String,Evidence> evidence=new TreeMap<>();
    private final NavigableMap<String,GraphEdge> edges=new TreeMap<>();
    private final Map<String,LinkedHashSet<String>> outgoing=new HashMap<>();

    public synchronized void addEvidence(Evidence e){ if(evidence.putIfAbsent(e.evidenceId(),e)!=null) throw new IllegalArgumentException("duplicate evidence id: "+e.evidenceId()); }
    public synchronized void addNode(GraphNode n){ GraphNode prior=nodes.putIfAbsent(n.id(),n); if(prior!=null&&!prior.equals(n)) throw new IllegalArgumentException("conflicting duplicate node: "+n.id()); }
    public synchronized void addEdge(GraphEdge e){
        if(!nodes.containsKey(e.source())||!nodes.containsKey(e.target())) throw new IllegalArgumentException("graph edge references nonexistent node");
        for(String id:e.evidenceIds()) if(!evidence.containsKey(id)) throw new IllegalArgumentException("graph edge references nonexistent evidence: "+id);
        GraphEdge prior=edges.putIfAbsent(e.edgeId(),e); if(prior!=null&&!prior.equals(e)) throw new IllegalArgumentException("conflicting duplicate edge");
        outgoing.computeIfAbsent(e.source(),k->new LinkedHashSet<>()).add(e.edgeId());
    }
    public synchronized void mergeFrom(SecurityContextGraph other){
        if(other==null) throw new IllegalArgumentException("other graph required");
        for(Evidence e:other.evidenceList()) { if(!evidence.containsKey(e.evidenceId())) addEvidence(e); }
        for(GraphNode n:other.nodes()) addNode(n);
        for(GraphEdge e:other.edges()) { if(!edges.containsKey(e.edgeId())) addEdge(e); }
    }
    public Optional<GraphNode> node(String id){ return Optional.ofNullable(nodes.get(id)); }
    public Optional<Evidence> evidence(String id){ return Optional.ofNullable(evidence.get(id)); }
    public List<GraphNode> nodes(){ return List.copyOf(nodes.values()); }
    public List<GraphEdge> edges(){ return List.copyOf(edges.values()); }
    public List<Evidence> evidenceList(){ return List.copyOf(evidence.values()); }
    public List<GraphEdge> outgoing(String source,RelationType relation){
        return outgoing.getOrDefault(source,new LinkedHashSet<>()).stream().map(edges::get).filter(Objects::nonNull).filter(e->relation==null||e.relation()==relation).sorted(Comparator.comparing(GraphEdge::edgeId)).toList();
    }
    public int nodeCount(){return nodes.size();} public int edgeCount(){return edges.size();} public int evidenceCount(){return evidence.size();}
}
