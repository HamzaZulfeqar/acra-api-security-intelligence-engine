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
    "ready": 3,
    "partial": 4,
    "missing_fixture": 8,
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
assert states.count("READY") == 3
assert states.count("PARTIAL") == 4
assert states.count("MISSING_FIXTURE") == 8

for index,item in enumerate(readiness["cases"], start=1):
    assert item["case_id"] == f"S11-EVAL-{index:03d}"
    if item["state"] == "MISSING_FIXTURE":
        assert item["routes"] == []
        assert item["evidence"] == []
    else:
        assert item["routes"]
        assert item["evidence"]

print("SPRINT11_EVIDENCE_READINESS_CONTRACT PASS ready=3 partial=4 missing=8 executed=0")
PY

CP="$BUILD/main:$BUILD/test"
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

echo "SPRINT11_RESEARCH_ABLATION_FOUNDATION_VERIFICATION PASS"
