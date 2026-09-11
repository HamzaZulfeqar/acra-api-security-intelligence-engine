package io.acra.core.analysis;
import java.util.List;
public record ResponseDifference(ResponseComparisonMode mode, boolean equivalent, List<String> changedSignals) {
    public ResponseDifference { if(mode==null) throw new IllegalArgumentException("mode required"); changedSignals=List.copyOf(changedSignals==null?List.of():changedSignals); }
}
