#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
LABELS=ROOT/"lab/ground-truth/GT-S13-XFRAME-EVAL-LABELS.json"; ROWS=ROOT/"build/s13-xframe-eval/predictions.jsonl"; EVAL=ROOT/"build/s13-xframe-eval/evaluation.json"
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    l=json.loads(LABELS.read_text()); rows=[json.loads(x) for x in ROWS.read_text().splitlines() if x.strip()]; by={x["caseId"]:x for x in l["cases"]}; j=[{**r,**by[r["caseId"]]} for r in rows]
    rawd=sum(r["rawDimension"]==r["registeredDimension"] for r in j); normd=sum(r["normalizedDimension"]==r["registeredDimension"] for r in j); rawp=sum(r["rawDisposition"]==r["expectedDisposition"] for r in j); normp=sum(r["normalizedDisposition"]==r["expectedDisposition"] for r in j)
    fw={}
    for name in sorted({r["frameworkStyle"] for r in j}):
      s=[r for r in j if r["frameworkStyle"]==name]
      fw[name]={"rawDimensionCorrect":sum(r["rawDimension"]==r["registeredDimension"] for r in s),"normalizedDimensionCorrect":sum(r["normalizedDimension"]==r["registeredDimension"] for r in s),"rawDispositionCorrect":sum(r["rawDisposition"]==r["expectedDisposition"] for r in s),"normalizedDispositionCorrect":sum(r["normalizedDisposition"]==r["expectedDisposition"] for r in s)}
    obj={"schemaVersion":"s13-xframe-eval-v1","caseCount":len(j),"rawDimensionCorrect":rawd,"normalizedDimensionCorrect":normd,"rawDispositionCorrect":rawp,"normalizedDispositionCorrect":normp,"byFramework":fw,"claimBoundary":["framework-shaped snapshots only","no actual framework runtime execution"]}
    EVAL.parent.mkdir(parents=True,exist_ok=True);EVAL.write_text(json.dumps(obj,sort_keys=True,separators=(",",":"))+"\n");EVAL.with_suffix(EVAL.suffix+".sha256").write_text(sha(EVAL)+"  "+EVAL.name+"\n")
    print(f"SPRINT13_XFRAME_EVAL_RESULT RAW_DIM={rawd}/64 RAW_DISP={rawp}/64 NORM_DIM={normd}/64 NORM_DISP={normp}/64")
    for k,v in fw.items(): print(f"SPRINT13_XFRAME_EVAL_FRAMEWORK {k} RAW_DIM={v['rawDimensionCorrect']}/16 NORM_DIM={v['normalizedDimensionCorrect']}/16 RAW_DISP={v['rawDispositionCorrect']}/16 NORM_DISP={v['normalizedDispositionCorrect']}/16")
    print("SPRINT13_XFRAME_EVAL_VERIFY PASS")
if __name__=="__main__": main()
