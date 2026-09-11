package io.acra.core.analysis.semantic;
import java.util.List;
public record ResourceSemanticMatch(boolean sameResource,double confidence,List<String> reasons) {public ResourceSemanticMatch {if(confidence<0||confidence>1)throw new IllegalArgumentException("confidence");reasons=List.copyOf(reasons==null?List.of():reasons);}}
