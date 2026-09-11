package io.acra.core.active.model;

public enum TestContract {
    CROSS_USER(TestCategory.AUTHORIZATION),
    CROSS_RESOURCE(TestCategory.AUTHORIZATION),
    CROSS_TENANT(TestCategory.AUTHORIZATION),
    ROLE_COMPARISON(TestCategory.AUTHORIZATION),
    ANONYMOUS_VS_AUTHENTICATED(TestCategory.AUTHORIZATION),
    URI_REPRESENTATION(TestCategory.ROUTING),
    NORMALIZATION(TestCategory.ROUTING),
    ROUTE_EQUIVALENCE(TestCategory.ROUTING),
    METHOD_VARIATION(TestCategory.ROUTING),
    IDENTITY(TestCategory.CONTEXT),
    ROLE(TestCategory.CONTEXT),
    TENANT(TestCategory.CONTEXT),
    RESOURCE(TestCategory.CONTEXT),
    TOKEN_CONTEXT(TestCategory.CONTEXT),
    PROPERTY(TestCategory.DATA),
    COLLECTION(TestCategory.DATA),
    BATCH(TestCategory.DATA),
    INDIRECT_REFERENCE(TestCategory.DATA),
    STATE(TestCategory.WORKFLOW),
    TRANSITION(TestCategory.WORKFLOW),
    CONTEXT_SWITCH(TestCategory.WORKFLOW);

    private final TestCategory category;

    TestContract(TestCategory category) {
        this.category = category;
    }

    public TestCategory category() {
        return category;
    }
}
