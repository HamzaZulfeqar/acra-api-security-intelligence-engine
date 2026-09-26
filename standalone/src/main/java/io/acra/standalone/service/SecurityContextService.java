package io.acra.standalone.service;

import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.PrincipalContextRecord;
import io.acra.standalone.model.ResourceContextRecord;
import io.acra.standalone.model.RoleContextRecord;
import io.acra.standalone.model.TenantContextRecord;
import io.acra.standalone.store.LocalWorkspaceStore;
import io.acra.standalone.store.SecurityContextStore;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SecurityContextService {
    private final LocalWorkspaceStore workspace;
    private final SecurityContextStore contextStore;

    public SecurityContextService(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.contextStore = new SecurityContextStore(workspace);
    }

    public PrincipalContextRecord addPrincipal(
            UUID projectId,
            String principalId,
            String displayName,
            String authenticationType
    ) throws IOException {
        ensureUniquePrincipal(projectId, principalId);
        AuthenticationType auth = parse(AuthenticationType.class, authenticationType, "authenticationType");
        return contextStore.savePrincipal(new PrincipalContextRecord(
                UUID.randomUUID(), projectId, principalId, displayName, auth, Instant.now()));
    }

    public RoleContextRecord addRole(UUID projectId, String roleId, String name) throws IOException {
        ensureUniqueRole(projectId, roleId);
        return contextStore.saveRole(new RoleContextRecord(
                UUID.randomUUID(), projectId, roleId, name, Instant.now()));
    }

    public TenantContextRecord addTenant(UUID projectId, String tenantId, String name) throws IOException {
        ensureUniqueTenant(projectId, tenantId);
        return contextStore.saveTenant(new TenantContextRecord(
                UUID.randomUUID(), projectId, tenantId, name, Instant.now()));
    }

    public ResourceContextRecord addResource(
            UUID projectId,
            String resourceId,
            String resourceType,
            String ownerPrincipalId,
            String tenantId,
            String state
    ) throws IOException {
        ensureUniqueResource(projectId, resourceId);
        if (present(ownerPrincipalId) && findPrincipal(projectId, ownerPrincipalId) == null) {
            throw new IllegalArgumentException("ownerPrincipalId references an unknown project principal");
        }
        if (present(tenantId) && findTenant(projectId, tenantId) == null) {
            throw new IllegalArgumentException("tenantId references an unknown project tenant");
        }
        return contextStore.saveResource(new ResourceContextRecord(
                UUID.randomUUID(), projectId, resourceId, resourceType,
                ownerPrincipalId, tenantId, state, Instant.now()));
    }

    public AuthorizationExpectationRecord addExpectation(
            UUID projectId,
            UUID targetId,
            String endpoint,
            String action,
            String principalId,
            String roleId,
            String tenantId,
            String resourceId,
            String expectedDecision,
            String rationale
    ) throws IOException {
        workspace.findTarget(projectId, targetId);
        if (findPrincipal(projectId, principalId) == null) {
            throw new IllegalArgumentException("principalId references an unknown project principal");
        }
        if (present(roleId) && findRole(projectId, roleId) == null) {
            throw new IllegalArgumentException("roleId references an unknown project role");
        }
        if (present(tenantId) && findTenant(projectId, tenantId) == null) {
            throw new IllegalArgumentException("tenantId references an unknown project tenant");
        }
        if (present(resourceId) && findResource(projectId, resourceId) == null) {
            throw new IllegalArgumentException("resourceId references an unknown project resource");
        }
        boolean endpointKnown = workspace.listInventory(projectId).stream()
                .anyMatch(record -> record.targetId().equals(targetId)
                        && (record.canonicalPath().equals(endpoint) || record.rawPath().equals(endpoint)));
        if (!endpointKnown) throw new IllegalArgumentException("endpoint is not present in the selected target inventory");

        ActionType actionType = parse(ActionType.class, action, "action");
        AuthorizationDecision decision = parse(AuthorizationDecision.class, expectedDecision, "expectedDecision");

        return contextStore.saveExpectation(new AuthorizationExpectationRecord(
                UUID.randomUUID(), projectId, targetId, endpoint, actionType,
                principalId, roleId, tenantId, resourceId, decision, rationale, Instant.now()));
    }

    public List<PrincipalContextRecord> principals(UUID projectId) throws IOException {
        return contextStore.listPrincipals(projectId);
    }

    public List<RoleContextRecord> roles(UUID projectId) throws IOException {
        return contextStore.listRoles(projectId);
    }

    public List<TenantContextRecord> tenants(UUID projectId) throws IOException {
        return contextStore.listTenants(projectId);
    }

    public List<ResourceContextRecord> resources(UUID projectId) throws IOException {
        return contextStore.listResources(projectId);
    }

    public List<AuthorizationExpectationRecord> expectations(UUID projectId) throws IOException {
        return contextStore.listExpectations(projectId);
    }

    private PrincipalContextRecord findPrincipal(UUID projectId, String id) throws IOException {
        if (!present(id)) return null;
        return contextStore.listPrincipals(projectId).stream()
                .filter(record -> record.principalId().equals(id.strip())).findFirst().orElse(null);
    }

    private RoleContextRecord findRole(UUID projectId, String id) throws IOException {
        if (!present(id)) return null;
        return contextStore.listRoles(projectId).stream()
                .filter(record -> record.roleId().equals(id.strip())).findFirst().orElse(null);
    }

    private TenantContextRecord findTenant(UUID projectId, String id) throws IOException {
        if (!present(id)) return null;
        return contextStore.listTenants(projectId).stream()
                .filter(record -> record.tenantId().equals(id.strip())).findFirst().orElse(null);
    }

    private ResourceContextRecord findResource(UUID projectId, String id) throws IOException {
        if (!present(id)) return null;
        return contextStore.listResources(projectId).stream()
                .filter(record -> record.resourceId().equals(id.strip())).findFirst().orElse(null);
    }

    private void ensureUniquePrincipal(UUID projectId, String id) throws IOException {
        if (findPrincipal(projectId, id) != null) throw new IllegalArgumentException("principalId already exists");
    }

    private void ensureUniqueRole(UUID projectId, String id) throws IOException {
        if (findRole(projectId, id) != null) throw new IllegalArgumentException("roleId already exists");
    }

    private void ensureUniqueTenant(UUID projectId, String id) throws IOException {
        if (findTenant(projectId, id) != null) throw new IllegalArgumentException("tenantId already exists");
    }

    private void ensureUniqueResource(UUID projectId, String id) throws IOException {
        if (findResource(projectId, id) != null) throw new IllegalArgumentException("resourceId already exists");
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (!present(value)) throw new IllegalArgumentException(field + " is required");
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " is not supported", ex);
        }
    }
}
