# Sprint 6 — Controlled Tenant/RBAC Live Experiment

Experiment: `EXP-S6-TENANT-RBAC-001`  
Dataset: `GT-S6-TENANT-RBAC`  
Status: **COMPLETED / CONTROLLED LOCAL**

## Execution evidence

GitHub Actions run `35878508170` completed successfully on exact Temurin JDK 21.0.12.1.

The verification lane:
- compiled core and tests with `--release 21 -Xlint:all -Werror`
- syntax-checked all S6 Python lab services
- validated the independent 10-case ground-truth contract
- started secure localhost service on port 18082
- started vulnerable localhost service on port 18081
- waited for both health endpoints
- executed the live S6 Java experiment
- stopped both local services
- uploaded the experiment JSON and lab logs as workflow evidence

## Measured controlled metrics

| Treatment | TP | TN | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Naive tenant/role baseline | 2 | 1 | 7 | 0 | 0.222222 | 1.000000 | 0.363636 |
| ACRA S6 | 2 | 8 | 0 | 0 | 1.000000 | 1.000000 | 1.000000 |

Artifact ZIP digest:
`sha256:3cc486fc55b61ba8c3f76155e7468f770dc6dab834f071ed992294fac276680e`

Committed JSON evidence:
`docs/testing/artifacts/EXP-S6-TENANT-RBAC-001.json`

## Scope

This result is intentionally narrow: controlled localhost synthetic tenant/RBAC fixtures only. It is not a
real-world accuracy claim and it does not validate the historical Burp runtime path.
