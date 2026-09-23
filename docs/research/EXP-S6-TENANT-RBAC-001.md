# EXP-S6-TENANT-RBAC-001

Status: **COMPLETED_CONTROLLED_LOCAL**

GitHub Actions run: `35878508170`  
Head commit measured: `c3b28e2007360075b60cbb6cc5812964ff3e844b`  
Dataset: `GT-S6-TENANT-RBAC`  
Artifact ID: `10759437669`  
Artifact ZIP digest: `sha256:3cc486fc55b61ba8c3f76155e7468f770dc6dab834f071ed992294fac276680e`  
Committed result JSON SHA-256: `0c28ba4275d9212c1fdc100e376f86664317d66521a26840f747d12f2c51898d`

## Research question

Can explicit tenant membership, role hierarchy, effective permissions, delegation, shared/global scope and
policy-aware expected decisions reduce false-positive tenant/RBAC classifications compared with a deliberately
simple tenant/role baseline on the controlled ACRA-Lab dataset?

## Method

The experiment started the secure and deliberately vulnerable localhost ACRA-Lab services in CI, then executed
ten labelled live HTTP cases. Ground truth was declared before the live campaign in
`lab/ground-truth/GT-S6-TENANT-RBAC.json`.

The baseline classified using tenant inequality and simple privileged-role-name comparison. The ACRA treatment
used the Sprint 6 effective authorization resolver with role assignments, scopes, permissions, global/shared
access, delegation and explicit default deny.

For this controlled fixture only, 2xx responses map to observed ALLOW and explicit denial responses map to
observed DENY. This is an experiment adapter, not a general production vulnerability rule.

## Measured results

| Treatment | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Naive baseline | 2 | 1 | 7 | 0 | 0.222222 | 1.000000 | 0.363636 |
| ACRA S6 policy-aware treatment | 2 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

Within this ten-case synthetic dataset, the ACRA treatment retained both deliberately vulnerable cases and
correctly treated secure cross-tenant denial, global administration, delegated administration, shared-resource
access and low-role denied access as negatives.

## Case-level evidence

- `S6-LIVE-001`: same-tenant secure access → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-002`: secure cross-tenant denial → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-003`: vulnerable cross-tenant allow against expected DENY → POSITIVE / ACRA POSITIVE
- `S6-LIVE-004`: legitimate global-admin access → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-005`: legitimate delegated access → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-006`: legitimate shared-resource access → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-007`: low-role privileged operation correctly denied → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-008`: low-role privileged operation incorrectly allowed → POSITIVE / ACRA POSITIVE
- `S6-LIVE-009`: global-admin privileged operation → NEGATIVE / ACRA NEGATIVE
- `S6-LIVE-010`: delegated privileged operation → NEGATIVE / ACRA NEGATIVE

## Defect discovered by the campaign

The first live experiment run exposed a real S6 policy defect: a SHARED-scoped permission could match a private
cross-tenant resource because shared scope was being treated as universally applicable. The resolver was changed
so SHARED scope applies only when the request is explicitly marked as a shared resource. Regression tests now
cover both private cross-tenant denial and legitimate shared-resource allow.

The successful measured run occurred only after that correction.

## Limitations

These metrics are evidence only for this controlled localhost dataset. They do not establish real-world scanner
precision, production authorization accuracy, novelty, or Burp-runtime behavior. The lab uses unsigned synthetic
tokens and intentionally small deterministic policy fixtures.
