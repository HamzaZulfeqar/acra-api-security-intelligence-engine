# First-Run Verification

Use this checklist immediately after loading ACRA v0.3.0 into Burp.

## A. Extension load

In **Extensions > Installed**:

- [ ] extension name is `ACRA`;
- [ ] extension is marked loaded;
- [ ] no fatal initialization error appears.

Expected Output message:

```text
ACRA v0.3.0 initialized in passive observation mode. Active vulnerability scanning is disabled.
```

## B. Suite tab

Confirm an **ACRA** top-level tab exists.

At minimum, verify these views:
- [ ] Overview
- [ ] Traffic
- [ ] Contexts
- [ ] Endpoints
- [ ] Parameters
- [ ] Identities
- [ ] Tenants
- [ ] Resources
- [ ] Routes
- [ ] Context Graph
- [ ] Configuration

Additional authorization/review workspaces are also installed by the extension.

## C. Safety defaults

Open **ACRA > Configuration**.

Expected:
- [ ] `Scope mode: IN_SCOPE_ONLY`
- [ ] `Active execution: DISABLED by default`
- [ ] `Active request budget: 0`
- [ ] `Mutation budget: 0`
- [ ] `Kill switch: ENGAGED`

Do not continue if the active-execution state differs unexpectedly.

## D. Authorized target scope

ACRA's normal passive collection follows Burp's suite scope.

For an application you are explicitly authorized to test:
- [ ] add the exact target to Burp scope;
- [ ] keep unrelated hosts out of scope;
- [ ] verify the request is visible in Burp Proxy history.

Official Burp scope reference:
https://portswigger.net/burp/documentation/desktop/tools/target/scope

## E. Passive observation smoke test

Send one benign request through Burp Proxy.

Then confirm:
- [ ] the ACRA Overview observation count increases;
- [ ] Traffic contains the request;
- [ ] Contexts shows the reconstructed context when evidence is available;
- [ ] Endpoints contains the observed endpoint;
- [ ] no active testing had to be enabled.

## F. Version and release identity

Repository/release:
- version: `0.3.0`;
- license: `Apache-2.0`;
- tag: `v0.3.0`.

Published release:
https://github.com/HamzaZulfeqar/acra-api-security-intelligence-engine/releases/tag/v0.3.0

## G. Claim boundary

Successful first-run verification proves that the installed extension loads and passively processes your authorized
traffic.

It does not by itself prove that a candidate is a confirmed vulnerability or that ACRA has production-wide accuracy.
