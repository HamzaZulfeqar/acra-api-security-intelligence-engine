#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
OUT="$ROOT/build/s9-final-package"
rm -rf "$OUT"
mkdir -p "$OUT"

python3 - <<'PY'
from pathlib import Path
import hashlib, json, subprocess, tempfile, zipfile

root=Path.cwd()
out=root/"build"/"s9-final-package"
zip_path=out/"acra-sprint-09-final.zip"
manifest_path=out/"sprint9-package-manifest.txt"
verify_path=out/"sprint9-package-verification.json"
sha_path=out/"acra-sprint-09-final.zip.sha256"

excluded_parts={".git","build","target","__pycache__",".idea",".vscode"}
excluded_suffixes={".class",".pyc",".pyo",".zip"}
files=[]
for path in root.rglob("*"):
    if not path.is_file():
        continue
    rel=path.relative_to(root)
    if any(part in excluded_parts for part in rel.parts):
        continue
    if path.suffix.lower() in excluded_suffixes:
        continue
    files.append(rel)
files=sorted(files,key=lambda value:value.as_posix())

def sha256(data):
    return hashlib.sha256(data).hexdigest()

manifest=[]
with zipfile.ZipFile(zip_path,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for rel in files:
        data=(root/rel).read_bytes()
        info=zipfile.ZipInfo(rel.as_posix(),date_time=(1980,1,1,0,0,0))
        info.compress_type=zipfile.ZIP_DEFLATED
        info.external_attr=(0o100644 & 0xFFFF)<<16
        archive.writestr(info,data)
        manifest.append(f"{sha256(data)}  {rel.as_posix()}")

manifest_path.write_text("\n".join(manifest)+"\n",encoding="utf-8")
archive_sha=sha256(zip_path.read_bytes())
sha_path.write_text(f"{archive_sha}  {zip_path.name}\n",encoding="utf-8")

with zipfile.ZipFile(zip_path) as archive:
    names=archive.namelist()
    unsafe=[name for name in names if name.startswith("/") or ".." in Path(name).parts or "\\" in name]
    if unsafe:
        raise SystemExit(f"unsafe archive paths: {unsafe[:5]}")
    if len(names)!=len(files):
        raise SystemExit("archive entry count mismatch")
    if len(names)!=len(set(names)):
        raise SystemExit("duplicate archive entry")
    with tempfile.TemporaryDirectory(prefix="acra-s9-verify-") as temp:
        target=Path(temp)
        archive.extractall(target)
        mismatches=[]
        for rel in files:
            source=(root/rel).read_bytes()
            extracted=(target/rel).read_bytes()
            if hashlib.sha256(source).digest()!=hashlib.sha256(extracted).digest():
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
    "checkpoint":"S9-FINAL-SOFTWARE",
    "gitCommit":commit,
    "gitBranch":branch,
    "archive":zip_path.name,
    "archiveSha256":archive_sha,
    "entryCount":len(files),
    "unsafePaths":0,
    "duplicateEntries":0,
    "cleanExtractionEquality":"PASS",
    "perFileSha256Equality":"PASS",
    "excludedBuildAndGitArtifacts":True,
}
verify_path.write_text(json.dumps(verification,indent=2,sort_keys=True)+"\n",encoding="utf-8")
print(json.dumps(verification,sort_keys=True))
PY

echo "SPRINT9_PACKAGE_VERIFICATION PASS"
