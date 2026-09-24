package io.acra.burp.tests.sprint8;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.engine.S8RoutingAssessmentEvaluator;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.route.RouteBoundaryObservation;
import io.acra.core.route.RouteNormalizationAnalyzer;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteSecurityBoundaryAnalyzer;
import io.acra.core.route.RouteStageObservation;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint8RoutingUiTestSuite {
    private static int assertions;

    private Sprint8RoutingUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S8RoutingWorkspace routing = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                new S7WorkflowWorkspace(),
                routing)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT8_ROUTING_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Routing") >= 0, "Routing top-level tab installed");
        assertions++;

        JTabbedPane routingTabs = find(tab.component(), JTabbedPane.class, "s8-routing-tabs");
        for (String title : Set.of("Overview", "Stage Traces", "Boundary Matrix", "Assessments", "Candidates", "Report", "JSON Export")) {
            check(indexOf(routingTabs, title) >= 0, "routing sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(tab.component(), JTextArea.class, "s8-routing-overview");
        check(overview.getText().contains("Normalization traces: 1"),
                "routing overview renders normalization trace count");
        assertions++;
        check(overview.getText().contains("Boundary traces: 1"),
                "routing overview renders boundary trace count");
        assertions++;
        check(overview.getText().contains("Finding candidates: 1"),
                "routing overview renders candidate count");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "routing overview preserves candidate review boundary");
        assertions++;
        check(overview.getText().contains("Routing divergence != vulnerability severity"),
                "routing overview separates divergence from severity");
        assertions++;
        check(overview.getText().contains("Unknown processing stages remain unknown"),
                "routing overview preserves unknown-stage boundary");
        assertions++;

        check(rows(tab, "s8-routing-stage-table") == 2,
                "stage trace table renders two explicit observations");
        assertions++;
        check(rows(tab, "s8-routing-boundary-table") == 1,
                "boundary matrix renders one adjacent transition");
        assertions++;
        check(rows(tab, "s8-routing-assessment-table") == 1,
                "assessment table renders one routing assessment");
        assertions++;
        check(rows(tab, "s8-routing-candidate-table") == 1,
                "candidate table renders one review-only candidate");
        assertions++;

        check(tab.routingWorkspace().snapshot().candidateCount() == 1,
                "routing workspace exposes one candidate without confirmation promotion");
        assertions++;
        JTextArea report = find(tab.component(), JTextArea.class, "s8-routing-report-view");
        check(report.getText().contains("ACRA Sprint 8 Routing Normalization Report"),
                "routing report view renders canonical Markdown");
        assertions++;
        check(report.getText().contains("Confirmed findings: 0"),
                "routing report preserves zero confirmed findings");
        assertions++;

        JTextArea json = find(tab.component(), JTextArea.class, "s8-routing-json-export-view");
        check(json.getText().contains("s8-routing-report-v1"),
                "routing JSON export view renders canonical report version");
        assertions++;
        check(json.getText().contains("\"confirmedFindingCount\":0"),
                "routing JSON export preserves review-only boundary");
        assertions++;

    }

    private static S8RoutingWorkspace fixtureWorkspace() {
        S8RoutingWorkspace workspace = new S8RoutingWorkspace();

        var normalization = new RouteNormalizationAnalyzer().analyze(
                "S8-UI-NORMALIZATION",
                List.of(
                        new RouteStageObservation(
                                "S8-UI-RAW",
                                RouteProcessingStage.RAW_URI,
                                "/api//v1/s8/admin",
                                RouteObservationSource.OBSERVED,
                                List.of("e-raw")),
                        new RouteStageObservation(
                                "S8-UI-PROXY",
                                RouteProcessingStage.PROXY,
                                "/api/v1/s8/admin",
                                RouteObservationSource.OBSERVED,
                                List.of("e-proxy"))));
        workspace.recordNormalizationTrace(normalization);

        var boundary = new RouteSecurityBoundaryAnalyzer().analyze(
                "S8-UI-BOUNDARY",
                List.of(
                        new RouteBoundaryObservation(
                                "S8-UI-FRAMEWORK",
                                RouteProcessingStage.FRAMEWORK,
                                "/api/v1/s8/admin",
                                HttpMethod.GET,
                                "localhost",
                                "v1",
                                AuthorizationDecision.DENY,
                                AuthorizationDecision.DENY,
                                "s8-ui-policy",
                                RouteObservationSource.CONFIGURED,
                                List.of("e-framework")),
                        new RouteBoundaryObservation(
                                "S8-UI-APPLICATION",
                                RouteProcessingStage.APPLICATION,
                                "/api//v1/s8/admin",
                                HttpMethod.GET,
                                "localhost",
                                "v1",
                                AuthorizationDecision.DENY,
                                AuthorizationDecision.ALLOW,
                                "s8-ui-policy",
                                RouteObservationSource.OBSERVED,
                                List.of("e-application"))));
        workspace.recordBoundaryTrace(boundary);

        var assessment = new S8RoutingAssessmentEvaluator().evaluate(
                boundary.transitions().getFirst(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e-framework", "e-application"));
        workspace.recordAssessment(assessment);

        FindingCandidate candidate = new FindingCandidate(
                "fc-s8-ui",
                FindingCandidateState.CANDIDATE,
                "acra-s8-ui",
                List.of("S8-UI-TEST"),
                List.of("S8-UI-EXEC"),
                List.of("S8-UI-OBS"),
                List.of(assessment.assessmentId()),
                List.of("ROUTING_NORMALIZATION", "PATH_REPRESENTATION", "AUTHORIZATION_BOUNDARY"),
                "/api/v1/s8/admin",
                "route:s8-admin",
                "viewer-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e-framework", "e-application"),
                List.of(),
                List.of("s8-ui-policy"),
                "HIGH",
                "Controlled UI fixture candidate; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        "/api/v1/s8/admin",
                        "route:s8-admin",
                        "viewer-a",
                        "tenant-a",
                        "ROUTING_NORMALIZATION",
                        "CANDIDATE"));
        workspace.recordCandidate(candidate);
        return workspace;
    }

    private static int rows(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        if (table == null) throw new AssertionError("table missing: " + name);
        return table.getRowCount();
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        if (tabs == null) return -1;
        for (int index = 0; index < tabs.getTabCount(); index++) {
            if (title.equals(tabs.getTitleAt(index))) return index;
        }
        return -1;
    }

    private static <T extends Component> T find(Component root, Class<T> type, String name) {
        if (root == null) return null;
        if (type.isInstance(root) && (name == null
                || root instanceof JComponent component && name.equals(component.getName()))) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = find(child, type, name);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
