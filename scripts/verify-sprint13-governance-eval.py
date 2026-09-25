#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FEATURES=ROOT/"lab/ground-truth/GT-S13-GOV-EVAL-FEATURES.json"
LABELS=ROOT/"lab/ground-truth/GT-S13-GOV-EVAL-LABELS.json"
RESULT=ROOT/"build/s13-gov-eval/predictions.json"
ROWS=ROOT/"build/s13-gov-eval/predictions.jsonl"
EVAL=ROOT/"build/s13-gov-eval/evaluation.json"
EVAL_ROWS=ROOT/"build/s13-gov-eval/evaluation-cases.jsonl"
FREEZE="11afbb80be24116c9facd0d4b0791f4a12efed3a"

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def canon(v): return json.dumps(v,sort_keys=True,separators=(",",":")).encode()
def ratio(a,b): return None if b==0 else a/b

def binary(rows,key):
    tp=tn=fp=fn=0
    for r in rows:
        pred=r[key]
        truth=r["groundTruth"]
        positive = pred=="POSITIVE" if isinstance(pred,str) else bool(pred)
        if truth=="POSITIVE" and positive: tp+=1
        elif truth=="NEGATIVE" and not positive: tn+=1
        elif truth=="NEGATIVE" and positive: fp+=1
        else: fn+=1
    p=ratio(tp,tp+fp); rec=ratio(tp,tp+fn)
    f1=None if p is None or rec is None or p+rec==0 else 2*p*rec/(p+rec)
    return {"tp":tp,"tn":tn,"fp":fp,"fn":fn,"precision":p,"recall":rec,"f1":f1}

def main():
    f=json.loads(FEATURES.read_text(encoding="utf-8"))
    l=json.loads(LABELS.read_text(encoding="utf-8"))
    pred=json.loads(RESULT.read_text(encoding="utf-8"))
    rows=[json.loads(x) for x in ROWS.read_text(encoding="utf-8").splitlines() if x.strip()]
    if l.get("untouchedEvaluation") is not True: raise AssertionError("untouched marker required")
    if l.get("governanceFreeze")!=FREEZE or pred.get("governanceFreeze")!=FREEZE: raise AssertionError("freeze provenance drift")
    if l.get("featureDatasetId")!=f.get("datasetId") or pred.get("featureDatasetId")!=f.get("datasetId"): raise AssertionError("dataset identity mismatch")
    if pred.get("featureDatasetSha256")!=sha(FEATURES): raise AssertionError("feature digest drift")
    if len(rows)!=64: raise AssertionError("expected 64 eval rows")
    if pred.get("sourceCommit")!=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip(): raise AssertionError("commit drift")
    by={x["caseId"]:x for x in l["cases"]}
    if len(by)!=64: raise AssertionError("expected 64 labels")
    joined=[]
    for r in rows:
        lab=by[r["caseId"]]
        joined.append({**r,**lab,"dispositionCorrect":r["disposition"]==lab["expectedDisposition"]})
    dim_correct=sum(1 for r in joined if r["inferredDimension"]==r["registeredDimension"])
    disp_correct=sum(1 for r in joined if r["dispositionCorrect"])
    positives=[r for r in joined if r["groundTruth"]=="POSITIVE"]
    negatives=[r for r in joined if r["groundTruth"]=="NEGATIVE"]
    actionable_rows=[{**r,"actionableBinary":"POSITIVE" if r["actionableFinding"] else "NEGATIVE"} for r in joined]
    action=binary(actionable_rows,"actionableBinary")
    legacy=binary(joined,"legacyG1Prediction")
    a7=binary(joined,"lockedA7Prediction")
    reviewed_pos=sum(1 for r in positives if r["reviewRequired"])
    reviewed_neg=sum(1 for r in negatives if r["reviewRequired"])
    silent_pos=sum(1 for r in positives if not r["actionableFinding"] and not r["reviewRequired"])
    escalation=ratio(action["tp"]+reviewed_pos,len(positives))
    review_rate=ratio(reviewed_pos+reviewed_neg,len(joined))
    candidate_rate=ratio(action["tp"]+action["fp"],len(joined))
    counts={}
    for r in joined: counts[r["disposition"]]=counts.get(r["disposition"],0)+1
    evaluation={
      "schemaVersion":"s13-gov-eval-evaluation-v1","sourceCommit":pred["sourceCommit"],"governanceFreeze":FREEZE,
      "caseCount":64,"dimensionAccuracy":dim_correct/64,"dispositionAccuracy":disp_correct/64,
      "lockedA7":a7,"legacyG1":legacy,"governedActionable":action,
      "reviewRouting":{"positiveReviewCount":reviewed_pos,"negativeReviewCount":reviewed_neg,"reviewRate":review_rate,
        "candidateRate":candidate_rate,"escalationCoverage":escalation,"silentPositiveCount":silent_pos},
      "dispositionCounts":counts,
      "claimBoundary":[
        "untouched synthetic internal governance evaluation created after governance freeze",
        "actionable-candidate recall is expected to be lower than escalation coverage because uncertain positives are routed to review",
        "review routing does not prove real-world analyst workload or production effectiveness"
      ]
    }
    EVAL.write_bytes(canon(evaluation)+b"\n")
    EVAL_ROWS.write_text("".join(json.dumps(r,sort_keys=True,separators=(",",":"))+"\n" for r in joined),encoding="utf-8")
    for p in (RESULT,ROWS,EVAL,EVAL_ROWS):
        if p in (EVAL,EVAL_ROWS): p.with_suffix(p.suffix+".sha256").write_text(sha(p)+"  "+p.name+"\n",encoding="utf-8")
        side=p.with_suffix(p.suffix+".sha256")
        if sha(p)!=side.read_text(encoding="utf-8").split()[0]: raise AssertionError("sha mismatch")
    blob=RESULT.read_text(encoding="utf-8")+ROWS.read_text(encoding="utf-8")+EVAL.read_text(encoding="utf-8")+EVAL_ROWS.read_text(encoding="utf-8")
    for forbidden in ("Bearer ","\"Authorization\":","synthetic-cookie-secret"):
        if forbidden in blob: raise AssertionError("secret-like material leaked")
    print(f"SPRINT13_GOV_EVAL_RESULT DIM={dim_correct}/64 DISP={disp_correct}/64")
    print(f"SPRINT13_GOV_EVAL_A7 TP={a7['tp']} TN={a7['tn']} FP={a7['fp']} FN={a7['fn']} P={a7['precision']} R={a7['recall']} F1={a7['f1']}")
    print(f"SPRINT13_GOV_EVAL_LEGACY_G1 TP={legacy['tp']} TN={legacy['tn']} FP={legacy['fp']} FN={legacy['fn']} P={legacy['precision']} R={legacy['recall']} F1={legacy['f1']}")
    print(f"SPRINT13_GOV_EVAL_ACTIONABLE TP={action['tp']} TN={action['tn']} FP={action['fp']} FN={action['fn']} P={action['precision']} R={action['recall']} F1={action['f1']}")
    print(f"SPRINT13_GOV_EVAL_REVIEW POS_REVIEW={reviewed_pos} NEG_REVIEW={reviewed_neg} ESCALATION={escalation} SILENT_POS={silent_pos} REVIEW_RATE={review_rate} CANDIDATE_RATE={candidate_rate}")
    for k in sorted(counts): print(f"SPRINT13_GOV_EVAL_DISPOSITION {k}={counts[k]}")
    print("SPRINT13_GOV_EVAL_VERIFY PASS")

if __name__=="__main__": main()
