#!/usr/bin/env python3
from __future__ import annotations
import subprocess, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
jar=Path(sys.argv[1])
suite=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/ui/AcraSuiteTab.java").read_text(encoding="utf-8")
panel=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/ui/S11FindingReviewPanel.java").read_text(encoding="utf-8")
adapter=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/reporting/S11BurpIssueAdapter.java").read_text(encoding="utf-8")
extension=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/ACRAExtension.java").read_text(encoding="utf-8")
scope=(ROOT/"extension/burp-extension/src/main/java/io/acra/burp/scope/ScopeConfiguration.java").read_text(encoding="utf-8")

checks={
    "review_panel_constructed":"new S11FindingReviewPanel" in suite,
    "review_panel_installed":"findingReviewPanel.install(tabs)" in suite,
    "candidate_not_confirmed_banner":"Candidate != confirmed vulnerability." in panel,
    "draft_not_published_banner":"Burp Issue Draft != published Burp issue." in panel,
    "publication_eligibility_guard":"if (!draft.publicationEligible())" in adapter,
    "adapter_not_auto_invoked":"S11BurpIssueAdapter" not in extension,
    "active_default_disabled":"10_000,false" in scope.replace(" ",""),
    "probe_active_disabled":"d.timeoutMillis(),false" in extension.replace(" ",""),
}
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise AssertionError(f"review/publication boundary failed: {failed}")

entries=subprocess.check_output(["jar","tf",str(jar)],text=True).splitlines()
required={
    "io/acra/burp/ui/AcraSuiteTab.class",
    "io/acra/burp/ui/S11FindingReviewPanel.class",
    "io/acra/burp/reporting/S11BurpIssueAdapter.class",
}
missing=required-set(entries)
if missing:
    raise AssertionError(f"shaded extension missing review-boundary classes: {sorted(missing)}")

print("SPRINT13_BURP_REVIEW_SURFACE PASS")
print("SPRINT13_BURP_PUBLICATION_ELIGIBILITY_GUARD PASS")
print("SPRINT13_BURP_NO_AUTO_PUBLICATION_WIRING PASS")
print("SPRINT13_BURP_ACTIVE_DEFAULT_DISABLED PASS")
