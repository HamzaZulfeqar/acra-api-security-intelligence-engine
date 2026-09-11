# ACRA-Lab Sprint 2 baseline

Two intentionally small local HTTP APIs provide deterministic ground truth for integration work. The secure service enforces tenant/owner checks; the vulnerable service intentionally omits cross-tenant authorization checks for future controlled research. Sprint 2 uses only same-tenant benign requests to validate context reconstruction, not vulnerability exploitation.

Docker Compose is specified for reproducibility. The Sprint 2 verification environment did not provide Docker, so the same Python 3.13 standard-library servers are also executable directly.
