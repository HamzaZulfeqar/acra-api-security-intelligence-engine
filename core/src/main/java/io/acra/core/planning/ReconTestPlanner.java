package io.acra.core.planning;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.engine.SecurityContextSnapshot;
import io.acra.core.recon.*;
import java.util.*;
public final class ReconTestPlanner {
    public DryRunPlan plan(String target,SecurityContextSnapshot s,ReconnaissanceSnapshot recon,Collection<String> contexts,Collection<String> resources){List<PlannedTest> tests=new ArrayList<>();boolean hasRes=s.resource().resolved().isPresent();boolean hasTenant=s.tenant().resolved().isPresent();boolean hasOwner=s.resource().resolved().map(r->r.ownerPrincipalId()!=null).orElse(false);int ctx=Math.max(1,contexts==null?0:contexts.size());int res=Math.max(1,resources==null?0:resources.size());
        if(hasRes&&hasOwner&&ctx>1)tests.add(new PlannedTest(TestFamily.BOLA,"resource ownership and multiple contexts observed",ctx*res,List.of("principal","resource","owner")));
        if(hasTenant&&ctx>1)tests.add(new PlannedTest(TestFamily.TENANT_ISOLATION,"tenant context and multiple identities observed",ctx*res,List.of("principal","tenant","resource")));
        if(Set.of(ActionType.UPDATE,ActionType.DELETE,ActionType.APPROVE,ActionType.SHARE,ActionType.EXPORT,ActionType.EXECUTE).contains(s.action().actionType()))tests.add(new PlannedTest(TestFamily.BFLA,"sensitive or state-changing action",ctx,List.of("role","action")));
        if(s.endpoint().version()!=null&&!s.endpoint().version().isBlank())tests.add(new PlannedTest(TestFamily.VERSION_DRIFT,"versioned API endpoint",Math.max(2,ctx),List.of("version","endpoint")));
        if(s.uri().normalizationMetadata().size()>0)tests.add(new PlannedTest(TestFamily.ROUTING,"URI normalization behavior observed",2,List.of("rawUri","canonicalUri","route")));
        int estimate=tests.stream().mapToInt(PlannedTest::estimatedRequests).sum();return new DryRunPlan(target,s.endpoint().method()+" "+s.endpoint().routeTemplate(),contexts==null?List.of():List.copyOf(contexts),resources==null?List.of():List.copyOf(resources),tests,estimate,0,recon==null?EndpointPriority.UNKNOWN:recon.priority().priority(),List.of("DRY_RUN_ONLY","NO_NETWORK_REQUESTS","ACTIVE_EXECUTION_REMAINS_DISABLED"));}
}
