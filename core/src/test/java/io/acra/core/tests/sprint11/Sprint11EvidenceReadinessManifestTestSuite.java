package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationExecutionState;
import io.acra.core.active.research.FixtureReadinessState;
import io.acra.core.active.research.ResearchFixtureReadiness;
import io.acra.core.active.research.S11AblationCampaignPlan;
import io.acra.core.active.research.S11EvidenceReadinessManifest;
import io.acra.core.tests.TestSupport;
import java.util.ArrayList;
import java.util.List;

public final class Sprint11EvidenceReadinessManifestTestSuite {
    private Sprint11EvidenceReadinessManifestTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_EVIDENCE_READINESS_MANIFEST PASS assertions=" + assertions);
    }

    public static int run() {
        S11EvidenceReadinessManifest manifest = S11EvidenceReadinessManifest.canonical();
        int assertions = 0;

        TestSupport.assertEquals(AblationExecutionState.NOT_RUN, manifest.executionState(),
                "readiness mapping cannot claim experiment execution");
        assertions++;
        TestSupport.assertEquals(15, manifest.cases().size(),
                "readiness manifest covers all dataset cases");
        assertions++;
        TestSupport.assertEquals(3L, manifest.count(FixtureReadinessState.READY),
                "three existing controls are independently ready");
        assertions++;
        TestSupport.assertEquals(4L, manifest.count(FixtureReadinessState.PARTIAL),
                "four existing controls combine effects and remain partial");
        assertions++;
        TestSupport.assertEquals(8L, manifest.count(FixtureReadinessState.MISSING_FIXTURE),
                "eight positive research controls still require dedicated fixtures");
        assertions++;

        for (int index = 0; index < manifest.cases().size(); index++) {
            ResearchFixtureReadiness item = manifest.cases().get(index);
            TestSupport.assertEquals(String.format("S11-EVAL-%03d", index + 1), item.caseId(),
                    "readiness ordering matches dataset ordering");
            assertions++;
            if (item.state() == FixtureReadinessState.MISSING_FIXTURE) {
                TestSupport.assertTrue(item.routes().isEmpty(),
                        "missing fixture cannot claim executable route");
                assertions++;
                TestSupport.assertTrue(item.evidenceReferences().isEmpty(),
                        "missing fixture cannot claim executable evidence");
                assertions++;
            } else {
                TestSupport.assertTrue(!item.routes().isEmpty(),
                        "ready/partial fixture requires explicit route");
                assertions++;
                TestSupport.assertTrue(!item.evidenceReferences().isEmpty(),
                        "ready/partial fixture requires evidence reference");
                assertions++;
            }
        }

        var campaign = S11AblationCampaignPlan.canonical();
        TestSupport.assertEquals(120, campaign.coverage().plannedCells(),
                "fixture readiness alone does not promote campaign cells");
        assertions++;
        TestSupport.assertEquals(0, campaign.coverage().evidenceReadyCells(),
                "fixture readiness is not equivalent to ablation evidence readiness");
        assertions++;
        TestSupport.assertEquals(0, campaign.coverage().executedCells(),
                "readiness mapping executes no campaign cells");
        assertions++;

        S11EvidenceReadinessManifest repeated = S11EvidenceReadinessManifest.canonical();
        TestSupport.assertEquals(manifest.fingerprint(), repeated.fingerprint(),
                "readiness fingerprint is deterministic");
        assertions++;

        List<ResearchFixtureReadiness> tampered = new ArrayList<>(manifest.cases());
        ResearchFixtureReadiness first = tampered.getFirst();
        tampered.set(0, new ResearchFixtureReadiness(
                first.caseId(),
                first.sourceCaseId(),
                FixtureReadinessState.READY,
                List.of(),
                first.evidenceReferences(),
                first.reason()));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvidenceReadinessManifest(
                        "bad-ready", "1", manifest.datasetId(), AblationExecutionState.NOT_RUN, tampered, ""),
                "READY status without route fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvidenceReadinessManifest(
                        "executed", "1", manifest.datasetId(), AblationExecutionState.COMPLETE,
                        manifest.cases(), ""),
                "readiness manifest cannot self-promote execution");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11EvidenceReadinessManifest(
                        "bad-hash", "1", manifest.datasetId(), AblationExecutionState.NOT_RUN,
                        manifest.cases(), "tampered"),
                "readiness fingerprint tampering fails closed");
        assertions++;

        return assertions;
    }
}
