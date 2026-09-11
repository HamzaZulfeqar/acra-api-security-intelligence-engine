package io.acra.core.active.evidence;

import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EvidenceReferenceValidator {
	private final ExecutionEvidenceStore store;

	public EvidenceReferenceValidator(ExecutionEvidenceStore store) {
		this.store = store;
	}

	public EvidenceReferenceValidation validate(List<String> evidenceIds, String observationId,
												String executionId, String testId, String projectId) {
		List<String> failures = new ArrayList<>();
		if (store == null) {
			failures.add("EVIDENCE_STORE_MISSING");
			return EvidenceReferenceValidation.rejected(failures);
		}
		if (blankOrUnknown(projectId)) failures.add("PROJECT_REFERENCE_MISSING");
		else if (blankOrUnknown(store.projectId())) failures.add("PROJECT_OWNERSHIP_UNPROVEN");
		else if (!store.projectId().equals(projectId)) failures.add("CROSS_PROJECT_REFERENCE");

		if (blankOrUnknown(observationId) || blankOrUnknown(executionId) || blankOrUnknown(testId)) {
			failures.add("PROVENANCE_REFERENCE_MISSING");
		}

		List<String> references = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
		if (references.isEmpty()) failures.add("EVIDENCE_REFERENCE_MISSING");
		Set<String> distinctReferences = new LinkedHashSet<>();
		for (String reference : references) {
			if (blankOrUnknown(reference)) {
				failures.add("EVIDENCE_REFERENCE_INVALID");
				continue;
			}
			if (!distinctReferences.add(reference)) failures.add("EVIDENCE_REFERENCE_DUPLICATED");
			if (!store.contains(reference)) {
				failures.add("EVIDENCE_UNKNOWN");
				continue;
			}
			List<EvidenceChainEntry> entries = store.chainForObject(reference);
			if (entries.size() != 1) {
				failures.add("EVIDENCE_PROVENANCE_CONTRADICTORY");
				continue;
			}
			EvidenceChainEntry entry = entries.getFirst();
			if (!entry.executionId().equals(executionId)) failures.add("EXECUTION_OWNERSHIP_MISMATCH");
			if (!entry.testId().equals(testId)) failures.add("TEST_OWNERSHIP_MISMATCH");
			if (entry.stage() == EvidenceStage.OBSERVATION) failures.add("EVIDENCE_REFERENCE_IS_OBSERVATION");
		}

		validateObservation(observationId, executionId, testId, distinctReferences, failures);
		return failures.isEmpty() ? EvidenceReferenceValidation.accepted()
				: EvidenceReferenceValidation.rejected(List.copyOf(new LinkedHashSet<>(failures)));
	}

	public EvidenceReferenceValidation validateEvidenceReferences(List<String> evidenceIds, String executionId,
																  String testId, String projectId) {
		List<String> failures = new ArrayList<>();
		if (store == null) {
			failures.add("EVIDENCE_STORE_MISSING");
			return EvidenceReferenceValidation.rejected(failures);
		}
		if (blankOrUnknown(projectId)) failures.add("PROJECT_REFERENCE_MISSING");
		else if (blankOrUnknown(store.projectId())) failures.add("PROJECT_OWNERSHIP_UNPROVEN");
		else if (!store.projectId().equals(projectId)) failures.add("CROSS_PROJECT_REFERENCE");
		if (blankOrUnknown(executionId) || blankOrUnknown(testId)) failures.add("PROVENANCE_REFERENCE_MISSING");
		List<String> references = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
		if (references.isEmpty()) failures.add("EVIDENCE_REFERENCE_MISSING");
		Set<String> distinctReferences = new LinkedHashSet<>();
		for (String reference : references) {
			if (blankOrUnknown(reference)) {
				failures.add("EVIDENCE_REFERENCE_INVALID");
				continue;
			}
			if (!distinctReferences.add(reference)) failures.add("EVIDENCE_REFERENCE_DUPLICATED");
			if (!store.contains(reference)) {
				failures.add("EVIDENCE_UNKNOWN");
				continue;
			}
			List<EvidenceChainEntry> entries = store.chainForObject(reference);
			if (entries.size() != 1) {
				failures.add("EVIDENCE_PROVENANCE_CONTRADICTORY");
				continue;
			}
			EvidenceChainEntry entry = entries.getFirst();
			if (!entry.executionId().equals(executionId)) failures.add("EXECUTION_OWNERSHIP_MISMATCH");
			if (!entry.testId().equals(testId)) failures.add("TEST_OWNERSHIP_MISMATCH");
			if (entry.stage() == EvidenceStage.OBSERVATION) failures.add("EVIDENCE_REFERENCE_IS_OBSERVATION");
		}
		return failures.isEmpty() ? EvidenceReferenceValidation.accepted()
				: EvidenceReferenceValidation.rejected(List.copyOf(new LinkedHashSet<>(failures)));
	}

	public EvidenceReferenceValidation validateObservation(String observationId, String executionId,
														   String testId, String projectId) {
		List<String> failures = new ArrayList<>();
		if (store == null) failures.add("EVIDENCE_STORE_MISSING");
		if (blankOrUnknown(projectId)) failures.add("PROJECT_REFERENCE_MISSING");
		else if (store != null && blankOrUnknown(store.projectId())) failures.add("PROJECT_OWNERSHIP_UNPROVEN");
		else if (store != null && !store.projectId().equals(projectId)) failures.add("CROSS_PROJECT_REFERENCE");
		if (blankOrUnknown(observationId) || blankOrUnknown(executionId) || blankOrUnknown(testId)) {
			failures.add("PROVENANCE_REFERENCE_MISSING");
		}
		validateObservationObject(observationId, executionId, testId, failures);
		return failures.isEmpty() ? EvidenceReferenceValidation.accepted()
				: EvidenceReferenceValidation.rejected(List.copyOf(new LinkedHashSet<>(failures)));
	}

	private void validateObservation(String observationId, String executionId, String testId,
									 Set<String> references, List<String> failures) {
		if (store == null || blankOrUnknown(observationId) || blankOrUnknown(executionId)
				|| blankOrUnknown(testId)) return;
		Observation observation = null;
		if (!store.contains(observationId)) {
			failures.add("OBSERVATION_UNKNOWN");
		} else {
			Object value = store.object(observationId);
			if (!(value instanceof Observation)) failures.add("OBSERVATION_TYPE_INVALID");
			else observation = (Observation) value;
		}
		if (observation == null) return;
		if (!observation.observationId().equals(observationId)
				|| !observation.testId().equals(testId)
				|| !observation.executionFingerprint().executionId().equals(executionId)
				|| !observation.executionFingerprint().testId().equals(testId)) {
			failures.add("PROVENANCE_CONTRADICTORY");
		}
		Set<String> observedReferences = new LinkedHashSet<>(observation.evidenceIds());
		if (!observedReferences.equals(references)) failures.add("EVIDENCE_PROVENANCE_CONTRADICTORY");
		for (String reference : observedReferences) {
			if (blankOrUnknown(reference) || !store.contains(reference)) continue;
			for (EvidenceChainEntry entry : store.chainForObject(reference)) {
				if (!entry.executionId().equals(executionId) || !entry.testId().equals(testId)) {
					failures.add("REPLAY_LINEAGE_INVALID");
				}
			}
		}
	}

	private void validateObservationObject(String observationId, String executionId, String testId,
										   List<String> failures) {
		if (store == null || blankOrUnknown(observationId)) return;
		if (!store.contains(observationId)) {
			failures.add("OBSERVATION_UNKNOWN");
			return;
		}
		Object value = store.object(observationId);
		if (!(value instanceof Observation observation)) {
			failures.add("OBSERVATION_TYPE_INVALID");
			return;
		}
		List<EvidenceChainEntry> entries = store.chainForObject(observationId);
		if (entries.size() != 1 || entries.getFirst().stage() != EvidenceStage.OBSERVATION) {
			failures.add("OBSERVATION_PROVENANCE_INVALID");
		}
		if (!observation.observationId().equals(observationId)
				|| !observation.testId().equals(testId)
				|| !observation.executionFingerprint().executionId().equals(executionId)
				|| !observation.executionFingerprint().testId().equals(testId)) {
			failures.add("REPLAY_LINEAGE_INVALID");
		}
	}

	private boolean blankOrUnknown(String value) {
		return value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value.strip())
				|| value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED);
	}
}
