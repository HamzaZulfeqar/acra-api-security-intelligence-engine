package io.acra.core.graph;

import io.acra.core.domain.common.Validation;
import java.util.*;

public record GraphNode(String id, NodeType type, String label, Map<String,String> attributes) {
    public GraphNode {
        id=Validation.requireNonBlank(id,"graph node id");
        if(type==null) throw new IllegalArgumentException("graph node type required");
        label=label==null?"":label;
        TreeMap<String,String> copy=new TreeMap<>(); if(attributes!=null) copy.putAll(attributes); attributes=Collections.unmodifiableMap(copy);
    }
}
