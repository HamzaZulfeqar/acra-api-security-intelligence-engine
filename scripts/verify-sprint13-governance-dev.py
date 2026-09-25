#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-GOV-DEV-FEATURES.json"
LABELS=ROOT/"lab/ground-truth/GT-S13-GOV-DEV-LABELS.json"
RESULT=ROOT/"build/s13-gov-dev/predictions.json"
ROWS=ROOT/"build/s13-gov-dev/predictions.jsonl"
EVAL=ROOT/"build/s13-gov-dev/evaluation.json"
EVAL_ROWS=ROOT/"build/s13-gov-dev/evaluation-cases.jsonl"

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def canon(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()
def ratio(a,b): return None if b==0 else a/b

def main():
    f=json.loads(FEATURES.read_text(encoding="utf-8"))
    l=json.loads(LABELS.read_text(encoding="utf-8"))
    pred=json.loads(RESULT.read_text(encoding="utf-8"))
    rows=[json.loads(x) for x in ROWS.read_text(encoding="utf-8").splitlines() if x.strip()]
    if len(rows)!=64: raise AssertionError("expected 64 predictions")
    if pred["featureDatasetSha256"]!=sha(FEATURES): raise AssertionError("feature drift")
    if pred["sourceCommit"]!=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip(): raise AssertionError("commit drift")
    by={x["caseId"]:x for x in l["cases"]}
    joined=[]
    for r in rows:
        lab=by[r["caseId"]]
        joined.append({**r,**lab,"dispositionCorrect":r["disposition"]==lab["expectedDisposition"]})
    disp_correct=sum(1 for r in joined if r["dispositionCorrect"])
    dim_correct=sum(1 for r in joined if r["inferredDimension"]==r["registeredDimension"])
    positives=[r for r in joined if r["groundTruth"]=="POSITIVE"]
    negatives=[r for r in joined if r["groundTruth"]=="NEGATIVE"]
    cand_tp=sum(1 for r in positives if r["actionableFinding"])
    cand_fn=len(positives)-cand_tp
    cand_fp=sum(1 for r in negatives if r["actionableFinding"])
    cand_tn=len(negatives)-cand_fp
    candidate_precision=ratio(cand_tp,cand_tp+cand_fp)
    candidate_recall=ratio(cand_tp,len(positives))
    reviewed_positives=sum(1 for r in positives if r["reviewRequired"])
    escalation_coverage=ratio(cand_tp+reviewed_positives,len(positives))
    silent_positive=sum(1 for r in positives if not r["actionableFinding"] and not r["reviewRequired"])
    review_negatives=sum(1 for r in negatives if r["reviewRequired"])
    dispositions={}
    for r in joined: dispositions[r["disposition"]]=dispositions.get(r["disposition"],0)+1
    eval_obj={
      "schemaVersion":"s13-gov-dev-evaluation-v1","caseCount":64,
      "dimensionAccuracy":dim_correct/64,"dispositionAccuracy":disp_correct/64,
      "candidate":{"tp":cand_tp,"tn":cand_tn,"fp":cand_fp,"fn":cand_fn,"precision":candidate_precision,"recall":candidate_recall},
      "review":{"positiveReviewCount":reviewed_positives,"negativeReviewCount":review_negatives,"escalationCoverage":escalation_coverage,"silentPositiveCount":silent_positive},
      "dispositionCounts":dispositions,
      "claimBoundary":["development-only governance corpus","candidate recall excludes positives intentionally routed to review","escalation coverage counts candidate or review-required positives"]
    }
    EVAL.write_bytes(canon(eval_obj)+b"\n")
    EVAL_ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in joined),encoding="utf-8")
    for p in (RESULT,ROWS,EVAL,EVAL_ROWS):
        if p in (EVAL,EVAL_ROWS): p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n",encoding="utf-8")
        if sha(p)!=p.with_suffix(p.suffix+".sha256").read_text(encoding="utf-8").split()[0]: raise AssertionError("sha mismatch")
    print(f"SPRINT13_GOV_DEV_RESULT DIM={dim_correct}/64 DISP={disp_correct}/64")
    print(f"SPRINT13_GOV_DEV_CANDIDATE TP={cand_tp} TN={cand_tn} FP={cand_fp} FN={cand_fn} P={candidate_precision} R={candidate_recall}")
    print(f"SPRINT13_GOV_DEV_REVIEW POS_REVIEW={reviewed_positives} NEG_REVIEW={review_negatives} ESCALATION={escalation_coverage} SILENT_POS={silent_positive}")
    print("SPRINT13_GOV_DEV_VERIFY PASS")

if __name__=="__main__": main()
