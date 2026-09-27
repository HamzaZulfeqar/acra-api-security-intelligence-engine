package io.acra.standalone.tests;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneCoreProjectionService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneCoreProjectionTestSuite {
    private static int assertions;

    private StandaloneCoreProjectionTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-core-projection-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Projection Review", "Phase 4");
            var target = workspace.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-P4-001",
                    "IMPORT_ONLY"
            );

            String openApi = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Authorized API","version":"1"},
                      "paths":{
                        "/users/{userId}":{
                          "get":{"responses":{"200":{"description":"ok"}}}
                        }
                      }
                    }
                    """;
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "api.json", openApi);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            contexts.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");

            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42",
                    "ALLOW", "Owner may read");

            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-b", "standard-user", "tenant-a", "user-42",
                    "DENY", "Non-owner denied");

            var snapshot = new StandaloneCoreProjectionService(workspace).project(project.id());

            check(!snapshot.policyFingerprint().isBlank(), "Core policy fingerprint projected");
            check(snapshot.membershipCount() == 2, "tenant memberships projected");
            check(snapshot.roleAssignmentCount() == 2, "role assignments projected");
            check(snapshot.permissionCount() == 2, "permissions projected");
            check(snapshot.ruleCount() == 2, "binary rules projected");
            check(snapshot.authorization().size() == 2, "expectations projected");

            var allow = snapshot.authorization().stream()
                    .filter(row -> row.principalId().equals("user-a"))
                    .findFirst().orElseThrow();
            check(allow.configuredDecision() == AuthorizationDecision.ALLOW, "configured allow retained");
            check(allow.resolvedDecision() == AuthorizationDecision.ALLOW, "Core resolver returns allow");
            check(allow.resolutionState() == PolicyResolutionState.RESOLVED_ALLOW, "allow resolution state");
            check(allow.bolaStatus() == BolaAssessmentStatus.INCONCLUSIVE, "BOLA waits for provenance");
            check(allow.bflaStatus() == BflaAssessmentStatus.INCONCLUSIVE, "BFLA waits for provenance");

            var deny = snapshot.authorization().stream()
                    .filter(row -> row.principalId().equals("user-b"))
                    .findFirst().orElseThrow();
            check(deny.configuredDecision() == AuthorizationDecision.DENY, "configured deny retained");
            check(deny.resolvedDecision() == AuthorizationDecision.DENY, "Core resolver returns deny");
            check(deny.resolutionState() == PolicyResolutionState.RESOLVED_DENY, "deny resolution state");
            check(deny.bolaStatus() == BolaAssessmentStatus.INCONCLUSIVE, "deny BOLA waits for observation");
            check(deny.bflaStatus() == BflaAssessmentStatus.INCONCLUSIVE, "deny BFLA waits for observation");

            check(snapshot.workflowWorkspaceProjected(), "workflow workspace projected");
            check(snapshot.routingWorkspaceProjected(), "routing workspace projected");
            check(snapshot.propertyWorkspaceProjected(), "property workspace projected");
            check(snapshot.workflowResolutionCount() == 0, "workflow awaits evidence");
            check(snapshot.routingAssessmentCount() == 0, "routing awaits evidence");
            check(snapshot.propertyAssessmentCount() == 0, "property awaits evidence");

            System.out.println("SPRINT10_STANDALONE_CORE_PROJECTION PASS assertions=" + assertions);
        } finally {
            if (Files.exists(temp)) {
                try (var walk = Files.walk(temp)) {
                    for (Path pathToDelete : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(pathToDelete);
                    }
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
