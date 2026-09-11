# Security Policy

ACRA must only perform active authorization testing against explicitly authorized targets and within configured scope, rate, concurrency, and mutation limits.

Core security principles:

- deny out-of-scope active testing
- preserve identity-context separation
- protect tokens, cookies, authorization headers, and other secrets
- redact or hash sensitive material in exports
- support dry-run and emergency stop controls
- avoid treating HTTP status alone as an authorization conclusion
- require evidence correlation for findings

Detailed controls are specified under `docs/security/`.
