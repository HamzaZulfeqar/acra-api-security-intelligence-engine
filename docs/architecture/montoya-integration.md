# Montoya Integration Architecture

## Boundary

```text
Burp Suite
   |
   v
ACRAExtension
   |-- registers AcraHttpHandler
   |-- registers ACRA suite tab
   |-- registers unload cleanup
   v
TrafficCollector
   |
   v
TrafficIntelligencePipeline
   |
   +--> acra-core SecurityContextEngine
   +--> EndpointInventory
   +--> SessionCorrelationStore
   +--> ObservationStore
   +--> SecurityContextGraph
```

`acra-core` contains no Montoya dependency. The adapter translates current Montoya request/response interfaces into immutable core `HttpTransaction` records.

## Passive versus active boundary

`AcraHttpHandler` only observes traffic and always returns the original Burp message unchanged. Sprint 2 defines `ActiveRequestExecutor` and scanner-integration contracts but wires the default executor to `DisabledActiveRequestExecutor`.

No passive observation is a vulnerability finding.

## Lifecycle

`ACRAExtension.initialize()` performs component construction and registration only. Unload closes the Swing refresh timer and registration handles. Security-context extraction lives in the traffic/core services, not the bootstrap class.

## Compatibility

See ADR-0006. The repository targets Montoya API `2026.7`, with Burp `2026.7.3` Stable as the release-promotion runtime gate.
