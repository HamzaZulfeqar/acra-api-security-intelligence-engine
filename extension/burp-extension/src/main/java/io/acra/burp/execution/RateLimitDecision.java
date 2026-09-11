package io.acra.burp.execution;
public record RateLimitDecision(boolean allowed,String reason) {}
