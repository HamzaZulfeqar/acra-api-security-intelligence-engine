package io.acra.core.analysis.semantic;
import java.util.*;
public final class ResourceSemanticMatcher {
    public ResourceSemanticMatch compare(ResponseSemanticFingerprint a,ResponseSemanticFingerprint b){List<String> reasons=new ArrayList<>();double score=0;int weights=0;if(!a.resourceIds().isEmpty()&&!b.resourceIds().isEmpty()){weights+=4;if(!Collections.disjoint(a.resourceIds(),b.resourceIds())){score+=4;reasons.add("shared-resource-id");}}if(!a.tenantIds().isEmpty()&&!b.tenantIds().isEmpty()){weights+=2;if(!Collections.disjoint(a.tenantIds(),b.tenantIds())){score+=2;reasons.add("shared-tenant-id");}}if(!a.ownerIds().isEmpty()&&!b.ownerIds().isEmpty()){weights+=2;if(!Collections.disjoint(a.ownerIds(),b.ownerIds())){score+=2;reasons.add("shared-owner-id");}}weights+=2;if(a.fields().equals(b.fields())){score+=2;reasons.add("same-field-set");}double c=weights==0?0:score/weights;return new ResourceSemanticMatch(c>=0.6,c,reasons);}
}
