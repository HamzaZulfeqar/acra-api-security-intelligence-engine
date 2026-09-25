# GitHub Repository Hardening

This file tracks repository-admin settings required for the professional public-repository posture.

## Current automated status after repository-side Sprint 16 implementation

- repository visibility: public;
- default branch: `main`;
- stable release: `v0.3.0`;
- `main` branch protection: **pending owner admin setting**;
- CodeQL: configured;
- Gitleaks: configured;
- Dependency Review workflow: configured;
- GitHub Dependency Graph: **pending owner admin setting** before native Dependency Review is meaningful.

Repository-side onboarding validation is complete at GitHub Actions run `36202814898`.

Tracking issue: #6.

The connected GitHub automation used by Sprint 16 does not have repository-administration mutation access, so the two
settings below must be enabled by the repository owner in GitHub Settings.

## 1. Protect `main`

GitHub documents protected branches and required status checks at:

https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches

Recommended minimum rule for `main`:

- [ ] require a pull request before merging;
- [ ] require status checks before merging;
- [ ] require the branch to be up to date before merging;
- [ ] require conversation resolution before merging;
- [ ] prevent bypass of the rule unless emergency administration requires it.

Recommended required checks:
- [ ] **Build and Test**
- [ ] **Public Onboarding Smoke**
- [ ] **CodeQL Analysis**
- [ ] **Secret Scanning**
- [ ] **Dependency Review** — only after Dependency Graph is enabled and the check performs real analysis.

Do not require historical sprint-specific workflows as permanent branch checks unless they are intentionally maintained
for all future development.

## 2. Enable Dependency Graph

GitHub's current repository procedure:

1. open the repository on GitHub;
2. open **Settings**;
3. under **Security and quality**, open **Advanced Security**;
4. next to **Dependency Graph**, choose **Enable**.

Official reference:
https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/enable-dependency-graph

After enabling:
- confirm the graph populates;
- open/re-run a pull request;
- confirm **Dependency Review** performs an actual dependency analysis rather than the prior unavailable-feature path.

GitHub documents Dependency Review at:
https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review

## 3. Re-verify after settings changes

After both settings are enabled:

- [ ] confirm GitHub reports `main` as protected;
- [ ] confirm direct pushes are constrained by the selected rule;
- [ ] confirm a PR cannot merge while required checks are failing;
- [ ] confirm Dependency Review reports a real analyzed result;
- [ ] close the Sprint 16 repository-admin tracking issue.

## Security rationale

Branch protection prevents a future direct write from bypassing review and CI. Required status checks can block merges
until the configured checks succeed.

Dependency Graph enables GitHub to understand repository dependencies and is a prerequisite for meaningful native
Dependency Review.
