# EXP-S6-TENANT-RBAC-001

Status: SOFTWARE FIXTURES IMPLEMENTED / LIVE CAMPAIGN NOT YET MEASURED

## Research question

Can explicit tenant membership, role hierarchy, effective permissions, delegation, shared/global scope and
policy precedence reduce false-positive tenant/RBAC classifications compared with naive tenant/role comparison
on the controlled ACRA-Lab dataset?

## Ground truth

`lab/ground-truth/GT-S6-TENANT-RBAC.json` is defined before live experiment results.

It includes same-tenant, cross-tenant deny, global-admin, delegated-admin, shared-resource, inherited-permission,
low-role privileged action and unresolved-policy-conflict controls.

## Current evidence

The S6 Java policy engine and planning-advisor tests execute synthetic labelled policy fixtures. The secure and
vulnerable localhost ACRA-Lab source now exposes S6 report and admin-export controls.

No live S6 localhost campaign has been executed in this checkpoint, therefore TP/TN/FP/FN, precision, recall and
F1 remain **NOT MEASURED** here.
