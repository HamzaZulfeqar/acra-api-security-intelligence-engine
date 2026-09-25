package io.acra.burp;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import io.acra.burp.scope.*;
import io.acra.burp.runtime.BurpRuntimeProbe;
import io.acra.burp.traffic.*;
import io.acra.burp.ui.AcraSuiteTab;
import java.util.ArrayList;
import java.util.List;
public final class ACRAExtension implements BurpExtension {
    private final List<Registration> registrations=new ArrayList<>(); private AcraSuiteTab tab; private BurpRuntimeProbe runtimeProbe;
    @Override public void initialize(MontoyaApi api){
        runtimeProbe=BurpRuntimeProbe.fromSystemProperty();
        try{
            api.extension().setName("ACRA"); ScopeController scope=new ScopeController(); if(runtimeProbe.enabled()){var d=scope.configuration();scope.update(new ScopeConfiguration(ScopeMode.ALL_TRAFFIC,List.of(),d.maxBodyBytes(),d.maxTransactions(),d.maxConcurrentActiveRequests(),d.requestsPerSecond(),d.timeoutMillis(),false));} TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(scope.configuration().maxTransactions());
            TrafficCollector collector=new TrafficCollector(scope,result->{try{var processed=pipeline.process(result); runtimeProbe.processed(processed);}catch(RuntimeException ex){runtimeProbe.error("pipeline",ex); api.logging().logToError("ACRA passive processing rejected transaction: "+ex.getMessage());}});
            registrations.add(api.http().registerHttpHandler(new AcraHttpHandler(collector,runtimeProbe))); tab=new AcraSuiteTab(pipeline,scope); registrations.add(api.userInterface().registerSuiteTab("ACRA",tab.component()));
            registrations.add(api.extension().registerUnloadingHandler(this::shutdown));
            runtimeProbe.initialized();
            api.logging().logToOutput("ACRA v0.2.0-rc1 initialized in passive observation mode. Active vulnerability scanning is disabled.");
        }catch(RuntimeException ex){ runtimeProbe.error("initialize",ex); api.logging().logToError("ACRA initialization failed: "+ex.getMessage()); throw ex; }
    }
    private void shutdown(){ if(runtimeProbe!=null) runtimeProbe.unloading(); if(tab!=null) tab.stop(); for(Registration registration:registrations){try{registration.deregister();}catch(RuntimeException ignored){}} registrations.clear(); }
}
