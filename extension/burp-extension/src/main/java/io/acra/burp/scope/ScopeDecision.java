package io.acra.burp.scope;
public record ScopeDecision(boolean accepted,String reason) { public ScopeDecision { reason=reason==null?"":reason; } }
