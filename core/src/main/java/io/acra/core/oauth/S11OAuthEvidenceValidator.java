package io.acra.core.oauth;

import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class S11OAuthEvidenceValidator {
    private final ExecutionEvidenceStore store;

    public S11OAuthEvidenceValidator(ExecutionEvidenceStore store) {
        if (store == null) throw new IllegalArgumentException("evidenceStore required");
        this.store = store;
    }

    public EvidenceReferenceValidation validate(OAuthEvidenceBinding binding) {
        if (binding == null) {
            return EvidenceReferenceValidation.rejected(List.of("OAUTH_EVIDENCE_BINDING_MISSING"));
        }

        List<String> failures = new ArrayList<>();
        if (store.projectId() == null || store.projectId().isBlank()) {
            failures.add("PROJECT_OWNERSHIP_UNPROVEN");
        } else if (!store.projectId().equals(binding.projectId())) {
            failures.add("CROSS_PROJECT_REFERENCE");
        }

        Set<String> distinctEvidence = new LinkedHashSet<>();
        for (String evidenceId : binding.evidenceObjectIds()) {
            if (evidenceId == null || evidenceId.isBlank()) {
                failures.add("EVIDENCE_REFERENCE_INVALID");
                continue;
            }
            if (!distinctEvidence.add(evidenceId)) {
                failures.add("EVIDENCE_REFERENCE_DUPLICATED");
                continue;
            }
            validateEvidenceObject(evidenceId, binding, failures);
        }

        OAuthContextObservation observation = validateOAuthObservation(binding, failures);
        if (observation != null) {
            Set<String> observedReferences = new LinkedHashSet<>(observation.evidenceIds());
            if (!observedReferences.equals(distinctEvidence)) {
                failures.add("OAUTH_EVIDENCE_PROVENANCE_CONTRADICTORY");
            }
            if (!observation.requestId().equals(binding.requestId())) {
                failures.add("OAUTH_REQUEST_LINEAGE_MISMATCH");
            }
        }

        List<String> reasons = List.copyOf(new LinkedHashSet<>(failures));
        return reasons.isEmpty()
                ? EvidenceReferenceValidation.accepted()
                : EvidenceReferenceValidation.rejected(reasons);
    }

    private void validateEvidenceObject(
            String evidenceId,
            OAuthEvidenceBinding binding,
            List<String> failures) {

        if (!store.contains(evidenceId)) {
            failures.add("EVIDENCE_UNKNOWN");
            return;
        }
        List<EvidenceChainEntry> entries = store.chainForObject(evidenceId);
        if (entries.size() != 1) {
            failures.add("EVIDENCE_PROVENANCE_CONTRADICTORY");
            return;
        }
        EvidenceChainEntry entry = entries.getFirst();
        if (!entry.executionId().equals(binding.executionId())) {
            failures.add("EXECUTION_OWNERSHIP_MISMATCH");
        }
        if (!entry.testId().equals(binding.testId())) {
            failures.add("TEST_OWNERSHIP_MISMATCH");
        }
        if (entry.stage() == EvidenceStage.OBSERVATION) {
            failures.add("EVIDENCE_REFERENCE_IS_OBSERVATION");
        }
    }

    private OAuthContextObservation validateOAuthObservation(
            OAuthEvidenceBinding binding,
            List<String> failures) {

        if (!store.contains(binding.observationObjectId())) {
            failures.add("OAUTH_OBSERVATION_UNKNOWN");
            return null;
        }
        Object value = store.object(binding.observationObjectId());
        if (!(value instanceof OAuthContextObservation observation)) {
            failures.add("OAUTH_OBSERVATION_TYPE_INVALID");
            return null;
        }

        List<EvidenceChainEntry> entries = store.chainForObject(binding.observationObjectId());
        if (entries.size() != 1 || entries.getFirst().stage() != EvidenceStage.OBSERVATION) {
            failures.add("OAUTH_OBSERVATION_PROVENANCE_INVALID");
        } else {
            EvidenceChainEntry entry = entries.getFirst();
            if (!entry.executionId().equals(binding.executionId())) {
                failures.add("OAUTH_EXECUTION_LINEAGE_MISMATCH");
            }
            if (!entry.testId().equals(binding.testId())) {
                failures.add("OAUTH_TEST_LINEAGE_MISMATCH");
            }
        }

        if (!observation.observationId().equals(binding.observationObjectId())) {
            failures.add("OAUTH_OBSERVATION_ID_MISMATCH");
        }
        return observation;
    }
}
