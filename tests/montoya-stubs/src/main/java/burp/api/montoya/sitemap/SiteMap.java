package burp.api.montoya.sitemap;

import burp.api.montoya.scanner.audit.issues.AuditIssue;

public interface SiteMap {
    void add(AuditIssue issue);
}
