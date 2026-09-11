package io.acra.core.graph;

import java.util.List;

public final class GraphQuery {
    private final SecurityContextGraph graph;
    public GraphQuery(SecurityContextGraph graph){ this.graph=graph; }
    public List<GraphNode> targets(String source,RelationType relation){ return graph.outgoing(source,relation).stream().map(e->graph.node(e.target()).orElseThrow()).toList(); }
    public boolean hasDirectRelation(String source,String target,RelationType relation){ return graph.outgoing(source,relation).stream().anyMatch(e->e.target().equals(target)); }
}
