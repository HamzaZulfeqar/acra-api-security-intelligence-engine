package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTokenBinding;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class WorkflowAuthorizationResolver {

    public WorkflowAuthorizationResolution resolve(
            WorkflowPolicySnapshot workflowPolicy,
            AuthorizationPolicySnapshot authorizationPolicy,
            WorkflowAuthorizationRequest request) {
        if (workflowPolicy == null) throw new IllegalArgumentException("workflowPolicy required");
        if (request == null) throw new IllegalArgumentException("request required");

        List<String> reasons = new ArrayList<>();
        Set<String> evidence = new TreeSet<>(workflowPolicy.evidenceIds());
        Set<String> matchedBindings = new TreeSet<>();
        Set<String> delegationIds = new TreeSet<>();

        List<WorkflowTransitionRule> transitionMatches = workflowPolicy.transitionRules().stream()
                .filter(rule -> rule.workflowId().equals(request.workflowId()))
                .filter(rule -> rule.fromState().equals(request.fromState()))
                .filter(rule -> rule.toState().equals(request.toState()))
                .filter(rule -> rule.action().equals(request.action()))
                .filter(rule -> rule.tenantId().isBlank() || rule.tenantId().equals(request.tenantId()))
                .sorted(Comparator.comparing(WorkflowTransitionRule::ruleId))
                .toList();

        if (transitionMatches.isEmpty()) {
            reasons.add("NO_MATCHING_WORKFLOW_TRANSITION_RULE");
            return defaultResult(workflowPolicy, request, evidence, reasons);
        }

        Map<String, WorkflowTokenBinding> bindings = new TreeMap<>();
        for (WorkflowTokenBinding binding : workflowPolicy.tokenBindings()) {
            bindings.put(binding.bindingId(), binding);
        }

        List<WorkflowTransitionRule> eligible = new ArrayList<>();
        for (WorkflowTransitionRule rule : transitionMatches) {
            List<String> localReasons = new ArrayList<>();
            if (rule.terminalSource() && rule.effect() == AuthorizationRuleEffect.ALLOW) {
                localReasons.add("TERMINAL_SOURCE_ALLOW_CONFLICT");
            }
            if (!rule.requiredRoleIds().isEmpty()
                    && rule.requiredRoleIds().stream().noneMatch(request.roleIds()::contains)) {
                localReasons.add("REQUIRED_ROLE_NOT_PRESENT");
            }
            if (rule.approvalRequired() && !request.approvalProvided()) {
                localReasons.add("APPROVAL_REQUIRED");
            }
            if (rule.roleSeparationRequired() && !request.roleSeparationSatisfied()) {
                localReasons.add("ROLE_SEPARATION_REQUIRED");
            }
            if (!rule.tokenBindingId().isBlank()) {
                WorkflowTokenBinding binding = bindings.get(rule.tokenBindingId());
                if (binding == null) {
                    localReasons.add("TOKEN_BINDING_REFERENCE_MISSING");
                } else if (!bindingMatches(binding, request)) {
                    localReasons.add("TOKEN_BINDING_MISMATCH");
                } else {
                    matchedBindings.add(binding.bindingId());
                    evidence.addAll(binding.evidenceIds());
                }
            }

            if (!request.delegationId().isBlank()) {
                if (!rule.delegationAllowed()) {
                    localReasons.add("DELEGATION_NOT_ALLOWED_FOR_TRANSITION");
                } else {
                    Delegation delegation = validDelegation(authorizationPolicy, request);
                    if (delegation == null) {
                        localReasons.add("DELEGATION_INVALID_OR_EXPIRED");
                    } else {
                        delegationIds.add(delegation.delegationId());
                        evidence.addAll(delegation.evidenceIds());
                    }
                }
            }

            if (localReasons.isEmpty()) {
                eligible.add(rule);
                evidence.addAll(rule.evidenceIds());
            } else {
                localReasons.forEach(reason -> reasons.add(rule.ruleId() + ":" + reason));
            }
        }

        if (eligible.isEmpty()) {
            reasons.add("NO_ELIGIBLE_WORKFLOW_TRANSITION_RULE");
            return defaultResult(workflowPolicy, request, evidence, reasons);
        }

        Decision decision = decide(eligible, workflowPolicy.defaultDecision());
        reasons.addAll(decision.reasons());
        Set<String> ruleIds = new TreeSet<>();
        eligible.forEach(rule -> ruleIds.add(rule.ruleId()));

        return result(workflowPolicy, request, ruleIds, matchedBindings, delegationIds,
                decision.expected(), decision.state(), evidence, reasons);
    }

    private static boolean bindingMatches(WorkflowTokenBinding binding, WorkflowAuthorizationRequest request) {
        if (!binding.workflowId().equals(request.workflowId())) return false;
        if (!binding.action().equals(request.action())) return false;
        if (!binding.principalId().isBlank() && !binding.principalId().equals(request.principalId())) return false;
        if (!binding.roleId().isBlank() && !request.roleIds().contains(binding.roleId())) return false;
        if (!binding.tenantId().isBlank() && !binding.tenantId().equals(request.tenantId())) return false;
        return !request.tokenContextFingerprint().isBlank()
                && binding.tokenContextFingerprint().equals(request.tokenContextFingerprint());
    }

    private static Delegation validDelegation(
            AuthorizationPolicySnapshot authorizationPolicy,
            WorkflowAuthorizationRequest request) {
        if (authorizationPolicy == null) return null;
        return authorizationPolicy.delegations().stream()
                .filter(d -> d.delegationId().equals(request.delegationId()))
                .filter(d -> d.delegatePrincipalId().equals(request.principalId()))
                .filter(d -> d.targetTenantId().isBlank() || d.targetTenantId().equals(request.tenantId()))
                .filter(d -> d.actions().isEmpty() || d.actions().contains("*") || d.actions().contains(request.action()))
                .filter(d -> d.activeAt(request.evaluatedAt()))
                .filter(d -> d.roleId().isBlank() || request.roleIds().contains(d.roleId()))
                .findFirst()
                .orElse(null);
    }

    private static Decision decide(List<WorkflowTransitionRule> rules, AuthorizationDecision defaultDecision) {
        List<String> reasons = new ArrayList<>();
        List<WorkflowTransitionRule> explicit = rules.stream()
                .filter(WorkflowTransitionRule::hasExplicitPrecedence)
                .toList();
        if (!explicit.isEmpty()) {
            int max = explicit.stream().map(WorkflowTransitionRule::precedence)
                    .max(Integer::compareTo).orElse(0);
            Set<AuthorizationRuleEffect> effects = new HashSet<>();
            explicit.stream().filter(rule -> rule.precedence() == max)
                    .forEach(rule -> effects.add(rule.effect()));
            if (effects.size() > 1) {
                reasons.add("SAME_PRECEDENCE_ALLOW_DENY_CONFLICT");
                return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, reasons);
            }
            AuthorizationRuleEffect effect = effects.iterator().next();
            reasons.add("EXPLICIT_PRECEDENCE:" + max);
            return effect == AuthorizationRuleEffect.DENY
                    ? new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, reasons)
                    : new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, reasons);
        }

        boolean allow = rules.stream().anyMatch(rule -> rule.effect() == AuthorizationRuleEffect.ALLOW);
        boolean deny = rules.stream().anyMatch(rule -> rule.effect() == AuthorizationRuleEffect.DENY);
        if (allow && deny) {
            reasons.add("ALLOW_DENY_CONFLICT_WITHOUT_PROVEN_PRECEDENCE");
            return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, reasons);
        }
        if (deny) return new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, reasons);
        if (allow) return new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, reasons);
        return defaultDecision(defaultDecision, reasons);
    }

    private static WorkflowAuthorizationResolution defaultResult(
            WorkflowPolicySnapshot policy,
            WorkflowAuthorizationRequest request,
            Set<String> evidence,
            List<String> reasons) {
        Decision decision = defaultDecision(policy.defaultDecision(), reasons);
        return result(policy, request, Set.of(), Set.of(), Set.of(),
                decision.expected(), decision.state(), evidence, reasons);
    }

    private static Decision defaultDecision(AuthorizationDecision defaultDecision, List<String> reasons) {
        if (defaultDecision == AuthorizationDecision.DENY) {
            reasons.add("EXPLICIT_DEFAULT_DENY");
            return new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, List.of());
        }
        if (defaultDecision == AuthorizationDecision.ALLOW) {
            reasons.add("EXPLICIT_DEFAULT_ALLOW");
            return new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, List.of());
        }
        reasons.add("WORKFLOW_POLICY_INCOMPLETE");
        return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.INCOMPLETE, List.of());
    }

    private static WorkflowAuthorizationResolution result(
            WorkflowPolicySnapshot policy,
            WorkflowAuthorizationRequest request,
            Set<String> ruleIds,
            Set<String> bindingIds,
            Set<String> delegationIds,
            AuthorizationDecision expected,
            PolicyResolutionState state,
            Set<String> evidence,
            List<String> reasons) {
        String material = policy.fingerprint() + "|" + request.workflowId() + "|" + request.principalId() + "|"
                + request.tenantId() + "|" + request.resourceId() + "|" + request.action() + "|"
                + request.fromState() + "|" + request.toState() + "|" + expected + "|" + state;
        return new WorkflowAuthorizationResolution(
                "s7-workflow-" + TokenFingerprint.sha256(material).substring(0, 24),
                policy.fingerprint(),
                request.workflowId(),
                request.principalId(),
                request.tenantId(),
                request.resourceId(),
                request.action(),
                request.fromState(),
                request.toState(),
                List.copyOf(ruleIds),
                List.copyOf(bindingIds),
                List.copyOf(delegationIds),
                expected,
                request.observedDecision(),
                state,
                List.copyOf(evidence),
                reasons);
    }

    private record Decision(
            AuthorizationDecision expected,
            PolicyResolutionState state,
            List<String> reasons) { }
}
