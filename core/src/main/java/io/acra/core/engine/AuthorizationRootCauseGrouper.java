package io.acra.core.engine;

import io.acra.core.domain.finding.AuthorizationRootCauseCluster;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class AuthorizationRootCauseGrouper {

    public List<AuthorizationRootCauseCluster> group(List<FindingCandidate> candidates) {
        Map<String, Bucket> buckets = new TreeMap<>();
        for (FindingCandidate candidate : candidates == null ? List.<FindingCandidate>of() : candidates) {
            if (candidate == null || candidate.state() != FindingCandidateState.CANDIDATE) continue;
            String policyKey = String.join(",", candidate.policyReferences());
            String dimensionKey = String.join(",", candidate.dimensions());
            String key = policyKey + "|" + dimensionKey + "|" + candidate.tenantRelationship()
                    + "|" + candidate.expectedDecision() + "|" + candidate.observedDecision();
            Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
            bucket.candidateIds.add(candidate.candidateId());
            bucket.dimensions.addAll(candidate.dimensions());
            bucket.policies.addAll(candidate.policyReferences());
            if (!candidate.endpoint().isBlank()) bucket.endpoints.add(candidate.endpoint());
            if (!candidate.resourceId().isBlank()) bucket.resources.add(candidate.resourceId());
        }

        List<AuthorizationRootCauseCluster> out = new ArrayList<>();
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            String clusterId = "cluster-" + TokenFingerprint.sha256(entry.getKey()).substring(0, 24);
            Bucket b = entry.getValue();
            out.add(new AuthorizationRootCauseCluster(clusterId, entry.getKey(),
                    List.copyOf(b.candidateIds), List.copyOf(b.dimensions), List.copyOf(b.policies),
                    List.copyOf(b.endpoints), List.copyOf(b.resources)));
        }
        return List.copyOf(out);
    }

    private static final class Bucket {
        private final TreeSet<String> candidateIds = new TreeSet<>();
        private final TreeSet<String> dimensions = new TreeSet<>();
        private final TreeSet<String> policies = new TreeSet<>();
        private final TreeSet<String> endpoints = new TreeSet<>();
        private final TreeSet<String> resources = new TreeSet<>();
    }
}
