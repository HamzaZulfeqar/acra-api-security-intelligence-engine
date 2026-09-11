# EXP-RESP-001 — Response Semantic Stability

**State:** COMPLETED_CONTROLLED  
**Result:** PASS for selected false-positive fixtures

Validated fixtures:

- HTTP 200 body containing application-level `Access denied` is classified `ERROR_LIKE`.
- Same resource with reordered JSON and changed timestamp/request-id fields is recognized as the same semantic resource by the Sprint 3 matcher.

No authorization verdict is derived from either result.
