package io.acra.burp.traffic;
import io.acra.core.domain.observation.TrafficObservation;
import io.acra.core.engine.SecurityContextSnapshot;
public record TrafficProcessingResult(TrafficObservation observation,SecurityContextSnapshot snapshot,String sessionId) {}
