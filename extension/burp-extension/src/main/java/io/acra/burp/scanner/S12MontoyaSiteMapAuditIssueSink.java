package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.sitemap.SiteMap;

public final class S12MontoyaSiteMapAuditIssueSink implements S12AuditIssueSink {
    private final SiteMap siteMap;

    public S12MontoyaSiteMapAuditIssueSink(SiteMap siteMap) {
        if (siteMap == null) throw new IllegalArgumentException("siteMap required");
        this.siteMap = siteMap;
    }

    @Override
    public void add(AuditIssue issue) {
        if (issue == null) throw new IllegalArgumentException("issue required");
        siteMap.add(issue);
    }
}
