package io.acra.core.product.reproduction;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.reporting.s12.S12BurpIssueProjector;
import io.acra.core.reporting.s12.S12ReproductionJsonExporter;
import io.acra.core.reporting.s12.S12ReproductionPackage;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import io.acra.core.reporting.s12.S12SarifExporter;
import java.util.List;
import java.util.TreeMap;

public final class S12ReproductionWorkspace {
    private final TreeMap<String, S12ReproductionPackage> packages = new TreeMap<>();
    private final TreeMap<String, String> candidatePackages = new TreeMap<>();
    private final S12ReproductionPackageFactory packageFactory = new S12ReproductionPackageFactory();
    private final S12ReproductionJsonExporter jsonExporter = new S12ReproductionJsonExporter();
    private final S12SarifExporter sarifExporter = new S12SarifExporter();
    private final S12BurpIssueProjector burpProjector = new S12BurpIssueProjector();

    public synchronized S12ReproductionPackage recordCandidate(FindingCandidate candidate) {
        S12ReproductionPackage value = packageFactory.from(candidate);
        recordPackage(value);
        return value;
    }

    public synchronized void recordPackage(S12ReproductionPackage value) {
        if (value == null) throw new IllegalArgumentException("reproduction package required");

        String existingPackageId = candidatePackages.get(value.sourceCandidateId());
        if (existingPackageId != null && !existingPackageId.equals(value.packageId())) {
            throw new IllegalArgumentException("candidate reproduction package drift");
        }

        S12ReproductionPackage existing = packages.get(value.packageId());
        if (existing != null && !existing.fingerprint().equals(value.fingerprint())) {
            throw new IllegalArgumentException("reproduction package identity collision");
        }

        packages.put(value.packageId(), value);
        candidatePackages.put(value.sourceCandidateId(), value.packageId());
    }

    public synchronized S12ReproductionProductSnapshot snapshot() {
        List<S12ReproductionProductEntry> entries = packages.values().stream()
                .map(value -> new S12ReproductionProductEntry(
                        value,
                        jsonExporter.export(value),
                        sarifExporter.export(value),
                        burpProjector.project(value)))
                .toList();
        return new S12ReproductionProductSnapshot(entries);
    }

    public synchronized void clear() {
        packages.clear();
        candidatePackages.clear();
    }
}
