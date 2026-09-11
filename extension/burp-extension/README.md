# ACRA Burp Extension Adapter

Current repository candidate: `v0.3.0-rc1`.

The extension remains a thin adapter over `acra-core`. It observes HTTP traffic, maps it to ACRA transactions, runs passive context/reconnaissance intelligence and presents observation views. Core authorization logic must not be moved into Burp-specific classes.

Sprint 3 source includes tabs for Overview, Traffic, Contexts, Endpoints, Parameters, Identities, Tenants, Resources, Routes, Context Graph and Configuration.

## Runtime status

Source compiles against the preserved local Montoya contract harness, but real Burp loading/UI/runtime behavior is UNVERIFIED in this environment. Use the runtime validation procedure in `docs/testing/BURP_RUNTIME_VALIDATION.md` and the Sprint 3 experiment record before promoting the candidate.

Active request execution remains disabled by default with zero request/mutation budgets and kill switch engaged.
