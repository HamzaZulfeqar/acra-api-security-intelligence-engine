package burp.api.montoya.scanner.audit.issues;

import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import java.util.List;

public interface AuditIssue {
    String name();
    String detail();
    String remediation();
    HttpService httpService();
    String baseUrl();
    AuditIssueSeverity severity();
    AuditIssueConfidence confidence();
    List<HttpRequestResponse> requestResponses();
    List<Interaction> collaboratorInteractions();
    AuditIssueDefinition definition();
}
