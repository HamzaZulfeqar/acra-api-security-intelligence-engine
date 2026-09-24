package io.acra.core.session;

import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class S10SessionEvidenceValidator {
    private final ExecutionEvidenceStore store;

    public S10SessionEvidenceValidator(ExecutionEvidenceStore store) {
        if (store == null) throw new IllegalArgumentException("evidenceStore required");
        this.store = store;
    }

    public EvidenceReferenceValidation validate(SessionEvidenceBinding binding) {
        if (binding == null) {
            return EvidenceReferenceValidation.rejected(List.of("SESSION_EVIDENCE_BINDING_MISSING"));
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

        AuthenticationSessionObservation observation = validateSessionObservation(binding, failures);
        if (observation != null) {
            Set<String> observedReferences = new LinkedHashSet<>(observation.evidenceIds());
            if (!observedReferences.equals(distinctEvidence)) {
                failures.add("SESSION_EVIDENCE_PROVENANCE_CONTRADICTORY");
            }
        }

        List<String> reasons = List.copyOf(new LinkedHashSet<>(failures));
        return reasons.isEmpty()
                ? EvidenceReferenceValidation.accepted()
                : EvidenceReferenceValidation.rejected(reasons);
    }

    private void validateEvidenceObject(
            String evidenceId,
            SessionEvidenceBinding binding,
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

    private AuthenticationSessionObservation validateSessionObservation(
            SessionEvidenceBinding binding,
            List<String> failures) {

        if (!store.contains(binding.observationObjectId())) {
            failures.add("SESSION_OBSERVATION_UNKNOWN");
            return null;
        }

        Object value = store.object(binding.observationObjectId());
        if (!(value instanceof AuthenticationSessionObservation observation)) {
            failures.add("SESSION_OBSERVATION_TYPE_INVALID");
            return null;
        }

        List<EvidenceChainEntry> entries = store.chainForObject(binding.observationObjectId());
        if (entries.size() != 1 || entries.getFirst().stage() != EvidenceStage.OBSERVATION) {
            failures.add("SESSION_OBSERVATION_PROVENANCE_INVALID");
        } else {
            EvidenceChainEntry entry = entries.getFirst();
            if (!entry.executionId().equals(binding.executionId())) {
                failures.add("SESSION_EXECUTION_LINEAGE_MISMATCH");
            }
            if (!entry.testId().equals(binding.testId())) {
                failures.add("SESSION_TEST_LINEAGE_MISMATCH");
            }
        }

        if (!observation.observationId().equals(binding.observationObjectId())) {
            failures.add("SESSION_OBSERVATION_ID_MISMATCH");
        }

        return observation;
    }
}
