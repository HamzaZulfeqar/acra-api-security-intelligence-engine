package io.acra.core.active.evidence;

import java.util.List;

public record EvidenceReferenceValidation(boolean valid, List<String> reasons) {
	public EvidenceReferenceValidation {
		reasons = List.copyOf(reasons == null ? List.of() : reasons);
		if (valid && !reasons.isEmpty()) throw new IllegalArgumentException("valid result cannot have failure reasons");
		if (!valid && reasons.isEmpty()) throw new IllegalArgumentException("invalid result requires failure reasons");
	}

	public static EvidenceReferenceValidation accepted() {
		return new EvidenceReferenceValidation(true, List.of());
	}

	public static EvidenceReferenceValidation rejected(List<String> reasons) {
		return new EvidenceReferenceValidation(false, reasons);
	}
}
