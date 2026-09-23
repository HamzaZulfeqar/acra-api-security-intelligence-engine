package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.AuthorizationAssessmentAggregate;
import io.acra.core.domain.authorization.AuthorizationCorrelationEnvelope;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class AuthorizationAssessmentCorrelationService {

    public AuthorizationCorrelationEnvelope correlate(BolaAssessment bola, BflaAssessment bfla,
            TenantAuthorizationAssessment tenant, WorkflowAuthorizationAssessment workflow,
            PropertyAuthorizationAssessment property, EvidenceReferenceValidator validator,
            String projectId, String policyReference, boolean independenceVerified) {

        List<BolaAssessment> bolas = bola == null ? List.of() : List.of(bola);
        List<BflaAssessment> bflas = bfla == null ? List.of() : List.of(bfla);
        AuthorizationAssessmentAggregate aggregate =
                AuthorizationAssessmentCorrelator.correlate(bolas, bflas, validator, projectId);

        Set<String> testIds = new TreeSet<>();
        Set<String> executions = new TreeSet<>();
        Set<String> propertyReferences = new TreeSet<>();
        Set<String> reviews = new TreeSet<>();
        if (bola != null) {
            add(testIds, bola.testId());
            add(executions, bola.executionId());
        }
        if (bfla != null) {
            add(testIds, bfla.testId());
            add(executions, bfla.executionId());
        }
        if (tenant != null) {
            add(reviews, tenant.assessmentId());
            propertyReferences.add("tenant");
        }
        if (workflow != null) {
            add(reviews, workflow.assessmentId());
            propertyReferences.add("workflow");
        }
        if (property != null) {
            add(reviews, property.assessmentId());
            propertyReferences.add("property:" + property.property());
        }

        int independentCount = executions.size();
        boolean corroborated = independenceVerified && independentCount >= 2
                && aggregate.state() == io.acra.core.domain.authorization.CorrelationState.CONSISTENT;
        return new AuthorizationCorrelationEnvelope(aggregate, projectId, policyReference, List.copyOf(testIds),
                List.copyOf(propertyReferences), List.copyOf(reviews), independentCount, corroborated);
    }

    private void add(Set<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value);
    }
}
