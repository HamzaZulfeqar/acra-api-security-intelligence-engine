#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
VERSION=(ROOT/"VERSION").read_text(encoding="utf-8").strip()
if VERSION!="0.3.0":
    raise SystemExit(f"stable packager requires VERSION=0.3.0, got {VERSION}")

OUT=ROOT/"build/release"
STAGE=OUT/f"acra-{VERSION}"

def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()

def git(*args: str) -> str:
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True).strip()

def copy(rel: str) -> None:
    src=ROOT/rel
    if not src.is_file():
        raise AssertionError(f"required release file missing: {rel}")
    dst=STAGE/rel
    dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(src,dst)

def main() -> None:
    jar=ROOT/f"extension/burp-extension/target/acra-burp-extension-{VERSION}.jar"
    if not jar.is_file():
        raise AssertionError(f"built shaded extension JAR missing: {jar}")

    if OUT.exists():
        shutil.rmtree(OUT)
    STAGE.mkdir(parents=True)

    shutil.copyfile(jar,STAGE/jar.name)

    for rel in [
        "README.md","LICENSE","NOTICE","SECURITY.md","CHANGELOG.md",
        "docs/release/RELEASE_0.3.0.md",
        "docs/research/FINAL_CLAIM_BOUNDARY.md",
        "docs/research/FINAL_EVIDENCE_MANIFEST.json",
        "docs/research/FINAL_REPRODUCIBILITY.md",
        "release/promotion-decision.json",
    ]:
        copy(rel)

    manifest={
        "schemaVersion":"acra-stable-release-manifest-v1",
        "product":"ACRA API Access Control & Routing Auditor",
        "version":VERSION,
        "releaseStatus":"STABLE",
        "sourceCommit":git("rev-parse","HEAD"),
        "license":{"spdx":"Apache-2.0","licenseFile":"LICENSE","noticeFile":"NOTICE"},
        "javaRelease":21,
        "montoyaApi":"2026.7",
        "validatedBurpBoundary":"Community Edition 2026.7.3 controlled localhost",
        "activeExecutionDefault":"DISABLED",
        "automaticIssuePublication":"NOT_WIRED",
        "externalTargetValidation":"NOT_PERFORMED_BEFORE_SPRINT13_FREEZE",
        "publicReleaseEligible":True,
        "files":[],
    }
    for p in sorted(x for x in STAGE.rglob("*") if x.is_file()):
        manifest["files"].append({
            "path":p.relative_to(STAGE).as_posix(),
            "sha256":sha256(p),
            "bytes":p.stat().st_size,
        })

    stage_manifest=STAGE/"release-manifest.json"
    stage_manifest.write_text(json.dumps(manifest,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    shutil.copyfile(stage_manifest,OUT/"release-manifest.json")

    bundle=OUT/f"acra-{VERSION}.zip"
    with zipfile.ZipFile(bundle,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in sorted(x for x in STAGE.rglob("*") if x.is_file()):
            rel=(Path(STAGE.name)/p.relative_to(STAGE)).as_posix()
            info=zipfile.ZipInfo(rel,date_time=(1980,1,1,0,0,0))
            info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=(0o100644 & 0xFFFF)<<16
            z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)

    sums=[]
    for p in [STAGE/jar.name,OUT/"release-manifest.json",bundle]:
        sums.append(f"{sha256(p)}  {p.name}")
    (OUT/"SHA256SUMS").write_text("\n".join(sums)+"\n",encoding="utf-8")

    print(f"SPRINT15_STABLE_PACKAGE PASS version={VERSION}")
    print(f"SPRINT15_STABLE_JAR {jar.name} sha256={sha256(STAGE/jar.name)}")
    print(f"SPRINT15_STABLE_BUNDLE {bundle.name} sha256={sha256(bundle)}")
    print("SPRINT15_STABLE_LICENSE PASS spdx=Apache-2.0")
    print("SPRINT15_PUBLIC_RELEASE_ELIGIBLE true")

if __name__=="__main__":
    main()
