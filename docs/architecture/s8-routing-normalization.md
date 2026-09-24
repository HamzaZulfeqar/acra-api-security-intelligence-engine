# Sprint 8 Routing Normalization Architecture

## Existing layer reused

Sprint 8 builds on:
- `DefaultUriExtractor` for raw/decoded/normalized/canonical URI representations;
- `RouteTemplateEngine` for route syntax/canonical templates;
- `RouteEquivalenceEngine` for syntactic/canonical/family comparison.

Those components remain the only route-template/equivalence engines.

## New evidence-backed layer

```
RouteStageObservation
  stage + path + source + evidence
        ↓
RouteNormalizationAnalyzer
        ↓
existing RouteEquivalenceEngine
        ↓
RouteNormalizationTransition
        ↓
RouteNormalizationTrace
```

Processing order is fixed:

```
RAW_URI → PROXY → GATEWAY → FRAMEWORK → APPLICATION
```

A trace may omit stages. The analyzer compares known observations in order, but a comparison crossing a missing
intermediary stage is marked `INCONCLUSIVE` for attribution even when the endpoint representations appear equal.

## Divergence taxonomy

- `NONE`: exact path equality at adjacent observed stages.
- `REPRESENTATION_CHANGE`: different strings with canonical equivalence.
- `FAMILY_VARIATION`: same static route family but non-identical canonical representation.
- `CANONICAL_DIVERGENCE`: directly adjacent evidence resolves to different route structures.
- `INCONCLUSIVE`: attribution cannot be established, including stage gaps.

These are routing observations. None is a vulnerability verdict.

## Provenance and security

Every populated stage requires at least one evidence identifier. Stage values are path-only; query and fragment
data are rejected at this boundary to avoid conflating routing with secret-bearing query state.

The analyzer does not infer proxy, gateway, framework or application behavior from product names, headers,
common defaults or heuristics. Later phases may import evidence from controlled fixtures or supported connectors,
but unknown stages remain unknown.
