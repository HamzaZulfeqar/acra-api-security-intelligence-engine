package io.acra.burp;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import io.acra.burp.scope.*;
import io.acra.burp.traffic.*;
import io.acra.burp.ui.AcraSuiteTab;
import java.util.ArrayList;
import java.util.List;
public final class ACRAExtension implements BurpExtension {
    private final List<Registration> registrations=new ArrayList<>(); private AcraSuiteTab tab;
    @Override public void initialize(MontoyaApi api){
        try{
            api.extension().setName("ACRA"); ScopeController scope=new ScopeController(); TrafficIntelligencePipeline pipeline=new TrafficIntelligencePipeline(scope.configuration().maxTransactions());
            TrafficCollector collector=new TrafficCollector(scope,result->{try{pipeline.process(result);}catch(RuntimeException ex){api.logging().logToError("ACRA passive processing rejected transaction: "+ex.getMessage());}});
            registrations.add(api.http().registerHttpHandler(new AcraHttpHandler(collector))); tab=new AcraSuiteTab(pipeline,scope); registrations.add(api.userInterface().registerSuiteTab("ACRA",tab.component()));
            registrations.add(api.extension().registerUnloadingHandler(this::shutdown));
            api.logging().logToOutput("ACRA v0.2.0-rc1 initialized in passive observation mode. Active vulnerability scanning is disabled.");
        }catch(RuntimeException ex){ api.logging().logToError("ACRA initialization failed: "+ex.getMessage()); throw ex; }
    }
    private void shutdown(){ if(tab!=null) tab.stop(); for(Registration registration:registrations){try{registration.deregister();}catch(RuntimeException ignored){}} registrations.clear(); }
}
