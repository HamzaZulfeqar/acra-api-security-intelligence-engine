package io.acra.core.active.model;

public enum TestProfile {
    SAFE_LAB("Safe Lab"),
    AUTHORIZATION_DIFFERENTIAL("Authorization Audit"),
    ROUTING_DIFFERENTIAL("Routing Audit"),
    CONTEXT_DIFFERENTIAL("Context Audit"),
    RESEARCH_EXPERIMENT("Research Differential"),
    EXPERT_CONTROLLED("Expert Custom");

    private final String displayName;

    TestProfile(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
