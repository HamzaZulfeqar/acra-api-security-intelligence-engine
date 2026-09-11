# Threat Model

## Protected assets

- authorization tokens and cookies
- identity and tenant context
- captured HTTP evidence
- target configuration
- research datasets
- finding evidence chains
- local workspace state

## Primary threats

- out-of-scope request execution
- accidental destructive mutation
- target overload
- identity-context contamination
- secret leakage in logs or exports
- false authorization conclusions caused by dynamic fields
- false positives caused by soft-403 responses
- experiment contamination caused by session expiry or context drift

## Required mitigations

- explicit allow/deny scope evaluation
- request and mutation budgets
- circuit breakers
- dry-run mode
- context continuity checks
- universal sanitization pipeline
- evidence-based classification
- reproducible experiment fixtures
