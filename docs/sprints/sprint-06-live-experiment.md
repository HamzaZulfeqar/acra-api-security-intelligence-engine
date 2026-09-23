# Sprint 6 — Controlled Tenant/RBAC Live Experiment

Experiment: `EXP-S6-TENANT-RBAC-001`  
Dataset: `GT-S6-TENANT-RBAC`  
Scope: controlled localhost ACRA-Lab only

## Purpose

This experiment executes the secure and deliberately vulnerable S6 localhost fixtures against independently
declared tenant/RBAC ground truth. It compares:

1. a deliberately simple baseline based on tenant inequality and privileged role-name comparison; and
2. ACRA S6 effective policy reasoning using tenant scope, role assignments, permissions, delegation, shared/global
   scope and explicit default deny.

## Evidence discipline

Metrics are not written into this document until a live CI campaign has executed. The live suite emits an immutable
JSON result artifact containing each case, observed HTTP status, policy expectation, baseline prediction, ACRA
prediction and TP/TN/FP/FN-derived metrics.

The synthetic lab maps 2xx to ALLOW and explicit lab denial responses to DENY for this controlled fixture only.
This is not a general ACRA production rule and does not replace semantic/evidence correlation.

## Limitations

- localhost synthetic services only
- unsigned synthetic lab tokens
- no real-world target
- no real-world accuracy claim
- no Burp-runtime claim
