package io.acra.burp.ui;

import io.acra.burp.model.ContextObservation;
import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.core.domain.observation.EvidenceTimelineEntry;
import io.acra.core.domain.observation.TrafficObservation;
import io.acra.core.inventory.EndpointAggregate;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.Clock;
import java.util.List;

public final class AcraSuiteTab {
    private final JPanel root=new JPanel(new BorderLayout());
    private final TrafficModel trafficModel;
    private final ContextModel contextModel;
    private final EndpointModel endpointModel;
    private final JLabel overview=new JLabel();
    private final JTextArea parametersView=new JTextArea();
    private final JTextArea identitiesView=new JTextArea();
    private final JTextArea tenantsView=new JTextArea();
    private final JTextArea resourcesView=new JTextArea();
    private final JTextArea routesView=new JTextArea();
    private final JTextArea graphView=new JTextArea();
    private final ActiveTestingPanel activeTestingPanel;
    private final S6AuthorizationPanel authorizationPanel;
    private final S7WorkflowPanel workflowPanel;
    private final S8RoutingPanel routingPanel;
    private final Timer refresh;

    public AcraSuiteTab(TrafficIntelligencePipeline pipeline,ScopeController scope){
        this(pipeline,scope,new ActiveEngineWorkspace(Clock.systemUTC(),null),
                new S6AuthorizationWorkspace(),new S7WorkflowWorkspace(),new S8RoutingWorkspace());
    }

    public AcraSuiteTab(TrafficIntelligencePipeline pipeline,ScopeController scope,ActiveEngineWorkspace activeWorkspace){
        this(pipeline,scope,activeWorkspace,new S6AuthorizationWorkspace(),new S7WorkflowWorkspace(),
                new S8RoutingWorkspace());
    }

    public AcraSuiteTab(TrafficIntelligencePipeline pipeline,ScopeController scope,
                        ActiveEngineWorkspace activeWorkspace,S6AuthorizationWorkspace authorizationWorkspace){
        this(pipeline,scope,activeWorkspace,authorizationWorkspace,new S7WorkflowWorkspace(),
                new S8RoutingWorkspace());
    }

    public AcraSuiteTab(TrafficIntelligencePipeline pipeline,ScopeController scope,
                        ActiveEngineWorkspace activeWorkspace,S6AuthorizationWorkspace authorizationWorkspace,
                        S7WorkflowWorkspace workflowWorkspace){
        this(pipeline,scope,activeWorkspace,authorizationWorkspace,workflowWorkspace,new S8RoutingWorkspace());
    }

    public AcraSuiteTab(TrafficIntelligencePipeline pipeline,ScopeController scope,
                        ActiveEngineWorkspace activeWorkspace,S6AuthorizationWorkspace authorizationWorkspace,
                        S7WorkflowWorkspace workflowWorkspace,S8RoutingWorkspace routingWorkspace){
        JTabbedPane tabs=new JTabbedPane();
        trafficModel=new TrafficModel(pipeline); contextModel=new ContextModel(pipeline); endpointModel=new EndpointModel(pipeline);
        JPanel overviewPanel=new JPanel(new BorderLayout()); overviewPanel.add(overview,BorderLayout.NORTH); tabs.addTab("Overview",overviewPanel);
        tabs.addTab("Traffic",trafficPanel(pipeline));
        tabs.addTab("Contexts",new JScrollPane(new JTable(contextModel)));
        tabs.addTab("Endpoints",new JScrollPane(new JTable(endpointModel)));
        configureText(parametersView); configureText(identitiesView); configureText(tenantsView); configureText(resourcesView); configureText(routesView); configureText(graphView);
        tabs.addTab("Parameters",new JScrollPane(parametersView));
        tabs.addTab("Identities",new JScrollPane(identitiesView));
        tabs.addTab("Tenants",new JScrollPane(tenantsView));
        tabs.addTab("Resources",new JScrollPane(resourcesView));
        tabs.addTab("Routes",new JScrollPane(routesView));
        tabs.addTab("Context Graph",new JScrollPane(graphView));
        activeTestingPanel=new ActiveTestingPanel(activeWorkspace);
        activeTestingPanel.install(tabs);
        authorizationPanel=new S6AuthorizationPanel(authorizationWorkspace);
        authorizationPanel.install(tabs);
        workflowPanel=new S7WorkflowPanel(workflowWorkspace);
        workflowPanel.install(tabs);
        routingPanel=new S8RoutingPanel(routingWorkspace);
        routingPanel.install(tabs);
        tabs.addTab("Configuration",configPanel(scope)); root.add(tabs,BorderLayout.CENTER);
        refresh=new Timer(1000,e->{trafficModel.refresh();contextModel.refresh();endpointModel.refresh();refreshReconViews(pipeline);activeTestingPanel.refresh();authorizationPanel.refresh();workflowPanel.refresh();routingPanel.refresh();overview.setText(" Observations: "+pipeline.store().size()+" | Endpoints: "+pipeline.inventory().size()+" | Sessions: "+pipeline.sessions().size()+" | Recon: "+pipeline.reconnaissanceStore().size()+" | Findings: not assessed");});
        refresh.start();
    }


    private static void configureText(JTextArea area){area.setEditable(false);area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));}

    private void refreshReconViews(TrafficIntelligencePipeline pipeline){
        StringBuilder parameters=new StringBuilder("Transaction | Name | Role | Values | Confidence\n");
        StringBuilder routes=new StringBuilder("Transaction | Route | Syntax | Priority | Documentation | Coverage\n");
        for(var e:pipeline.reconnaissanceStore().entries()){var r=e.getValue().reconnaissance(); for(var p:r.parameters()) parameters.append(e.getKey()).append(" | ").append(p.name()).append(" | ").append(p.role()).append(" | ").append(p.values()).append(" | ").append(String.format("%.2f",p.confidence().score())).append('\n'); routes.append(e.getKey()).append(" | ").append(r.route().canonical()).append(" | ").append(r.route().syntax()).append(" | ").append(r.priority().priority()).append(" | ").append(r.documentationStatus()).append(" | ").append(r.coverage().knownCount()).append('/').append(r.coverage().total()).append('\n');}
        parametersView.setText(parameters.toString()); routesView.setText(routes.toString());
        StringBuilder ids=new StringBuilder("Transaction | Principal | Role | Session | Confidence\n"); StringBuilder tenants=new StringBuilder("Transaction | Tenant | Confidence\n"); StringBuilder resources=new StringBuilder("Transaction | Resource | Owner | Action\n");
        for(ContextObservation c:pipeline.store().contexts()){ids.append(c.transactionId()).append(" | ").append(c.principal()).append(" | ").append(c.role()).append(" | ").append(c.session()).append(" | ").append(String.format("%.2f",c.confidence())).append('\n');tenants.append(c.transactionId()).append(" | ").append(c.tenant()).append(" | ").append(String.format("%.2f",c.confidence())).append('\n');resources.append(c.transactionId()).append(" | ").append(c.resource()).append(" | ").append(c.owner()).append(" | ").append(c.action()).append('\n');}
        identitiesView.setText(ids.toString());tenantsView.setText(tenants.toString());resourcesView.setText(resources.toString());
        StringBuilder graph=new StringBuilder("Nodes: ").append(pipeline.graph().nodeCount()).append(" | Edges: ").append(pipeline.graph().edgeCount()).append(" | Evidence: ").append(pipeline.graph().evidenceCount()).append("\n\n"); for(var edge:pipeline.graph().edges().stream().limit(500).toList()) graph.append(edge.source()).append(" --").append(edge.relation()).append("--> ").append(edge.target()).append(" evidence=").append(edge.evidenceIds()).append('\n'); graphView.setText(graph.toString());
    }

    public Component component(){return root;}
    public ActiveEngineWorkspace activeWorkspace(){return activeTestingPanel.workspace();}
    public S6AuthorizationWorkspace authorizationWorkspace(){return authorizationPanel.workspace();}
    public S7WorkflowWorkspace workflowWorkspace(){return workflowPanel.workspace();}
    public S8RoutingWorkspace routingWorkspace(){return routingPanel.workspace();}
    public void stop(){refresh.stop();}

    private Component trafficPanel(TrafficIntelligencePipeline pipeline){
        JTable table=new JTable(trafficModel); JTextArea details=new JTextArea(); details.setEditable(false); details.setRows(9);
        table.getSelectionModel().addListSelectionListener(e->{
            if(e.getValueIsAdjusting()) return; int viewRow=table.getSelectedRow(); if(viewRow<0){details.setText("");return;}
            int modelRow=table.convertRowIndexToModel(viewRow); TrafficObservation observation=trafficModel.rowAt(modelRow);
            ContextObservation context=pipeline.store().context(observation.transactionId()).orElse(null);
            List<EvidenceTimelineEntry> timeline=pipeline.store().timeline(observation.transactionId());
            details.setText(detailText(observation,context,timeline));
        });
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(table),new JScrollPane(details)); split.setResizeWeight(0.72); return split;
    }

    private static String detailText(TrafficObservation o,ContextObservation c,List<EvidenceTimelineEntry> timeline){
        StringBuilder b=new StringBuilder(); b.append("Transaction: ").append(o.transactionId()).append('\n');
        if(c!=null){b.append("Principal: ").append(c.principal()).append(" | Role: ").append(c.role()).append(" | Tenant: ").append(c.tenant()).append('\n'); b.append("Session: ").append(c.session()).append(" | Resource: ").append(c.resource()).append(" | Owner: ").append(c.owner()).append('\n'); b.append("Action: ").append(c.action()).append(" | URI: ").append(c.rawUri()).append('\n'); b.append("Context status: ").append(c.status()).append(" | Confidence: ").append(String.format("%.2f",c.confidence())).append('\n'); b.append("Evidence IDs: ").append(c.evidenceIds()).append('\n');}
        b.append("Timeline:\n"); for(EvidenceTimelineEntry e:timeline) b.append("  ").append(e.timestamp()).append(" ").append(e.stage()).append(" - ").append(e.summary()).append('\n');
        b.append("Vulnerability finding: NOT ASSESSED"); return b.toString();
    }

    private JPanel configPanel(ScopeController scope){JPanel p=new JPanel(new GridLayout(0,1)); var c=scope.configuration(); p.add(new JLabel("Scope mode: "+c.mode())); p.add(new JLabel("Max body bytes: "+c.maxBodyBytes())); p.add(new JLabel("Max observations: "+c.maxTransactions())); p.add(new JLabel("Active execution: DISABLED by default")); p.add(new JLabel("Active request budget: 0 | Mutation budget: 0 | Kill switch: ENGAGED")); return p;}

    @SuppressWarnings("serial")
    private static final class TrafficModel extends AbstractTableModel {
        private final TrafficIntelligencePipeline p; private List<TrafficObservation> rows=List.of(); private final String[] cols={"ID","Method","Host","Path","Status","Identity","Tenant","Resource","Action","Timestamp"};
        TrafficModel(TrafficIntelligencePipeline p){this.p=p;refresh();} void refresh(){rows=p.store().observations();fireTableDataChanged();} TrafficObservation rowAt(int r){return rows.get(r);}
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return cols.length;}@Override public String getColumnName(int c){return cols[c];}
        @Override public Object getValueAt(int r,int c){var o=rows.get(r);return switch(c){case 0->o.transactionId();case 1->o.endpoint().method();case 2->o.endpoint().host();case 3->o.endpoint().rawPath();case 4->o.responseStatus();case 5->blankUnknown(o.principalId());case 6->blankUnknown(o.tenantId());case 7->o.resourceId().isBlank()?"UNKNOWN":o.resourceType()+":"+o.resourceId();case 8->o.action();default->o.timestamp();};}
    }

    @SuppressWarnings("serial")
    private static final class ContextModel extends AbstractTableModel {
        private final TrafficIntelligencePipeline p; private List<ContextObservation> rows=List.of(); private final String[] cols={"Transaction","Principal","Role","Tenant","Session","Resource","Owner","Action","URI","Evidence","Confidence","Status"};
        ContextModel(TrafficIntelligencePipeline p){this.p=p;refresh();} void refresh(){rows=p.store().contexts();fireTableDataChanged();}
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return cols.length;}@Override public String getColumnName(int c){return cols[c];}
        @Override public Object getValueAt(int r,int c){var x=rows.get(r);return switch(c){case 0->x.transactionId();case 1->x.principal();case 2->x.role();case 3->x.tenant();case 4->x.session();case 5->x.resource();case 6->x.owner();case 7->x.action();case 8->x.rawUri();case 9->x.evidenceIds().size();case 10->String.format("%.2f",x.confidence());default->x.status();};}
    }

    @SuppressWarnings("serial")
    private static final class EndpointModel extends AbstractTableModel {
        private final TrafficIntelligencePipeline p; private List<EndpointAggregate> rows=List.of(); private final String[] cols={"Endpoint","Observed","IDs","Contexts","Tenants","Authentication","Risk"};
        EndpointModel(TrafficIntelligencePipeline p){this.p=p;refresh();} void refresh(){rows=p.inventory().snapshot();fireTableDataChanged();}
        @Override public int getRowCount(){return rows.size();}@Override public int getColumnCount(){return cols.length;}@Override public String getColumnName(int c){return cols[c];}
        @Override public Object getValueAt(int r,int c){var e=rows.get(r);return switch(c){case 0->e.endpoint().method()+" "+e.endpoint().routeTemplate();case 1->e.observationCount();case 2->e.observedIds();case 3->e.principals();case 4->e.tenants();case 5->e.authenticationTypes();default->"NOT ASSESSED";};}
    }
    private static String blankUnknown(String s){return s==null||s.isBlank()?"UNKNOWN":s;}
}
