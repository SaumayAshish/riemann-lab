package com.riemannlab.validation;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates a batch of {@link PrecisionSample} measurements into whether
 * an evaluator's error bound can be trusted: the worst ratio of actual
 * error to claimed bound, and how many samples broke the bound outright.
 *
 * @param samples the batch of precision measurements this report summarizes
 */
public record PrecisionReport(List<PrecisionSample> samples) {

    /** Validates that {@code samples} is non-null and non-empty, and defensively copies it. */
    public PrecisionReport {
        Objects.requireNonNull(samples, "samples must not be null");
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        samples = List.copyOf(samples);
    }

    /** How many samples this report covers. */
    public int count() {
        return samples.size();
    }

    /**
     * The largest ratio of actual error to claimed bound across every sample.
     *
     * @return the worst error ratio in this batch
     */
    public double worstErrorRatio() {
        return samples.stream()
                .mapToDouble(PrecisionSample::errorRatio)
                .max()
                .orElseThrow();
    }

    /**
     * How many samples exceeded the error bound the evaluator claimed for itself.
     *
     * @return the count of samples whose actual error exceeded their claimed bound
     */
    public long countExceedingClaimedBound() {
        return samples.stream().filter(sample -> !sample.isWithinClaimedBound()).count();
    }

    /**
     * Whether every sample's actual error stayed within its claimed bound.
     *
     * @return {@code true} if no sample exceeded its claimed bound
     */
    public boolean allWithinClaimedBound() {
        return countExceedingClaimedBound() == 0;
    }
}