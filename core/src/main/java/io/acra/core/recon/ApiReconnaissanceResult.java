package io.acra.core.recon;
import io.acra.core.analysis.semantic.ResponseSemanticFingerprint;
import io.acra.core.openapi.SchemaDriftObservation;
import io.acra.core.planning.DryRunPlan;
import java.util.List;
public record ApiReconnaissanceResult(ReconnaissanceSnapshot reconnaissance,ResponseSemanticFingerprint responseFingerprint,
                                      CollectionIntelligence collection,PaginationIntelligence pagination,
                                      SecurityContextFingerprint securityContextFingerprint,List<SchemaDriftObservation> schemaDrift,
                                      DryRunPlan dryRunPlan) {
    public ApiReconnaissanceResult {schemaDrift=List.copyOf(schemaDrift==null?List.of():schemaDrift);}
}
