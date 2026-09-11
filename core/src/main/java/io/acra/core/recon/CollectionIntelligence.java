package io.acra.core.recon;
import java.util.Set;
public record CollectionIntelligence(boolean collection,Set<String> memberResourceIds,String reason) {public CollectionIntelligence {memberResourceIds=Set.copyOf(memberResourceIds==null?Set.of():memberResourceIds);reason=reason==null?"":reason;}}
