package io.acra.burp.reporting;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.sitemap.SiteMap;
import io.acra.core.reproduction.ReproductionPackage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ReproductionIssuePublisher {
    private final SiteMap siteMap;
    private final ReproductionAuditIssueAdapter adapter;
    private final Set<String> publishedFingerprints = ConcurrentHashMap.newKeySet();

    public ReproductionIssuePublisher(SiteMap siteMap) {
        this(siteMap, new ReproductionAuditIssueAdapter());
    }

    ReproductionIssuePublisher(
            SiteMap siteMap,
            ReproductionAuditIssueAdapter adapter) {
        if (siteMap == null) throw new IllegalArgumentException("siteMap required");
        if (adapter == null) throw new IllegalArgumentException("adapter required");
        this.siteMap = siteMap;
        this.adapter = adapter;
    }

    public BurpIssuePublicationResult publish(
            ReproductionPackage value,
            HttpRequestResponse requestResponse) {
        if (value == null) throw new IllegalArgumentException("reproduction package required");
        String fingerprint = value.fingerprint();
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("reproduction fingerprint required");
        }

        AuditIssue issue = adapter.create(value, requestResponse);
        if (!publishedFingerprints.add(fingerprint)) {
            return new BurpIssuePublicationResult(
                    BurpIssuePublicationStatus.DUPLICATE_SUPPRESSED,
                    fingerprint,
                    issue);
        }

        try {
            siteMap.add(issue);
            return new BurpIssuePublicationResult(
                    BurpIssuePublicationStatus.PUBLISHED,
                    fingerprint,
                    issue);
        } catch (RuntimeException failure) {
            publishedFingerprints.remove(fingerprint);
            throw failure;
        }
    }

    public boolean hasPublished(String reproductionFingerprint) {
        return reproductionFingerprint != null
                && publishedFingerprints.contains(reproductionFingerprint);
    }

    public int publishedCount() {
        return publishedFingerprints.size();
    }
}
