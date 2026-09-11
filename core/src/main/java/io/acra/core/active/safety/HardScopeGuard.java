package io.acra.core.active.safety;

import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.domain.http.HttpRequest;
import java.util.Locale;

public final class HardScopeGuard {
    public ValidationDecision evaluate(String expectedProjectId, SecurityTest test, HttpRequest request) {
        if (test == null || request == null) return ValidationDecision.invalid("test and request are required");
        TargetDescriptor target = test.target();
        if (expectedProjectId == null || !target.projectId().equals(expectedProjectId)) return ValidationDecision.blocked("project outside active scope");
        if (!request.scheme().equalsIgnoreCase(target.scheme())) return ValidationDecision.blocked("scheme outside active scope");
        if (!request.host().equalsIgnoreCase(target.host())) return ValidationDecision.blocked("host outside active scope");
        if (request.port() != target.port()) return ValidationDecision.blocked("port outside active scope");
        if (!target.allowedMethods().contains(request.method()) || !test.safetyPolicy().allowedMethods().contains(request.method())) {
            return ValidationDecision.blocked("method outside active scope");
        }
        String path = request.path();
        if (!safePath(path)) return ValidationDecision.blocked("ambiguous or unsafe active path");
        boolean allowed = target.allowedPathPrefixes().stream().anyMatch(prefix -> boundaryMatch(path, prefix));
        if (!allowed) return ValidationDecision.blocked("path outside active scope");
        if (!request.host().equalsIgnoreCase(test.endpoint().host())) return ValidationDecision.blocked("endpoint host mismatch");
        return ValidationDecision.allowed();
    }

    public ValidationDecision evaluate(SecurityTest test, HttpRequest request) {
        return evaluate(test == null ? null : test.target().projectId(), test, request);
    }

    private static boolean safePath(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("%2e") || lower.contains("%2f") || lower.contains("%5c")) return false;
        for (String segment : path.split("/", -1)) if (segment.equals(".") || segment.equals("..")) return false;
        return true;
    }

    private static boolean boundaryMatch(String path, String configuredPrefix) {
        if ("/".equals(configuredPrefix)) return true;
        String prefix = configuredPrefix.endsWith("/")
                ? configuredPrefix.substring(0, configuredPrefix.length() - 1)
                : configuredPrefix;
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
