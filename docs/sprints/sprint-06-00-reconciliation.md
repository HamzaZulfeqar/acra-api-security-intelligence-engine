# Sprint 6 — Pre-Implementation Reconciliation

Date: 2026-09-23  
Branch: `s6-tenant-rbac`  
Immutable S5 base: `89ceb1eec0aca7ebb2c5db60b4bfce43251d0f84`  
Sprint 5 decision: SOFTWARE COMPLETE  
Sprint 6 decision at intake: IN PROGRESS

## Reuse map

Sprint 6 must reuse the existing `Principal`, `Role`, `Tenant`, `Resource`, `Action`,
`AuthorizationContext`, `AuthorizationContextAssessment`, `AuthorizationMatrix`,
`EvidenceReferenceValidator`, `SecurityContextGraph`, S4 differential execution,
S5 FindingCandidate/risk/reporting and existing serialization/redaction components.

No duplicate principal, role, tenant, resource, evidence, graph, finding or serializer architecture is authorized.

## Source-driven gap classification

| Area | Baseline at S6 start | Classification | S6 action |
|---|---|---|---|
| Principal/Role/Tenant/Resource | Existing S1-S5 records | EXISTS | Reuse |
| S5 tenant assessment | Typed policy assessment exists | EXISTS / LIMITED | Feed richer effective-policy evidence |
| AuthorizationMatrix | Principal/role/tenant/resource/action/endpoint tuple exists | EXISTS / LIMITED | Preserve; add effective-policy view later |
| Graph | Principal/role/tenant/resource relationships exist | EXISTS / EXTEND | Add policy/RBAC relation types |
| Tenant membership | No explicit multi-membership model | MISSING | Implement |
| Role assignment scope | Single Role in AuthorizationContext | MISSING | Implement multi-role assignments |
| Role hierarchy | No inheritance resolver | MISSING | Implement deterministic hierarchy |
| Permissions | No first-class permission model | MISSING | Implement |
| Role→Permission mapping | No first-class mapping | MISSING | Implement |
| Explicit allow/deny rules | No reusable S6 policy-rule model | MISSING | Implement |
| Delegated authorization | Existing S5 tenant flags only | PARTIAL | Implement first-class delegation |
| Global/shared scope | Represented only indirectly | PARTIAL | Implement explicit scope taxonomy |
| Effective permission resolver | None | MISSING | Later S6 slice |
| Effective expected-decision resolver | S4/S5 expected decision sources exist | PARTIAL | Add policy-derived resolver without replacing precedence model |
| Tenant isolation engine | S5 typed tenant assessment exists | PARTIAL | Expand with membership/scope/delegation evidence |
| Advanced RBAC engine | BFLA exists; no effective-role policy engine | MISSING | Implement |
| Finding integration | S5 FindingCandidate exists | EXISTS | Extend dimensions, no second finding engine |
| Severity | S5 internal risk evaluator exists | EXISTS | Reuse explicit impact inputs |
| Research lab | Existing local ACRA-Lab | EXISTS / EXTEND | Add labelled tenant/RBAC cases |
| Burp runtime debt | Historical S2/S3 debt remains | DEFERRED | Do not rewrite as Sprint 6 PASS |

## Initial S6 implementation slice

The first authorized slice implements only the policy-domain foundation:
explicit authorization scope, tenant membership, multi-role assignment, role inheritance,
permission, role-permission assignment, allow/deny rule, delegation, deterministic policy snapshot,
role hierarchy resolution and graph taxonomy extensions.

It does not yet claim complete effective permission resolution, tenant-isolation detection,
advanced RBAC FindingCandidate integration, research accuracy or Sprint 6 completion.
