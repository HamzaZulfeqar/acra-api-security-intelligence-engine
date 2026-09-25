#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];LABELS=ROOT/"lab/ground-truth/GT-S13-XRUNTIME-DEV-LABELS.json";ROWS=ROOT/"build/s13-xruntime-dev/predictions.jsonl";EVAL=ROOT/"build/s13-xruntime-dev/evaluation.json"
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 l=json.loads(LABELS.read_text());rows=[json.loads(x) for x in ROWS.read_text().splitlines() if x.strip()];by={x["caseId"]:x for x in l["cases"]};j=[{**r,**by[r["caseId"]]} for r in rows]
 dim=sum(r["inferredDimension"]==r["registeredDimension"] for r in j);disp=sum(r["disposition"]==r["expectedDisposition"] for r in j);http=sum(r["httpStatus"]==200 for r in j);fw={}
 for name in sorted({r["frameworkStyle"] for r in j}):
  s=[r for r in j if r["frameworkStyle"]==name];fw[name]={"count":len(s),"http200":sum(r["httpStatus"]==200 for r in s),"dimensionCorrect":sum(r["inferredDimension"]==r["registeredDimension"] for r in s),"dispositionCorrect":sum(r["disposition"]==r["expectedDisposition"] for r in s)}
 obj={"schemaVersion":"s13-xruntime-dev-evaluation-v1","caseCount":len(j),"liveHttp200":http,"dimensionCorrect":dim,"dispositionCorrect":disp,"byFramework":fw,"claimBoundary":["actual localhost framework runtime traffic","development corpus only","not Burp or external-target evidence"]}
 EVAL.parent.mkdir(parents=True,exist_ok=True);EVAL.write_text(json.dumps(obj,sort_keys=True,separators=(",",":"))+"\n");EVAL.with_suffix(EVAL.suffix+".sha256").write_text(sha(EVAL)+"  "+EVAL.name+"\n")
 print(f"SPRINT13_XRUNTIME_DEV_RESULT HTTP200={http}/64 DIM={dim}/64 DISP={disp}/64")
 for k,v in fw.items():print(f"SPRINT13_XRUNTIME_DEV_FRAMEWORK {k} HTTP200={v['http200']}/16 DIM={v['dimensionCorrect']}/16 DISP={v['dispositionCorrect']}/16")
 print("SPRINT13_XRUNTIME_DEV_VERIFY PASS")
if __name__=="__main__":main()
