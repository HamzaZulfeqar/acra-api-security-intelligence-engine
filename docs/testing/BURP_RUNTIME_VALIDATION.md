# Burp Runtime Validation

## Current verified status — Sprint 13 Phase 7

The latest controlled real-Burp validation is complete.

Canonical strengthened evidence:
- Burp Suite Community Edition 2026.7.3;
- official JAR SHA-256 `c8262dc5426f38bedc490d66c5d21b6ff77d6dc6d85cefe6a66c882690134069`;
- Montoya compile contract 2026.7;
- development workflow `36179678105`;
- development freeze `4bb7b952fb5cd0692efc6153d51b6885a7d38f9d`;
- final post-freeze workflow `36180569483`;
- measured head `e24a91297bf33bfe18a47453e8c191432b496b20`.

Measured final gate:
- real Montoya initialization: PASS;
- real Burp Proxy request callbacks: PASS;
- real Burp Proxy response callbacks: PASS;
- passive ACRA processing: PASS;
- post-freeze context reconstruction: 2/2 on each of two runs;
- semantic repeatability: PASS;
- secret-exclusion: PASS;
- review/publication boundary: PASS;
- active execution default: DISABLED.

This is controlled localhost/headless runtime evidence only.

The historical Sprint 2 runbook is preserved below for provenance.

---

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
