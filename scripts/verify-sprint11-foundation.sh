#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BUILD="$ROOT/build/s11-foundation"
rm -rf "$BUILD"
mkdir -p "$BUILD/main" "$BUILD/test"

find core/src/main/java -name '*.java' | sort > "$BUILD/main-sources.txt"
javac --release 21 -Xlint:all -Werror -d "$BUILD/main" @"$BUILD/main-sources.txt"

find core/src/test/java -name '*.java' | sort > "$BUILD/test-sources.txt"
javac --release 21 -Xlint:all -Werror -cp "$BUILD/main" -d "$BUILD/test" @"$BUILD/test-sources.txt"

python3 - <<'PY'
import json
from pathlib import Path

source=json.loads(Path("lab/ground-truth/GT-S4-RESEARCH-FIXTURES.json").read_text(encoding="utf-8"))
manifest=json.loads(Path("lab/ground-truth/GT-S11-ABLATION-DATASET.json").read_text(encoding="utf-8"))

assert source["ground_truth_id"] == "GT-S4-RESEARCH-FIXTURES"
assert source["version"] == "1"
assert source["independent_of_acra_output"] is True
assert len(source["cases"]) == 15

assert manifest["id"] == "GT-S11-ABLATION-DATASET"
assert manifest["version"] == "1"
assert manifest["execution_state"] == "NOT_RUN"
assert manifest["independent_of_treatment_output"] is True
assert manifest["summary"] == {"cases": 15, "positive": 8, "negative": 7}
assert len(manifest["cases"]) == 15

source_by_id={item["case_id"]: item for item in source["cases"]}
seen=set()
positive=negative=0
for index,item in enumerate(manifest["cases"], start=1):
    assert item["id"] == f"S11-EVAL-{index:03d}"
    sid=item["source_case_id"]
    assert sid not in seen
    seen.add(sid)
    original=source_by_id[sid]
    expected="POSITIVE" if original["expected_candidate"] else "NEGATIVE"
    assert item["ground_truth"] == expected
    assert item["family"] == original["family"]
    if expected == "POSITIVE":
        positive += 1
    else:
        negative += 1

assert seen == set(source_by_id)
assert (positive, negative) == (8, 7)
assert [item["id"] for item in manifest["excluded_sources"]] == [
    "GT-S6-TENANT-RBAC",
    "GT-S7-WORKFLOW-AUTHORIZATION",
    "GT-S8-ROUTING-NORMALIZATION",
    "GT-S9-PROPERTY-AUTHORIZATION",
    "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
]
print("SPRINT11_EVALUATION_DATASET_CONTRACT PASS cases=15 positive=8 negative=7")
PY

python3 - <<'PY'
import json
from pathlib import Path

readiness=json.loads(Path("lab/ground-truth/GT-S11-EVIDENCE-READINESS.json").read_text(encoding="utf-8"))
lab=Path("lab/common/basic_api.py").read_text(encoding="utf-8")
s4test=Path("core/src/test/java/io/acra/core/tests/sprint4/Sprint4LocalhostIntegrationTestSuite.java").read_text(encoding="utf-8")
gt_exec=json.loads(Path("lab/ground-truth/GT-EXEC-S4.json").read_text(encoding="utf-8"))

assert readiness["id"] == "GT-S11-EVIDENCE-READINESS"
assert readiness["execution_state"] == "NOT_RUN"
assert readiness["summary"] == {
    "cases": 15,
    "ready": 15,
    "partial": 0,
    "missing_fixture": 0,
    "executed": 0,
}
assert len(readiness["cases"]) == 15
assert "/api/v1/s4/application-denial" in lab
assert "/api/v1/s4/public" in lab
assert "variant=='b'" in lab
assert "dynamic timestamp/request id produce raw response variance" in s4test
assert "HTTP 200 application denial is semantically DENY" in s4test
assert "format/order variation remains semantically equivalent" in s4test

prep=set(gt_exec["false_positive_preparation"])
for required in {
    "HTTP 200 application denial",
    "dynamic timestamp",
    "dynamic request identifier",
    "response ordering and formatting variation",
    "public resource",
}:
    assert required in prep

states=[item["state"] for item in readiness["cases"]]
assert states.count("READY") == 15
assert states.count("PARTIAL") == 0
assert states.count("MISSING_FIXTURE") == 0

for index,item in enumerate(readiness["cases"], start=1):
    assert item["case_id"] == f"S11-EVAL-{index:03d}"
    assert item["state"] == "READY"
    assert item["routes"]
    assert item["evidence"]
    if index in (3,4,5,6,8,9,10,11,12,13,14,15):
        assert any("GT-S11-RESEARCH-LAB-FIXTURES" in value for value in item["evidence"])
        assert "Sprint11ControlledResearchLabFixtureTestSuite" in item["evidence"]

print("SPRINT11_EVIDENCE_READINESS_CONTRACT PASS ready=15 partial=0 missing=0 executed=0")
PY

python3 -m py_compile lab/common/basic_api.py lab/secure-api/basic-api/server.py lab/vulnerable-api/basic-api/server.py
python3 - <<'PY'
import json
from pathlib import Path

gt=json.loads(Path("lab/ground-truth/GT-S11-RESEARCH-LAB-FIXTURES.json").read_text(encoding="utf-8"))
assert gt["id"] == "GT-S11-RESEARCH-LAB-FIXTURES"
assert gt["version"] == "1"
assert gt["independent_of_treatment_output"] is True
assert gt["execution_state"] == "NOT_RUN"
assert gt["summary"] == {
    "new_isolated_negative_controls": 4,
    "new_positive_controls": 8,
    "total_new_fixtures": 12,
}
assert len(gt["cases"]) == 12
assert [item["case_id"] for item in gt["cases"]] == [
    "S11-EVAL-003","S11-EVAL-004","S11-EVAL-005","S11-EVAL-006",
    "S11-EVAL-008","S11-EVAL-009","S11-EVAL-010","S11-EVAL-011",
    "S11-EVAL-012","S11-EVAL-013","S11-EVAL-014","S11-EVAL-015",
]
assert sum(1 for item in gt["cases"] if item["expected_candidate"]) == 8
assert sum(1 for item in gt["cases"] if not item["expected_candidate"]) == 4
assert all("/api/v1/s11/research/case-" in item["route"] for item in gt["cases"])
print("SPRINT11_RESEARCH_LAB_GROUND_TRUTH PASS cases=12 negative=4 positive=8")
PY

ACRA_LAB_MODE=secure PORT=18082 python3 "$ROOT/lab/common/basic_api.py" >"$BUILD/secure-lab.log" 2>&1 &
SECURE_PID=$!
ACRA_LAB_MODE=vulnerable PORT=18081 python3 "$ROOT/lab/common/basic_api.py" >"$BUILD/vulnerable-lab.log" 2>&1 &
VULNERABLE_PID=$!
cleanup_lab() {
  kill "$SECURE_PID" "$VULNERABLE_PID" 2>/dev/null || true
  wait "$SECURE_PID" "$VULNERABLE_PID" 2>/dev/null || true
}
trap cleanup_lab EXIT

python3 - <<'PY'
import time, urllib.request
for url in ("http://127.0.0.1:18082/health","http://127.0.0.1:18081/health"):
    last=None
    for _ in range(50):
        try:
            with urllib.request.urlopen(url, timeout=0.5) as response:
                if response.status == 200:
                    break
        except Exception as exc:
            last=exc
            time.sleep(0.1)
    else:
        raise SystemExit(f"Sprint 11 lab not ready: {url}: {last}")
print("SPRINT11_RESEARCH_LAB_READY PASS")
PY

CP="$BUILD/main:$BUILD/test"
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11ControlledResearchLabFixtureTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11TreatmentEvidenceCollectionTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11PredictionExecutionTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11AblationEvaluationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11ResearchReportExportTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11ResearchAblationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11EvaluationDatasetManifestTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11AblationPredictionAdapterTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11AblationCampaignPlanTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint11.Sprint11EvidenceReadinessManifestTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint10.Sprint10BatchIndirectFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint9.Sprint9PropertyAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint8.Sprint8RoutingNormalizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint7.Sprint7WorkflowAuthorizationFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint6.Sprint6PolicyFoundationTestSuite
java -ea -cp "$CP" io.acra.core.tests.sprint5.Sprint5FinalClosureTestSuite

cleanup_lab
trap - EXIT
echo "SPRINT11_RESEARCH_ABLATION_FOUNDATION_VERIFICATION PASS"
