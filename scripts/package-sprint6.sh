#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
OUT="$ROOT/build/s6-final-package"
rm -rf "$OUT"
mkdir -p "$OUT"
python3 - <<'PY'
from pathlib import Path
import hashlib, json, os, shutil, subprocess, tempfile, zipfile

root=Path.cwd()
out=root/"build"/"s6-final-package"
zip_path=out/"acra-sprint-06-final.zip"
manifest_path=out/"sprint6-package-manifest.txt"
verify_path=out/"sprint6-package-verification.json"
sha_path=out/"acra-sprint-06-final.zip.sha256"

excluded_parts={".git","build","target","__pycache__",".idea",".vscode"}
excluded_suffixes={".class",".pyc",".pyo",".zip"}
files=[]
for p in root.rglob("*"):
    if not p.is_file():
        continue
    rel=p.relative_to(root)
    if any(part in excluded_parts for part in rel.parts):
        continue
    if p.suffix.lower() in excluded_suffixes:
        continue
    files.append(rel)
files=sorted(files,key=lambda x:x.as_posix())

def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()

manifest=[]
with zipfile.ZipFile(zip_path,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as zf:
    for rel in files:
        data=(root/rel).read_bytes()
        info=zipfile.ZipInfo(rel.as_posix(),date_time=(1980,1,1,0,0,0))
        info.compress_type=zipfile.ZIP_DEFLATED
        info.external_attr=(0o100644 & 0xFFFF)<<16
        zf.writestr(info,data)
        manifest.append(f"{sha256_bytes(data)}  {rel.as_posix()}")

manifest_path.write_text("\n".join(manifest)+"\n",encoding="utf-8")
zip_sha=sha256_bytes(zip_path.read_bytes())
sha_path.write_text(f"{zip_sha}  {zip_path.name}\n",encoding="utf-8")

with zipfile.ZipFile(zip_path) as zf:
    names=zf.namelist()
    unsafe=[n for n in names if n.startswith("/") or ".." in Path(n).parts or "\\" in n]
    if unsafe:
        raise SystemExit(f"unsafe archive paths: {unsafe[:5]}")
    if len(names)!=len(files):
        raise SystemExit("archive entry count mismatch")
    with tempfile.TemporaryDirectory(prefix="acra-s6-verify-") as td:
        target=Path(td)
        zf.extractall(target)
        mismatches=[]
        for rel in files:
            src=(root/rel).read_bytes()
            dst=(target/rel).read_bytes()
            if hashlib.sha256(src).digest()!=hashlib.sha256(dst).digest():
                mismatches.append(rel.as_posix())
        if mismatches:
            raise SystemExit(f"clean extraction mismatch: {mismatches[:5]}")

try:
    commit=subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip()
except Exception:
    commit="UNKNOWN"
try:
    branch=subprocess.check_output(["git","branch","--show-current"],text=True).strip() or "UNKNOWN"
except Exception:
    branch="UNKNOWN"
verification={
    "checkpoint":"S6-FINAL-SOFTWARE-CANDIDATE",
    "gitCommit":commit,
    "gitBranch":branch,
    "archive":zip_path.name,
    "archiveSha256":zip_sha,
    "entryCount":len(files),
    "unsafePaths":0,
    "cleanExtractionEquality":"PASS",
    "perFileSha256Equality":"PASS",
    "excludedBuildAndGitArtifacts":True,
}
verify_path.write_text(json.dumps(verification,indent=2,sort_keys=True)+"\n",encoding="utf-8")
print(json.dumps(verification,sort_keys=True))
PY
echo "SPRINT6_PACKAGE_VERIFICATION PASS"
