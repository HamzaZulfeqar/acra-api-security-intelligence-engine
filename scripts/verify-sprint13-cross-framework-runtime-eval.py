#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];FEATURES=ROOT/"lab/ground-truth/GT-S13-XRUNTIME-EVAL-FEATURES.json";LABELS=ROOT/"lab/ground-truth/GT-S13-XRUNTIME-EVAL-LABELS.json";RESULT=ROOT/"build/s13-xruntime-eval/predictions.json";ROWS=ROOT/"build/s13-xruntime-eval/predictions.jsonl";EVAL=ROOT/"build/s13-xruntime-eval/evaluation.json";EVAL_ROWS=ROOT/"build/s13-xruntime-eval/evaluation-cases.jsonl"
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 f=json.loads(FEATURES.read_text());l=json.loads(LABELS.read_text());p=json.loads(RESULT.read_text());rows=[json.loads(x) for x in ROWS.read_text().splitlines() if x.strip()]
 if l.get("untouchedEvaluation") is not True or len(rows)!=64:raise AssertionError("evaluation boundary")
 if p.get("featureDatasetSha256")!=sha(FEATURES):raise AssertionError("feature drift")
 if p.get("sourceCommit")!=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip():raise AssertionError("commit drift")
 by={x["caseId"]:x for x in l["cases"]};j=[{**r,**by[r["caseId"]]} for r in rows]
 http=sum(r["httpStatus"]==200 for r in j);dim=sum(r["inferredDimension"]==r["registeredDimension"] for r in j);disp=sum(r["disposition"]==r["expectedDisposition"] for r in j);fw={}
 for name in sorted({r["frameworkStyle"] for r in j}):
  s=[r for r in j if r["frameworkStyle"]==name];fw[name]={"count":len(s),"http200":sum(r["httpStatus"]==200 for r in s),"dimensionCorrect":sum(r["inferredDimension"]==r["registeredDimension"] for r in s),"dispositionCorrect":sum(r["disposition"]==r["expectedDisposition"] for r in s)}
 obj={"schemaVersion":"s13-xruntime-eval-evaluation-v1","sourceCommit":p["sourceCommit"],"caseCount":64,"liveHttp200":http,"dimensionCorrect":dim,"dispositionCorrect":disp,"byFramework":fw,"claimBoundary":["actual localhost framework runtime traffic","untouched evaluation after development freeze","not Burp or external-target evidence"]}
 EVAL.write_text(json.dumps(obj,sort_keys=True,separators=(",",":"))+"\n");EVAL_ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in j))
 for q in(RESULT,ROWS,EVAL,EVAL_ROWS):
  if q in(EVAL,EVAL_ROWS):q.with_suffix(q.suffix+".sha256").write_text(sha(q)+"  "+q.name+"\n")
  if sha(q)!=q.with_suffix(q.suffix+".sha256").read_text().split()[0]:raise AssertionError("sha mismatch")
 print(f"SPRINT13_XRUNTIME_EVAL_RESULT HTTP200={http}/64 DIM={dim}/64 DISP={disp}/64")
 for k,v in fw.items():print(f"SPRINT13_XRUNTIME_EVAL_FRAMEWORK {k} HTTP200={v['http200']}/16 DIM={v['dimensionCorrect']}/16 DISP={v['dispositionCorrect']}/16")
 print("SPRINT13_XRUNTIME_EVAL_VERIFY PASS")
if __name__=="__main__":main()
