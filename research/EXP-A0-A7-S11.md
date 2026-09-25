# EXP-A0-A7 — Sprint 11 Research Dataset Readiness

State: **SOFTWARE_READY / RESEARCH_NOT_RUN**

Dataset: `lab/ground-truth/GT-S11-AUTHORIZATION-RESEARCH.json`

Ground-truth verifier: `scripts/verify-sprint11-ground-truth.py`

## Purpose

Sprint 11 closes the dataset and oracle prerequisite for the later A0-A7 comparative authorization
research campaign. It does not execute the detector ablations and does not report accuracy metrics.

The dataset is declared independently of ACRA detector output and is exercised against two
localhost ACRA-Lab modes:

- secure fixture: intended authorization policy is enforced;
- intentionally vulnerable fixture: a narrowly controlled authorization weakness is enabled for
  the registered positive case.

## Registered dimensions

| Dimension | Positive control | Negative control |
|---|---|---|
| Object authorization | S11-GT-OBJECT-POS-001 | S11-GT-OBJECT-NEG-001 |
| Tenant authorization | S11-GT-TENANT-POS-001 | S11-GT-TENANT-NEG-001 |
| RBAC authorization | S11-GT-RBAC-POS-001 | S11-GT-RBAC-NEG-001 |
| Workflow authorization | S11-GT-WORKFLOW-POS-001 | S11-GT-WORKFLOW-NEG-001 |
| Routing authorization | S11-GT-ROUTING-POS-001 | S11-GT-ROUTING-NEG-001 |
| Property authorization | S11-GT-PROPERTY-POS-001 | S11-GT-PROPERTY-NEG-001 |
| Batch authorization | S11-GT-BATCH-POS-001 | S11-GT-BATCH-NEG-001 |
| Indirect-reference authorization | S11-GT-INDIRECT-POS-001 | S11-GT-INDIRECT-NEG-001 |

Total registered cases: **16**

- positive controls: 8
- negative controls: 8
- dimensions: 8

## Oracle contract

A positive case requires:

```text
secure fixture     -> DENY
vulnerable fixture -> ALLOW
expected candidate -> true
```

A negative case requires:

```text
secure fixture decision == vulnerable fixture decision
expected candidate      == false
```

Most cases use HTTP authorization status as the oracle. Batch authorization uses an explicit
per-item JSON decision oracle so aggregate HTTP success cannot be mistaken for item-level
authorization success.

## Verification behavior

The Sprint 11 verifier:

1. checks that both Docker-facing lab servers are byte-identical to the canonical
   `lab/common/basic_api.py` implementation;
2. validates dataset identity, uniqueness, balance, per-dimension positive/negative coverage and
   the explicit NOT_MEASURED metric state;
3. starts secure and vulnerable ACRA-Lab servers on ephemeral loopback ports;
4. executes every registered case using synthetic local identity claims;
5. resolves the registered authorization oracle for both modes;
6. requires the observed decisions to equal the independent ground truth;
7. writes a minimized verification artifact under `build/s11-ground-truth/verification.json`.

The verifier does not persist a bearer token or credential.

## A0-A7 boundary

The planned experiment sequence remains:

- A0 — naive differential
- A1 — A0 + identity
- A2 — A1 + resource ownership
- A3 — A2 + tenant
- A4 — A3 + role
- A5 — A4 + workflow
- A6 — A5 + semantic evidence
- A7 — full ACRA correlation

Sprint 11 does **not** execute those configurations. Accordingly:

```text
TP        = NOT_MEASURED
TN        = NOT_MEASURED
FP        = NOT_MEASURED
FN        = NOT_MEASURED
Precision = NOT_MEASURED
Recall    = NOT_MEASURED
F1        = NOT_MEASURED
```

Measured A0-A7 results may be recorded only after a later experiment runner executes each
registered configuration against this dataset and preserves its execution evidence.

## Scope limitations

This dataset is a controlled synthetic localhost research fixture. Even after A0-A7 are executed,
results will describe this registered fixture population unless separately validated on a broader,
authorized dataset. No real-world scanner-accuracy or novelty claim follows from Sprint 11 dataset
readiness alone.
