package io.acra.burp.execution;
public record ExecutionHandle(String executionId,State state,String message) { public enum State { BLOCKED, QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED } }
