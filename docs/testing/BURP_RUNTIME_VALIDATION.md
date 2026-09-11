# Sprint 2 Burp Runtime Validation Runbook

This runbook is the mandatory promotion gate from `v0.2.0-rc1` to `v0.2.0`.

## Preconditions

- Java 21 available.
- Apache Maven 3.9.16 available.
- Network or repository mirror can resolve `net.portswigger.burp.extensions:montoya-api:2026.7`.
- Burp Suite Professional/Community 2026.7.3 Stable installed.
- ACRA-Lab available locally.
- Only synthetic lab credentials are used.

## Build gate

```bash
mvn -version
mvn clean package
sha256sum extension/burp-extension/target/acra-burp-extension-0.2.0-rc1.jar
```

Record Maven, Java, OS, Montoya, Burp, build timestamp and repository commit if Git metadata is available.

## Level 3 gate

1. Start Burp 2026.7.3 Stable.
2. Load the Maven-built extension JAR.
3. Confirm clean initialization and the ACRA suite tab.
4. Verify Overview, Traffic, Contexts, Endpoints and Configuration.
5. Confirm unload and reload are clean.

## Level 4 gate

Run `EXP-BURP-INTEGRATION-001` exactly as documented. Route a benign request to the secure ACRA-Lab service through Burp Proxy and compare the ACRA-derived context with `GT-INTEGRATION-001`.

Expected ground truth:

```text
principal=user-a
tenant=tenant-a
resource=document:1001
owner=user-a
action=READ
```

Verify graph provenance, scope isolation, and secret redaction using only synthetic test credentials.

## Promotion rule

Only after all mandatory runtime rows in `docs/testing/TEST_MATRIX.md` are supported by real evidence may `VERSION` change from `0.2.0-rc1` to `0.2.0`.
