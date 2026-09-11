package io.acra.burp.scanner;
public final class PassiveScannerBoundary implements ScannerIntegrationBoundary { @Override public boolean activeChecksEnabled(){return false;} @Override public String status(){return "DEFINED_NOT_REGISTERED";} }
