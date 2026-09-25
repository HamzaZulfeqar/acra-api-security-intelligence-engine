# ACRA Release Promotion

**Stage:** Sprint 15 — Release Promotion  
**Branch:** `s15-release-promotion`  
**Source RC:** `0.3.0-rc1`  
**Source hardening head:** `88f2a75f190f6e8853609db2dc9d73929434ad0d`

## Current state

**PREPARED / BLOCKED — no public release has been authorized.**

Sprint 14 established a technically hardened release candidate. Release Promotion now governs the transition from that
candidate to a Git tag / GitHub Release.

Two explicit decisions are still required:

1. software license;
2. version/publication target.

No release tag or GitHub Release may be created until both are resolved.

## Promotion choices

### Version path A — publish the existing release candidate

Keep:

`0.3.0-rc1`

and publish it as a GitHub **pre-release**.

This preserves the current candidate identity and is appropriate when additional user/runtime feedback is still expected.

### Version path B — promote to stable

Promote:

`0.3.0-rc1` → `0.3.0`

This requires updating:
- `VERSION`;
- parent Maven version;
- core parent version;
- Burp-extension parent version;
- release notes/changelog;
- package/artifact names.

The full Sprint 14 hardening/security gates must then be rerun against the stable-version commit.

## License decision

The repository currently grants no explicit software reuse/distribution license.

Common choices for this type of project include:

| License | Practical effect |
|---|---|
| Apache-2.0 | Permissive; includes an explicit patent grant and notice requirements. |
| MIT | Very short permissive license; broad reuse with copyright/license notice preservation. |
| GPL-3.0-or-later | Strong copyleft; redistributed derivative works generally must remain GPL-compatible/open under its terms. |

The project owner must explicitly select the license. Sprint 15 will not infer one from project type.

## Promotion gates

Promotion to READY requires:

- Sprint 14 release-hardening evidence remains valid;
- Sprint 13 research remains frozen;
- license decision is explicit and `LICENSE` contains the selected license;
- version decision is explicit;
- version contract is internally consistent;
- release notes match the selected target;
- CodeQL and Gitleaks pass on the promotion commit;
- release bundle/checksums are regenerated from the promotion commit;
- no unsupported external-target/production claims are introduced.

## Publication boundary

Even after release promotion, the frozen research claim boundary remains unchanged:

- Phase 8 external-target validation: NOT PERFORMED;
- production accuracy: NOT ESTABLISHED;
- production safety: NOT ESTABLISHED;
- arbitrary framework/Burp compatibility: NOT ESTABLISHED.

A public software release is a packaging/distribution event, not new validation evidence.

## Machine-readable state

See:

`release/promotion-decision.json`

The promotion verifier intentionally passes in `PREPARED_BLOCKED` state only when publication remains disabled and the
unresolved decisions are represented honestly.

## Next transition

After explicit license + version decisions:

`PREPARED_BLOCKED → READY_FOR_PROMOTION → PUBLISHED`

The `PUBLISHED` state must record the exact tag, release URL/identifier and final artifact hashes.
