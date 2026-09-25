#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
LABELS=ROOT/"lab/ground-truth/GT-S13-XFRAME-DEV-LABELS.json"
ROWS=ROOT/"build/s13-xframe-dev/predictions.jsonl"
EVAL=ROOT/"build/s13-xframe-dev/evaluation.json"
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    labels=json.loads(LABELS.read_text(encoding="utf-8"))
    rows=[json.loads(x) for x in ROWS.read_text(encoding="utf-8").splitlines() if x.strip()]
    by={x["caseId"]:x for x in labels["cases"]}
    joined=[{**r,**by[r["caseId"]]} for r in rows]
    raw_dim=sum(1 for r in joined if r["rawDimension"]==r["registeredDimension"])
    norm_dim=sum(1 for r in joined if r["normalizedDimension"]==r["registeredDimension"])
    raw_disp=sum(1 for r in joined if r["rawDisposition"]==r["expectedDisposition"])
    norm_disp=sum(1 for r in joined if r["normalizedDisposition"]==r["expectedDisposition"])
    fw={}
    for name in sorted({r["frameworkStyle"] for r in joined}):
      subset=[r for r in joined if r["frameworkStyle"]==name]
      fw[name]={
        "count":len(subset),
        "rawDimensionCorrect":sum(1 for r in subset if r["rawDimension"]==r["registeredDimension"]),
        "normalizedDimensionCorrect":sum(1 for r in subset if r["normalizedDimension"]==r["registeredDimension"]),
        "rawDispositionCorrect":sum(1 for r in subset if r["rawDisposition"]==r["expectedDisposition"]),
        "normalizedDispositionCorrect":sum(1 for r in subset if r["normalizedDisposition"]==r["expectedDisposition"])
      }
    obj={"schemaVersion":"s13-xframe-dev-evaluation-v1","caseCount":len(joined),
      "raw":{"dimensionCorrect":raw_dim,"dispositionCorrect":raw_disp},
      "normalized":{"dimensionCorrect":norm_dim,"dispositionCorrect":norm_disp},"byFramework":fw,
      "claimBoundary":["framework-shaped snapshots only","not actual framework runtime execution"]}
    EVAL.parent.mkdir(parents=True,exist_ok=True); EVAL.write_text(json.dumps(obj,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    EVAL.with_suffix(EVAL.suffix+".sha256").write_text(sha(EVAL)+"  "+EVAL.name+"\n",encoding="utf-8")
    print(f"SPRINT13_XFRAME_DEV_RESULT RAW_DIM={raw_dim}/64 RAW_DISP={raw_disp}/64 NORM_DIM={norm_dim}/64 NORM_DISP={norm_disp}/64")
    for k,v in fw.items(): print(f"SPRINT13_XFRAME_DEV_FRAMEWORK {k} RAW_DIM={v['rawDimensionCorrect']}/16 NORM_DIM={v['normalizedDimensionCorrect']}/16 RAW_DISP={v['rawDispositionCorrect']}/16 NORM_DISP={v['normalizedDispositionCorrect']}/16")
    print("SPRINT13_XFRAME_DEV_VERIFY PASS")
if __name__=="__main__": main()
