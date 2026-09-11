package io.acra.core.recon;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.engine.SecurityContextSnapshot;
import java.util.*;
public final class EndpointRiskPrioritizer {
    public EndpointRiskAssessment assess(SecurityContextSnapshot s,ReconnaissanceSnapshot partial){int score=0;List<String> reasons=new ArrayList<>();
        if(!s.uri().identifierCandidates().isEmpty()){score+=2;reasons.add("dynamic-identifiers");}
        if(s.tenant().resolved().isPresent()){score+=2;reasons.add("tenant-context");}
        if(s.resource().resolved().isPresent()){score+=2;reasons.add("resource-context");}
        if(s.resource().resolved().map(r->r.ownerPrincipalId()!=null).orElse(false)){score+=2;reasons.add("ownership-evidence");}
        if(Set.of(ActionType.CREATE,ActionType.UPDATE,ActionType.DELETE,ActionType.APPROVE,ActionType.SHARE,ActionType.EXPORT,ActionType.EXECUTE).contains(s.action().actionType())){score+=2;reasons.add("state-changing-or-sensitive-action");}
        String p=s.endpoint().routeTemplate().toLowerCase(Locale.ROOT); if(p.contains("admin")||p.contains("export")||p.contains("transfer")||p.contains("refund")){score+=2;reasons.add("sensitive-route-semantics");}
        if(partial!=null&&partial.versions().size()>1){score+=1;reasons.add("multiple-version-signals");}
        EndpointPriority pr=score>=7?EndpointPriority.HIGH:score>=3?EndpointPriority.MEDIUM:EndpointPriority.LOW; return new EndpointRiskAssessment(pr,score,reasons);}
}
