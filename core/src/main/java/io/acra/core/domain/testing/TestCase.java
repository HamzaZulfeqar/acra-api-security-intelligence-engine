package io.acra.core.domain.testing;

import io.acra.core.domain.common.Validation;
import java.util.*;

public record TestCase(String testId, String analyzerId, List<String> preconditions, Map<String,String> inputVector,
                       Map<String,String> mutation, String expectedOutcome, List<String> evidenceRequirements,
                       List<String> safetyConstraints) {
    public TestCase {
        testId=Validation.requireNonBlank(testId,"testId"); analyzerId=Validation.requireNonBlank(analyzerId,"analyzerId");
        preconditions=List.copyOf(preconditions==null?List.of():preconditions);
        inputVector=freeze(inputVector); mutation=freeze(mutation);
        expectedOutcome=expectedOutcome==null?"UNKNOWN":expectedOutcome;
        evidenceRequirements=List.copyOf(evidenceRequirements==null?List.of():evidenceRequirements);
        safetyConstraints=List.copyOf(safetyConstraints==null?List.of():safetyConstraints);
    }
    private static Map<String,String> freeze(Map<String,String> m){ TreeMap<String,String> t=new TreeMap<>(); if(m!=null)t.putAll(m); return Collections.unmodifiableMap(t); }
}
