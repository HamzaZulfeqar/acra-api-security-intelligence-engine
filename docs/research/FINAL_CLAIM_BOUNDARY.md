# ACRA Final Claim Boundary

This file defines what the frozen Sprint 13 evidence supports and what it does not support.

| Area | Supported statement | Prohibited / unsupported extrapolation |
|---|---|---|
| Held-out A7 | On the frozen 16-case held-out synthetic corpus, A7 measured TP=8/TN=3/FP=5/FN=0. | A7 is production-accurate or generalizes to arbitrary APIs. |
| Dimension discovery | On the frozen 32-case S12+S13 corpus, automatic dimension discovery measured 31/32 correct. | 96.875% is a production or population accuracy estimate. |
| Configured policy | On the 24-case internal configured-policy evaluation, G1 measured 8 TP and 16 TN with no FP/FN. | ACRA automatically learns correct organizational policy. |
| Base-rate stress | On the 96-case negative-heavy corpus, G1 measured TP=8/TN=49/FP=39/FN=0. | Those rates represent production prevalence or SOC alert rates. |
| Uncertainty governance | On the 64-case evaluation, expected disposition was 64/64; actionable FP=0 and escalation coverage=1.0. | Review load, precision or recall is representative of a production SOC. |
| Framework normalization | Frozen normalization restored 64/64 dimensions and dispositions on the tested snapshot representations. | Arbitrary serialization/framework compatibility. |
| Real framework runtimes | Controlled localhost FastAPI, Flask, Express and Spring Boot evaluation measured 64/64 expected dimensions/dispositions. | Arbitrary framework versions, middleware stacks, gateways or production deployment compatibility. |
| Real Burp/Montoya | ACRA loaded in real Burp Community 2026.7.3, received real Proxy callbacks and processed controlled localhost traffic; post-freeze contexts were 2/2 in two runs. | Arbitrary Burp-version compatibility or production scanner accuracy. |
| Review/publication | The packaged review surface and publication-eligibility guard are present; no automatic issue-publication wiring was observed in the initialization path. | Confirmed real-world vulnerability publication has been validated. |
| Active execution | Active execution remained disabled in Phase 7. | Active scanning safety/effectiveness has been proven. |
| External target | No Phase 8 external-target experiment was performed before this freeze. | Any external-target, production-safety, production-effectiveness, or independent real-world validation claim. |
| Novelty | The repository contains a documented project design and measured internal research program. | Patentability, academic novelty, market superiority or scientific state-of-the-art superiority without separate evidence. |

## Mandatory wording discipline

Use qualifiers such as:
- controlled;
- synthetic;
- localhost;
- internally authored;
- configured policy;
- exact tested runtime/version;
- candidate;
- review-required.

Do not convert:
- candidate into confirmed vulnerability;
- internal score into CVSS;
- test-fixture metrics into population estimates;
- configured policy into learned organizational truth;
- local runtime success into production safety;
- absence of observed failures into proof of absence.

## Phase 8 status

**NOT PERFORMED.**

A future external-target experiment requires explicit authorization, independent scope definition and a new experiment
identifier after this freeze.
