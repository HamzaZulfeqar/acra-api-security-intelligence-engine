# Sprint 2 Test Plan

## Local executable gates

1. Sprint 1 regression suite.
2. Sprint 2 adapter and passive-intelligence suite.
3. Security-source and redaction checks.
4. Architecture boundary checks.
5. Direct local ACRA-Lab integration experiment.
6. 100/1,000/10,000 traffic performance baseline.

Run:

```bash
./scripts/verify-sprint2.sh
```

## Burp release gate

The following tests require an actual Burp Suite `2026.7.3` Stable installation and the Maven-built extension artifact:

- TEST-MONTOYA-001 extension loads without exception.
- TEST-MONTOYA-002 request event reaches ACRA.
- TEST-MONTOYA-003 response event reaches ACRA.
- TEST-MONTOYA-004 ACRA transaction is created.
- TEST-MONTOYA-005 URI intelligence appears in the Traffic/Context views.
- TEST-MONTOYA-006 identity/session correlation is visible.
- TEST-MONTOYA-007 tenant evidence is correlated.
- TEST-MONTOYA-008 resource evidence is correlated.
- TEST-MONTOYA-009 evidence-backed global graph is updated.
- TEST-MONTOYA-010 credential material is absent from persisted/exportable evidence.

Until these are executed in Burp, the Sprint 2 runtime capability is `UNVERIFIED` at evidence Level 3.
