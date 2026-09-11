package io.acra.core.active.research;

import java.util.List;
import java.util.OptionalDouble;

public record ResearchMetrics(
        int truePositive,
        int trueNegative,
        int falsePositive,
        int falseNegative,
        OptionalDouble precision,
        OptionalDouble recall,
        OptionalDouble f1) {
    public ResearchMetrics {
        if (truePositive < 0 || trueNegative < 0 || falsePositive < 0 || falseNegative < 0
                || precision == null || recall == null || f1 == null) {
            throw new IllegalArgumentException("invalid research metrics");
        }
    }

    public static ResearchMetrics from(List<ResearchExecutionRecord> records) {
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        for (ResearchExecutionRecord record : records == null ? List.<ResearchExecutionRecord>of() : records) {
            if (record.groundTruth() == ResearchGroundTruth.POSITIVE) {
                if (record.prediction() == ResearchPrediction.POSITIVE) tp++; else fn++;
            } else if (record.prediction() == ResearchPrediction.POSITIVE) fp++; else tn++;
        }
        OptionalDouble precision = ratio(tp, tp + fp);
        OptionalDouble recall = ratio(tp, tp + fn);
        OptionalDouble f1 = precision.isPresent() && recall.isPresent() && precision.getAsDouble() + recall.getAsDouble() > 0.0
                ? OptionalDouble.of(2.0 * precision.getAsDouble() * recall.getAsDouble()
                        / (precision.getAsDouble() + recall.getAsDouble())) : OptionalDouble.empty();
        return new ResearchMetrics(tp, tn, fp, fn, precision, recall, f1);
    }

    private static OptionalDouble ratio(int numerator, int denominator) {
        return denominator == 0 ? OptionalDouble.empty() : OptionalDouble.of(numerator / (double) denominator);
    }
}
