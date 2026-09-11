# Evidence Model

Each inferred or observed security-context property must be traceable to evidence.

```text
Evidence
- evidenceId
- source
- requestId
- location
- extractedValue
- extractionMethod
- confidence
- timestamp
```

Evidence IDs are deterministic SHA-256-derived identifiers over the evidence identity material. This provides stable references for fixtures and graph edges.

## Safety

Token evidence stores a SHA-256 token fingerprint, not the raw bearer token. Serializers apply the universal redaction boundary to headers, cookies, raw bytes, body text, and arbitrary string fields.

## Scientific limitation

Confidence values describe current engineering inference strength. They are not empirical probabilities until later calibration experiments are executed.
