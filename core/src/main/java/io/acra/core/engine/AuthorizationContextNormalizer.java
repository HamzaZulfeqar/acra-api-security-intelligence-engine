package io.acra.core.engine;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.Observation;
import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationContextNormalizationResult;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves an S4 Observation into the existing S5 AuthorizationContext.
 * This class authenticates observation/evidence references before constructing
 * the context and never converts missing context into an authorization verdict.
 */
public final class AuthorizationContextNormalizer {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final EvidenceReferenceValidator evidenceValidator;

    public AuthorizationContextNormalizer(EvidenceReferenceValidator evidenceValidator) {
        this.evidenceValidator = evidenceValidator;
    }

    public AuthorizationContextNormalizationResult normalizeTarget(Observation observation, String projectId) {
        return normalize(observation, projectId, true);
    }

    public AuthorizationContextNormalizationResult normalizeSource(Observation observation, String projectId) {
        return normalize(observation, projectId, false);
    }

    private AuthorizationContextNormalizationResult normalize(Observation observation, String projectId,
                                                               boolean target) {
        List<String> reasons = new ArrayList<>();
        if (observation == null) return AuthorizationContextNormalizationResult.rejected(
                List.of("OBSERVATION_MISSING"));
        if (evidenceValidator == null) return AuthorizationContextNormalizationResult.rejected(
                List.of("EVIDENCE_VALIDATION_UNAVAILABLE"));

        String executionId = observation.executionFingerprint().executionId();
        String testId = observation.testId();
        EvidenceReferenceValidation validation = evidenceValidator.validate(
                observation.evidenceIds(), observation.observationId(), executionId, testId, projectId);
        if (!validation.valid()) reasons.addAll(validation.reasons());

        SecurityContextFingerprint fingerprint = target ? observation.targetContext() : observation.sourceContext();
        if (fingerprint == null) reasons.add("SECURITY_CONTEXT_FINGERPRINT_MISSING");
        if (!reasons.isEmpty()) return AuthorizationContextNormalizationResult.rejected(reasons);

        Principal principal = principal(fingerprint.principal());
        Role role = role(fingerprint.role());
        Tenant tenant = tenant(fingerprint.tenant());
        Resource resource = resource(fingerprint);
        Action action = action(fingerprint.action());
        WorkflowState workflow = workflow(fingerprint.workflow());

        AuthorizationDecision expected = observation.expectedDecision().decision();
        AuthorizationDecision observed = decision(observation.observedDecision());
        ContextStatus status = status(principal, tenant, resource, action);

        AuthorizationContext context = new AuthorizationContext(
                principal,
                role,
                tenant,
                resource,
                known(fingerprint.owner()) ? fingerprint.owner() : "",
                action,
                workflow,
                expected,
                observed,
                observation.evidenceIds(),
                status);

        return AuthorizationContextNormalizationResult.accepted(context);
    }

    private Principal principal(String value) {
        if (!known(value)) return null;
        return new Principal(value, value, AuthenticationType.UNKNOWN,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA));
    }

    private Role role(String value) {
        if (!known(value)) return null;
        return new Role(value, value, EvidenceSource.CONFIGURATION,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA));
    }

    private Tenant tenant(String value) {
        if (!known(value)) return null;
        return new Tenant(value, value, EvidenceSource.CONFIGURATION,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA));
    }

    private Resource resource(SecurityContextFingerprint fingerprint) {
        if (!known(fingerprint.resource())) return null;
        return new Resource(
                fingerprint.resource(),
                "UNKNOWN",
                null,
                known(fingerprint.owner()) ? fingerprint.owner() : null,
                known(fingerprint.tenant()) ? fingerprint.tenant() : null,
                known(fingerprint.workflow()) ? fingerprint.workflow() : null,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA));
    }

    private Action action(String value) {
        if (!known(value)) return new Action(ActionType.UNKNOWN, EvidenceSource.UNKNOWN,
                Confidence.unknown(), "");
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        ActionType type = switch (normalized) {
            case "GET", "READ" -> ActionType.READ;
            case "POST", "CREATE" -> ActionType.CREATE;
            case "PUT", "PATCH", "UPDATE" -> ActionType.UPDATE;
            case "DELETE" -> ActionType.DELETE;
            case "EXECUTE" -> ActionType.EXECUTE;
            case "APPROVE" -> ActionType.APPROVE;
            case "SHARE" -> ActionType.SHARE;
            case "EXPORT" -> ActionType.EXPORT;
            default -> ActionType.UNKNOWN;
        };
        return new Action(type, EvidenceSource.CONFIGURATION,
                type == ActionType.UNKNOWN ? Confidence.unknown()
                        : Confidence.of(ConfidenceBasis.EXPLICIT_METADATA),
                value);
    }

    private WorkflowState workflow(String value) {
        if (!known(value)) return WorkflowState.unknown();
        return new WorkflowState(value, Confidence.of(ConfidenceBasis.EXPLICIT_METADATA));
    }

    private ContextStatus status(Principal principal, Tenant tenant, Resource resource, Action action) {
        if (principal != null && tenant != null && resource != null
                && action != null && action.actionType() != ActionType.UNKNOWN) {
            return ContextStatus.RESOLVED;
        }
        if (principal != null || tenant != null || resource != null
                || (action != null && action.actionType() != ActionType.UNKNOWN)) {
            return ContextStatus.PARTIAL;
        }
        return ContextStatus.UNKNOWN;
    }

    private AuthorizationDecision decision(AuthorizationOutcome outcome) {
        if (outcome == null) return AuthorizationDecision.UNKNOWN;
        return switch (outcome) {
            case ALLOW -> AuthorizationDecision.ALLOW;
            case DENY, AUTHENTICATION_REQUIRED, NOT_FOUND -> AuthorizationDecision.DENY;
            case PARTIAL -> AuthorizationDecision.CONDITIONAL;
            case ERROR -> AuthorizationDecision.ERROR;
            case UNKNOWN -> AuthorizationDecision.UNKNOWN;
        };
    }

    private boolean known(String value) {
        return value != null && !value.isBlank()
                && !"UNKNOWN".equalsIgnoreCase(value.strip())
                && !value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
                && value.equals(REDACTOR.redactText(value));
    }
}
