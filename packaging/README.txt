ACRA Sprint 10 Standalone Distribution

Standalone startup
==================
Requirements:
- Java 21 or newer.

Windows:
  acra-standalone.bat

Linux/macOS:
  chmod +x acra-standalone.sh
  ./acra-standalone.sh

Default management URL:
  http://127.0.0.1:8787/

The standalone application is the primary product path.
Burp Suite is NOT required.

Optional Burp adapter
=====================
The bundle also contains:
  acra-burp-extension.jar

This adapter is optional. Load it manually as a Java extension in Burp Suite.
The real Burp desktop runtime/load/UI remains a separate validation lane unless
explicitly verified.

Standalone Bridge workflow
==========================
1. Start standalone ACRA.
2. Create/select a project and authorized target.
3. In Burp's ACRA tab, open "Standalone Bridge".
4. Keep the bridge URL on loopback (default http://127.0.0.1:8787/).
5. Enter the standalone project ID and target ID.
6. Select an observed Burp transaction.
7. Probe, then explicitly Send Selected Transaction.

There is no automatic traffic forwarding. The bridge refuses non-loopback
standalone management URLs. Standalone target-scope validation and evidence
redaction remain authoritative.

Controlled active validation
============================
Sprint 10 Phase 7 standalone active validation is deliberately restricted to:
- loopback targets;
- LAB + CONTROLLED_LAB target declaration;
- explicit READ expectations;
- GET/HEAD/OPTIONS;
- explicit confirmation;
- transient tested and known-ALLOW positive-control authorization values;
- existing ACRA Core scope/consent/budget/concurrency/rate/kill-switch gates.

This bundle does not provide an unrestricted network scanner.

Integrity
=========
Verify the JAR hashes against SHA256SUMS.txt before use.
