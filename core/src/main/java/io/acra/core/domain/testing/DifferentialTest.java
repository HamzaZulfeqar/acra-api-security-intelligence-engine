package io.acra.core.domain.testing;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;
public record DifferentialTest(String testId,String baselineRef,String controlRef,String mutation,String contextFingerprint,
                               AuthorizationDecision expected,AuthorizationDecision observed,List<String> differences,
                               List<String> evidenceIds,double confidence) {
    public DifferentialTest {if(testId==null||testId.isBlank())throw new IllegalArgumentException("testId required");baselineRef=s(baselineRef);controlRef=s(controlRef);mutation=s(mutation);contextFingerprint=s(contextFingerprint);if(expected==null)expected=AuthorizationDecision.UNKNOWN;if(observed==null)observed=AuthorizationDecision.UNKNOWN;differences=List.copyOf(differences==null?List.of():differences);evidenceIds=List.copyOf(evidenceIds==null?List.of():evidenceIds);if(confidence<0||confidence>1)throw new IllegalArgumentException("confidence");}private static String s(String v){return v==null?"":v;}
}
