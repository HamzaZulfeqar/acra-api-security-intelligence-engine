# ACRA Project State

## Current state — 2026-09-23

**Current sprint:** Sprint 6 — Tenant Isolation, Advanced RBAC and Policy Intelligence.  
**Decision:** S6 IN PROGRESS.  
**Working branch:** `s6-tenant-rbac`.  
**Immutable Sprint 5 base:** `89ceb1eec0aca7ebb2c5db60b4bfce43251d0f84`.  
**Sprint 5:** SOFTWARE COMPLETE and preserved as a separate reproducible checkpoint.  
**Sprint 7:** NOT STARTED.

Sprint 6 began with source-driven reconciliation before feature changes. Existing Principal, Role, Tenant,
Resource, AuthorizationContext, AuthorizationMatrix, SecurityContextGraph, S4 differential execution and
S5 evidence/finding/risk/reporting architectures remain canonical and must be reused.

### Current S6 slice

Implemented foundation:
- authorization scope taxonomy
- explicit tenant memberships
- multi-role assignments
- tenant-scoped/global role assignments
- role inheritance model
- cycle-safe role hierarchy resolver
- permission model
- role-to-permission assignment
- explicit allow/deny rule model
- delegated authorization model
- immutable deterministic authorization policy snapshot
- policy/RBAC graph relation/node taxonomy extensions
- dedicated Sprint 6 foundation test/CI lane

This is **not Sprint 6 completion**. Effective permission/decision resolution, tenant-isolation and RBAC
assessment engines, S5 FindingCandidate integration, expanded ACRA-Lab experiments, UI/report extensions
and final S6 verification remain pending.

### Historical validation boundary

Sprint 5 exact-JDK-21 verification remains the immutable predecessor evidence. Historical S2/S3 real Burp
runtime validation debt remains separate and is not promoted by Sprint 6 work.
